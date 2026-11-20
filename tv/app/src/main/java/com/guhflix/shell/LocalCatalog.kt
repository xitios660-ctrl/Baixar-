package com.guhflix.shell
import android.content.Context
import android.net.Uri
import br.com.saimo.tv.*
import com.gfilmes.license.CatalogFilter
import com.gfilmes.license.CatalogRow
import com.gfilmes.license.Crypto
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.LinkedHashMap

/** Same authorized public source files as the native player. No source password or API token. */
object LocalCatalog {
 private val indexLock=Any()
 @Volatile private var rows:List<CatalogRow> = emptyList();private var byId:Map<String,CatalogRow> = emptyMap();private var loadedAt=0L;private var sourceKey=""
 private val views=object:LinkedHashMap<String,List<CatalogRow>>(16,.75f,true){override fun removeEldestEntry(e:MutableMap.MutableEntry<String,List<CatalogRow>>)=size>12}
 @Synchronized fun file(ctx:Context,name:String):String {
  val safe=Uri.decode(name).replace("%23","#");require(safe.matches(Regex("(?:[A-Za-z0-9_#-]+\\.txt|redeflix/links-(?:animes|doramas)\\.txt)")))
  val base=Enderecos.filmes?:error("Catálogo desativado");val sourceHash=Crypto.sha(base.toByteArray()).take(16);val folder=File(ctx.filesDir,"offline-catalog").apply{mkdirs()};val cache=File(folder,sourceHash+"_"+safe.replace('/','_'));val limit=if(safe.startsWith("redeflix/links-"))32*1024*1024 else 12*1024*1024
  if(cache.isFile&&System.currentTimeMillis()-cache.lastModified()<6*3600000)return cache.readText()
  try{
   val uri=java.net.URI(base);require(uri.scheme=="https"&&uri.userInfo==null&&uri.query==null&&uri.fragment==null)
   val connection=URL(base+safe.split('/').joinToString("/"){Uri.encode(it)}).openConnection() as HttpURLConnection
   try{connection.connectTimeout=10000;connection.readTimeout=15000;connection.instanceFollowRedirects=false;check(connection.responseCode==200);val text=connection.inputStream.use{input->val out=java.io.ByteArrayOutputStream();val b=ByteArray(16384);while(true){val n=input.read(b);if(n<0)break;check(out.size()+n<=limit);out.write(b,0,n)};out.toString("UTF-8")};check(!text.take(300).contains("<html",true));val tmp=File(folder,cache.name+".tmp");tmp.writeText(text);check(tmp.renameTo(cache));folder.listFiles()?.filter{it!=cache}?.sortedBy{it.lastModified()}?.let{old->var bytes=folder.listFiles()?.sumOf{it.length()}?:0;for(f in old){if(bytes<=96*1024*1024)break;bytes-=f.length();f.delete()}};return text}finally{connection.disconnect()}
  }catch(e:Exception){if(cache.isFile)return cache.readText();throw e}
 }
 private fun index(ctx:Context)=synchronized(indexLock){if(sourceKey==Enderecos.filmes&&rows.isNotEmpty()&&System.currentTimeMillis()-loadedAt<6*3600000)return
  val next=runBlocking{Vod.indice(ctx);Generos.carregar(ctx);(Vod.todos(ctx,false)+Vod.todos(ctx,true)).map{a->val kind=if(a.serie)"series" else "movie";CatalogRow(Crypto.sha(listOf(kind,a.titulo,a.letra,a.ano).joinToString("\u0000").toByteArray()).take(32),a.titulo,kind,a.letra,a.ano,Generos.de(a.titulo,a.serie))}}
  check(next.isNotEmpty()){ "Nenhum catálogo em cache. Conecte à internet para atualizar as fontes autorizadas." };rows=next;byId=next.associateBy{it.id};views.clear();sourceKey=Enderecos.filmes.orEmpty();loadedAt=System.currentTimeMillis()
 }
 private fun item(r:CatalogRow):JSONObject=JSONObject().put("id",r.id).put("title",r.title).put("kind",r.kind).put("year",r.year).put("letter",r.letter).put("genres",JSONArray(r.genres)).put("poster",Generos.capa(r.title,r.kind=="series")?:"").put("tmdbId",Generos.id(r.title,r.kind=="series")?:0)
 private fun target(r:CatalogRow)=Alvo(r.title,r.kind=="series",r.letter,r.year,tmdbId=Generos.id(r.title,r.kind=="series")?:0)
 private fun resolve(ctx:Context,r:CatalogRow)=runBlocking{Titulos.resolver(ctx,target(r))}?:error("Título indisponível na fonte autorizada")
 private fun detail(ctx:Context,r:CatalogRow):JSONObject {val j=item(r);if(r.kind=="series"){val resolved=resolve(ctx,r);j.put("seasons",JSONArray(resolved.temporadas));j.put("episodes",JSONArray().also{a->resolved.episodios.sortedWith(compareBy({it.temporada},{it.numero})).forEach{a.put(JSONObject().put("season",it.temporada).put("episode",it.numero))}})};return j}
 private fun channels(ctx:Context):List<Channel>{Remote.loadCached(ctx);if(Remote.channels.isEmpty())runBlocking{Remote.refresh(ctx)};return Remote.channels.filter{it.sources.any{source->source.key==null&&source.keyId==null&&source.referer==null&&source.userAgent==null&&source.url.startsWith("https://")}}}
 private fun liveId(c:Channel)=Crypto.sha(("live\u0000"+c.name).toByteArray()).take(32)
 private fun live(c:Channel)=JSONObject().put("id",liveId(c)).put("title",c.name).put("kind","live").put("genres",JSONArray(listOfNotNull(c.categoria))).put("poster",c.logo?:"")
 fun route(ctx:Context,method:String,path:String,body:JSONObject?):JSONObject {
  val uri=Uri.parse("https://local.invalid"+path);val p=uri.path.orEmpty();val q=uri.getQueryParameter("q").orEmpty();val offset=(uri.getQueryParameter("offset")?.toIntOrNull()?:0).coerceIn(0,100000)
  if(p.startsWith("/api/connect/live")){val all=channels(ctx);if(p=="/api/connect/live"){val selected=all.filter{CatalogFilter.fold(it.name+" "+it.categoria.orEmpty()).contains(CatalogFilter.fold(q))};return JSONObject().put("items",JSONArray(selected.drop(offset).take(60).map{live(it)})).put("total",selected.size).put("categories",JSONArray(all.mapNotNull{it.categoria}.distinct()))};val id=p.substringAfter("/api/connect/live/").substringBefore('/');val channel=all.firstOrNull{liveId(it)==id}?:error("Canal indisponível");if(p.endsWith("/play"))return JSONObject().put("item",live(channel)).put("urls",JSONArray(channel.sources.filter{it.key==null&&it.keyId==null&&it.referer==null&&it.userAgent==null&&it.url.startsWith("https://")}.map{it.url}));return live(channel)}
  if(p=="/api/connect/pairing"||p.contains("/devices"))throw Gate.ApiError(503,"ONLINE_REMOTE","O controle pela nuvem exige um serviço de coordenação; não é necessário para assistir localmente")
  index(ctx)
  if(p=="/api/connect/home"){val first=rows.firstOrNull{it.kind=="movie"}?:rows.first();return JSONObject().put("featured",item(first)).put("rows",JSONArray().put(JSONObject().put("title","Filmes no seu catálogo").put("items",JSONArray(rows.filter{it.kind=="movie"}.take(18).map{item(it)}))).put(JSONObject().put("title","Séries no seu catálogo").put("items",JSONArray(rows.filter{it.kind=="series"}.take(18).map{item(it)}))))}
  if(p=="/api/connect/catalog"){
   val kind=uri.getQueryParameter("kind").orEmpty();val genre=uri.getQueryParameter("genre").orEmpty();val year=uri.getQueryParameter("year").orEmpty();val sort=uri.getQueryParameter("sort").orEmpty();val key=listOf(kind,q,genre,year,sort).joinToString("\u0000");val selection=synchronized(indexLock){views[key]?:CatalogFilter.select(rows,kind,q,genre,year,sort).also{views[key]=it}};val pool=rows.filter{kind.isEmpty()||it.kind==kind};val years=pool.map{it.year}.filter{it.toIntOrNull()?.let{y->y in 1800..2200}==true}.distinct().sortedDescending();val page=selection.drop(offset).take(60)
   return JSONObject().put("items",JSONArray(page.map{item(it)})).put("total",selection.size).put("categories",JSONArray(pool.flatMap{it.genres}.distinct().sorted())).put("years",JSONArray(years)).put("pageSize",60).put("nextOffset",if(offset+page.size<selection.size)offset+page.size else JSONObject.NULL).put("availableSorts",JSONArray(if(years.isEmpty())listOf("title_asc","title_desc") else listOf("title_asc","title_desc","year_desc","year_asc")))
  }
  if(p.startsWith("/api/connect/catalog/")){val id=p.substringAfter("/api/connect/catalog/").substringBefore('/');val row=byId[id]?:error("Título indisponível");if(p.endsWith("/play")){val r=resolve(ctx,row);val season=uri.getQueryParameter("season")?.toIntOrNull();val episode=uri.getQueryParameter("episode")?.toIntOrNull();val urls=if(row.kind=="series")r.episodio(season?:0,episode?:0)?.fontes?.values?.flatten().orEmpty() else r.filme?.fontes?.values?.flatten().orEmpty();check(urls.isNotEmpty());return JSONObject().put("item",detail(ctx,row)).put("urls",JSONArray(urls)).also{if(season!=null)it.put("season",season).put("episode",episode)}};if(p.endsWith("/related"))return JSONObject().put("collections",JSONArray()).put("similar",JSONArray());return detail(ctx,row)}
  throw Gate.ApiError(503,"ONLINE_REQUIRED","Este recurso usa um serviço externo opcional")
 }
}
