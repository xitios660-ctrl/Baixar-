package com.guhflix.shell
import com.gfilmes.license.CatalogRow
import com.gfilmes.license.CatalogFilter
import org.junit.Test
import org.junit.Assert.*
class CatalogSearchIndexTest {
 @Test fun combinedFiltersAndAllSortsMatchExistingCatalogRules(){
  val rows=listOf(CatalogRow("a","Ação Final","movie",year="2025",genres=listOf("Ação")),CatalogRow("b","Ação Final","series",year="2024",genres=listOf("Ação")),CatalogRow("c","#Alive","movie",year="2020",genres=listOf("Terror")),CatalogRow("d","Os Simpsons","series",year="1989",genres=listOf("Animação")))
  val index=CatalogSearchIndex(rows)
  for(kind in listOf("","movie","series"))for(q in listOf("","acao","simpsons","alive"))for(genre in listOf("","Ação","Horror"))for(year in listOf("","2025"))for(sort in listOf("","title_asc","title_desc","year_asc","year_desc"))assertEquals(CatalogFilter.select(rows,kind,q,genre,year,sort),index.select(kind,q,genre,year,sort).items)
 }
 @Test fun successiveKeystrokesReuseTheSameScopeAndClearInvalidatesIt(){val cache=CatalogViewCache();var reads=0;val rows=listOf(CatalogRow("a","Os Simpsons","series"),CatalogRow("b","#Alive","movie"));for(q in listOf("s","si","sim","simpsons"))assertEquals(listOf(rows[0]),cache.get("adult\u0000$q",{reads++;rows},"series",q,"","","title_asc").items);assertEquals(1,reads);cache.clear();cache.get("adult\u0000sim",{reads++;rows},"series","sim","","","title_asc");assertEquals(2,reads)}
 @Test fun largeCatalogWarmQueriesAvoidRepeatedNormalizationAndSorting(){val rows=(0..39999).map{CatalogRow(it.toString(),"Filme de Ação $it",if(it%2==0)"movie" else "series",year="2025")};val index=CatalogSearchIndex(rows);index.select("movie","","","","title_asc");val start=System.nanoTime();repeat(20){val found=index.select("movie","acao 12","","2025","title_asc");assertTrue(found.items.isNotEmpty());assertTrue(found.items.all{it.kind=="movie"})};val ms=(System.nanoTime()-start)/1000000;println("warmSearches=20 rows=40000 elapsedMs=$ms desktopOnly=true");assertTrue("Warm query batch exceeded 10 s: $ms",ms<10000)}
}
