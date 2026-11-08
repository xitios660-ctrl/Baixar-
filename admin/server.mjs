import http from 'node:http';import https from 'node:https';import {readFile} from 'node:fs/promises';import {fileURLToPath} from 'node:url';
const api=new URL(process.env.API_URL||'https://guhflix-marley-api.onrender.com');if(api.protocol!=='https:'&&process.env.LOCAL_TEST!=='1')throw Error('HTTPS API required');
const files={'/':'index.html','/app.js':'app.js','/style.css':'style.css','/icon.png':'icon.png'};
const unavailable={error:'SERVICE_UNAVAILABLE',message:'Servidor temporariamente indisponível. Aguarde alguns segundos e tente novamente.'};
function json(res,status,data){if(res.destroyed||res.writableEnded)return;res.writeHead(status,{'Content-Type':'application/json; charset=utf-8'});res.end(JSON.stringify(data))}
async function proxyApi(req,res,path){
 let body;try{const chunks=[];let size=0;for await(const chunk of req){size+=chunk.length;if(size>16384){json(res,413,{error:'BODY_TOO_LARGE',message:'Solicitação muito grande.'});return}chunks.push(chunk)}body=Buffer.concat(chunks);}catch{return}
 const headers={Accept:'application/json'};for(const name of ['authorization','cookie','content-type','idempotency-key','origin'])if(req.headers[name])headers[name]=req.headers[name];
 if(req.method==='POST'||req.method==='PATCH')headers['content-length']=String(body.length);
 const target=new URL(path==='/api/health'?'/health':req.url,api);let proxy;
 const fail=(code)=>{console.error(JSON.stringify({service:'guhflix-admin',event:'api_unavailable',path,code}));json(res,503,unavailable)};
 try{proxy=(api.protocol==='https:'?https:http).request(target,{method:req.method,headers,timeout:105000},up=>{
  const chunks=[];let size=0,failed=false;
  up.on('data',chunk=>{size+=chunk.length;if(size>1048576){failed=true;up.destroy();fail('RESPONSE_TOO_LARGE');return}chunks.push(chunk)});
  up.on('error',()=>{if(!failed)fail('RESPONSE_INTERRUPTED')});
  up.on('end',()=>{if(failed||res.destroyed||res.writableEnded)return;const text=Buffer.concat(chunks).toString('utf8');let data;try{data=JSON.parse(text)}catch{fail('NON_JSON_RESPONSE_'+up.statusCode);return}if(!data||typeof data!=='object'){fail('INVALID_RESPONSE');return}if(up.headers['set-cookie'])res.setHeader('Set-Cookie',up.headers['set-cookie']);if(up.headers['retry-after'])res.setHeader('Retry-After',up.headers['retry-after']);json(res,up.statusCode||503,data);});
 });proxy.on('timeout',()=>proxy.destroy(Object.assign(new Error('Upstream timeout'),{code:'ETIMEDOUT'})));proxy.on('error',error=>fail(error.code||'CONNECTION_FAILED'));res.on('close',()=>{if(!res.writableEnded)proxy.destroy()});proxy.end(body);
 }catch{if(proxy)proxy.destroy();fail('REQUEST_FAILED')}
}
http.createServer(async(req,res)=>{
 res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','no-referrer');res.setHeader('Cache-Control','no-store, no-cache, must-revalidate');res.setHeader('Pragma','no-cache');res.setHeader('Expires','0');res.setHeader('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
 if(!req.url?.startsWith('/')||req.url.startsWith('//'))return json(res,400,{error:'INVALID_URL',message:'Endereço inválido.'});
 let path;try{path=new URL(req.url,'http://localhost').pathname}catch{return json(res,400,{error:'INVALID_URL',message:'Endereço inválido.'})}
 if(path==='/health')return json(res,200,{ok:true,service:'guhflix-admin',version:'2.1.1'});
 if(path.startsWith('/api/')&&['GET','POST','PATCH','OPTIONS'].includes(req.method))return proxyApi(req,res,path);
 if(req.method!=='GET'||!files[path]){res.writeHead(404);return res.end('Não encontrado')}
 try{const data=await readFile(fileURLToPath(new URL('./'+files[path],import.meta.url)));res.setHeader('Content-Type',path.endsWith('.js')?'text/javascript; charset=utf-8':path.endsWith('.css')?'text/css; charset=utf-8':path.endsWith('.png')?'image/png':'text/html; charset=utf-8');res.end(data)}catch{res.writeHead(404);res.end('Não encontrado')}
}).listen(Number(process.env.PORT||10000),'0.0.0.0');
