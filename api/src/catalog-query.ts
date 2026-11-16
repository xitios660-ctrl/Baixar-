import type {CatalogItem} from './catalog.js';
export type CatalogSort='title_asc'|'title_desc'|'year_desc'|'year_asc';
export type CatalogFilters={genre?:string;year?:number;sort?:CatalogSort;titleOnly?:boolean};
export type CatalogPage={items:CatalogItem[];total:number;categories:string[];years?:number[];pageSize?:number;nextOffset?:number|null;availableSorts?:CatalogSort[]};
const fold=(s:string)=>s.normalize('NFD').replace(/\p{Diacritic}/gu,'').toLocaleLowerCase('pt-BR').trim();
const aliases:Record<string,string>={'science fiction':'ficcao cientifica','sci-fi':'ficcao cientifica','sci fi':'ficcao cientifica','documentarios':'documentario','documentaries':'documentario','animation':'animacao','family':'familia','adventure':'aventura','comedy':'comedia','horror':'terror','romance':'romance','thriller':'suspense','action':'acao'};
export const genreKey=(s:string)=>aliases[fold(s)]??fold(s);
const yearOf=(item:CatalogItem)=>/^\d{4}$/.test(item.year)&&Number(item.year)>=1800&&Number(item.year)<=2200?Number(item.year):0;
const sorts:CatalogSort[]=['title_asc','title_desc','year_desc','year_asc'];
/** Immutable metadata snapshot. At most twelve filtered/sorted views; each response is sixty items. */
export class CatalogQuery {
 private views=new Map<string,CatalogItem[]>();
 private facets=new Map<string,{items:CatalogItem[];categories:string[];years:number[]}>();
 private collator=new Intl.Collator('pt-BR',{sensitivity:'base',numeric:true});
 constructor(private items:CatalogItem[]){}
 private facet(kind:string){
  let value=this.facets.get(kind);if(value)return value;
  const items=this.items.filter(i=>!kind||(kind==='anime'?i.collection===true:i.kind===kind));
  const genres=new Map<string,string>();for(const i of items)for(const g of i.genres)if(g.trim()&&!genres.has(genreKey(g)))genres.set(genreKey(g),g);
  value={items,categories:[...genres.values()].sort(this.collator.compare),years:[...new Set(items.map(yearOf).filter(Boolean))].sort((a,b)=>b-a)};
  this.facets.set(kind,value);return value;
 }
 page(q:string,kind:string,offset:number,filters:CatalogFilters={}):CatalogPage{
  const facet=this.facet(kind),needle=fold(q),genre=genreKey(filters.genre??''),sort=filters.sort??'catalog';
  const key=JSON.stringify([needle,kind,genre,filters.year??0,sort,!!filters.titleOnly]);
  let rows=this.views.get(key);
  if(rows){this.views.delete(key);this.views.set(key,rows)}else{
   rows=facet.items.filter(i=>(!needle||fold(i.title).includes(needle)||!filters.titleOnly&&i.genres.some(g=>fold(g).includes(needle)))&&(!genre||i.genres.some(g=>genreKey(g)===genre))&&(!filters.year||yearOf(i)===filters.year));
   if(sort!=='catalog')rows.sort((a,b)=>{const title=this.collator.compare(a.title,b.title)||a.id.localeCompare(b.id);if(sort==='title_desc')return -title;if(sort==='title_asc')return title;const ay=yearOf(a),by=yearOf(b);if(!ay||!by)return ay? -1:by?1:title;return (sort==='year_desc'?by-ay:ay-by)||title});
   if(this.views.size>=12)this.views.delete(this.views.keys().next().value!);this.views.set(key,rows);
  }
  const page=rows.slice(offset,offset+60),next=offset+page.length;
  return {items:page,total:rows.length,categories:facet.categories,years:facet.years,pageSize:60,nextOffset:next<rows.length?next:null,availableSorts:facet.years.length?sorts:sorts.filter(s=>s.startsWith('title_'))};
 }
}
