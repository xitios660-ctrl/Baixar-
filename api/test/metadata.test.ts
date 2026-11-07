import test from 'node:test';import assert from 'node:assert/strict';import {Metadata} from '../src/metadata.js';
test('metadata keeps credentials server-side, restricts paths, filters query and caches',async()=>{
 let count=0;const key='fake-test-only-key';const metadata=new Metadata(key,async(input:any)=>{count++;const u=new URL(String(input));assert.equal(u.host,'api.themoviedb.org');assert.equal(u.searchParams.get('api_key'),key);assert.equal(u.searchParams.get('language'),'pt-BR');assert(!u.searchParams.has('redirect'));return Response.json({overview:'Sinopse de teste'})});
 const params=new URLSearchParams({language:'pt-BR',api_key:'attacker',redirect:'https://evil.invalid'});assert.equal((await metadata.request('movie/1',params)).overview,'Sinopse de teste');await metadata.request('movie/1',params);assert.equal(count,1);
 await assert.rejects(metadata.request('../configuration',params),/inválida/);await assert.rejects(metadata.request('https://evil.invalid/',params),/inválida/);await assert.rejects(new Metadata(undefined).request('movie/1',params),/configurados/);
});
