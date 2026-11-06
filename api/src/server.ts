import {connect} from './db.js';import {createApp} from './app.js';import {createRuntime} from './runtime.js';
const secret=process.env.JWT_SECRET,adminOrigin=process.env.ADMIN_ORIGIN;
if(!secret||!adminOrigin)throw new Error('Configure JWT_SECRET and ADMIN_ORIGIN');
const runtime=createRuntime();await runtime.listen(Number(process.env.PORT??10000));
let connection:Awaited<ReturnType<typeof connect>>|undefined,app:Awaited<ReturnType<typeof createApp>>|undefined,stopping=false,retry:NodeJS.Timeout|undefined;
async function initialize(){
 const uri=process.env.MONGODB_URI;
 if(!uri||uri.includes('<db_password>')){runtime.setPhase('configuration_required');console.log(JSON.stringify({service:'guhflix-api',state:'configuration_required',missing:'MONGODB_URI'}));return;}
 runtime.setPhase('connecting');
 try{connection=await connect(uri);app=await createApp({...connection,secret:secret!,adminOrigin:adminOrigin!,bootstrapToken:process.env.BOOTSTRAP_TOKEN,logger:true});await app.ready();
 if(stopping){await app.close();await connection.client.close();return;}
 runtime.activate((req,res)=>app!.server.emit('request',req,res));console.log(JSON.stringify({service:'guhflix-api',state:'ready'}));
 }catch{if(connection)await connection.client.close().catch(()=>{});connection=undefined;runtime.setPhase('database_unavailable');console.error(JSON.stringify({service:'guhflix-api',state:'database_unavailable'}));if(!stopping)retry=setTimeout(initialize,15000);}
}
void initialize();
for(const signal of ['SIGTERM','SIGINT'])process.once(signal,async()=>{stopping=true;if(retry)clearTimeout(retry);await runtime.close();if(app)await app.close();if(connection)await connection.client.close();process.exit(0)});
