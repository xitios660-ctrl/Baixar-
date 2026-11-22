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
 @Volatile private var rows:List<CatalogRow> = emptyList()
 private var byId:Map<String,CatalogRow> = emptyMap()
 private var sourceKey=""
 @Volatile private var refreshing=false
 @Volatile private var sourceError=""
 @Volatile private var lastAttempt=0L
 @Volatile private var fresh=false
 @Volatile private var bundledSnapshot=false
 private val stores=java.util.concurrent.ConcurrentHashMap<String,CatalogFileStore>()
 private val background=java.util.concurrent.Executors.newSingleThreadExecutor{r->Thread(r,"gflix-catalog-refresh").apply{isDaemon=true;priority=Thread.MIN_PRIORITY}}
 private val deadlines=java.util.concurrent.Executors.newSingleThreadScheduledExecutor{r->Thread(r,"gflix-source-deadline").apply{isDaemon=true}}
 private val views=CatalogViewCache()
 private fun download(base:String,name:String):String {
  val uri=java.net.URI(base);require(uri.scheme=="https"&&uri.userInfo==null&&uri.query==null&&uri.fragment==null){"Use uma fonte HTTPS válida"}
  val connection=URL(base+name.split('/').joinToString("/"){Uri.encode(it)}).openConnection() as HttpURLConnection
  connection.connectTimeout=8000;connection.readTimeout=6000;connection.instanceFollowRedirects=false
  val end=System.nanoTime()+12_000_000_000L
  val timeout=deadlines.schedule({connection.disconnect()},12,java.util.concurrent.TimeUnit.SECONDS)
  try{
   val code=connection.responseCode
   if(code!=200)throw java.io.IOException("A fonte recusou a atualização (HTTP $code). Catálogo salvo mantido.")
   val limit=if(name.startsWith("redeflix/links-"))32*1024*1024 else 12*1024*1024
   return connection.inputStream.use{input->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(16384);while(true){check(System.nanoTime()<end){"A fonte demorou para responder"};val n=input.read(buffer);if(n<0)break;check(out.size()+n<=limit){"Resposta do catálogo excedeu o limite"};out.write(buffer,0,n)};out.toString("UTF-8")}
  }catch(e:Exception){if(e.message?.startsWith("A fonte recusou")==true)throw e;throw java.io.IOException("Não foi possível atualizar a fonte; catálogo salvo preservado.",e)}finally{timeout.cancel(false);connection.disconnect()}
 }
 private fun store(ctx:Context,base:String)=stores.getOrPut(Crypto.sha(base.toByteArray()).take(16)){CatalogFileStore(File(ctx.filesDir,"offline-catalog"),Crypto.sha(base.toByteArray()).take(16),{name->download(base,name).also{CatalogSnapshotParser.validate(name,it)}})}
 fun file(ctx:Context,name:String):String {val safe=Uri.decode(name).replace("%23","#");val base=Enderecos.filmes?:error("Catálogo desativado nas configurações");return store(ctx,base).read(safe)}
 private fun bundled(ctx:Context,name:String):String=ctx.assets.open("gfilmes/catalog-snapshot/$name.bin").use{java.util.zip.GZIPInputStream(it).bufferedReader().use{r->r.readText()}}
 private fun metadata(ctx:Context,base:String,cache:CatalogFileStore):String = cache.cached("fichas.txt")?:if(base==Enderecos.PADRAO_FILMES)runCatching{bundled(ctx,"fichas.txt")}.getOrDefault("") else ""
 private fun publish(base:String,next:List<CatalogRow>){synchronized(indexLock){if(Enderecos.filmes!=base)return;byId=next.associateBy{it.id};views.clear();sourceKey=base;rows=next}}
 private fun index(ctx:Context){
  val base=Enderecos.filmes?:throw Gate.ApiError(503,"CATALOG_DISABLED","Catálogo desativado nas configurações")
  synchronized(indexLock){
   if(sourceKey!=base||rows.isEmpty()){
    val cache=store(ctx,base);val meta=metadata(ctx,base,cache)
    val saved=cache.cached("busca.txt")?.let{runCatching{CatalogSnapshotParser.parse(it,meta)}.getOrNull()}
    val bootstrap=if(saved==null&&base==Enderecos.PADRAO_FILMES)runCatching{CatalogSnapshotParser.parse(bundled(ctx,"busca.txt"),meta)}.getOrNull() else null
    val next=saved?:bootstrap?:try{CatalogSnapshotParser.parse(cache.read("busca.txt"),meta)}catch(e:Exception){throw Gate.ApiError(503,"CATALOG_SOURCE",e.message?:"Fonte indisponível e sem catálogo salvo")}
    sourceError="";fresh=false;bundledSnapshot=saved==null&&bootstrap!=null;lastAttempt=0;publish(base,next)
   }
  }
  if(System.currentTimeMillis()-lastAttempt>6*3600000L)refresh(ctx,false)
 }
 fun status():JSONObject=JSONObject().put("state",if(fresh)"updated" else "saved").put("refreshing",refreshing).put("titles",rows.size).put("message",if(sourceError.isNotEmpty())sourceError else if(fresh)"Catálogo atualizado" else "Catálogo salvo · atualizando a fonte em segundo plano").put("error",sourceError).put("historicalSnapshotDate",if(bundledSnapshot)"2026-11-07" else JSONObject.NULL)
 fun refresh(ctx:Context,force:Boolean=true):JSONObject {
  val base=Enderecos.filmes?:return JSONObject().put("message","Catálogo desativado")
  synchronized(indexLock){if(refreshing||force&&System.currentTimeMillis()-lastAttempt<60000)return status();refreshing=true;lastAttempt=System.currentTimeMillis()}
  val app=ctx.applicationContext
  background.execute{try{
   val cache=store(app,base)
   val search=cache.read("busca.txt",force=true,staleOnFailure=false)
   val next=CatalogSnapshotParser.parse(search,metadata(app,base,cache));publish(base,next);if(Enderecos.filmes==base){fresh=true;bundledSnapshot=false;sourceError=""}
   // Posters/genres may finish later; they never prevent the first list from rendering.
   runCatching{val meta=cache.read("fichas.txt",force=true,staleOnFailure=false);publish(base,CatalogSnapshotParser.parse(search,meta))}
  }catch(e:Exception){if(Enderecos.filmes==base)sourceError=e.message?:"Fonte indisponível; catálogo salvo mantido"}finally{refreshing=false}}
  return status()
 }
 private fun item(r:CatalogRow):JSONObject=JSONObject().put("id",r.id).put("title",r.title).put("kind",r.kind).put("year",r.year).put("letter",r.letter).put("genres",JSONArray(r.genres)).put("poster",r.poster.ifEmpty{Generos.capa(r.title,r.kind=="series")?:""}).put("tmdbId",r.tmdbId.takeIf{it>0}?:Generos.id(r.title,r.kind=="series")?:0)
 fun preview(ctx:Context,id:String):JSONObject {index(ctx);val row=synchronized(indexLock){byId[id]}?:error("Título indisponível");require(LocalProfiles.allowed(ctx,"vod:"+id)){"Título não aprovado para o perfil infantil"};return item(row)}
 private fun target(r:CatalogRow)=Alvo(r.title,r.kind=="series",r.letter,r.year,tmdbId=r.tmdbId.takeIf{it>0}?:Generos.id(r.title,r.kind=="series")?:0)
 private fun resolve(ctx:Context,r:CatalogRow)=runBlocking{Titulos.resolver(ctx,target(r))}?:error("Título indisponível na fonte autorizada")
 private fun detail(ctx:Context,r:CatalogRow):JSONObject {val j=item(r);if(r.kind=="series"){val resolved=resolve(ctx,r);j.put("seasons",JSONArray(resolved.temporadas));j.put("episodes",JSONArray().also{a->resolved.episodios.sortedWith(compareBy({it.temporada},{it.numero})).forEach{a.put(JSONObject().put("season",it.temporada).put("episode",it.numero))}})};return j}
 private fun channels(ctx:Context):List<Channel>{Remote.loadCached(ctx);if(Remote.channels.isEmpty())runBlocking{Remote.refresh(ctx)};return Remote.channels.filter{it.sources.any{source->source.key==null&&source.keyId==null&&source.referer==null&&source.userAgent==null&&source.url.startsWith("https://")}}}
 private fun liveId(c:Channel)=Crypto.sha(("live\u0000"+c.name).toByteArray()).take(32)
 private fun live(c:Channel)=JSONObject().put("id",liveId(c)).put("title",c.name).put("kind","live").put("genres",JSONArray(listOfNotNull(c.categoria))).put("poster",c.logo?:"")
 fun route(ctx:Context,method:String,path:String,body:JSONObject?):JSONObject {
  val uri=Uri.parse("https://local.invalid"+path);val p=uri.path.orEmpty();val q=uri.getQueryParameter("q").orEmpty();val offset=(uri.getQueryParameter("offset")?.toIntOrNull()?:0).coerceIn(0,100000)
  val kids=LocalProfiles.isKids(ctx)
  if(kids&&p.startsWith("/api/connect/live")){if(p=="/api/connect/live")return JSONObject().put("items",JSONArray()).put("total",0).put("categories",JSONArray());error("TV ao vivo não está disponível no perfil infantil")}
  if(p.startsWith("/api/connect/live")){val all=channels(ctx);if(p=="/api/connect/live"){val selected=all.filter{CatalogFilter.fold(it.name+" "+it.categoria.orEmpty()).contains(CatalogFilter.fold(q))};return JSONObject().put("items",JSONArray(selected.drop(offset).take(60).map{live(it)})).put("total",selected.size).put("categories",JSONArray(all.mapNotNull{it.categoria}.distinct()))};val id=p.substringAfter("/api/connect/live/").substringBefore('/');val channel=all.firstOrNull{liveId(it)==id}?:error("Canal indisponível");if(p.endsWith("/play"))return JSONObject().put("item",live(channel)).put("urls",JSONArray(channel.sources.filter{it.key==null&&it.keyId==null&&it.referer==null&&it.userAgent==null&&it.url.startsWith("https://")}.map{it.url}));return live(channel)}
  if(p=="/api/connect/pairing"||p.contains("/devices"))throw Gate.ApiError(503,"ONLINE_REMOTE","O controle pela nuvem exige um serviço de coordenação; não é necessário para assistir localmente")
  index(ctx)
  val approved=if(kids)LocalProfiles.guardian(ctx).approvedKeys() else emptySet()
  if(p=="/api/connect/home"){val visible=KidsCatalog.visible(rows,kids,approved);val first=visible.firstOrNull{it.kind=="movie"}?:visible.firstOrNull();return JSONObject().put("status",status()).put("kids",kids).put("featured",first?.let{item(it)}?:JSONObject.NULL).put("rows",JSONArray().put(JSONObject().put("title","Filmes no seu catálogo").put("items",JSONArray(visible.filter{it.kind=="movie"}.take(18).map{item(it)}))).put(JSONObject().put("title","Séries no seu catálogo").put("items",JSONArray(visible.filter{it.kind=="series"}.take(18).map{item(it)}))))}
  if(p=="/api/connect/catalog"){
   val kind=uri.getQueryParameter("kind").orEmpty();val genre=uri.getQueryParameter("genre").orEmpty();val year=uri.getQueryParameter("year").orEmpty();val sort=uri.getQueryParameter("sort").orEmpty()
   val scope=if(kids)"kids:"+Crypto.sha(approved.sorted().joinToString().toByteArray()) else "adult"
   val key=listOf(scope,kind,q,genre,year,sort).joinToString("\u0000")
   val view=synchronized(indexLock){views.get(key,{KidsCatalog.visible(rows,kids,approved)},kind,q,genre,year,sort)}
   val page=view.page(offset)
   return JSONObject().put("status",status()).put("items",JSONArray(page.map{item(it)})).put("total",view.items.size).put("categories",JSONArray(view.categories)).put("years",JSONArray(view.years)).put("pageSize",60).put("nextOffset",if(offset+page.size<view.items.size)offset+page.size else JSONObject.NULL).put("availableSorts",JSONArray(if(view.years.isEmpty())listOf("title_asc","title_desc") else listOf("title_asc","title_desc","year_desc","year_asc")))
  }
  if(p.startsWith("/api/connect/catalog/")){val id=p.substringAfter("/api/connect/catalog/").substringBefore('/');val row=byId[id]?:error("Título indisponível");require(!kids||"vod:"+id in approved){"Título não aprovado para o perfil infantil"};if(p.endsWith("/play")){val r=resolve(ctx,row);val season=uri.getQueryParameter("season")?.toIntOrNull();val episode=uri.getQueryParameter("episode")?.toIntOrNull();val urls=if(row.kind=="series")r.episodio(season?:0,episode?:0)?.fontes?.values?.flatten().orEmpty() else r.filme?.fontes?.values?.flatten().orEmpty();check(urls.isNotEmpty());return JSONObject().put("item",item(row)).put("episodes",JSONArray().also{a->r.episodios.sortedWith(compareBy({it.temporada},{it.numero})).forEach{a.put(JSONObject().put("season",it.temporada).put("episode",it.numero))}}).put("urls",JSONArray(urls)).also{if(season!=null)it.put("season",season).put("episode",episode)}};if(p.endsWith("/related"))return JSONObject().put("collections",JSONArray()).put("similar",JSONArray());return detail(ctx,row)}
  throw Gate.ApiError(503,"ONLINE_REQUIRED","Este recurso usa um serviço externo opcional")
 }
 fun requireChildTarget(ctx:Context,target:Alvo?,remote:String?){
  if(!LocalProfiles.isKids(ctx))return
  require(target!=null){"Título indisponível no perfil infantil"}
  val allowed=LocalProfiles.guardian(ctx).approvedKeys()
  val remoteItem=remote?.let{runCatching{JSONObject(it).getJSONObject("item")}.getOrNull()}
  require(remoteItem?.optString("kind")!="live"){"TV ao vivo indisponível no perfil infantil"}
  val animeId=java.security.MessageDigest.getInstance("MD5").digest(("animes|"+target.titulo+"|"+target.ano).toByteArray()).joinToString(""){"%02x".format(it)}
  val granted=if(target.colecao.isNotEmpty())"anime:"+animeId in allowed else synchronized(indexLock){rows.any{it.title==target.titulo&&(it.kind=="series")==target.serie&&(target.ano.isEmpty()||it.year==target.ano)&&"vod:"+it.id in allowed}}
  require(granted){"Título não aprovado para o perfil infantil"}
 }

}
