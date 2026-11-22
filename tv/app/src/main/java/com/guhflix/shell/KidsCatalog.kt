package com.guhflix.shell
import com.gfilmes.license.CatalogRow
/** No guessing from genres or animation: only titles explicitly approved by the responsible adult. */
object KidsCatalog {
 fun visible(rows:List<CatalogRow>,kids:Boolean,approved:Set<String>)=if(kids)rows.filter{"vod:"+it.id in approved} else rows
}
