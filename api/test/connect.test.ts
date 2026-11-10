import {test} from 'node:test';import assert from 'node:assert/strict';import {randomUUID} from 'node:crypto';import {MongoMemoryReplSet} from 'mongodb-memory-server';import argon2 from 'argon2';
import {connect} from '../src/db.js';import {createApp} from '../src/app.js';import {ProviderCatalog} from '../src/catalog.js';
const pages:Record<string,string>={
 'indice.txt':'base:0 https://media.example.invalid/\nA\t1\t1',
 'busca.txt':'A Jornada (2026)\tf\tA\t2026\nA Série\ts\tA\t2025',
 'fichas.txt':'capa:https://image.tmdb.org/t/p/w500\nf\tA Jornada (2026)\t42\t/poster.jpg\tAventura,Drama\ns\tA Série\t43\t/serie.jpg\tDrama',
 'filmes-A.txt':'A Jornada (2026)\tdub=0:movie.m3u8,0:backup.mp4',
 'series-A.txt':'A Série\t2025\t0\t2',
 'series-A-0.txt':'@A Série\t2025\n4\t6\tdub\t0:episode.m3u8\n4\t7\tdub\t0:episode7.m3u8',
};
const catalog=()=>new ProviderCatalog('https://catalog.example.invalid/',async(input:any)=>{const name=new URL(String(input)).pathname.slice(1);return new Response(pages[name]??'',{status:pages[name]===undefined?404:200})});

test('authorized catalog parses movie, genres, artwork and season/episode without fetching streams',async()=>{
 const c=catalog();const all=await c.list('','',0);assert.equal(all.total,2);assert(all.categories.includes('Drama'));
 const movie=all.items.find(x=>x.kind==='movie')!,series=all.items.find(x=>x.kind==='series')!;
 assert.equal((await c.resolve(movie.id)).urls[0],'https://media.example.invalid/movie.m3u8');
 assert.equal((await c.list('jornada','movie',0)).total,1);assert.equal((await c.detail(series.id)).episodes?.length,2);
 assert.equal((await c.resolve(series.id,4,6)).urls[0],'https://media.example.invalid/episode.m3u8');
 await assert.rejects(c.resolve(series.id),/temporada/);await assert.rejects(c.resolve(series.id,99,6),/fonte/);
 const denied=new ProviderCatalog('https://catalog.example.invalid/',async()=>new Response('',{status:403}));
 assert.deepEqual(await denied.health(),{available:false,reason:'CATALOG_PROVIDER'});await assert.rejects(denied.list('','',0),/HTTP 403/);
});

