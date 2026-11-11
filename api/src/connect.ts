import type {LiveSource} from './live.js';
import type {FastifyInstance,FastifyRequest} from 'fastify';
import {ObjectId,type Db} from 'mongodb';
import {randomBytes,createHash} from 'node:crypto';
import {z} from 'zod';
import {Fault} from './errors.js';
import type {Catalog} from './catalog.js';
import {Metadata} from './metadata.js';

type Config={db:Db;now:()=>Date;identity:(req:FastifyRequest)=>Promise<{s:any;u:any}>;requireAccess:(u:any)=>void;catalog:Catalog;live:LiveSource};
const sha=(v:string)=>createHash('sha256').update(v).digest('hex');
const id=(v:unknown)=>{if(typeof v!=='string'||!ObjectId.isValid(v))throw new Fault(400,'INVALID_ID','Identificador inválido');return new ObjectId(v)};
const command=z.discriminatedUnion('type',[
 z.object({type:z.literal('play'),live:z.boolean().optional(),contentId:z.string().regex(/^[a-f0-9]{32}$/),season:z.number().int().min(0).max(1000).optional(),episode:z.number().int().min(1).max(100000).optional()}),
 z.object({type:z.enum(['pause','resume','stop'])}),
 z.object({type:z.literal('seek'),positionMs:z.number().int().min(0).max(172800000)}),
 z.object({type:z.literal('seekRelative'),deltaMs:z.number().int().min(-300000).max(300000)}),
]);
const state=z.object({live:z.boolean().optional(),mode:z.enum(['idle','loading','playing','paused','error']),contentId:z.string().regex(/^[a-f0-9]{32}$/).nullable().optional(),title:z.string().max(240).default(''),positionMs:z.number().int().min(0).max(172800000).default(0),durationMs:z.number().int().min(0).max(172800000).default(0),season:z.number().int().min(0).max(1000).optional(),episode:z.number().int().min(1).max(100000).optional(),error:z.string().max(300).optional()}).strict();

export async function connectIndexes(db:Db){await Promise.all([
 db.collection('connect_pairings').createIndex({codeHash:1},{unique:true}),
 db.collection('connect_pairings').createIndex({expiresAt:1},{expireAfterSeconds:0}),
 db.collection('connect_links').createIndex({tvSessionId:1,phoneSessionId:1},{unique:true}),
 db.collection('connect_links').createIndex({phoneSessionId:1,revoked:1}),
 db.collection('connect_commands').createIndex({userId:1,phoneSessionId:1,requestId:1},{unique:true}),
 db.collection('connect_commands').createIndex({tvSessionId:1,state:1,createdAt:1}),
 db.collection('connect_commands').createIndex({expiresAt:1},{expireAfterSeconds:0}),
 db.collection('connect_state').createIndex({tvSessionId:1},{unique:true}),
 db.collection('connect_state').createIndex({expiresAt:1},{expireAfterSeconds:0}),
]);}

