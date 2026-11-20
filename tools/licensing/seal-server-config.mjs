#!/usr/bin/env node
/** Wrap an existing private TLS PKCS12 for the TV; plaintext never goes through the LAN portal. */
import {createPublicKey,createHash,randomBytes,publicEncrypt,createCipheriv,constants} from 'node:crypto';
import {readFile,writeFile,mkdir,rm} from 'node:fs/promises';import {resolve,dirname} from 'node:path';import {spawnSync} from 'node:child_process';
const [publicFile,output]=process.argv.slice(2);
if(!publicFile||!output||!process.env.GFILMES_TLS_P12_FILE||!process.env.GFILMES_TLS_PASSWORD_FILE)throw Error('Provide public-key file and private output path; set GFILMES_TLS_P12_FILE and GFILMES_TLS_PASSWORD_FILE privately');
const out=resolve(output);if(out.startsWith(resolve('.')+'/')&&!out.startsWith(resolve('.private')+'/'))throw Error('Output must be outside repository or ignored .private/');
try{
 const raw=Buffer.from((await readFile(publicFile,'utf8')).trim(),'base64url'),pub=createPublicKey({key:raw,format:'der',type:'spki'});if(pub.asymmetricKeyType!=='rsa'||pub.asymmetricKeyDetails.modulusLength<2048)throw Error();
 const p12=await readFile(process.env.GFILMES_TLS_P12_FILE);if(p12.length>128*1024)throw Error();const password=(await readFile(process.env.GFILMES_TLS_PASSWORD_FILE,'utf8')).replace(/\r?\n$/,'');
 const plain=Buffer.from(JSON.stringify({v:1,kind:'gfilmes-server-config',tls:{p12:p12.toString('base64url'),password}}));
 const aes=randomBytes(32),iv=randomBytes(12),cipher=createCipheriv('aes-256-gcm',aes,iv);const encrypted=Buffer.concat([cipher.update(plain),cipher.final(),cipher.getAuthTag()]);const wrapped=publicEncrypt({key:pub,padding:constants.RSA_PKCS1_OAEP_PADDING,oaepHash:'sha1'},aes);plain.fill(0);aes.fill(0);p12.fill(0);
 const pack=Buffer.from(JSON.stringify({deviceId:createHash('sha256').update(raw).digest('hex'),key:wrapped.toString('base64url'),iv:iv.toString('base64url'),encrypted:encrypted.toString('base64url')}));
 await mkdir(dirname(out),{recursive:true,mode:0o700});const temp=out+'.encrypted.tmp',sig=out+'.signature.tmp';await writeFile(temp,pack,{mode:0o600,flag:'wx'});
 try{const java=process.env.JAVA_HOME?resolve(process.env.JAVA_HOME,'bin/java'):'java';const result=spawnSync(java,['tools/licensing/AuthorityTool.java','snapshot-sign','tv',temp,sig],{stdio:['ignore','ignore','pipe']});if(result.status!==0)throw Error();const envelope=Buffer.from(JSON.stringify({data:pack.toString('base64url'),signature:(await readFile(sig,'utf8')).trim()}));await writeFile(out,envelope,{mode:0o600,flag:'wx'});console.log(JSON.stringify({encrypted:true,ownerSigned:true,sha256:createHash('sha256').update(envelope).digest('hex')}));}finally{await rm(temp,{force:true});await rm(sig,{force:true});}
}catch{console.error('Configuração não gerada. Verifique arquivos privados e assinatura original; nenhum segredo foi registrado.');process.exitCode=1;}
