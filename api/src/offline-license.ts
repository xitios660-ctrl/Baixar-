import {createPrivateKey,createPublicKey,createHash,randomBytes,sign,verify,type KeyObject} from 'node:crypto';
import {readFileSync} from 'node:fs';
import type {FastifyInstance,FastifyRequest} from 'fastify';
import type {Db,MongoClient} from 'mongodb';
import {z} from 'zod';import {Fault} from './errors.js';import {OFFLINE_ROOT_SPKI} from './offline-root.js';
export const MAX_OFFLINE_MS=30*86400000;
const b64=(b:Buffer|string)=>Buffer.from(b).toString('base64url');
export const digest=(b:Buffer)=>createHash('sha256').update(b).digest('hex');
export function readJws(token:string,key:KeyObject){const parts=token.split('.');if(parts.length!==3||token.length>32768)throw Error('Invalid JWS');const h=JSON.parse(Buffer.from(parts[0],'base64url').toString());if(h.alg!=='RS256'||h.crit||h.jku||h.jwk||!verify('RSA-SHA256',Buffer.from(parts[0]+'.'+parts[1]),key,Buffer.from(parts[2],'base64url')))throw Error('Invalid signature');return JSON.parse(Buffer.from(parts[1],'base64url').toString());}
export function signJws(payload:object,key:KeyObject){const s=b64('{"alg":"RS256","typ":"JWT"}')+'.'+b64(JSON.stringify(payload));return s+'.'+sign('RSA-SHA256',Buffer.from(s),key).toString('base64url');}
export class OfflineAuthority {
 readonly info:any;readonly issuerId:string;
 constructor(readonly privateKey:KeyObject,readonly grant:string,root=createPublicKey({key:Buffer.from(OFFLINE_ROOT_SPKI,'base64url'),format:'der',type:'spki'})){
  this.info=readJws(grant,root);this.issuerId=digest(createPublicKey(privateKey).export({format:'der',type:'spki'}) as Buffer);
  const a=this.info;if(a.v!==1||a.kind!=='gfilmes-authority'||a.issuerId!==this.issuerId||a.publicKey!==(createPublicKey(privateKey).export({format:'der',type:'spki'}) as Buffer).toString('base64url')||!Number.isSafeInteger(a.maxLicenseMs)||a.maxLicenseMs<1||a.maxLicenseMs>MAX_OFFLINE_MS)throw Error('Invalid delegation');
 }
 issue(user:{id:string;username:string;expiresAt:Date},deviceId:string,now:number){
  if(now<this.info.notBefore||now>=this.info.expiresAt)throw new Fault(503,'AUTHORITY_EXPIRED','A autoridade precisa ser revalidada pelo titular');
  const expiresAt=Math.min(now+this.info.maxLicenseMs,user.expiresAt.getTime(),this.info.expiresAt);if(expiresAt<=now)throw new Fault(403,'LICENSE_EXPIRED','Assinatura vencida');
  const p={v:1,kind:'gfilmes-offline-license',issuerId:this.issuerId,licenseId:randomBytes(18).toString('hex'),customerId:user.id,username:user.username,deviceId,issuedAt:now,expiresAt,subscriptionExpiresAt:user.expiresAt.getTime()};
  return {envelope:{authority:this.grant,license:signJws(p,this.privateKey)},claims:p};
 }
 static configured(){const key=process.env.GFILMES_LICENSE_KEY_FILE,grant=process.env.GFILMES_LICENSE_GRANT_FILE;if(!key&&!grant)return undefined;if(!key||!grant)throw Error('Both offline authority files are required');return new OfflineAuthority(createPrivateKey(readFileSync(key)),readFileSync(grant,'utf8').trim());}
}
type Config={db:Db;client:MongoClient;now:()=>Date;identity:(r:FastifyRequest)=>Promise<{s:any;u:any}>;authority?:OfflineAuthority};
export async function registerOfflineLicenses(app:FastifyInstance,c:Config){
 const authority=c.authority??OfflineAuthority.configured();const nonces=new Map<string,{user:string;session:string;expires:number}>();
 if(authority)await Promise.all([c.db.collection('offline_devices').createIndex({userId:1,deviceId:1},{unique:true}),c.db.collection('offline_licenses').createIndex({userId:1,issuedAt:-1})]);
 const enabled=()=>{if(!authority)throw new Fault(503,'OFFLINE_NOT_CONFIGURED','Ativação offline ainda não configurada; o serviço atual permanece disponível');return authority;};
 app.get('/api/offline/challenge',{config:{rateLimit:{max:12,timeWindow:'1 minute'}}},async req=>{enabled();const {u,s}=await c.identity(req);if(u.role!=='customer')throw new Fault(403,'CUSTOMER_REQUIRED','Use uma conta de cliente');const now=c.now().getTime();for(const [k,v]of nonces)if(v.expires<now)nonces.delete(k);if(nonces.size>=512)throw new Fault(429,'BUSY','Tente novamente');const nonce=randomBytes(24).toString('base64url');nonces.set(nonce,{user:u._id.toString(),session:s._id.toString(),expires:now+60000});return {nonce,serverTime:now};});
 app.post('/api/offline/activate',{logLevel:'silent',config:{rateLimit:{max:12,timeWindow:'1 minute'}}},async req=>{
  const issuer=enabled();const who=await c.identity(req);if(who.u.role!=='customer')throw new Fault(403,'CUSTOMER_REQUIRED','Use uma conta de cliente');
  const b=z.object({nonce:z.string().min(20).max(100),devicePublicKey:z.string().min(300).max(1000),proof:z.string().min(300).max(1000),label:z.string().max(100).default('Android TV')}).strict().parse(req.body);const challenge=nonces.get(b.nonce);nonces.delete(b.nonce);
  const now=c.now();if(!challenge||challenge.expires<now.getTime()||challenge.user!==who.u._id.toString()||challenge.session!==who.s._id.toString())throw new Fault(403,'DEVICE_CHALLENGE','Desafio inválido ou expirado');
  let deviceId:string;try{const raw=Buffer.from(b.devicePublicKey,'base64url'),pub=createPublicKey({key:raw,type:'spki',format:'der'});if(pub.asymmetricKeyType!=='rsa'||(pub.asymmetricKeyDetails?.modulusLength??0)<2048)throw Error();deviceId=digest(raw);if(!verify('RSA-SHA256',Buffer.from('gfilmes-device-proof-v1|'+b.nonce+'|'+deviceId),pub,Buffer.from(b.proof,'base64url')))throw Error();}catch{throw new Fault(403,'DEVICE_PROOF','O dispositivo não comprovou sua identidade');}
  const session=c.client.startSession();try{return await session.withTransaction(async()=>{
   const opts={session};const user=await c.db.collection('users').findOne({_id:who.u._id},opts);if(!user||user.role!=='customer'||user.status!=='enabled')throw new Fault(403,'LICENSE_BLOCKED','Conta bloqueada');if(!(user.expiresAt instanceof Date)||user.expiresAt<=now)throw new Fault(403,'LICENSE_EXPIRED','Assinatura vencida');
   // Serialize activations on the account, including on different API instances.
   await c.db.collection('users').updateOne({_id:user._id},{$inc:{offlineLicenseEpoch:1}},opts);
   const devices=c.db.collection('offline_devices');const existing=await devices.findOne({userId:user._id.toString(),deviceId},opts);
   if(existing?.retired)throw new Fault(403,'DEVICE_RETIRED','Dispositivo removido pelo administrador');
   if(!existing||existing.expiresAt<=now){const active=await devices.countDocuments({userId:user._id.toString(),expiresAt:{$gt:now}},opts);if(active>=3)throw new Fault(409,'DEVICE_LIMIT','Três dispositivos ainda têm licenças válidas. A remoção não apaga uma licença já emitida.');}
   const result=issuer.issue({id:user._id.toString(),username:user.username,expiresAt:user.expiresAt},deviceId,now.getTime());
   await devices.updateOne({userId:user._id.toString(),deviceId},{$set:{label:b.label,expiresAt:new Date(result.claims.expiresAt),updatedAt:now},$setOnInsert:{createdAt:now,retired:false}}, {...opts,upsert:true});
   await c.db.collection('offline_licenses').insertOne({userId:user._id.toString(),...result.claims,issuedAt:now,expiresAt:new Date(result.claims.expiresAt),envelope:result.envelope},opts);
   await c.db.collection('audit_logs').insertOne({actorId:user._id.toString(),targetUserId:user._id.toString(),action:'offline_license_issue',deviceId,licenseId:result.claims.licenseId,createdAt:now},opts);
   return {...result.envelope,serverTime:now.getTime()};
  },{readConcern:{level:'snapshot'},writeConcern:{w:'majority'}});}finally{await session.endSession();}
 });
}