export async function registerConnect(app:FastifyInstance,c:Config){
 const {db,now}=c;const metadata=new Metadata();
 const owner=async(req:FastifyRequest,active=true)=>{const value=await c.identity(req);if(value.u.role!=='customer')throw new Fault(403,'CUSTOMER_REQUIRED','Use sua conta de cliente');if(active)c.requireAccess(value.u);return value};
 const tv=async(req:FastifyRequest)=>{const value=await owner(req);if(value.s.client==='pwa'&&!value.s.browserReceiver)throw new Fault(403,'TV_REQUIRED','Esta operação é exclusiva da TV');return value};
 async function linkFor(req:FastifyRequest,linkId:unknown,active=true){const {s,u}=await owner(req,active);const link=await db.collection('connect_links').findOne({_id:id(linkId),userId:u._id.toString(),phoneSessionId:s._id.toString(),revoked:false});if(!link)throw new Fault(404,'DEVICE_NOT_FOUND','TV não vinculada a este iPhone');return {s,u,link}}
 app.post('/api/connect/browser-tv/enable',async req=>{const {s,u}=await owner(req);if(s.client!=='pwa')throw new Fault(400,'BROWSER_REQUIRED','Use o catálogo da TV no navegador');const r=await db.collection('sessions').updateOne({_id:s._id,userId:u._id.toString(),revoked:false,expiresAt:{$gt:now()},version:u.sessionVersion},{$set:{browserReceiver:true}});if(!r.matchedCount)throw new Fault(401,'REVOKED','Sessão encerrada');return {success:true}});
 app.get('/api/connect/health',{config:{rateLimit:{max:12,timeWindow:'1 minute'}}},async()=>({service:'guhflix-connect',version:'1.0.0',catalog:await c.catalog.health()}));
 app.get('/api/connect/metadata/*',async req=>{await owner(req);const path=(req.params as any)['*'];return metadata.request(path,new URL(req.url,'https://guhflix.invalid').searchParams)});
 app.get('/api/connect/catalog-files/*',async(req,reply)=>{await owner(req);if(!c.catalog.fileText)throw new Fault(503,'CATALOG_FORMAT','Catálogo não fornece arquivos nativos');const text=await c.catalog.fileText((req.params as any)['*']);return reply.header('Content-Type','text/plain; charset=utf-8').header('Cache-Control','private, no-store').send(text)});
 app.get('/api/connect/live',async req=>{await owner(req);const q=z.object({q:z.string().max(120).default(''),offset:z.coerce.number().int().min(0).max(100000).default(0)}).parse(req.query);return c.live.list(q.q,q.offset)});
 app.get('/api/connect/live/:id',async req=>{await owner(req);return c.live.detail(z.string().regex(/^[a-f0-9]{32}$/).parse((req.params as any).id))});
 app.get('/api/connect/home',async req=>{await owner(req);if(!c.catalog.home)throw new Fault(503,'HOME_UNAVAILABLE','Destaques temporariamente indisponíveis');return c.catalog.home()});
 app.get('/api/connect/catalog',async req=>{await owner(req);const q=z.object({q:z.string().max(120).default(''),kind:z.enum(['','movie','series']).default(''),offset:z.coerce.number().int().min(0).max(100000).default(0)}).parse(req.query);return c.catalog.list(q.q,q.kind,q.offset)});
 app.get('/api/connect/catalog/:id',async req=>{await owner(req);return c.catalog.detail((req.params as any).id)});
 app.get('/api/connect/catalog/:id/related',async req=>{await owner(req);if(!c.catalog.related)throw new Fault(503,'RELATED_UNAVAILABLE','Recomendações temporariamente indisponíveis');return c.catalog.related((req.params as any).id)});
 app.get('/api/connect/catalog-related/:kind/:tmdb',async req=>{await owner(req);const p=z.object({kind:z.enum(['movie','series']),tmdb:z.coerce.number().int().positive()}).parse(req.params);const item=await c.catalog.byTmdb?.(p.kind,p.tmdb);if(!item||!c.catalog.related)throw new Fault(404,'TITLE_NOT_FOUND','Título não encontrado no catálogo');return c.catalog.related(item.id)});
 app.get('/api/connect/live/:id/play',async req=>{await tv(req);return c.live.resolve(z.string().regex(/^[a-f0-9]{32}$/).parse((req.params as any).id))});
 app.get('/api/connect/catalog/:id/play',async req=>{await tv(req);const q=z.object({season:z.coerce.number().int().min(0).max(1000).optional(),episode:z.coerce.number().int().min(1).max(100000).optional()}).parse(req.query);return c.catalog.resolve((req.params as any).id,q.season,q.episode)});
 app.post('/api/connect/pairing',{config:{rateLimit:{max:10,timeWindow:'1 minute'}}},async req=>{
   const {s,u}=await tv(req);const code=randomBytes(5).toString('hex').toUpperCase(),expiresAt=new Date(now().getTime()+300000);
   await db.collection('connect_pairings').deleteMany({tvSessionId:s._id.toString()});
   await db.collection('connect_pairings').insertOne({userId:u._id.toString(),tvSessionId:s._id.toString(),label:s.label,codeHash:sha(code),createdAt:now(),expiresAt,claimed:false});
   return {code:code.slice(0,5)+'-'+code.slice(5),expiresAt,website:'https://guhflix-marley-api.onrender.com/pwa/'};
 });
 app.post('/api/connect/pairing/claim',{config:{rateLimit:{max:8,timeWindow:'1 minute'}}},async req=>{
   const {s,u}=await owner(req);if(s.client!=='pwa')throw new Fault(403,'PHONE_REQUIRED','Vincule usando o site do iPhone');
   const input=z.object({code:z.string().max(30),label:z.string().trim().min(1).max(60).optional()}).parse(req.body);const code=input.code.replace(/[\s-]/g,'').toUpperCase();if(!/^[A-F0-9]{10}$/.test(code))throw new Fault(400,'INVALID_CODE','Código de vínculo inválido');
   const pair=await db.collection('connect_pairings').findOneAndUpdate({codeHash:sha(code),userId:u._id.toString(),claimed:false,expiresAt:{$gt:now()}},{$set:{claimed:true,phoneSessionId:s._id.toString()}},{returnDocument:'after'});
   if(!pair)throw new Fault(404,'PAIRING_EXPIRED','Código incorreto, já utilizado ou expirado. Confira se a mesma conta GUHFLIX está conectada no site e na TV.');
   const live=await db.collection('sessions').findOne({_id:id(pair.tvSessionId),userId:u._id.toString(),revoked:false,expiresAt:{$gt:now()}});if(!live)throw new Fault(409,'TV_OFFLINE','A sessão da TV precisa ser renovada');
   const link=await db.collection('connect_links').findOneAndUpdate({tvSessionId:pair.tvSessionId,phoneSessionId:s._id.toString()},{$set:{userId:u._id.toString(),label:input.label??pair.label,revoked:false,updatedAt:now()},$setOnInsert:{createdAt:now()}},{upsert:true,returnDocument:'after'});
   return {id:link!._id.toString(),label:link!.label};
 });
 app.delete('/api/connect/tv/devices',async req=>{
   const {s,u}=await tv(req);const target={tvSessionId:s._id.toString(),userId:u._id.toString()};
   await db.collection('connect_links').updateMany(target,{$set:{revoked:true,updatedAt:now()}});
   await db.collection('connect_commands').updateMany({...target,state:{$in:['queued','delivered']}},{$set:{state:'revoked'}});
   return {success:true};
 });
 app.get('/api/connect/devices',async req=>{
   const {s,u}=await owner(req,false);const links=await db.collection('connect_links').find({phoneSessionId:s._id.toString(),userId:u._id.toString(),revoked:false}).limit(30).toArray();
   return {devices:await Promise.all(links.map(async link=>{const [current,session]=await Promise.all([db.collection('connect_state').findOne({tvSessionId:link.tvSessionId,expiresAt:{$gt:now()}}),db.collection('sessions').findOne({_id:id(link.tvSessionId),revoked:false,expiresAt:{$gt:now()},version:u.sessionVersion})]);return {id:link._id.toString(),label:link.label,online:!!session&&!!current&&now().getTime()-new Date(current.updatedAt).getTime()<18000,state:current?.value??{mode:'idle',title:'',positionMs:0,durationMs:0},lastSeenAt:current?.updatedAt??null}}))};
 });
 app.patch('/api/connect/devices/:id',async req=>{const {link}=await linkFor(req,(req.params as any).id,false);const b=z.object({label:z.string().trim().min(1).max(60)}).parse(req.body);await db.collection('connect_links').updateOne({_id:link._id},{$set:{label:b.label,updatedAt:now()}});return {id:link._id.toString(),label:b.label}});
 app.delete('/api/connect/devices/:id',async req=>{const {link}=await linkFor(req,(req.params as any).id,false);await db.collection('connect_links').updateOne({_id:link._id},{$set:{revoked:true,updatedAt:now()}});await db.collection('connect_commands').updateMany({linkId:link._id.toString(),state:{$ne:'acked'}},{$set:{state:'revoked'}});return {success:true}});
 app.post('/api/connect/devices/:id/commands',{config:{rateLimit:{max:120,timeWindow:'1 minute'}}},async req=>{
   const {s,u,link}=await linkFor(req,(req.params as any).id);const b=z.object({requestId:z.string().uuid(),command}).parse(req.body);
   const prior=await db.collection('connect_commands').findOne({userId:u._id.toString(),phoneSessionId:s._id.toString(),requestId:b.requestId});
   if(prior){if(JSON.stringify(prior.command)!==JSON.stringify(b.command))throw new Fault(409,'IDEMPOTENCY_CONFLICT','O identificador já foi usado para outro comando');return {id:prior._id.toString(),state:prior.state}}
   const live=await db.collection('sessions').findOne({_id:id(link.tvSessionId),userId:u._id.toString(),revoked:false,expiresAt:{$gt:now()},version:u.sessionVersion});if(!live)throw new Fault(409,'TV_SESSION_EXPIRED','Entre novamente na TV');
   if(b.command.type==='play'){if(b.command.live)await c.live.resolve(b.command.contentId);else await c.catalog.resolve(b.command.contentId,b.command.season,b.command.episode);}
   if(await db.collection('connect_commands').countDocuments({tvSessionId:link.tvSessionId,state:{$in:['queued','delivered']},expiresAt:{$gt:now()}})>=24)throw new Fault(429,'QUEUE_FULL','A TV precisa receber os comandos anteriores');
   const row={userId:u._id.toString(),phoneSessionId:s._id.toString(),tvSessionId:link.tvSessionId,linkId:link._id.toString(),requestId:b.requestId,command:b.command,state:'queued',createdAt:now(),expiresAt:new Date(now().getTime()+60000)};
   try{const result=await db.collection('connect_commands').insertOne(row);return {id:result.insertedId.toString(),state:'queued'}}catch(e:any){if(e.code===11000){const old=await db.collection('connect_commands').findOne({userId:row.userId,phoneSessionId:row.phoneSessionId,requestId:row.requestId});if(old&&JSON.stringify(old.command)===JSON.stringify(row.command))return {id:old._id.toString(),state:old.state}}throw e}
 });
 app.get('/api/connect/commands/:id',async req=>{const {s,u}=await owner(req,false);const row=await db.collection('connect_commands').findOne({_id:id((req.params as any).id),userId:u._id.toString(),phoneSessionId:s._id.toString()});if(!row)throw new Fault(404,'COMMAND_NOT_FOUND','Comando não encontrado');return {id:row._id.toString(),state:row.expiresAt<=now()&&row.state!=='acked'?'expired':row.state,result:row.result??null}});
 app.post('/api/connect/tv/poll',{config:{rateLimit:{max:240,timeWindow:'1 minute'}}},async req=>{
   const {s,u}=await tv(req),time=now();const b=z.object({state:state.optional()}).parse(req.body??{});
   if(b.state)await db.collection('connect_state').updateOne({tvSessionId:s._id.toString()},{$set:{userId:u._id.toString(),value:b.state,updatedAt:time,expiresAt:new Date(time.getTime()+86400000)}},{upsert:true});
   const links=await db.collection('connect_links').find({tvSessionId:s._id.toString(),userId:u._id.toString(),revoked:false}).limit(30).toArray();
   const active=await db.collection('sessions').find({_id:{$in:links.map(l=>id(l.phoneSessionId))},userId:u._id.toString(),revoked:false,expiresAt:{$gt:time},version:u.sessionVersion}).project({_id:1}).toArray();const phones=new Set(active.map(v=>v._id.toString()));
   const row=await db.collection('connect_commands').findOneAndUpdate({tvSessionId:s._id.toString(),linkId:{$in:links.filter(l=>phones.has(l.phoneSessionId)).map(l=>l._id.toString())},expiresAt:{$gt:time},$or:[{state:'queued'},{state:'delivered',deliveryUntil:{$lte:time}}]},{$set:{state:'delivered',deliveryUntil:new Date(time.getTime()+10000)}},{sort:{createdAt:1},returnDocument:'after'});
   return {serverTime:time,command:row?{id:row._id.toString(),command:row.command,expiresAt:row.expiresAt}:null,paired:links.length};
 });
 app.post('/api/connect/tv/commands/:id/ack',async req=>{const {s,u}=await tv(req);const b=z.object({success:z.boolean(),message:z.string().max(240).optional()}).parse(req.body);const result=await db.collection('connect_commands').updateOne({_id:id((req.params as any).id),userId:u._id.toString(),tvSessionId:s._id.toString(),state:'delivered',expiresAt:{$gt:now()}},{$set:{state:'acked',result:b,ackedAt:now()}});if(!result.matchedCount)throw new Fault(409,'COMMAND_EXPIRED','Comando não está mais disponível');return {success:true}});
}
