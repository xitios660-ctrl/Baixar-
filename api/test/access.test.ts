import {createRuntime} from '../src/runtime.js';
import {backup} from '../src/backup.js';import {readFile,unlink} from 'node:fs/promises';import {createDecipheriv,randomBytes} from 'node:crypto';import {gunzipSync} from 'node:zlib';
import {test} from 'node:test';import assert from 'node:assert/strict';import {randomUUID} from 'node:crypto';import {MongoMemoryReplSet} from 'mongodb-memory-server';import {connect} from '../src/db.js';import {createApp} from '../src/app.js';import argon2 from 'argon2';import {TOTP,Secret} from 'otpauth';
test('GUHFLIX full subscription, authentication and concurrency flow',async()=>{
 const repl=await MongoMemoryReplSet.create({replSet:{count:1},binary:{version:'7.0.14'}});const {client,db}=await connect(repl.getUri(),'guhflix_test');let clock=new Date();const now=()=>new Date(clock);const app=await createApp({client,db,secret:'x'.repeat(64),adminOrigin:'https://admin.example.com',production:false,now,bootstrapToken:'b'.repeat(64)});
 const pass='Safe-Integration-Password-2026',secret=new Secret().base32;
 const request=async(method:string,url:string,body?:any,access?:string,extra:any={})=>app.inject({method:method as any,url,headers:{...(access?{authorization:'Bearer '+access}:{}),...extra},...(body?{payload:body}:{})});
 const login=async(user:string,clientType='tv')=>request('POST','/api/auth/login',{username:user,password:pass,installationId:randomUUID(),client:clientType,...(clientType==='admin'?{otp:new TOTP({secret:Secret.fromBase32(secret)}).generate({timestamp:clock.getTime()})}:{})});
 try{
 const setup={token:'b'.repeat(64),username:'admin',password:pass,totpSecret:secret,otp:new TOTP({secret:Secret.fromBase32(secret)}).generate({timestamp:clock.getTime()})};
 assert.equal((await request('POST','/api/setup/create',{...setup,token:'z'.repeat(64)})).statusCode,403);
 assert.equal((await request('POST','/api/setup/create',setup)).statusCode,200);
 assert.equal((await request('POST','/api/setup/create',setup)).statusCode,409);
 assert.equal((await request('GET','/api/admin/users')).statusCode,401);
 assert.equal((await request('POST','/api/auth/login',{username:{$ne:null},password:pass,installationId:randomUUID()})).statusCode,400);
 assert.equal((await request('POST','/api/auth/login',{username:'admin',password:'wrong',installationId:randomUUID()})).statusCode,401);
 const ar=await login('admin','admin');assert.equal(ar.statusCode,200,ar.body);const at=ar.json().accessToken;
 const created=await request('POST','/api/admin/users',{username:'customer',password:pass},at);assert.equal(created.statusCode,200,created.body);const uid=created.json().id;
 const first=await login('customer');assert.equal(first.statusCode,200);const one=first.json();
 const restarted=await createApp({client,db,secret:'x'.repeat(64),adminOrigin:'https://admin.example.com',production:false,now});
 const resumed=await restarted.inject({method:'POST',url:'/api/auth/refresh',payload:{refreshToken:one.refreshToken}});assert.equal(resumed.statusCode,200);one.refreshToken=resumed.json().refreshToken;one.accessToken=resumed.json().accessToken;await restarted.close();
assert.equal((await request('GET','/api/admin/users',undefined,one.accessToken)).statusCode,403);
 assert.equal((await request('GET','/api/app/access',undefined,one.accessToken)).json().state,'expired');assert.equal((await request('POST','/api/app/playback/start',{playbackId:randomUUID()},one.accessToken)).statusCode,403);
 const k=randomUUID(),renew={days:30,paymentConfirmed:true,amountCents:3000,method:'pix'};
 const duplicate=await Promise.all(Array.from({length:6},()=>request('POST',`/api/admin/users/${uid}/renew`,renew,at,{'idempotency-key':k})));assert(duplicate.every(r=>r.statusCode===200),duplicate.map(r=>r.body).join('\n'));assert.equal(await db.collection('payments').countDocuments({userId:uid}),1);assert.equal(new Date(duplicate[0].json().expiresAt).getTime(),clock.getTime()+30*86400000);
 assert.equal((await request('POST',`/api/admin/users/${uid}/renew`,{...renew,paymentConfirmed:false},at,{'idempotency-key':randomUUID()})).statusCode,400);
 assert.equal((await request('GET','/api/app/access',undefined,one.accessToken)).json().state,'active');
 const two=(await login('customer')).json(),three=(await login('customer')).json(),four=(await login('customer')).json();const all=[one,two,three,four],pids=all.map(()=>randomUUID());
 await request('POST','/api/admin/users',{username:'second-customer',password:pass},at);const stranger=(await login('second-customer')).json();const firstSession=await db.collection('sessions').findOne({userId:uid});assert.equal((await request('POST','/api/app/sessions/'+firstSession!._id.toString()+'/revoke',{},stranger.accessToken)).statusCode,404);
 const starts=await Promise.all(all.map((a,i)=>request('POST','/api/app/playback/start',{playbackId:pids[i]},a.accessToken)));assert.equal(starts.filter(r=>r.statusCode===200).length,3,starts.map(r=>r.body).join('\n'));assert.equal(starts.filter(r=>r.statusCode===409).length,1);
 const blocked=starts.findIndex(r=>r.statusCode===409),playing=starts.findIndex(r=>r.statusCode===200);
 assert.equal((await request('POST','/api/app/playback/heartbeat',{playbackId:pids[playing]},all[playing].accessToken)).statusCode,200);
 await request('POST','/api/app/playback/stop',{playbackId:pids[playing]},all[playing].accessToken);assert.equal((await request('POST','/api/app/playback/start',{playbackId:pids[blocked]},all[blocked].accessToken)).statusCode,200);
 clock=new Date(clock.getTime()+121000);assert.equal((await request('POST','/api/app/playback/start',{playbackId:randomUUID()},all[playing].accessToken)).statusCode,200);
 // Windows can have multiple players in one authenticated session; all share the account limit.
 clock=new Date(clock.getTime()+121000);
 const winPids=Array.from({length:4},()=>randomUUID());
 const winStarts=await Promise.all(winPids.map(playbackId=>request('POST','/api/app/playback/start',{playbackId},one.accessToken)));
 assert.equal(winStarts.filter(r=>r.statusCode===200).length,3);assert.equal(winStarts.filter(r=>r.statusCode===409).length,1);
 const winPlaying=winPids.filter((_,i)=>winStarts[i].statusCode===200);
 assert.equal((await request('POST','/api/app/playback/stop',{playbackId:winPlaying[0]},one.accessToken)).statusCode,200);
 const ownSession=(await request('GET','/api/app/sessions',undefined,one.accessToken)).json().find((s:any)=>s.playbackActive);
 assert(ownSession,'Stopping one player must preserve other players in session metadata');
 for(const playbackId of winPlaying.slice(1))assert.equal((await request('POST','/api/app/playback/heartbeat',{playbackId},one.accessToken)).statusCode,200);
 for(const playbackId of winPlaying.slice(1))await request('POST','/api/app/playback/stop',{playbackId},one.accessToken);
 assert((await request('GET','/api/app/sessions',undefined,one.accessToken)).json().every((s:any)=>!s.playbackActive));
 await request('POST',`/api/admin/users/${uid}/block`,{},at);const key2=randomUUID();await request('POST',`/api/admin/users/${uid}/renew`,renew,at,{'idempotency-key':key2});assert.equal((await request('GET','/api/app/access',undefined,one.accessToken)).json().state,'blocked');
 await request('POST',`/api/admin/users/${uid}/unblock`,{},at);assert.equal((await request('GET','/api/app/access',undefined,one.accessToken)).json().state,'active');
 await db.collection('users').updateOne({username:'customer'},{$set:{expiresAt:new Date(clock.getTime()-1)}});assert.equal((await request('GET','/api/app/access',undefined,one.accessToken)).json().state,'expired'); // server clock, independent of device
 // Test refresh before actual session expiry using the original server clock.
 const rr=await request('POST','/api/auth/refresh',{refreshToken:one.refreshToken});assert.equal(rr.statusCode,200,rr.body);assert.notEqual(rr.json().refreshToken,one.refreshToken);assert.equal((await request('GET','/api/app/access',undefined,rr.json().accessToken)).statusCode,200);
 assert.equal((await request('POST','/api/auth/refresh',{refreshToken:one.refreshToken})).statusCode,401);assert.equal((await request('GET','/api/app/access',undefined,rr.json().accessToken)).statusCode,401);
 const userSessions=(await request('GET',`/api/admin/users/${uid}/sessions`,undefined,at)).json();assert(userSessions.length>0);await request('POST',`/api/admin/sessions/${userSessions[0].id}/revoke`,{},at);
 const other=(await login('customer')).json();assert.equal((await request('POST','/api/auth/logout',{},other.accessToken)).statusCode,200);assert.equal((await request('POST','/api/auth/refresh',{refreshToken:other.refreshToken})).statusCode,401);
 const dash=(await request('GET','/api/admin/dashboard',undefined,at)).json();assert.equal(dash.revenueCents,6000);assert.equal(dash.renewals,2);assert(await db.collection('audit_logs').countDocuments()>5);
 assert.equal((await request('POST','/api/auth/refresh',{client:'admin'},undefined,{origin:'https://evil.example'})).statusCode,403);
 const backupFile='/dev/shm/guhflix-backup-'+randomUUID()+'.guhbackup',backupKey=randomBytes(32),snapshot=client.startSession({snapshot:true});await backup(db,backupKey,backupFile,snapshot);await snapshot.endSession();const encrypted=await readFile(backupFile);assert.equal(encrypted.subarray(0,8).toString(),'GUHFLIX1');assert(!encrypted.includes(Buffer.from('customer')));const cipher=createDecipheriv('aes-256-gcm',backupKey,encrypted.subarray(8,20));cipher.setAuthTag(encrypted.subarray(-16));const decoded=gunzipSync(Buffer.concat([cipher.update(encrypted.subarray(20,-16)),cipher.final()])).toString();assert(decoded.includes('"collection":"payments"'));assert(decoded.includes('"collection":"audit_logs"'));await unlink(backupFile);
 const transport=createRuntime();const port=await transport.listen(0);try{await app.ready();transport.activate((req,res)=>app.server.emit('request',req,res));assert.equal((await fetch(`http://127.0.0.1:${port}/health`)).status,200);assert.equal((await fetch(`http://127.0.0.1:${port}/api/admin/users`)).status,401);const me=await fetch(`http://127.0.0.1:${port}/api/auth/me`,{headers:{Authorization:'Bearer '+at}});assert.equal(me.status,200);assert.equal((await me.json()).username,'admin');const denied=await fetch(`http://127.0.0.1:${port}/api/app/playback/start`,{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+at},body:JSON.stringify({playbackId:randomUUID()})});assert.equal(denied.status,403);}finally{await transport.close()}
 console.log('Verified: RBAC, injection rejection, password, expiration, duplicate atomic renewal, 3/4 concurrent streams, stop/timeout, block preservation, refresh rotation/reuse, revocation, logout, revenue and audit.');
 }finally{await app.close();await client.close();await repl.stop()}
});
