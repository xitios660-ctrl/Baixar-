import { MongoClient, type Db } from 'mongodb';
export async function connect(uri:string,name='GUHFLIX') {
 if(!/^GUHFLIX(?:_TEST)?$/.test(name))throw new Error('Exclusive GUHFLIX database required');
 // One long-lived client. Low-traffic, single Render instance; use driver defaults until measured.
 const client=new MongoClient(uri,{appName:'GUHFLIX'});await client.connect();const db=client.db(name);await indexes(db);return {client,db};
}
export async function indexes(db:Db){
 await Promise.all([
 db.collection('users').createIndex({username:1},{unique:true}),
 db.collection('subscriptions').createIndex({userId:1,createdAt:-1}),
 db.collection('payments').createIndex({userId:1,confirmedAt:-1}),
 db.collection('sessions').createIndex({refreshHash:1},{unique:true}),
 db.collection('sessions').createIndex({userId:1,revoked:1}),
 db.collection('sessions').createIndex({expiresAt:1},{expireAfterSeconds:0}),
 db.collection('devices').createIndex({userId:1,installationId:1},{unique:true}),
 db.collection('idempotency_keys').createIndex({actorId:1,key:1},{unique:true}),
 db.collection('audit_logs').createIndex({createdAt:-1}),
 db.collection('audit_logs').createIndex({targetUserId:1,createdAt:-1})]);
}
