import {createCipheriv,randomBytes} from 'node:crypto';import {createGzip} from 'node:zlib';import {createWriteStream} from 'node:fs';import {writeFile,appendFile} from 'node:fs/promises';import {Readable} from 'node:stream';import {pipeline} from 'node:stream/promises';import {BSON,type Db,type ClientSession} from 'mongodb';const {EJSON}=BSON;
export const collections=['users','subscriptions','payments','sessions','devices','idempotency_keys','audit_logs'];
export async function backup(db:Db,key:Buffer,file:string,session:ClientSession){
 if(key.length!==32||!/^guhflix(?:_test)?$/.test(db.databaseName))throw Error('Exclusive database and 256-bit key required');
 const iv=randomBytes(12),cipher=createCipheriv('aes-256-gcm',key,iv);await writeFile(file,Buffer.concat([Buffer.from('GUHFLIX1'),iv]),{flag:'wx',mode:0o600});
 async function* records(){yield EJSON.stringify({format:'GUHFLIX1',database:db.databaseName,createdAt:new Date()})+'\n';for(const collection of collections)for await(const document of db.collection(collection).find({},{session}))yield EJSON.stringify({collection,document})+'\n';}
 await pipeline(Readable.from(records()),createGzip(),cipher,createWriteStream(file,{flags:'a',mode:0o600}));await appendFile(file,cipher.getAuthTag());
}
