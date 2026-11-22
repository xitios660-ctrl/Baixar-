package com.guhflix.shell

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.KeyEvent
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import android.graphics.drawable.ColorDrawable
import android.webkit.*
import android.graphics.Color
import br.com.saimo.tv.BuildConfig
import org.json.JSONObject
import java.util.concurrent.Executors

/** Packaged presentation. Edition-specific access and playback remain in native code. */
class CinemaActivity:Activity(){
 private lateinit var web:WebView
 private val worker=CinemaWorkQueue()
 @Volatile private var closed=false
 @Volatile private var uiEpoch=0
 private var webLive=false
 private lateinit var stage:FrameLayout
 private lateinit var cover:LinearLayout
 private val startup=CinemaStartupPolicy()
 private val ui=Handler(Looper.getMainLooper())
 private var bootTimeout:Runnable?=null
 private var bootProbe:Runnable?=null
 private var probing=false
 private var initialPage="home"
 private var compatibility=false
 private val anime=java.util.concurrent.ConcurrentHashMap<String,br.com.saimo.tv.Alvo>()
 private fun remember(item:JSONObject,profileKey:String?=null){
  val user=profileKey?:Gate.request(this,"GET","/api/auth/me",null,true).getString("username")
  val prefs=getSharedPreferences("cinema-history",0);val id=item.getString("id")
  val old=org.json.JSONArray(prefs.getString(user,"[]"));val next=org.json.JSONArray().put(item)
  for(i in 0 until old.length()){val v=old.getJSONObject(i);if(v.optString("id")!=id&&next.length()<20)next.put(v)}
  prefs.edit().putString(user,next.toString()).apply()
 }
 private fun history():JSONObject {
  val user=Gate.request(this,"GET","/api/auth/me",null,true).getString("username")
  val prefs=getSharedPreferences("cinema-history",0)
  val records=br.com.saimo.tv.Progresso.emAndamento(this)
  val stored=org.json.JSONArray(prefs.getString(user,"[]"))
  fun clean(s:String)=s.replace(Regex("\\s*\\([0-9]{4}\\)\\s*$"),"").trim()
  if(!BuildConfig.GFILMES_LOCAL&&!prefs.contains("migrationOwner"))prefs.edit().putString("migrationOwner",user).commit()
  if(!BuildConfig.GFILMES_LOCAL&&prefs.getString("migrationOwner",null)==user&&!prefs.getBoolean("migrated",false)){
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
  for(i in 0 until stored.length()){
   val item=stored.getJSONObject(i)
   if(!LocalProfiles.allowed(this,(if(item.optBoolean("collection"))"anime:" else "vod:")+item.getString("id")))continue
   if(item.optBoolean("collection")&&item.optString("collectionName").isNotEmpty())anime[item.getString("id")]=br.com.saimo.tv.Alvo(item.getString("title"),true,"",item.optString("year"),item.getString("collectionName"),item.optInt("tmdbId"))
   val current=records.firstOrNull{clean(it.titulo).equals(clean(item.optString("title")),true)}
   if(current!=null&&!current.terminado){item.put("progress",current.fracao);if(current.serie)item.put("season",current.temporada).put("episode",current.episodio);out.put(item)}
  }
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
  val child=LocalProfiles.isKids(this@CinemaActivity)
  val approved=if(child)LocalProfiles.guardian(this@CinemaActivity).approvedKeys() else emptySet()
  val safe=if(child)all.filter{c->val id=java.security.MessageDigest.getInstance("MD5").digest(("animes|"+c.titulo+"|"+c.ano).toByteArray()).joinToString(""){"%02x".format(it)};"anime:"+id in approved} else all
  for(c in safe.drop(offset).take(60)){val id=java.security.MessageDigest.getInstance("MD5").digest(("animes|"+c.titulo+"|"+c.ano).toByteArray()).joinToString(""){"%02x".format(it)};val a=br.com.saimo.tv.Alvo(c.titulo,true,"",c.ano,"animes",c.tmdbId.toIntOrNull()?:0);anime[id]=a;items.put(JSONObject().put("id",id).put("title",c.titulo).put("year",c.ano).put("kind","series").put("collection",true).put("genres",org.json.JSONArray().put("Animes")).put("poster",br.com.saimo.tv.Generos.capa(c.titulo,true)?:""))}
  JSONObject().put("items",items).put("total",safe.size)
 }
 private fun animeDetail(id:String):JSONObject=kotlinx.coroutines.runBlocking {
  require(Gate.request(this@CinemaActivity,"GET","/api/app/access",null,true).optString("state")=="active"){"Seu acesso não está ativo"}
  require(LocalProfiles.allowed(this@CinemaActivity,"anime:"+id)){"Título não aprovado para o perfil infantil"}
  val a=anime[id]?:error("Abra a categoria Animes novamente")
  val r=br.com.saimo.tv.Titulos.resolver(this@CinemaActivity,a)?:error("Título indisponível")
  val m=br.com.saimo.tv.Detalhes.de(a.titulo,true,a.tmdbId.takeIf{it>0})
  val episodes=org.json.JSONArray();r.episodios.forEach{episodes.put(JSONObject().put("season",it.temporada).put("episode",it.numero))}
  JSONObject().put("id",id).put("title",a.titulo).put("year",a.ano).put("kind","series").put("collection",true).put("genres",org.json.JSONArray().put("Animes")).put("poster",br.com.saimo.tv.Generos.capa(a.titulo,true)?:"").put("backdrop",m?.fundo?:"").put("synopsis",m?.sinopse?:"").put("seasons",org.json.JSONArray(r.temporadas)).put("episodes",episodes)
 }

 private fun disposeWeb(){if(::web.isInitialized&&webLive){webLive=false;stage.removeView(web);runCatching{web.removeJavascriptInterface("GfilmesNative")};runCatching{web.stopLoading()};runCatching{web.destroy()}}}
 private fun clearBootCallbacks(){bootTimeout?.let{ui.removeCallbacks(it)};bootProbe?.let{ui.removeCallbacks(it)};bootTimeout=null;bootProbe=null;probing=false}
 private fun showCover(failure:Boolean,reason:String=""){
  if(::cover.isInitialized)stage.removeView(cover)
  cover=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER;setPadding(36,24,36,24);setBackgroundColor(Color.rgb(7,9,13));isFocusable=true;isClickable=true}
  fun label(text:String,size:Float){cover.addView(TextView(this).apply{this.text=text;setTextColor(Color.WHITE);textSize=size;gravity=android.view.Gravity.CENTER;setPadding(12,10,12,10)})}
  label("Gfilmes",34f)
  label(if(failure)"Não foi possível abrir o catálogo." else "Abrindo seu catálogo…",22f)
  label(if(failure)"Tente novamente ou altere o modo de compatibilidade." else "Se a abertura não terminar, use o modo compatível.",16f)
  if(failure)label(reason+" · "+BuildConfig.VERSION_NAME+if(compatibility)" · Modo compatível" else "",13f)
  val actions=LinearLayout(this).apply{gravity=android.view.Gravity.CENTER}
  fun action(text:String,run:()->Unit):Button {val button=Button(this).apply{this.text=text;setOnClickListener{run()}};actions.addView(button);return button}
  val retry=action("Tentar novamente"){openCinema(true,compatibility)}
  action(if(compatibility)"Modo padrão" else "Modo compatível"){openCinema(true,!compatibility)}
  action("Voltar à entrada"){startActivity(Intent(this,AccessActivity::class.java));finish()}
  cover.addView(actions);stage.addView(cover,FrameLayout.LayoutParams(-1,-1));if(failure)retry.requestFocus()
 }
 private fun bootFailure(ticket:Int,reason:String){
  if(closed)return
  when(startup.fail(ticket)){
   CinemaStartupPolicy.Failure.IGNORE->return
   CinemaStartupPolicy.Failure.RETRY_COMPATIBLE->openCinema(false,true)
   CinemaStartupPolicy.Failure.SHOW_RECOVERY->{clearBootCallbacks();showCover(true,reason)}
  }
 }
 private fun probeBoot(ticket:Int){
  if(closed||ticket!=uiEpoch||startup.ready||probing)return
  probing=true
  web.evaluateJavascript("Boolean(window.gfilmesUiReady && document.getElementById('nav') && document.getElementById('nav').children.length)"){value->
   if(closed||ticket!=uiEpoch)return@evaluateJavascript
   probing=false
   if(value=="true"){
    probing=true
    web.postVisualStateCallback(ticket.toLong(),object:WebView.VisualStateCallback(){override fun onComplete(requestId:Long){
     if(closed||ticket!=uiEpoch||!startup.markReady(ticket))return
     clearBootCallbacks();cover.visibility=View.GONE;web.requestFocus()
     if(initialPage!="home"){val page=initialPage;initialPage="home";web.evaluateJavascript("window.gfilmesNavigate&&window.gfilmesNavigate("+JSONObject.quote(page)+")",null)}
    }})
   }else{bootProbe=Runnable{probeBoot(ticket)};ui.postDelayed(bootProbe!!,250)}
  }
 }
 private fun openCinema(manual:Boolean,compatible:Boolean){
  if(closed)return
  clearBootCallbacks();val ticket=startup.begin(manual);uiEpoch=ticket;compatibility=compatible
  getSharedPreferences("cinema-display",0).edit().putBoolean("compatible",compatible).apply()
  showCover(false)
  disposeWeb()
  try{
   web=WebView(this);webLive=true;web.setBackgroundColor(Color.rgb(7,9,13));web.overScrollMode=View.OVER_SCROLL_NEVER
   if(compatible)web.setLayerType(View.LAYER_TYPE_SOFTWARE,null)
   web.settings.apply{javaScriptEnabled=true;domStorageEnabled=false;allowFileAccess=false;allowContentAccess=false;mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;setSupportZoom(false);defaultTextEncodingName="UTF-8"}
   web.webChromeClient=object:WebChromeClient(){override fun onConsoleMessage(message:ConsoleMessage):Boolean {
    if(message.messageLevel()==ConsoleMessage.MessageLevel.ERROR&&message.message().startsWith("Uncaught ")&&!startup.ready){ui.post{bootFailure(ticket,"Falha ao iniciar a interface")}}
    return true
   }}
   web.webViewClient=object:WebViewClient(){
    override fun shouldOverrideUrlLoading(v:WebView?,url:String?)=true
    override fun onPageFinished(v:WebView?,url:String?){if(ticket==uiEpoch&&!closed)probeBoot(ticket)}
    override fun onReceivedError(v:WebView?,request:WebResourceRequest?,error:WebResourceError?){if(request?.isForMainFrame==true)bootFailure(ticket,"Falha ao abrir a interface")}
    override fun onReceivedHttpError(v:WebView?,request:WebResourceRequest?,response:WebResourceResponse?){if(request?.isForMainFrame==true)bootFailure(ticket,"Interface indisponível")}
    override fun onRenderProcessGone(v:WebView?,detail:RenderProcessGoneDetail?):Boolean {
     if(ticket==uiEpoch&&!closed){disposeWeb();bootFailure(ticket,"A interface foi interrompida")}
     return true
    }
    override fun shouldInterceptRequest(v:WebView?,request:WebResourceRequest?):WebResourceResponse?{
     val url=request?.url?.toString()?:return null;val file=url.removePrefix("https://gfilmes.invalid/")
     return if(url.startsWith("https://gfilmes.invalid/")&&(file in listOf("stadium.png","neon.png","universes.png","ui-regular.ttf","ui-bold.ttf","ui-bolditalic.ttf")||file.matches(Regex("avatars/(?:gifs|posters)/[a-z0-9-]+\\.(?:gif|png)")))){
      try{WebResourceResponse(if(file.endsWith(".gif"))"image/gif" else if(file.endsWith(".png"))"image/png" else "font/ttf",null,assets.open("gfilmes/$file"))}catch(_:Exception){WebResourceResponse("text/plain","UTF-8",404,"Not found",emptyMap(),java.io.ByteArrayInputStream(byteArrayOf()))}
     }else null
    }
   }
   web.addJavascriptInterface(Bridge(),"GfilmesNative");stage.addView(web,0,FrameLayout.LayoutParams(-1,-1))
   val html=assets.open("gfilmes/cinema.html").bufferedReader().use{it.readText()}
   bootTimeout=Runnable{bootFailure(ticket,"A abertura não terminou")};ui.postDelayed(bootTimeout!!,15000)
   web.loadDataWithBaseURL("https://gfilmes.invalid/",html,"text/html","UTF-8",null)
  }catch(_:Exception){bootFailure(ticket,"Não foi possível iniciar a interface")}
 }
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState);window.setFlags(1024,1024);window.decorView.systemUiVisibility=5894;window.setBackgroundDrawable(ColorDrawable(Color.rgb(7,9,13)))
  stage=FrameLayout(this).apply{setBackgroundColor(Color.rgb(7,9,13))};setContentView(stage)
  initialPage=intent.getStringExtra("cinema.page")?.takeIf{it in listOf("home","catalog","search","live","account","settings")}?:"home"
  compatibility=getSharedPreferences("cinema-display",0).getBoolean("compatible",BuildConfig.APPLICATION_ID=="com.guhflix.tv.connect"||BuildConfig.GFILMES_LOCAL)
  openCinema(true,compatibility)
 }
 inner class Bridge{
  @JavascriptInterface fun fastPreview():Boolean=BuildConfig.GFILMES_LOCAL
  @JavascriptInterface fun familyProfiles():Boolean=BuildConfig.GFILMES_LOCAL
  @JavascriptInterface fun edition():String=if(BuildConfig.GFILMES_LOCAL)"local" else "account"
  @JavascriptInterface fun request(id:String,raw:String){
   if(closed||!id.matches(Regex("[0-9]{1,9}")))return
   val epoch=uiEpoch
   val operation=runCatching{JSONObject(raw).optString("op")}.getOrDefault("")
   val person=if(BuildConfig.GFILMES_LOCAL)LocalProfiles.store(this@CinemaActivity).activeId() else null
   worker.submit(operation){
    if(closed||epoch!=uiEpoch)return@submit
    val result=try{
     val p=JSONObject(raw);val op=p.getString("op")
     if(BuildConfig.GFILMES_LOCAL&&op in listOf("detail","anime","live")&&person!=LocalProfiles.store(this@CinemaActivity).activeId())error("Perfil alterado")
     val profiles=if(BuildConfig.GFILMES_LOCAL)LocalProfiles.store(this@CinemaActivity) else null
     if(profiles!=null&&!op.startsWith("profile"))LocalProfiles.requireSelected(this@CinemaActivity)
     val data=when(op){
      "profiles"->{require(profiles!=null);profiles.list().put("guardian",LocalProfiles.guardian(this@CinemaActivity).summary())}
      "profileCreate"->{require(profiles!=null);val parent=LocalProfiles.guardian(this@CinemaActivity);parent.requireAdult(p.optString("guardianPin"));val kids=p.optBoolean("kids");require(p.optString("name").trim().length in 1..24&&profiles.list().getJSONArray("items").length()<6){"Confira o nome e o limite de perfis"};if(kids&&!parent.configured())parent.configure(p.optString("newGuardianPin"));profiles.create(p.optString("name"),p.optInt("avatar"),kids)}
      "profileSelect"->{require(profiles!=null);val id=p.getString("profileId");LocalProfiles.guardian(this@CinemaActivity).select(profiles.person(id).optBoolean("kids"),p.optString("guardianPin"));profiles.select(id)}
      "profileLock"->{require(profiles!=null);profiles.lock();JSONObject()}
      "profileUpdate"->{require(profiles!=null);val parent=LocalProfiles.guardian(this@CinemaActivity);parent.requireAdult(p.optString("guardianPin"));val id=p.getString("profileId");val kids=if(p.has("kids"))p.getBoolean("kids") else profiles.person(id).optBoolean("kids");if(kids&&!parent.configured())parent.configure(p.optString("newGuardianPin"));val result=profiles.update(id,p.optString("name"),p.optInt("avatar"),kids);if(id==profiles.activeId()&&!kids)parent.select(false,p.optString("guardianPin"));result}
      "profileDelete"->{require(profiles!=null);val parent=LocalProfiles.guardian(this@CinemaActivity);parent.requireAdult(p.optString("guardianPin"));val id=p.getString("profileId");val active=id==profiles.activeId();val result=profiles.delete(id);if(active)parent.select(false,p.optString("guardianPin"));result}
      "kidsAllow"->{require(profiles!=null);val key=(if(p.optBoolean("collection"))"anime:" else "vod:")+p.getString("id");val parent=LocalProfiles.guardian(this@CinemaActivity);val next=!parent.allowed(key);parent.approve(key,next,p.optString("guardianPin"));JSONObject().put("approved",next)}
      "anime"->animePage(p.optString("q").take(120),p.optInt("offset").coerceIn(0,100000))
      "catalogStatus"->LocalCatalog.status()
      "catalogRefresh"->LocalCatalog.refresh(this@CinemaActivity)
      "home"->Gate.request(this@CinemaActivity,"GET","/api/connect/home",null,true)
      "catalog"->{
       val kind=p.optString("kind");require(kind in listOf("","movie","series"))
       val sort=p.optString("sort");require(sort in listOf("","title_asc","title_desc","year_desc","year_asc"))
       val year=p.optString("year");require(year.isEmpty()||year.matches(Regex("[12][0-9]{3}")))
       val params=linkedMapOf("kind" to kind,"q" to p.optString("q").take(120),"offset" to p.optInt("offset").coerceIn(0,100000).toString(),"genre" to p.optString("genre").take(100),"year" to year,"sort" to sort,"titleOnly" to p.optBoolean("titleOnly").toString())
       val query=params.filterValues{it.isNotEmpty()}.entries.joinToString("&"){android.net.Uri.encode(it.key)+"="+android.net.Uri.encode(it.value)}
       Gate.request(this@CinemaActivity,"GET","/api/connect/catalog?$query",null,true)
      }
      "live"->livePage(p.optInt("offset").coerceIn(0,100000),p.optString("q"))
      "preview"->{require(BuildConfig.GFILMES_LOCAL);val id=p.getString("id");require(id.matches(Regex("[a-f0-9]{32}")));val a=anime[id];if(a!=null){require(LocalProfiles.allowed(this@CinemaActivity,"anime:"+id));JSONObject().put("id",id).put("title",a.titulo).put("kind","series").put("collection",true).put("year",a.ano).put("poster",br.com.saimo.tv.Generos.capa(a.titulo,true)?:"")}else LocalCatalog.preview(this@CinemaActivity,id)}
      "detail"->{val content=p.getString("id");require(content.matches(Regex("[a-f0-9]{32}")));(if(anime.containsKey(content))animeDetail(content) else Gate.request(this@CinemaActivity,"GET",(if(p.optBoolean("live"))"/api/connect/live/" else "/api/connect/catalog/")+content,null,true)).let{if(BuildConfig.GFILMES_LOCAL)it.put("kidsApproved",LocalProfiles.guardian(this@CinemaActivity).allowed((if(anime.containsKey(content))"anime:" else "vod:")+content)) else it}}
      "me"->Gate.request(this@CinemaActivity,"GET","/api/auth/me",null,true).let{JSONObject().put("username",it.optString("displayName",it.optString("username"))).put("version",BuildConfig.VERSION_NAME).put("local",BuildConfig.GFILMES_LOCAL).put("kids",LocalProfiles.isKids(this@CinemaActivity))}
      "pair"->Gate.request(this@CinemaActivity,"POST","/api/connect/pairing",JSONObject(),true)
      "history"->history()
      "favorites"->{val u=Gate.request(this@CinemaActivity,"GET","/api/auth/me",null,true);val saved=org.json.JSONArray(getSharedPreferences("cinema-favorites",0).getString(u.getString("username"),"[]"));val safe=org.json.JSONArray();for(i in 0 until saved.length()){val item=saved.getJSONObject(i);if(LocalProfiles.allowed(this@CinemaActivity,"vod:"+item.getString("id")))safe.put(item)};JSONObject().put("items",safe)}
      "favorite"->{val content=p.getJSONObject("item");val cid=content.getString("id");require(cid.matches(Regex("[a-f0-9]{32}")));val u=Gate.request(this@CinemaActivity,"GET","/api/auth/me",null,true);val prefs=getSharedPreferences("cinema-favorites",0);val a=org.json.JSONArray(prefs.getString(u.getString("username"),"[]"));val next=org.json.JSONArray();var found=false;for(i in 0 until a.length()){val v=a.getJSONObject(i);if(v.getString("id")==cid)found=true else next.put(v)};if(!found&&next.length()<500)next.put(JSONObject().put("id",cid).put("title",content.optString("title").take(240)).put("poster",content.optString("poster").take(4096)).put("kind",content.optString("kind")));prefs.edit().putString(u.getString("username"),next.toString()).apply();JSONObject().put("saved",!found)}
      else->throw IllegalArgumentException("Operação indisponível")
     };if(BuildConfig.GFILMES_LOCAL&&op in listOf("detail","anime","live")&&person!=LocalProfiles.store(this@CinemaActivity).activeId())error("Perfil alterado");JSONObject().put("data",data)
    }catch(e:Exception){JSONObject().put("error",e.message?.take(240)?:"Não foi possível carregar. Tente novamente.")}
    runOnUiThread{if(!closed&&webLive&&epoch==uiEpoch)web.evaluateJavascript("window.gfilmesResult&&window.gfilmesResult($id,${result});",null)}
   }
  }
  @JavascriptInterface fun play(raw:String){val submitted=if(BuildConfig.GFILMES_LOCAL)LocalProfiles.store(this@CinemaActivity).activeId() else null;worker.submit("play"){try{
   LocalProfiles.requireSelected(this@CinemaActivity)
   require(!BuildConfig.GFILMES_LOCAL||submitted==LocalProfiles.store(this@CinemaActivity).activeId()){ "Perfil alterado" }
   val person=if(BuildConfig.GFILMES_LOCAL)LocalProfiles.store(this@CinemaActivity).activeId() else null
   fun samePerson()=!BuildConfig.GFILMES_LOCAL||LocalProfiles.store(this@CinemaActivity).activeId()==person
   val p=JSONObject(raw);val id=p.getString("id");require(id.matches(Regex("[a-f0-9]{32}")))
   val live=p.optBoolean("live");val collection=anime[id];val recordKey=if(BuildConfig.GFILMES_LOCAL)"profile-"+person else null
   if(collection!=null){
    require(LocalProfiles.allowed(this@CinemaActivity,"anime:"+id)){"Título não aprovado para o perfil infantil"}
    remember(JSONObject().put("id",id).put("title",collection.titulo).put("year",collection.ano).put("poster",br.com.saimo.tv.Generos.capa(collection.titulo,true)?:"").put("kind","series").put("collection",true).put("collectionName",collection.colecao).put("tmdbId",collection.tmdbId),recordKey)
    runOnUiThread{if(!closed&&samePerson())br.com.saimo.tv.PlayerActivity.abrir(this@CinemaActivity,collection,p.optInt("season"),p.optInt("episode"))};return@submit
   }
   if(!live){val item=if(BuildConfig.GFILMES_LOCAL)LocalCatalog.preview(this@CinemaActivity,id) else Gate.request(this@CinemaActivity,"GET","/api/connect/catalog/$id",null,true);remember(JSONObject().put("id",id).put("title",item.getString("title")).put("year",item.optString("year")).put("poster",item.optString("poster")).put("kind",item.getString("kind")),recordKey)}
   runOnUiThread{if(!closed&&samePerson()){if(live)CloudRemote.playLive(this@CinemaActivity,id) else if(p.has("season"))CloudRemote.playEpisode(this@CinemaActivity,id,p.getInt("season"),p.getInt("episode")) else CloudRemote.playTitle(this@CinemaActivity,id)}}
  }catch(e:Exception){runOnUiThread{if(!closed&&(!BuildConfig.GFILMES_LOCAL||submitted==LocalProfiles.store(this@CinemaActivity).activeId()))web.evaluateJavascript("window.gfilmesPlaybackError("+JSONObject.quote(e.message?:"Não foi possível abrir o vídeo")+")",null)}}}}
  @JavascriptInterface fun account(){runOnUiThread{if(LocalProfiles.isKids(this@CinemaActivity)){android.widget.Toast.makeText(this@CinemaActivity,"Troque para um perfil adulto para abrir os ajustes",android.widget.Toast.LENGTH_LONG).show();return@runOnUiThread};startActivity(Intent(this@CinemaActivity,HubActivity::class.java))}}
  @JavascriptInterface fun exit(){runOnUiThread{android.app.AlertDialog.Builder(this@CinemaActivity).setMessage("Sair do Gfilmes?").setPositiveButton("Sair"){_,_->finish()}.setNegativeButton("Continuar assistindo",null).show()}}
  @JavascriptInterface fun update(){runOnUiThread{OtaManager.check(this@CinemaActivity,true)}}
  @JavascriptInterface fun settings(){runOnUiThread{if(LocalProfiles.isKids(this@CinemaActivity)){android.widget.Toast.makeText(this@CinemaActivity,"Troque para um perfil adulto para abrir os ajustes",android.widget.Toast.LENGTH_LONG).show();return@runOnUiThread};startActivity(Intent().setClassName(packageName,"br.com.saimo.tv.AjustesActivity"))}}
 }
 override fun onNewIntent(next:Intent){super.onNewIntent(next);setIntent(next);val page=next.getStringExtra("cinema.page");if(page in listOf("home","catalog","search","live","account","settings")){if(webLive&&startup.ready)web.evaluateJavascript("window.gfilmesNavigate&&window.gfilmesNavigate("+JSONObject.quote(page)+")",null) else initialPage=page!!}}
 override fun onPause(){if(webLive){web.evaluateJavascript("window.gfilmesSetVisible&&window.gfilmesSetVisible(false)",null);web.onPause()};super.onPause()}
 override fun onResume(){super.onResume();if(webLive){web.onResume();web.evaluateJavascript("window.gfilmesSetVisible&&window.gfilmesSetVisible(true)",null)};OtaManager.resume(this);if(webLive&&startup.ready)web.evaluateJavascript("window.gfilmesResume&&window.gfilmesResume()",null)}
 override fun onKeyDown(keyCode:Int,event:KeyEvent?):Boolean {if(keyCode==KeyEvent.KEYCODE_MENU){showCover(true,"Recuperar a interface");return true};return super.onKeyDown(keyCode,event)}
 @Deprecated("Android TV back key") override fun onBackPressed(){if(!startup.ready||cover.visibility==View.VISIBLE){finish();return};web.evaluateJavascript("window.gfilmesBack&&window.gfilmesBack()",null)}
 override fun onDestroy(){closed=true;uiEpoch++;clearBootCallbacks();disposeWeb();worker.close();super.onDestroy()}
}
