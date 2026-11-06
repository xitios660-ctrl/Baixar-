import {connect} from './db.js';import {backup} from './backup.js';
const {MONGODB_URI,BACKUP_KEY,BACKUP_FILE}=process.env;
if(!MONGODB_URI||!BACKUP_KEY||!/^[a-fA-F0-9]{64}$/.test(BACKUP_KEY)||!BACKUP_FILE)throw Error('Set MONGODB_URI, BACKUP_KEY (64 hex chars) and private BACKUP_FILE path');
const {db,client}=await connect(MONGODB_URI);const session=client.startSession({snapshot:true});try{await backup(db,Buffer.from(BACKUP_KEY,'hex'),BACKUP_FILE,session);console.log('Encrypted GUHFLIX backup completed. Store outside Render and separately from encryption key.');}finally{await session.endSession();await client.close()}
