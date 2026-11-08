import {lookup} from 'node:dns/promises';import {isIP} from 'node:net';import http from 'node:http';import https from 'node:https';import {Readable} from 'node:stream';import {Fault} from './errors.js';
export type MediaOpen=(url:URL,range?:string)=>Promise<{response:Response;url:URL}>;
export function publicAddress(address:string):boolean{
 let a=address.toLowerCase();if(a.startsWith('::ffff:'))a=a.slice(7);
 if(isIP(a)===4){const p=a.split('.').map(Number);return !(p[0]===0||p[0]===10||p[0]===127||p[0]>=224||p[0]===169&&p[1]===254||p[0]===172&&p[1]>=16&&p[1]<=31||p[0]===192&&(p[1]===168||p[1]===0||p[1]===2)||p[0]===100&&p[1]>=64&&p[1]<=127||p[0]===198&&(p[1]===18||p[1]===19||p[1]===51)||p[0]===203&&p[1]===0)}
 return isIP(a)===6&&/^[23]/.test(a)&&!a.startsWith('2001:db8:')&&!a.startsWith('2001:0:')&&!a.startsWith('2002:');
}
export const openMedia:MediaOpen=async function open(url,range){
 for(let redirects=0;redirects<=4;redirects++){
  if(!['http:','https:'].includes(url.protocol)||url.username||url.password||url.href.length>4096)throw new Fault(400,'MEDIA_URL','Fonte incompatível com este player');
  const hostname=url.hostname.replace(/^\[|\]$/g,'');const addresses=await lookup(hostname,{all:true}).catch(()=>[]);if(!addresses.length||addresses.some(v=>!publicAddress(v.address)))throw new Fault(403,'MEDIA_ADDRESS','Endereço de mídia não permitido');
  const chosen=addresses[0];const result=await new Promise<http.IncomingMessage>((resolve,reject)=>{
   const request=(url.protocol==='https:'?https:http).get(url,{headers:{'User-Agent':'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15','Accept':'*/*','Accept-Encoding':'identity',...(range?{Range:range}:{})},lookup:((_host:any,_options:any,callback:any)=>_options?.all?callback(null,[chosen]):callback(null,chosen.address,chosen.family)) as any,timeout:30000},resolve);request.on('timeout',()=>request.destroy(new Error('Media timeout')));request.on('error',()=>reject(new Fault(503,'MEDIA_NETWORK','A fonte não respondeu. Tentaremos outra.')));
  });
  if([301,302,303,307,308].includes(result.statusCode??0)&&result.headers.location){result.destroy();url=new URL(result.headers.location,url);continue}
  const headers=new Headers();for(const key of ['content-type','content-length','content-range','accept-ranges']){const v=result.headers[key];if(typeof v==='string')headers.set(key,v)}
  if(result.statusCode!==200&&result.statusCode!==206){result.destroy();throw new Fault(503,'MEDIA_SOURCE','Esta fonte não está disponível. Tentaremos outra.')}
  return {response:new Response(Readable.toWeb(result) as any,{status:result.statusCode,headers}),url};
 }
 throw new Fault(503,'MEDIA_REDIRECT','A fonte não concluiu a conexão');
};
export function rewritePlaylist(text:string,base:URL,resource:(url:URL)=>string){
 if(!text.trimStart().startsWith('#EXTM3U'))throw new Fault(503,'MEDIA_FORMAT','Formato de vídeo incompatível');
 const link=(value:string)=>value.startsWith('data:')?value:resource(new URL(value,base));
 return text.split(/\r?\n/).map(line=>line.startsWith('#')?line.replace(/URI="([^"]+)"/g,(_all,v)=>'URI="'+link(v)+'"'):line.trim()?link(line.trim()):line).join('\n');
}
