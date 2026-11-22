package com.guhflix.shell
import com.gfilmes.license.CatalogRow
import org.junit.Test
import org.junit.Assert.*
class CatalogViewCacheTest {
 @Test fun pagingReusesSelectionAndFacetsInsteadOfRescanningTheLibrary(){val rows=(0..39999).map{CatalogRow(it.toString(),"Title $it",if(it%2==0)"movie" else "series",year="2025",genres=listOf("Drama"))};var scans=0;val cache=CatalogViewCache();val one=cache.get("adult-movies",{scans++;rows},"movie","","","","title_asc");for(page in 0..20){val next=cache.get("adult-movies",{scans++;rows},"movie","","","","title_asc");assertSame(one,next);assertEquals(60,next.page(page*60).size)};assertEquals(1,scans);assertEquals(listOf("Drama"),one.categories);assertEquals(listOf("2025"),one.years)}
 @Test fun childScopesAndRefreshCannotReuseAdultRows(){val a=CatalogRow("a","Adult title","movie");val b=CatalogRow("b","Approved title","movie");val c=CatalogViewCache();assertEquals(2,c.get("adult",{listOf(a,b)},"movie","","","","").items.size);assertEquals(listOf(b),c.get("child-approved-b",{listOf(b)},"movie","","","","").items);c.clear();assertEquals(listOf(a),c.get("adult",{listOf(a)},"movie","","","","").items)}
 @Test fun pageReadsOnlyItsWindowAndHandlesTheEndOfTheCatalog(){var reads=0;val backing=object:AbstractList<CatalogRow>(){override val size=40000;override fun get(index:Int):CatalogRow{reads++;return CatalogRow(index.toString(),"Title $index","movie")}};val view=CatalogViewCache.View(backing,emptyList(),emptyList());val page=view.page(120);assertEquals(60,page.size);assertEquals("120",page.first().id);assertEquals("179",page.last().id);assertEquals(2,reads);assertTrue(view.page(50000).isEmpty())}
}
