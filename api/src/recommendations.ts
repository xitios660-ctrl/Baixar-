import type {CatalogItem} from './catalog.js';
import type {Metadata} from './metadata.js';
export type Related={label:string;items:(CatalogItem&{reason?:string})[]};
/** Recommendations are intersected with the authorized catalog; no invented availability. */
export async function recommend(source:CatalogItem,all:CatalogItem[],metadata:Metadata):Promise<Related>{
 const scores=new Map<string,{score:number;reason:string}>();const byTmdb=new Map<number,CatalogItem[]>();
 for(const item of all){if(item.id===source.id||item.kind!==source.kind)continue;const group=byTmdb.get(item.tmdbId??0)??[];group.push(item);byTmdb.set(item.tmdbId??0,group)}
 const add=(ids:any[],score:number,reason:string)=>{for(const entry of ids)for(const item of byTmdb.get(Number(entry?.id))??[]){const old=scores.get(item.id);if(!old||old.score<score)scores.set(item.id,{score,reason})}};
 let label='Mais como este';
 if(source.tmdbId)try{
  const kind=source.kind==='series'?'tv':'movie';const meta=await metadata.request(`${kind}/${source.tmdbId}`,new URLSearchParams({language:'pt-BR',append_to_response:'recommendations,similar'}));
  add(meta.recommendations?.results??[],220,'Recomendado para este título');add(meta.similar?.results??[],160,'Histórias semelhantes');
  if(meta.belongs_to_collection?.id){try{const collection=await metadata.request(`collection/${meta.belongs_to_collection.id}`,new URLSearchParams({language:'pt-BR'}));add(collection.parts??[],1000,'Da mesma coleção');label='Continue nesta coleção'}catch{}}
  const companies=(meta.production_companies??[]).filter((company:any)=>Number.isInteger(company.id)&&/marvel|^dc (films|studios|entertainment)$/i.test(company.name??''));
  if(companies.length){const universe=companies.some((c:any)=>/marvel/i.test(c.name))?'Marvel':'DC';try{const discoveries=await metadata.request(`discover/${kind}`,new URLSearchParams({language:'pt-BR',with_companies:companies.map((c:any)=>c.id).join('|'),sort_by:'popularity.desc',include_adult:'false'}));add(discoveries.results??[],800,`Do universo ${universe}`);label=`Mais do universo ${universe}`}catch{}}
 }catch{}
 for(const item of all){if(item.id===source.id||item.kind!==source.kind)continue;const common=item.genres.filter(g=>source.genres.includes(g)).length;if(common&&!scores.has(item.id))scores.set(item.id,{score:common*12,reason:'Gêneros que combinam com este título'})}
 const items=all.filter(i=>scores.has(i.id)).sort((a,b)=>scores.get(b.id)!.score-scores.get(a.id)!.score||a.title.localeCompare(b.title,'pt-BR')).slice(0,20).map(i=>({...i,reason:scores.get(i.id)!.reason}));
 const universe=items.find(i=>i.reason?.startsWith("Do universo "));label=universe?"Mais "+universe.reason!.slice(0,1).toLowerCase()+universe.reason!.slice(1):items.some(i=>i.reason==='Da mesma coleção')?'Continue nesta coleção':'Mais como este';
 return {label,items};
}
