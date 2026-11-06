import {test} from 'node:test';import assert from 'node:assert/strict';import http from 'node:http';import {spawn} from 'node:child_process';import {once} from 'node:events';
const wait=ms=>new Promise(resolve=>setTimeout(resolve,ms));
test('admin proxy returns JSON on provider HTML/errors and preserves valid auth responses',async()=>{
 let mode='html',received;const upstream=http.createServer(async(req,res)=>{const chunks=[];for await(const c of req)chunks.push(c);received={method:req.method,url:req.url,headers:req.headers,body:Buffer.concat(chunks).toString()};if(mode==='html'){res.writeHead(502,{'Content-Type':'text/html'});return res.end('<html>Starting service</html>')}res.writeHead(mode==='valid'?401:200,{'Content-Type':'application/json',...(mode==='cookie'?{'Set-Cookie':'guhflix_refresh=fixture; HttpOnly; Path=/api/auth; SameSite=Strict'}:{})});res.end(JSON.stringify(mode==='valid'?{error:'INVALID_CREDENTIALS',message:'Usuário ou senha incorretos'}:{ok:true}));});
 await new Promise(resolve=>upstream.listen(0,'127.0.0.1',resolve));const upstreamPort=upstream.address().port;
 const reserve=http.createServer();await new Promise(resolve=>reserve.listen(0,'127.0.0.1',resolve));const port=reserve.address().port;await new Promise(resolve=>reserve.close(resolve));
 const child=spawn(process.execPath,['admin/server.mjs'],{cwd:new URL('../../',import.meta.url),env:{...process.env,PORT:String(port),API_URL:`http://127.0.0.1:${upstreamPort}`,LOCAL_TEST:'1'},stdio:'pipe'});let logs='';child.stderr.on('data',c=>logs+=c.toString());
 try{for(let i=0;i<80;i++){try{if((await fetch(`http://127.0.0.1:${port}/health`)).ok)break}catch{}await wait(25)}
 const request=()=>fetch(`http://127.0.0.1:${port}/api/auth/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:'á-test',password:'fixture-only'})});
 let r=await request();assert.equal(r.status,503);assert.equal((await r.json()).error,'SERVICE_UNAVAILABLE');assert(!logs.includes('fixture-only'));
 mode='valid';r=await request();assert.equal(r.status,401);assert.equal((await r.json()).message,'Usuário ou senha incorretos');assert.equal(received.headers['content-length'],String(Buffer.byteLength(received.body)));assert.equal(JSON.parse(received.body).username,'á-test');
 mode='cookie';r=await request();assert(r.headers.get('set-cookie').includes('HttpOnly'));assert.equal((await r.json()).ok,true);
 r=await fetch(`http://127.0.0.1:${port}/api/health`);assert.equal(r.status,200);assert.equal(received.url,'/health');assert.equal((await r.json()).ok,true);
 }finally{child.kill('SIGTERM');await once(child,'exit');await new Promise(resolve=>upstream.close(resolve));}
});
