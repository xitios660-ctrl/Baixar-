import {Fault} from './errors.js';
const allowed=/^(?:(?:movie|tv)\/\d+(?:\/(?:season\/\d+|recommendations|similar|credits|release_dates|content_ratings|images))?|trending\/(?:movie|tv)\/week|collection\/\d+|discover\/(?:movie|tv)|person\/\d+(?:\/combined_credits)?|search\/(?:movie|tv|person|multi))$/;
const params=new Set(['language','append_to_response','include_image_language','query','year','first_air_date_year','include_adult','page','with_companies','sort_by']);
export class Metadata {
 private cache=new Map<string,{until:number;value:any}>();
 constructor(private key=process.env.TMDB_API_KEY,private get:typeof fetch=fetch){}
 async request(path:string,query:URLSearchParams){
  if(!allowed.test(path)||path.length>130)throw new Fault(400,'METADATA_PATH','Consulta de metadados inválida');
  if(!this.key)throw new Fault(503,'METADATA_CONFIG','Metadados adicionais ainda não foram configurados');
  const url=new URL('https://api.themoviedb.org/3/'+path);
  for(const [k,v] of query){if(params.has(k)&&v.length<=300)url.searchParams.set(k,v)}
  const cacheKey=url.toString(),hit=this.cache.get(cacheKey);if(hit&&hit.until>Date.now())return hit.value;
  url.searchParams.set('api_key',this.key);
  let r:Response;try{r=await this.get(url,{signal:AbortSignal.timeout(12000),redirect:'error'})}catch{throw new Fault(503,'METADATA_NETWORK','Os detalhes do título estão temporariamente indisponíveis')}
  if(!r.ok)throw new Fault(503,'METADATA_PROVIDER','O provedor de metadados não disponibilizou os detalhes');
  const text=await r.text();if(text.length>2*1024*1024)throw new Fault(503,'METADATA_SIZE','Resposta de metadados excedeu o limite');let value:any;try{value=JSON.parse(text)}catch{throw new Fault(503,'METADATA_FORMAT','Resposta de metadados inválida')}
  if(this.cache.size>=200)this.cache.delete(this.cache.keys().next().value!);this.cache.set(cacheKey,{until:Date.now()+3600000,value});return value;
 }
}
