import type {FastifyInstance} from 'fastify';import {z} from 'zod';import {Fault} from './errors.js';
/** Packaged TV runtime bridge: no cookies, admin routes, arbitrary URLs or upstream credentials. */
export async function registerSamsung(app:FastifyInstance){
 const allowed=(method:string,path:string)=>method==='GET'&&/^(?:connect\/(?:home|catalog(?:\?[a-zA-Z0-9%=&+_.-]*)?|catalog\/[a-f0-9]{32}(?:\/related)?)|auth\/me|app\/access)$/.test(path)||method==='POST'&&/^(?:auth\/(?:login|refresh|logout)|connect\/(?:watch|watch\/[A-Za-z0-9_-]{54}\/(?:heartbeat|stop)|pairing|tv\/poll|tv\/commands\/[a-f0-9]{24}\/ack))$/.test(path);
 app.route({method:['GET','POST','OPTIONS'],url:'/api/samsung/*',handler:async(req,reply)=>{
  const origin=req.headers.origin;if(origin&&!['null','file://'].includes(origin))throw new Fault(403,'ORIGIN','Origem não autorizada');if(req.headers.cookie)throw new Fault(403,'COOKIE_FORBIDDEN','Use a sessão própria da TV');
  reply.header('Access-Control-Allow-Origin',origin??'null').header('Access-Control-Allow-Headers','Content-Type, Authorization').header('Access-Control-Allow-Methods','GET, POST, OPTIONS').header('Vary','Origin').removeHeader('Access-Control-Allow-Credentials');
  if(req.method==='OPTIONS')return reply.code(204).send();
  const path=req.url.slice('/api/samsung/'.length);if(!allowed(req.method,path))throw new Fault(404,'NOT_FOUND','Recurso indisponível');let body=req.body;
  if(path==='auth/login')body={...z.object({username:z.string(),password:z.string(),installationId:z.string().uuid()}).parse(body),label:'GUHFLIX Samsung TV',client:'tv'};
  else if(path==='auth/refresh')body=z.object({refreshToken:z.string().min(20).max(300)}).parse(body);
  else {if(!req.headers.authorization?.startsWith('Bearer '))throw new Fault(401,'UNAUTHORIZED','Entre na sua conta');const me=await app.inject({method:'GET',url:'/api/auth/me',headers:{authorization:req.headers.authorization}});if(me.statusCode!==200)return reply.code(me.statusCode).send(me.json());if(me.json().role!=='customer')throw new Fault(403,'CUSTOMER_REQUIRED','Use um acesso de cliente');}
  const result=await app.inject({method:req.method as 'GET'|'POST',url:'/api/'+path,headers:{...(req.headers.authorization?{authorization:req.headers.authorization}:{}),'content-type':'application/json'},...(body?{payload:body}:{})});return reply.code(result.statusCode).send(result.json());
 }});
}