test('PWA cookie, persistent pairing, isolated commands, acknowledgement, reconnect, expiry and revocation',async()=>{
 const repl=await MongoMemoryReplSet.create({replSet:{count:1}});const {db,client}=await connect(repl.getUri(),'guhflix_test');let clock=new Date('2030-01-01T00:00:00Z');
 const origin='https://phone.example',pass='fixture-password-only';const hash=await argon2.hash(pass);
 await db.collection('users').insertMany(['owner','stranger'].map(username=>({username,passwordHash:hash,role:'customer',status:'enabled',sessionVersion:1,expiresAt:new Date(clock.getTime()+3*86400000),slots:[],createdAt:clock,updatedAt:clock})));
 const app=await createApp({db,client,secret:'x'.repeat(64),adminOrigin:'https://admin.example',pwaOrigin:origin,catalog:catalog(),now:()=>clock});
 const req=(method:any,url:string,payload:any=undefined,access?:string,extra:any={})=>app.inject({method,url,payload,headers:{...(access?{authorization:'Bearer '+access}:{}),...extra}});
 const login=async(username:string,kind='tv')=>req('POST','/api/auth/login',{username,password:pass,client:kind,installationId:randomUUID(),label:kind==='pwa'?'iPhone Safari':'TCL sala'},undefined,kind==='pwa'?{origin}:{});
 try{
  const tv=(await login('owner')).json();const phoneResponse=await login('owner','pwa'),phone=phoneResponse.json(),stranger=(await login('stranger','pwa')).json();
  assert(phone.accessToken);assert(!phone.refreshToken);const cookie=String(phoneResponse.headers['set-cookie']).split(';')[0];assert.match(String(phoneResponse.headers['set-cookie']),/HttpOnly/);assert.match(String(phoneResponse.headers['set-cookie']),/Secure/);
  assert.equal((await req('POST','/api/connect/tv/poll',{},phone.accessToken,{origin})).statusCode,403);
  const pairing=(await req('POST','/api/connect/pairing',{},tv.accessToken)).json();assert.match(pairing.code,/^[A-F0-9]{5}-[A-F0-9]{5}$/);
  assert.equal((await req('POST','/api/connect/pairing/claim',{code:pairing.code},stranger.accessToken,{origin})).statusCode,404);
  const claim=await req('POST','/api/connect/pairing/claim',{code:pairing.code,label:'Sala personalizada'},phone.accessToken,{origin});assert.equal(claim.statusCode,200,claim.body);const link=claim.json().id;assert.equal(claim.json().label,'Sala personalizada');assert.equal((await req('PATCH',`/api/connect/devices/${link}`,{label:'Quarto'},stranger.accessToken,{origin})).statusCode,404);assert.equal((await req('PATCH',`/api/connect/devices/${link}`,{label:'Quarto'},phone.accessToken,{origin})).statusCode,200);assert.equal((await req('GET','/api/connect/devices',undefined,phone.accessToken)).json().devices[0].label,'Quarto');assert.equal((await req('PATCH',`/api/connect/devices/${link}`,{label:' '},phone.accessToken,{origin})).statusCode,400);
  assert.equal((await req('POST','/api/connect/pairing/claim',{code:pairing.code},phone.accessToken,{origin})).statusCode,404);
  const items=(await req('GET','/api/connect/catalog',undefined,phone.accessToken)).json().items;const movie=items.find((i:any)=>i.kind==='movie');
  assert.equal((await req('GET',`/api/connect/catalog/${movie.id}/play`,undefined,phone.accessToken)).statusCode,403);
  const requestId=randomUUID(),body={requestId,command:{type:'play',contentId:movie.id}};
  const sent=await req('POST',`/api/connect/devices/${link}/commands`,body,phone.accessToken,{origin});assert.equal(sent.statusCode,200,sent.body);const cid=sent.json().id;
  const strangerTv=(await login('stranger')).json();assert.equal((await req('POST','/api/connect/tv/poll',{},strangerTv.accessToken)).json().command,null,'Another account television cannot receive the owner command');assert.equal((await req('GET',`/api/connect/commands/${cid}`,undefined,stranger.accessToken)).statusCode,404);assert.equal((await req('GET','/api/connect/devices',undefined,stranger.accessToken)).json().devices.length,0);
  assert.equal((await req('POST',`/api/connect/devices/${link}/commands`,body,phone.accessToken,{origin})).json().id,cid);
  assert.equal(await db.collection('connect_commands').countDocuments({requestId}),1);
  assert.equal((await req('POST',`/api/connect/devices/${link}/commands`,{requestId,command:{type:'pause'}},phone.accessToken,{origin})).statusCode,409);
  assert.equal((await req('POST',`/api/connect/devices/${link}/commands`,{requestId:randomUUID(),command:{type:'stop'}},stranger.accessToken,{origin})).statusCode,404);
  const poll=await req('POST','/api/connect/tv/poll',{state:{mode:'loading',title:movie.title,contentId:movie.id,positionMs:0,durationMs:0}},tv.accessToken);assert.equal(poll.json().command.id,cid);
  assert.equal((await req('POST','/api/connect/tv/poll',{},tv.accessToken)).json().command,null);
  clock=new Date(clock.getTime()+11000);assert.equal((await req('POST','/api/connect/tv/poll',{},tv.accessToken)).json().command.id,cid,'redelivery after disconnect keeps command ID for TV deduplication');
  assert.equal((await req('POST',`/api/connect/tv/commands/${cid}/ack`,{success:true},tv.accessToken)).statusCode,200);
  assert.equal((await req('GET',`/api/connect/commands/${cid}`,undefined,phone.accessToken)).json().state,'acked');
  const devices=(await req('GET','/api/connect/devices',undefined,phone.accessToken)).json();assert.equal(devices.devices[0].online,true);assert.equal(devices.devices[0].state.title,movie.title);
  assert.equal((await req('POST','/api/connect/tv/poll',{state:{mode:'playing',url:'https://secret.invalid/token'}},tv.accessToken)).statusCode,400);
  const refresh=await req('POST','/api/auth/refresh',{client:'pwa'},undefined,{origin,cookie});assert.equal(refresh.statusCode,200,refresh.body);assert(!refresh.json().refreshToken);const renewed=refresh.json().accessToken;
  assert.equal((await req('GET','/api/connect/devices',undefined,renewed)).json().devices[0].id,link,'pairing survives refresh rotation');
  await req('POST',`/api/connect/devices/${link}/commands`,{requestId:randomUUID(),command:{type:'pause'}},renewed,{origin});
  assert.equal((await req('DELETE',`/api/connect/devices/${link}`,undefined,renewed,{origin})).statusCode,200);
  assert.equal((await req('POST','/api/connect/tv/poll',{},tv.accessToken)).json().command,null,'revocation removes queued control');
  const pair2=(await req('POST','/api/connect/pairing',{},tv.accessToken)).json();const link2=(await req('POST','/api/connect/pairing/claim',{code:pair2.code},renewed,{origin})).json().id;
  const expiring=(await req('POST',`/api/connect/devices/${link2}/commands`,{requestId:randomUUID(),command:{type:'seekRelative',deltaMs:10000}},renewed,{origin})).json();clock=new Date(clock.getTime()+61000);
  assert.equal((await req('POST','/api/connect/tv/poll',{},tv.accessToken)).json().command,null);assert.equal((await req('GET',`/api/connect/commands/${expiring.id}`,undefined,renewed)).json().state,'expired');
  const tv2=(await login('owner')).json();const pair3=(await req('POST','/api/connect/pairing',{},tv2.accessToken)).json();const link3=(await req('POST','/api/connect/pairing/claim',{code:pair3.code},renewed,{origin})).json().id;
  assert.equal((await req('DELETE','/api/connect/tv/devices',undefined,renewed,{origin})).statusCode,403);
  assert.equal((await req('DELETE','/api/connect/tv/devices',undefined,tv.accessToken)).statusCode,200);
  const remaining=(await req('GET','/api/connect/devices',undefined,renewed)).json().devices;
  assert(remaining.some((d:any)=>d.id===link3),'TV revocation does not affect another television');assert(!remaining.some((d:any)=>d.id===link2));
  const oldPair=(await req('POST','/api/connect/pairing',{},tv.accessToken)).json();clock=new Date(clock.getTime()+301000);assert.equal((await req('POST','/api/connect/pairing/claim',{code:oldPair.code},renewed,{origin})).statusCode,404);
  const browser=(await login('owner','pwa')).json();
  assert.equal((await req('POST','/api/connect/browser-tv/enable',{})).statusCode,401);
  assert.equal((await req('POST','/api/connect/browser-tv/enable',{},browser.accessToken,{origin:'https://evil.example'})).statusCode,403);
  assert.equal((await req('POST','/api/connect/browser-tv/enable',{},browser.accessToken,{origin})).statusCode,200);
  const browserPair=(await req('POST','/api/connect/pairing',{},browser.accessToken,{origin})).json();assert.match(browserPair.code,/^[A-F0-9]{5}-[A-F0-9]{5}$/);
  assert.equal((await req('POST','/api/connect/pairing/claim',{code:browserPair.code},stranger.accessToken,{origin})).statusCode,404);
  const browserLink=(await req('POST','/api/connect/pairing/claim',{code:browserPair.code,label:'Philco navegador'},renewed,{origin})).json().id;assert(browserLink);
  const browserCommand=(await req('POST','/api/connect/devices/'+browserLink+'/commands',{requestId:randomUUID(),command:{type:'pause'}},renewed,{origin})).json();
  const browserPoll=await req('POST','/api/connect/tv/poll',{state:{mode:'idle',title:'',positionMs:0,durationMs:0}},browser.accessToken,{origin});assert.equal(browserPoll.statusCode,200);assert.equal(browserPoll.json().command.id,browserCommand.id);
  assert.equal((await req('POST','/api/connect/tv/commands/'+browserCommand.id+'/ack',{success:true},browser.accessToken,{origin})).statusCode,200);
  assert.equal((await req('POST','/api/auth/logout',{},browser.accessToken,{origin})).statusCode,200);assert.equal((await req('POST','/api/connect/tv/poll',{},browser.accessToken,{origin})).statusCode,401);
  await db.collection('users').updateOne({username:'owner'},{$set:{status:'blocked'}});assert.equal((await req('POST',`/api/connect/devices/${link2}/commands`,{requestId:randomUUID(),command:{type:'resume'}},renewed,{origin})).statusCode,403);
  assert.equal((await req('POST','/api/auth/refresh',{client:'pwa'},undefined,{origin:'https://evil.example',cookie})).statusCode,403);
  assert.equal((await req('GET','/pwa/../../api/src/server.ts')).statusCode,404);
 }finally{await app.close();await client.close();await repl.stop()}
});
