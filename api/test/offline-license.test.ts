import {test} from 'node:test';
import assert from 'node:assert/strict';
import {generateKeyPairSync, verify} from 'node:crypto';
import {issueOfflinePermit} from '../src/offline-license.js';

const userId = '0123456789abcdef01234567';
const device = '11111111-2222-3333-4444-555555555555';
const now = 1_795_100_000, day = 86400;
const keys = generateKeyPairSync('rsa', {
  modulusLength: 2048,
  publicKeyEncoding: {type:'spki',format:'pem'},
  privateKeyEncoding: {type:'pkcs8',format:'pem'},
});

function unpack(token:string) {
  const fields=token.split('.');
  assert.equal(fields[0],'GFL1');
  assert.equal(fields.length,3);
  const payload=Buffer.from(fields[1],'base64url').toString('utf8').split('|');
  assert(verify('RSA-SHA256',Buffer.from(fields[0]+'.'+fields[1],'ascii'),
    keys.publicKey,Buffer.from(fields[2],'base64url')));
  return payload;
}
test('issuer uses the real paid end time for less than 30 days',()=>{
  const signed=issueOfflinePermit(keys.privateKey,userId,device,now,now+7*day);
  const parts=unpack(signed.token);
  assert.equal(signed.expiresAt,now+7*day);
  assert.deepEqual(parts.slice(0,6),['1',userId,device,String(now),String(now),String(now+7*day)]);
});
test('issuer caps an active account at 30 days even when prepaid beyond',()=>{
  const signed=issueOfflinePermit(keys.privateKey,userId,device,now,now+90*day);
  assert.equal(signed.expiresAt,now+30*day);
  assert.equal(unpack(signed.token)[5],String(now+30*day));
});
test('invalid and expired accounts cannot issue a signed grant',()=>{
  assert.throws(()=>issueOfflinePermit(keys.privateKey,userId,device,now,now-1));
  assert.throws(()=>issueOfflinePermit(keys.privateKey,userId,device,now,now));
  assert.throws(()=>issueOfflinePermit(keys.privateKey,'not-an-id',device,now,now+day));
  assert.throws(()=>issueOfflinePermit(keys.privateKey,userId,'not-a-uuid',now,now+day));
});
test('nonce is unique, and modifying payload invalidates signature',()=>{
  const one=issueOfflinePermit(keys.privateKey,userId,device,now,now+day).token;
  const two=issueOfflinePermit(keys.privateKey,userId,device,now,now+day).token;
  assert.notEqual(one,two);
  const fields=one.split('.');
  const altered='GFL1.'+Buffer.from('1|'+userId+'|'+device+'|'+now+'|'+now+'|'+(now+2*day)+'|fake','utf8').toString('base64url');
  assert.equal(verify('RSA-SHA256',Buffer.from(altered,'ascii'),keys.publicKey,
    Buffer.from(fields[2],'base64url')),false);
});
