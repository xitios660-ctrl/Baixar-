package com.guhflix.shell
import com.gfilmes.license.CatalogFilter
import com.gfilmes.license.CatalogRow

/** Normalize titles and sort each scope once, instead of doing it for every typed query. */
class CatalogSearchIndex(private val rows:List<CatalogRow>) {
 private val titles=rows.associate { it.id to CatalogFilter.fold(it.title) }
 private val ordered=mutableMapOf<String,List<CatalogRow>>()
 private val facets=mutableMapOf<String,Pair<List<String>,List<String>>>()
 fun select(kind:String,q:String,genre:String,year:String,sort:String):CatalogViewCache.View {
  require(kind in listOf("","movie","series"));require(sort in listOf("","title_asc","title_desc","year_desc","year_asc"))
  val base=ordered.getOrPut(sort){CatalogFilter.select(rows,"","","","",sort)}
  val needle=CatalogFilter.fold(q)
  val matching=base.filter{(kind.isEmpty()||it.kind==kind)&&(needle.isEmpty()||titles.getValue(it.id).contains(needle))&&(year.isEmpty()||it.year==year)}
  val selected=if(genre.isEmpty())matching else CatalogFilter.select(matching,"","",genre,"","")
  val f=facets.getOrPut(kind){val pool=rows.filter{kind.isEmpty()||it.kind==kind};pool.flatMap{it.genres}.distinct().sorted() to pool.map{it.year}.filter{it.toIntOrNull()?.let{y->y in 1800..2200}==true}.distinct().sortedDescending()}
  return CatalogViewCache.View(selected,f.first,f.second)
 }
}
