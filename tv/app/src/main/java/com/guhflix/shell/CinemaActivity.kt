package com.guhflix.shell

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.*
import android.graphics.Color
import br.com.saimo.tv.BuildConfig
import org.json.JSONObject
import java.util.concurrent.Executors

/** Packaged presentation only. Credentials and playback remain in native authenticated code. */
class CinemaActivity:Activity(){
 private lateinit var web:WebView
 private val worker=Executors.newSingleThreadExecutor()
 private var closed=false
 private val anime=java.util.concurrent.ConcurrentHashMap<String,br.com.saimo.tv.Alvo>()
 private fun history():JSONObject {
  val user=Gate.request(this,"GET","/api/auth/me",null,true).getString("username")
  val prefs=getSharedPreferences("cinema-history",0)
  val records=br.com.saimo.tv.Progresso.emAndamento(this)
  val stored=org.json.JSONArray(prefs.getString(user,"[]"))
  fun clean(s:String)=s.replace(Regex("\\s*\\([0-9]{4}\\)\\s*$"),"").trim()
  if(!prefs.contains("migrationOwner"))prefs.edit().putString("migrationOwner",user).commit()
  if(prefs.getString("migrationOwner",null)==user&&!prefs.getBoolean("migrated",false)){
   for(record in records.take(12)){
    if(record.endereco?.colecao?.isNotEmpty()==true)continue
    val q=java.net.URLEncoder.encode(clean(record.titulo),"UTF-8")
    val kind=if(record.serie)"series" else "movie"
    val page=Gate.request(this,"GET","/api/connect/catalog?kind=$kind&q=$q&offset=0",null,true).getJSONArray("items")
    for(i in 0 until page.length()){
     val item=page.getJSONObject(i)
     if(!clean(item.getString("title")).equals(clean(record.titulo),true))continue
     val year=record.endereco?.ano.orEmpty()
     if(year.isNotBlank()&&item.optString("year")!=year&&!item.getString("title").contains(year))continue
     var exists=false;for(j in 0 until stored.length())if(stored.getJSONObject(j).getString("id")==item.getString("id"))exists=true
     if(!exists&&stored.length()<20)stored.put(item)
     break
    }
   }
   prefs.edit().putString(user,stored.toString()).putBoolean("migrated",true).apply()
  }
  val out=org.json.JSONArray()
  for(i in 0 until stored.length()){val item=stored.getJSONObject(i);val current=records.firstOrNull{clean(it.titulo).equals(clean(item.optString("title")),true)};if(current!=null&&!current.terminado){item.put("progress",current.fracao);if(current.serie)item.put("season",current.temporada).put("episode",current.episodio);out.put(item)}}
  return JSONObject().put("items",out)
 }
 private fun livePage(offset:Int,query:String=""):JSONObject {
  val data=Gate.request(this,"GET","/api/connect/live?offset=${offset.coerceAtLeast(0)}&q=${android.net.Uri.encode(query.take(120))}",null,true)
  kotlinx.coroutines.runBlocking{br.com.saimo.tv.Epg.carregarCache(this@CinemaActivity)}
  fun program(p:br.com.saimo.tv.Programme)=JSONObject().put("title",p.title.take(240)).put("start",p.start).put("stop",p.stop).put("poster",p.poster?:"").put("description",p.description?.take(1000)?:"")
  val items=data.getJSONArray("items")
  for(i in 0 until items.length()){val item=items.getJSONObject(i);val name=item.getString("title");val original=br.com.saimo.tv.Remote.channels.firstOrNull{br.com.saimo.tv.Epg.normalise(it.name)==br.com.saimo.tv.Epg.normalise(name)}?.name
   val guide=br.com.saimo.tv.Epg.nowNext(original?:name)?:continue
   item.put("now",program(guide.first));guide.second?.takeIf{it.title.isNotBlank()&&!it.title.equals("No Data",true)}?.let{item.put("next",program(it))}
  }
  return data
 }
 private fun animePage(q:String,offset:Int):JSONObject=kotlinx.coroutines.runBlocking {
  require(Gate.request(this@CinemaActivity,"GET","/api/app/access",null,true).optString("state")=="active"){"Seu acesso não está ativo"}
  br.com.saimo.tv.Generos.carregar(this@CinemaActivity)
  val all=br.com.saimo.tv.Vod.colecao(this@CinemaActivity,"animes").filter{it.titulo.contains(q,true)}
  val items=org.json.JSONArray()
  for(c in all.drop(offset).take(30)){val id=java.security.MessageDigest.getInstance("MD5").digest(("animes|"+c.titulo+"|"+c.ano).toByteArray()).joinToString(""){"%02x".format(it)};val a=br.com.saimo.tv.Alvo(c.titulo,true,"",c.ano,"animes",c.tmdbId.toIntOrNull()?:0);anime[id]=a;items.put(JSONObject().put("id",id).put("title",c.titulo).put("year",c.ano).put("kind","series").put("collection",true).put("genres",org.json.JSONArray().put("Animes")).put("poster",br.com.saimo.tv.Generos.capa(c.titulo,true)?:""))}
  JSONObject().put("items",items).put("total",all.size)
 }
 private fun animeDetail(id:String):JSONObject=kotlinx.coroutines.runBlocking {
  require(Gate.request(this@CinemaActivity,"GET","/api/app/access",null,true).optString("state")=="active"){"Seu acesso não está ativo"}
  val a=anime[id]?:error("Abra a categoria Animes novamente")
  val r=br.com.saimo.tv.Titulos.resolver(this@CinemaActivity,a)?:error("Título indisponível")
  val m=br.com.saimo.tv.Detalhes.de(a.titulo,true,a.tmdbId.takeIf{it>0})
  val episodes=org.json.JSONArray();r.episodios.forEach{episodes.put(JSONObject().put("season",it.temporada).put("episode",it.numero))}
  JSONObject().put("id",id).put("title",a.titulo).put("year",a.ano).put("kind","series").put("collection",true).put("genres",org.json.JSONArray().put("Animes")).put("poster",br.com.saimo.tv.Generos.capa(a.titulo,true)?:"").put("backdrop",m?.fundo?:"").put("synopsis",m?.sinopse?:"").put("seasons",org.json.JSONArray(r.temporadas)).put("episodes",episodes)
 }

 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);window.setFlags(1024,1024);window.decorView.systemUiVisibility=5894
  web=WebView(this);web.setBackgroundColor(Color.BLACK);web.overScrollMode=android.view.View.OVER_SCROLL_NEVER
  web.settings.apply{javaScriptEnabled=true;domStorageEnabled=false;allowFileAccess=false;allowContentAccess=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;setSupportZoom(false);defaultTextEncodingName="UTF-8"}
  web.webViewClient=object:WebViewClient(){override fun shouldOverrideUrlLoading(v:WebView?,u:String?)=true
   override fun onPageFinished(v:WebView?,url:String?){val page=intent.getStringExtra("cinema.page");if(page in listOf("home","catalog","search","live","account","settings"))web.evaluateJavascript("window.gfilmesNavigate&&window.gfilmesNavigate("+JSONObject.quote(page)+")",null)}
   override fun shouldInterceptRequest(v:WebView?,r:WebResourceRequest?):WebResourceResponse?{val url=r?.url?.toString()?:return null;val file=url.removePrefix("https://gfilmes.invalid/");return if(url.startsWith("https://gfilmes.invalid/")&&file in listOf("stadium.png","neon.png","universes.png","ui-regular.ttf","ui-bold.ttf","ui-bolditalic.ttf"))WebResourceResponse(if(file.endsWith(".png"))"image/png" else "font/ttf",null,assets.open("gfilmes/$file")) else null}}
  web.addJavascriptInterface(Bridge(),"GfilmesNative");setContentView(web)
  val html=assets.open("gfilmes/cinema.html").bufferedReader().use{it.readText()}
  web.loadDataWithBaseURL("https://gfilmes.invalid/",html,"text/html","UTF-8",null)
 }
 inner class Bridge{
  @JavascriptInterface fun request(id:String,raw:String){
   if(closed||!id.matches(Regex("[0-9]{1,9}")))return
   worker.execute{
    val result=try{
     val p=JSONObject(raw);val op=p.getString("op")
     val data=when(op){
      "anime"->animePage(p.optString("q").take(120),p.optInt("offset").coerceIn(0,100000))
      "home"->Gate.request(this@CinemaActivity,"GET","/api/connect/home",null,true)
      "catalog"->{val kind=p.optString("kind");require(kind in listOf("","movie","series"));val q=java.net.URLEncoder.encode(p.optString("q").take(120),"UTF-8");val offset=p.optInt("offset").coerceIn(0,100000);val genre=java.net.URLEncoder.encode(p.optString("genre").take(80),"UTF-8");val year=p.optString("year").takeIf{it.matches(Regex("\\d{4}"))}.orEmpty();val sort=p.optString("sort").takeIf{it in listOf("title_asc","title_desc","year_asc","year_desc")}.orEmpty();Gate.request(this@CinemaActivity,"GET","/api/connect/catalog?kind=$kind&q=$q&offset=$offset&genre=$genre&year=$year&sort=$sort",null,true)}
      "live"->livePage(p.optInt("offset").coerceIn(0,100000),p.optString("q"))
      "detail"->{val content=p.getString("id");require(content.matches(Regex("[a-f0-9]{32}")));if(anime.containsKey(content))animeDetail(content) else Gate.request(this@CinemaActivity,"GET",(if(p.optBoolean("live"))"/api/connect/live/" else "/api/connect/catalog/")+content,null,true)}
      "me"->Gate.request(this@CinemaActivity,"GET","/api/auth/me",null,true).let{JSONObject().put("username",it.optString("username")).put("version",BuildConfig.VERSION_NAME)}
      "pair"->Gate.request(this@CinemaActivity,"POST","/api/connect/pairing",JSONObject(),true)
      "history"->history()
      "favorites"->{val u=Gate.request(this@CinemaActivity,"GET","/api/auth/me",null,true);JSONObject().put("items",org.json.JSONArray(getSharedPreferences("cinema-favorites",0).getString(u.getString("username"),"[]")))}
      "favorite"->{val content=p.getJSONObject("item");val cid=content.getString("id");require(cid.matches(Regex("[a-f0-9]{32}")));val u=Gate.request(this@CinemaActivity,"GET","/api/auth/me",null,true);val prefs=getSharedPreferences("cinema-favorites",0);val a=org.json.JSONArray(prefs.getString(u.getString("username"),"[]"));val next=org.json.JSONArray();var found=false;for(i in 0 until a.length()){val v=a.getJSONObject(i);if(v.getString("id")==cid)found=true else next.put(v)};if(!found&&next.length()<500)next.put(JSONObject().put("id",cid).put("title",content.optString("title").take(240)).put("poster",content.optString("poster").take(4096)).put("kind",content.optString("kind")));prefs.edit().putString(u.getString("username"),next.toString()).apply();JSONObject().put("saved",!found)}
      else->throw IllegalArgumentException("Operação indisponível")
     };JSONObject().put("data",data)
    }catch(e:Exception){JSONObject().put("error",e.message?.take(240)?:"Não foi possível carregar. Tente novamente.")}
    runOnUiThread{if(!closed)web.evaluateJavascript("window.gfilmesResult($id,${result});",null)}
   }
  }
  @JavascriptInterface fun play(raw:String){worker.execute{try{val p=JSONObject(raw);val id=p.getString("id");require(id.matches(Regex("[a-f0-9]{32}")));val live=p.optBoolean("live");val collection=anime[id];if(collection!=null){runOnUiThread{if(!closed)br.com.saimo.tv.PlayerActivity.abrir(this@CinemaActivity,collection,p.optInt("season"),p.optInt("episode"))};return@execute};if(!live){val item=Gate.request(this@CinemaActivity,"GET","/api/connect/catalog/$id",null,true);val u=Gate.request(this@CinemaActivity,"GET","/api/auth/me",null,true);val prefs=getSharedPreferences("cinema-history",0);val old=org.json.JSONArray(prefs.getString(u.getString("username"),"[]"));val next=org.json.JSONArray().put(JSONObject().put("id",id).put("title",item.getString("title")).put("poster",item.optString("poster")).put("kind",item.getString("kind")));for(i in 0 until minOf(old.length(),19)){val v=old.getJSONObject(i);if(v.getString("id")!=id)next.put(v)};prefs.edit().putString(u.getString("username"),next.toString()).apply()};runOnUiThread{if(!closed){if(live)CloudRemote.playLive(this@CinemaActivity,id) else if(p.has("season"))CloudRemote.playEpisode(this@CinemaActivity,id,p.getInt("season"),p.getInt("episode")) else CloudRemote.playTitle(this@CinemaActivity,id)}}}catch(e:Exception){runOnUiThread{if(!closed)web.evaluateJavascript("window.gfilmesPlaybackError("+JSONObject.quote(e.message?:"Não foi possível abrir o vídeo")+")",null)}}}}
  @JavascriptInterface fun account(){runOnUiThread{startActivity(Intent(this@CinemaActivity,HubActivity::class.java))}}
  @JavascriptInterface fun exit(){runOnUiThread{android.app.AlertDialog.Builder(this@CinemaActivity).setMessage("Sair do Gfilmes?").setPositiveButton("Sair"){_,_->finish()}.setNegativeButton("Continuar assistindo",null).show()}}
  @JavascriptInterface fun update(){runOnUiThread{OtaManager.check(this@CinemaActivity,true)}}
  @JavascriptInterface fun settings(){runOnUiThread{startActivity(Intent().setClassName(packageName,"br.com.saimo.tv.AjustesActivity"))}}
 }
 override fun onNewIntent(next:Intent){super.onNewIntent(next);setIntent(next);val page=next.getStringExtra("cinema.page");if(page in listOf("home","catalog","search","live","account","settings"))web.evaluateJavascript("window.gfilmesNavigate&&window.gfilmesNavigate("+JSONObject.quote(page)+")",null)}
 override fun onResume(){super.onResume();OtaManager.resume(this);if(::web.isInitialized)web.evaluateJavascript("window.gfilmesResume&&window.gfilmesResume()",null)}
 @Deprecated("Android TV back key") override fun onBackPressed(){web.evaluateJavascript("window.gfilmesBack()",null)}
 override fun onDestroy(){closed=true;web.removeJavascriptInterface("GfilmesNative");web.destroy();worker.shutdownNow();super.onDestroy()}
}
