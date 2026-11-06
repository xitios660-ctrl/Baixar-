import http from 'node:http';import https from 'node:https';import {readFile} from 'node:fs/promises';import {fileURLToPath} from 'node:url';
const api=new URL(process.env.API_URL||'https://guhflix-api.onrender.com');if(api.protocol!=='https:'&&!process.env.LOCAL_TEST)throw Error('HTTPS API required');
const files={'/':'index.html','/app.js':'app.js','/style.css':'style.css','/icon.png':'icon.png'};
http.createServer(async(req,res)=>{
 res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','no-referrer');res.setHeader('Cache-Control','no-store');res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
 const path=new URL(req.url,'http://localhost').pathname;
 if(path==='/health'){res.writeHead(200,{'Content-Type':'application/json'});return res.end('{"ok":true,"service":"guhflix-admin"}')}
 if(path.startsWith('/api/')&&['GET','POST','PATCH','OPTIONS'].includes(req.method)){
  const headers={};for(const name of ['authorization','cookie','content-type','idempotency-key','origin'])if(req.headers[name])headers[name]=req.headers[name];
  const target=new URL(req.url,api);const proxy=(api.protocol==='https:'?https:http).request(target,{method:req.method,headers,timeout:15000},up=>{for(const name of ['content-type','set-cookie','retry-after'])if(up.headers[name])res.setHeader(name,up.headers[name]);res.writeHead(up.statusCode);up.pipe(res)});
  proxy.on('timeout',()=>proxy.destroy());proxy.on('error',()=>{if(!res.headersSent)res.writeHead(503,{'Content-Type':'application/json'});res.end('{"error":"UNAVAILABLE","message":"API temporariamente indisponível."}')});return req.pipe(proxy);
 }
 if(req.method!=='GET'||!files[path]){res.writeHead(404);return res.end('Não encontrado')}
 try{const data=await readFile(fileURLToPath(new URL('./'+files[path],import.meta.url)));res.setHeader('Content-Type',path.endsWith('.js')?'text/javascript; charset=utf-8':path.endsWith('.css')?'text/css; charset=utf-8':path.endsWith('.png')?'image/png':'text/html; charset=utf-8');res.end(data)}catch{res.writeHead(404);res.end('Não encontrado')}
}).listen(Number(process.env.PORT||10000),'0.0.0.0');
