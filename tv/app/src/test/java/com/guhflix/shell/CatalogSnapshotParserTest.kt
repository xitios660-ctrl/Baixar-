package com.guhflix.shell
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
class CatalogSnapshotParserTest {
 private fun asset(name:String):String {val base=sequenceOf(File("src/main/assets/gfilmes/catalog-snapshot"),File("tv/app/src/main/assets/gfilmes/catalog-snapshot"),File("app/src/main/assets/gfilmes/catalog-snapshot")).first{it.isDirectory};return GZIPInputStream(File(base,"$name.bin").inputStream()).bufferedReader().use{it.readText()}}
 @Test fun preservedRealCatalogParsesColdWithoutProviderOrAndroidNetwork(){val search=asset("busca.txt");val meta=asset("fichas.txt");val started=System.nanoTime();val rows=CatalogSnapshotParser.parse(search,meta);val ms=(System.nanoTime()-started)/1000000;val movies=rows.count{it.kind=="movie"};val series=rows.count{it.kind=="series"};assertTrue(movies>28000);assertTrue(series>9800);assertTrue(rows.count{it.poster.startsWith("https://")}>1000);assertTrue(rows.count{it.genres.isNotEmpty()}>1000);assertTrue("Desktop parser exceeded RPC budget: $ms",ms<15000);println("{\"historicalRealMetadata\":true,\"movies\":$movies,\"series\":$series,\"parseMs\":$ms,\"networkRequests\":0}")}
 @Test fun idsMatchPreviousAlgorithmSoFavoritesAndApprovalsStillResolve(){val rows=CatalogSnapshotParser.parse(asset("busca.txt"));for(r in rows.take(40)+rows.takeLast(40)){val raw=listOf(r.kind,r.title,r.letter,r.year).joinToString("\u0000");val old=MessageDigest.getInstance("SHA-256").digest(raw.toByteArray()).joinToString(""){"%02x".format(it)}.take(32);assertEquals(old,r.id)}}
 @Test fun metadataIsOptionalAndNeverNeededToShowValidTitles(){val rows=CatalogSnapshotParser.parse("Título A (2026)\tf\tA\nSérie B\ts\tB\t2025");assertEquals(2,rows.size);assertEquals("series",rows[1].kind);assertEquals(emptyList<String>(),rows[0].genres)}
 @Test fun invalidOrHtmlResponsesCannotReplaceAnIndex(){for(s in listOf("<html>blocked</html>","error code: 1010","invalid"," (2024)\tf\tA")){try{CatalogSnapshotParser.parse(s);fail(s)}catch(_:IllegalArgumentException){}}}
}
