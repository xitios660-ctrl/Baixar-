package com.guhflix.shell
import com.gfilmes.license.CatalogRow
import com.gfilmes.license.CatalogFilter
/** One immutable plan per filter set, shared by pages. Clear when catalog metadata changes. */
class CatalogViewCache(private val limit:Int=12) {
 data class View(val items:List<CatalogRow>,val categories:List<String>,val years:List<String>){
  fun page(offset:Int):List<CatalogRow>{val start=offset.coerceIn(0,items.size);return items.subList(start,minOf(items.size,start+60))}
 }
 private val plans=object:LinkedHashMap<String,View>(16,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,View>)=size>limit}
 private val scopes=object:LinkedHashMap<String,CatalogSearchIndex>(4,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,CatalogSearchIndex>)=size>4}
 @Synchronized fun get(key:String,rows:()->List<CatalogRow>,kind:String,q:String,genre:String,year:String,sort:String):View = plans[key]?:run{
  val scope=key.substringBefore('\u0000')
  val index=scopes.getOrPut(scope){CatalogSearchIndex(rows())}
  index.select(kind,q,genre,year,sort).also{plans[key]=it}
 }
 @Synchronized fun clear(){plans.clear();scopes.clear()}
}
