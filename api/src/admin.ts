import type {FastifyInstance} from 'fastify';
import {readFile} from 'node:fs/promises';

/** Same-origin console. All data operations still use the authenticated admin API. */
export async function registerAdmin(app:FastifyInstance){
 const files:Record<string,{file:string;type:string}>={
  '/admin/':{file:'index.html',type:'text/html; charset=utf-8'},
  '/admin/app.js':{file:'app.js',type:'application/javascript; charset=utf-8'},
  '/admin/style.css':{file:'style.css',type:'text/css; charset=utf-8'},
  '/admin/icon.png':{file:'icon.png',type:'image/png'},
 };
 app.get('/admin',async(_req,reply)=>reply.redirect('/admin/'));
 for(const [path,asset] of Object.entries(files))app.get(path,async(_req,reply)=>{
  reply.header('Cache-Control','no-store').type(asset.type).header('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
  try{return await readFile(new URL('../../admin/'+asset.file,import.meta.url))}
  catch{return reply.code(503).type('text/plain').send('Painel temporariamente indisponível.')}
 });
}
