import {connect} from './db.js';import {createApp} from './app.js';
const uri=process.env.MONGODB_URI,secret=process.env.JWT_SECRET,adminOrigin=process.env.ADMIN_ORIGIN;
if(!uri||!secret||!adminOrigin)throw new Error('Configure MONGODB_URI, JWT_SECRET and ADMIN_ORIGIN');
const {client,db}=await connect(uri);const app=await createApp({db,client,secret,adminOrigin,bootstrapToken:process.env.BOOTSTRAP_TOKEN,logger:true});
await app.listen({host:'0.0.0.0',port:Number(process.env.PORT??10000)});
for(const signal of ['SIGTERM','SIGINT'])process.once(signal,async()=>{await app.close();await client.close();process.exit(0)});
