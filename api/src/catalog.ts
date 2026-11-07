import {createHash} from 'node:crypto';
import {Fault} from './errors.js';
import {Metadata} from './metadata.js';

export type CatalogItem={id:string;title:string;kind:'movie'|'series';letter:string;year:string;poster?:string;backdrop?:string;synopsis?:string;genres:string[];tmdbId?:number;seasons?:number[];episodes?:{season:number;episode:number;title?:string}[]};
export type Playable={item:CatalogItem;urls:string[];season?:number;episode?:number};
export interface Catalog { list(q:string,kind:string,offset:number):Promise<{items:CatalogItem[];total:number;categories:string[]}>; detail(id:string):Promise<CatalogItem>; resolve(id:string,season?:number,episode?:number):Promise<Playable>; health():Promise<{available:boolean;reason?:string}>; fileText?(name:string):Promise<string> }
const idFor=(kind:string,title:string,letter:string,year:string)=>createHash('sha256').update([kind,title,letter,year].join('\0')).digest('hex').slice(0,32);
const http=(s:string)=>s.length<=8192&&/^https?:\/\//i.test(s)&&!new URL(s).username&&!new URL(s).password;
const fold=(s:string)=>s.normalize('NFD').replace(/\p{Diacritic}/gu,'').toLocaleLowerCase('pt-BR');
export class ProviderCatalog implements Catalog {
  private files=new Map<string,{expires:number;text:string}>();
  private inFlight=new Map<string,Promise<string>>();
  private items=new Map<string,CatalogItem>();
  private bases:string[]=[];
  private validUntil=0;
  private loading:Promise<void>|undefined;
  constructor(private base=process.env.CATALOG_BASE_URL??'https://filmes-saimo.pages.dev/',private get:typeof fetch=fetch,private metadata=new Metadata()) {
    const u=new URL(base);if(u.protocol!=='https:'&&u.hostname!=='127.0.0.1')throw new Error('Catalog requires HTTPS');
    if(u.username||u.password)throw new Error('Catalog credentials must be server-side headers');
    this.base=base.endsWith('/')?base:base+'/';
  }
  async fileText(name:string){return this.file(name.replace(/%23/gi,'#'))}
  private async file(name:string):Promise<string>{
    if(!/^(?:[a-zA-Z0-9_#-]+\.txt|redeflix\/links-(?:animes|doramas)\.txt)$/.test(name))throw new Fault(400,'CATALOG_PATH','Arquivo de catálogo inválido');
    const cached=this.files.get(name);if(cached&&cached.expires>Date.now())return cached.text;
    const current=this.inFlight.get(name);if(current)return current;
    if(this.inFlight.size>=6)throw new Fault(429,'CATALOG_BUSY','O catálogo está carregando. Tente novamente em instantes');
    const work=(async()=>{
      let r:Response;try{r=await this.get(this.base+name.split('/').map(encodeURIComponent).join('/'),{redirect:'error',signal:AbortSignal.timeout(20000),headers:{'User-Agent':'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15','Accept':'*/*',...(process.env.CATALOG_AUTH_TOKEN?{Authorization:`Bearer ${process.env.CATALOG_AUTH_TOKEN}`}:{})}})}catch{throw new Fault(503,'CATALOG_NETWORK','Não foi possível conectar ao catálogo autorizado. Tente novamente.')}
      if(!r.ok)throw new Fault(503,'CATALOG_PROVIDER',`O catálogo autorizado respondeu HTTP ${r.status}. O provedor precisa permitir esse acesso.`);
      let bytes=0;const chunks:Uint8Array[]=[];
      if(!r.body)throw new Fault(503,'CATALOG_EMPTY','O provedor não enviou o catálogo');
      const reader=r.body.getReader();try{for(;;){const {done,value}=await reader.read();if(done)break;bytes+=value.length;if(bytes>12*1024*1024){await reader.cancel();throw new Fault(503,'CATALOG_SIZE','Catálogo excedeu o limite de leitura')}chunks.push(value)}}finally{reader.releaseLock()}
      const text=Buffer.concat(chunks).toString('utf8');
      if(/<!doctype html|<html/i.test(text.slice(0,300)))throw new Fault(503,'CATALOG_FORMAT','O provedor retornou uma página em vez do catálogo');
      let size=Buffer.byteLength(text);for(const value of this.files.values())size+=Buffer.byteLength(value.text);
      while(this.files.size&&(this.files.size>=24||size>32*1024*1024)){const first=this.files.keys().next().value!;size-=Buffer.byteLength(this.files.get(first)!.text);this.files.delete(first)}
      this.files.set(name,{text,expires:Date.now()+5*60000});return text;
    })();this.inFlight.set(name,work);try{return await work}finally{this.inFlight.delete(name)}
  }
  private async load(){
    if(this.validUntil>Date.now()&&this.items.size)return;
    if(this.loading)return this.loading;
    this.loading=this.loadOnce();try{await this.loading}finally{this.loading=undefined}
  }
  private async loadOnce(){
    if(this.validUntil>Date.now()&&this.items.size)return;
    const [index,search]=await Promise.all([this.file('indice.txt'),this.file('busca.txt')]);
    const bases:string[]=[];for(const line of index.split(/\r?\n/)){const m=/^base:\s*(\d+)\s+(https?:\/\/\S+)/.exec(line);if(m&&http(m[2]))bases[Number(m[1])]=m[2]}
    const items=new Map<string,CatalogItem>();for(const line of search.split(/\r?\n/)){const [title,type,letter,year='']=line.split('\t');if(!title||!['s','f'].includes(type)||!/^([A-ZÀ-Üa-z0-9#]|%23)$/.test(letter))continue;const kind=type==='s'?'series':'movie';const id=idFor(kind,title,letter,year);items.set(id,{id,title,kind,letter,year,genres:[]});if(items.size>=100000)break}
    if(!items.size||!bases.some(Boolean))throw new Fault(503,'CATALOG_EMPTY','O catálogo autorizado não contém títulos ou bases de vídeo válidas');
    // Optional artwork/genres. A failure must not discard playable titles.
    try{const ficha=await this.file('fichas.txt');let artwork='';const byTitle=new Map([...items.values()].map(i=>[(i.kind==='series'?'s':'f')+'|'+i.title,i]));for(const line of ficha.split(/\r?\n/)){if(line.startsWith('capa:')){artwork=line.slice(5).trim();continue}const [type,title,tmdb,poster,genres]=line.split('\t');const item=byTitle.get(type+'|'+title);if(item){if(/^\d+$/.test(tmdb))item.tmdbId=Number(tmdb);item.genres=genres?.split(',').map(s=>s.trim()).filter(Boolean)??[];if(poster&&http(artwork+poster))item.poster=artwork+poster}}}catch{}
    this.bases=bases;this.items=items;this.validUntil=Date.now()+5*60000;
  }
  async health(){try{await this.load();return {available:true}}catch(e){return {available:false,reason:e instanceof Fault?e.code:'CATALOG_UNAVAILABLE'}}}
  async list(q:string,kind:string,offset:number){await this.load();const needle=fold(q);const all=[...this.items.values()];const filtered=all.filter(i=>(!kind||i.kind===kind)&&(!needle||fold(i.title).includes(needle)||i.genres.some(g=>fold(g).includes(needle))));return {items:filtered.slice(offset,offset+60),total:filtered.length,categories:[...new Set(all.flatMap(i=>i.genres))].slice(0,60)}}
  private async item(id:string){await this.load();const item=this.items.get(id);if(!item)throw new Fault(404,'TITLE_NOT_FOUND','Título não encontrado no catálogo autorizado');return item}
  private urls(value:string){return value.split(',').map(v=>{const m=/^(\d+):(.*)$/.exec(v);return m&&this.bases[+m[1]]?this.bases[+m[1]]+m[2]:v}).filter(v=>{try{return http(v)}catch{return false}}).slice(0,16)}
  private letter(i:CatalogItem){return i.letter.replace(/%23/gi,'#')}
  private async series(i:CatalogItem){
    const rows=await this.file(`series-${this.letter(i)}.txt`);const row=rows.split(/\r?\n/).find(l=>{const f=l.split('\t');return f[0]===i.title&&(!i.year||f[1]===i.year)});if(!row)throw new Fault(404,'TITLE_NOT_FOUND','Série indisponível no índice atual');
    const part=row.split('\t')[2];if(!/^\d+$/.test(part))throw new Fault(503,'CATALOG_FORMAT','Índice de episódios inválido');const data=await this.file(`series-${this.letter(i)}-${part}.txt`);let inside=false;const episodes:{season:number;episode:number;title?:string;urls:string[]}[]=[];
    for(const line of data.split(/\r?\n/)){if(line.startsWith('@')){const f=line.slice(1).split('\t');inside=f[0]===i.title&&(!i.year||f[1]===i.year);continue}if(!inside)continue;const [s,e,,links]=line.split('\t');if(!links)continue;const season=+s,episode=+e;if(!Number.isInteger(season)||!Number.isInteger(episode)||season<0||episode<1)continue;const urls=this.urls(links);const old=episodes.find(x=>x.season===season&&x.episode===episode);if(old)old.urls.push(...urls);else episodes.push({season,episode,urls})}return episodes;
  }
  async detail(id:string){let item={...await this.item(id)};if(item.tmdbId)try{const meta=await this.metadata.request((item.kind==='series'?'tv':'movie')+'/'+item.tmdbId,new URLSearchParams({language:'pt-BR'}));item={...item,synopsis:meta.overview||undefined,backdrop:meta.backdrop_path?'https://image.tmdb.org/t/p/w1280'+meta.backdrop_path:undefined}}catch{}if(item.kind==='movie')return {...item};const eps=await this.series(item);return {...item,seasons:[...new Set(eps.map(e=>e.season))],episodes:eps.map(({season,episode})=>({season,episode}))}}
  async resolve(id:string,season?:number,episode?:number){
    const item=await this.item(id);let urls:string[]=[];
    if(item.kind==='series'){if(season===undefined||episode===undefined)throw new Fault(400,'EPISODE_REQUIRED','Escolha a temporada e o episódio');urls=(await this.series(item)).find(e=>e.season===season&&e.episode===episode)?.urls??[]}
    else{const rows=await this.file(`filmes-${this.letter(item)}.txt`);const row=rows.split(/\r?\n/).find(l=>l.split('\t')[0]===item.title);if(row)for(const variant of row.split('\t').slice(1)){const at=variant.indexOf('=');if(at>=0)urls.push(...this.urls(variant.slice(at+1)))}}
    if(!urls.length)throw new Fault(404,'SOURCE_UNAVAILABLE','O catálogo não forneceu uma fonte autorizada para este título');return {item,urls:[...new Set(urls)].slice(0,16),season,episode};
  }
}
