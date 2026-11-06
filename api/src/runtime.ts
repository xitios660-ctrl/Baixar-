import {createServer,type IncomingMessage,type ServerResponse} from 'node:http';
import type {AddressInfo} from 'node:net';
export type Phase='configuration_required'|'connecting'|'database_unavailable';
export function createRuntime(){
 let route:((req:IncomingMessage,res:ServerResponse)=>void)|undefined,phase:Phase='configuration_required';
 const server=createServer((req,res)=>{
  if(route)return route(req,res);
  res.writeHead(503,{'Content-Type':'application/json; charset=utf-8','Cache-Control':'no-store','X-Content-Type-Options':'nosniff','Retry-After':'15'});
  res.end(JSON.stringify({ok:false,service:'guhflix-api',state:phase,message:'Serviço em configuração. O acesso ainda não está liberado.'}));
 });
 return {server,setPhase:(value:Phase)=>{phase=value},activate:(handler:typeof route)=>{route=handler},listen:async(port:number)=>{await new Promise<void>((resolve,reject)=>{server.once('error',reject);server.listen(port,'0.0.0.0',()=>{server.removeListener('error',reject);resolve()})});return (server.address() as AddressInfo).port},close:async()=>{await new Promise<void>(resolve=>server.close(()=>resolve()))}};
}
