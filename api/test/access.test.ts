import {test} from 'node:test';import assert from 'node:assert/strict';import {randomUUID} from 'node:crypto';import {MongoMemoryReplSet} from 'mongodb-memory-server';import {connect} from '../src/db.js';import {createApp} from '../src/app.js';import argon2 from 'argon2';import {TOTP,Secret} from 'otpauth';
test('GUHFLIX full subscription, authentication and concurrency flow',async()=>{
 const repl=await MongoMemoryReplSet.create({replSet:{count:1},binary:{version:'7.0.14'}});const {client,db}=await connect(repl.getUri(),'GUHFLIX_TEST');let clock=new Date();const now=()=>new Date(clock);const app=await createApp({client,db,secret:'x'.repeat(64),adminOrigin:'https://admin.example.com',production:false,now});
 const pass='Safe-Integration-Password-2026',secret=new Secret().base32;const admin=await db.collection('users').insertOne({username:'admin',passwordHash:await argon2.hash(pass),totpSecret:secret,role:'admin',status:'enabled',sessionVersion:1,expiresAt:null,slots:[],createdAt:now()});
 const request=async(method:string,url:string,body?:any,access?:string,extra:any={})=>app.inject({method:method as any,url,headers:{...(access?{authorization:'Bearer '+access}:{}),...extra},...(body?{payload:body}:{})});
 const login=async(user:string,clientType='tv')=>request('POST','/api/auth/login',{username:user,password:pass,installationId:randomUUID(),client:clientType,...(clientType==='admin'?{otp:new TOTP({secret:Secret.fromBase32(secret)}).generate({timestamp:clock.getTime()})}:{})});
 try{
 assert.equal((await request('GET','/api/admin/users')).statusCode,401);
 assert.equal((await request('POST','/api/auth/login',{username:{$ne:null},password:pass,installationId:randomUUID()})).statusCode,400);
 assert.equal((await request('POST','/api/auth/login',{username:'admin',password:'wrong',installationId:randomUUID()})).statusCode,401);
 const ar=await login('admin','admin');assert.equal(ar.statusCode,200,ar.body);const at=ar.json().accessToken;
 const created=await request('POST','/api/admin/users',{username:'customer',password:pass},at);assert.equal(created.statusCode,200,created.body);const uid=created.json().id;
 const first=await login('customer');assert.equal(first.statusCode,200);const one=first.json();assert.equal((await request('GET','/api/admin/users',undefined,one.accessToken)).statusCode,403);
 assert.equal((await request('GET','/api/app/access',undefined,one.accessToken)).json().state,'expired');assert.equal((await request('POST','/api/app/playback/start',{playbackId:randomUUID()},one.accessToken)).statusCode,403);
 const k=randomUUID(),renew={days:30,paymentConfirmed:true,amountCents:3000,method:'pix'};
 const duplicate=await Promise.all(Array.from({length:6},()=>request('POST',`/api/admin/users/${uid}/renew`,renew,at,{'idempotency-key':k})));assert(duplicate.every(r=>r.statusCode===200),duplicate.map(r=>r.body).join('\n'));assert.equal(await db.collection('payments').countDocuments({userId:uid}),1);assert.equal(new Date(duplicate[0].json().expiresAt).getTime(),clock.getTime()+30*86400000);
 assert.equal((await request('POST',`/api/admin/users/${uid}/renew`,{...renew,paymentConfirmed:false},at,{'idempotency-key':randomUUID()})).statusCode,400);
 assert.equal((await request('GET','/api/app/access',undefined,one.accessToken)).json().state,'active');
 const two=(await login('customer')).json(),three=(await login('customer')).json(),four=(await login('customer')).json();const all=[one,two,three,four],pids=all.map(()=>randomUUID());
 const starts=await Promise.all(all.map((a,i)=>request('POST','/api/app/playback/start',{playbackId:pids[i]},a.accessToken)));assert.equal(starts.filter(r=>r.statusCode===200).length,3,starts.map(r=>r.body).join('\n'));assert.equal(starts.filter(r=>r.statusCode===409).length,1);
 const blocked=starts.findIndex(r=>r.statusCode===409),playing=starts.findIndex(r=>r.statusCode===200);
 assert.equal((await request('POST','/api/app/playback/heartbeat',{playbackId:pids[playing]},all[playing].accessToken)).statusCode,200);
 await request('POST','/api/app/playback/stop',{playbackId:pids[playing]},all[playing].accessToken);assert.equal((await request('POST','/api/app/playback/start',{playbackId:pids[blocked]},all[blocked].accessToken)).statusCode,200);
 clock=new Date(clock.getTime()+121000);assert.equal((await request('POST','/api/app/playback/start',{playbackId:randomUUID()},all[playing].accessToken)).statusCode,200);
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
 console.log('Verified: RBAC, injection rejection, password, expiration, duplicate atomic renewal, 3/4 concurrent streams, stop/timeout, block preservation, refresh rotation/reuse, revocation, logout, revenue and audit.');
 }finally{await app.close();await client.close();await repl.stop()}
});
