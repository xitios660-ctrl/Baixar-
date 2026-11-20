#!/usr/bin/env node
/** Optional FREE temporary Cloudflare Quick Tunnel for desktop activation, never video. */
import {spawn} from 'node:child_process';
if(process.env.GFILMES_ACTIVATION_ONLY!=='1')throw Error('Start the desktop API with GFILMES_ACTIVATION_ONLY=1; this option must also be set here');
const url=new URL(process.env.LOCAL_ACTIVATION_URL??'http://127.0.0.1:10000');if(url.hostname!=='127.0.0.1'||url.protocol!=='http:')throw Error('Only local loopback upstream allowed');
const response=await fetch(new URL('/api/health',url));const health=await response.json();if(!response.ok||health.activationOnly!==true)throw Error('Upstream is not activation-only; tunnel refused');
console.log('Temporary FREE URL will appear below. URL may change at each restart; no availability guarantee. No video relay enabled.');
const child=spawn('cloudflared',['tunnel','--protocol','http2','--url',url.origin,'--no-autoupdate'],{stdio:'inherit'});child.on('error',()=>{console.error('Install official cloudflared on this computer first; no third-party binary was downloaded.');process.exitCode=1;});for(const signal of ['SIGINT','SIGTERM'])process.on(signal,()=>child.kill(signal));child.on('exit',code=>process.exitCode=code??1);
