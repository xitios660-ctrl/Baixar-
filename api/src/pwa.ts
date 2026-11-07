import type {FastifyInstance} from 'fastify';
import {readFile} from 'node:fs/promises';
const types:Record<string,string>={'index.html':'text/html; charset=utf-8','app.js':'application/javascript; charset=utf-8','style.css':'text/css; charset=utf-8','sw.js':'application/javascript; charset=utf-8','manifest.webmanifest':'application/manifest+json','icon-192.png':'image/png','icon-512.png':'image/png','apple-touch-icon.png':'image/png'};
export async function registerPwa(app:FastifyInstance){
 app.get('/pwa',async(_req,reply)=>reply.redirect('/pwa/'));
 app.get('/pwa/*',async(req,reply)=>{const path=new URL(req.url,'https://guhflix.invalid').pathname;const file=path==='/pwa/'?'index.html':path.slice('/pwa/'.length);if(!types[file])return reply.code(404).send({error:'NOT_FOUND'});
 reply.header('Content-Type',types[file]).header('Cache-Control','no-cache').header('X-Content-Type-Options','nosniff');
 if(file==='sw.js')reply.header('Service-Worker-Allowed','/pwa/').header('Content-Security-Policy',"default-src 'self'; script-src 'self'; connect-src 'self'");
 if(file==='index.html')reply.header('Content-Security-Policy',"default-src 'self'; script-src 'self'; style-src 'self'; style-src-attr 'unsafe-inline'; img-src 'self' data: https:; connect-src 'self'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'");
 try{return await readFile(new URL('../../pwa/'+file,import.meta.url))}catch{return reply.code(503).send('O aplicativo web está sendo atualizado. Tente novamente.')} });
}
