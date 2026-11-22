package com.gfilmes.license
import java.text.Normalizer
import java.text.Collator
import java.util.Locale

data class CatalogRow(val id:String,val title:String,val kind:String,val letter:String="",val year:String="",val genres:List<String> = emptyList(),val poster:String="",val tmdbId:Int=0)
object CatalogFilter {
 fun fold(s:String)=Normalizer.normalize(s,Normalizer.Form.NFD).replace(Regex("\\p{M}+"),"").lowercase(Locale.ROOT).trim()
 private fun genreKey(s:String):String {val k=fold(s);return mapOf("action" to "acao","comedy" to "comedia","horror" to "terror","animation" to "animacao","family" to "familia","adventure" to "aventura","science fiction" to "ficcao cientifica","documentarios" to "documentario")[k]?:k}
 fun select(rows:List<CatalogRow>,kind:String,q:String,genre:String,year:String,sort:String):List<CatalogRow>{
  require(kind in listOf("","movie","series"));require(sort in listOf("","title_asc","title_desc","year_desc","year_asc"));val needle=fold(q);val g=genreKey(genre)
  val filtered=rows.filter{(kind.isBlank()||it.kind==kind)&&(needle.isBlank()||fold(it.title).contains(needle))&&(g.isBlank()||it.genres.any{v->genreKey(v)==g})&&(year.isBlank()||it.year==year)}
  if(sort.isBlank())return filtered
  val collator=Collator.getInstance(Locale("pt","BR")).apply{strength=Collator.PRIMARY}
  return filtered.sortedWith(Comparator {a,b->val title=collator.compare(a.title,b.title).takeIf{it!=0}?:a.id.compareTo(b.id);when(sort){"title_desc"-> -title;"year_desc","year_asc"->{val ay=a.year.toIntOrNull()?.takeIf{it in 1800..2200};val by=b.year.toIntOrNull()?.takeIf{it in 1800..2200};if(ay==null||by==null){if(ay!=null)-1 else if(by!=null)1 else title}else (if(sort=="year_desc")by.compareTo(ay) else ay.compareTo(by)).takeIf{it!=0}?:title};else->title}})
 }
}
