package com.guhflix.shell

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import br.com.saimo.tv.Alvo
import br.com.saimo.tv.Connect
import br.com.saimo.tv.PlayerActivity
import br.com.saimo.tv.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Cloud control carries content IDs. URLs are resolved only by the authenticated TV. */
object CloudRemote {
    private lateinit var app: Application
    private var current = WeakReference<Activity>(null)
    private val worker = Executors.newSingleThreadScheduledExecutor()
    private val main = Handler(Looper.getMainLooper())
    private var started=false
    private var state = JSONObject().put("mode","idle").put("positionMs",0).put("durationMs",0)
    @Volatile private var lastId:String?=null
    @Volatile private var lastTitle=""
    @Volatile var lastError=""

    @JvmStatic fun init(application:Application) {
        if(started)return;started=true;app=application
        lastId=app.getSharedPreferences("guhflix-connect",0).getString("contentId",null)
        lastTitle=app.getSharedPreferences("guhflix-connect",0).getString("contentTitle","")?:""
        worker.scheduleWithFixedDelay({poll()},1,2,TimeUnit.SECONDS)
    }
    @JvmStatic fun resumed(activity:Activity) {
        current=WeakReference(activity)
        if(activity.javaClass.simpleName=="EscolhaActivity") OtaManager.check(activity,false)
    }
    @JvmStatic fun paused(activity:Activity) {if(current.get()===activity)current=WeakReference(null)}
    private fun snapshot(activity:Activity):JSONObject {
        val out=JSONObject().put("mode","idle").put("title","").put("positionMs",0).put("durationMs",0)
        if((activity is PlayerActivity||activity.javaClass.simpleName=="MainActivity")&&!Gate.initialized(activity))return out.put("mode","loading")
        return try {
            val raw=(activity as? Connect.Listener)?.obterEstado()?:return out
            val vod=raw.optJSONObject("vod")
            if(vod!=null){
                val title=vod.optString("title").take(240)
                val position=vod.optLong("pos").coerceIn(0,172800000)
                val duration=vod.optLong("dur").coerceIn(0,172800000)
                out.put("title",title).put("positionMs",position).put("durationMs",duration)
                out.put("mode",if(raw.optBoolean("playing"))"playing" else if(duration>0)"paused" else "loading")
                if(lastId!=null&&lastTitle.isNotBlank()&&(title==lastTitle||title==lastTitle.substringBefore(" (")))out.put("contentId",lastId)
            }
            out
        }catch(_:Exception){out.put("mode","loading")}
    }
    private fun poll(){
        val activity=current.get()?:return
        if(activity.isDestroyed||activity.isFinishing||activity is AccessActivity||activity is IntroActivity)return
        // UI properties are read on the UI thread; network work never runs there.
        val ready=java.util.concurrent.CountDownLatch(1)
        main.post {try{state=snapshot(activity);if(activity.javaClass.simpleName=="EscolhaActivity")OtaManager.check(activity,false)}finally{ready.countDown()}}
        try { if(!ready.await(2,TimeUnit.SECONDS))return } catch(_:InterruptedException){return}
        try {
            val response=Gate.request(app,"POST","/api/connect/tv/poll",JSONObject().put("state",state),true)
            val packet=response.optJSONObject("command")?:return
            val id=packet.getString("id")
            val prefs=app.getSharedPreferences("guhflix-connect",0)
            val applied=JSONArray(prefs.getString("applied","[]"))
            var previous:JSONObject?=null
            for(i in 0 until applied.length()){val entry=applied.getJSONObject(i);if(entry.optString("id")==id)previous=entry}
            val result=if(previous!=null)previous!!.getJSONObject("result") else execute(activity,packet.getJSONObject("command"))
            if(previous==null){val next=JSONArray();for(i in maxOf(0,applied.length()-63) until applied.length())next.put(applied.get(i));next.put(JSONObject().put("id",id).put("result",result));prefs.edit().putString("applied",next.toString()).commit()}
            Gate.request(app,"POST","/api/connect/tv/commands/$id/ack",result,true)
            lastError=""
        }catch(e:Exception){lastError=if(e is Gate.ApiError)e.message?:"Falha de conexão" else "Não foi possível sincronizar o controle. Tente novamente."}
    }
    private fun execute(activity:Activity,command:JSONObject):JSONObject {
        return try {
            when(command.getString("type")){
                "play"->{
                    val contentId=command.getString("contentId")
                    val suffix=if(command.has("season"))"?season=${command.getInt("season")}&episode=${command.getInt("episode")}" else ""
                    val isLive=command.optBoolean("live")
                    val path=if(isLive)"/api/connect/live/$contentId/play" else "/api/connect/catalog/$contentId/play$suffix"
                    val playable=Gate.request(app,"GET",path,null,true)
                    val item=playable.getJSONObject("item")
                    if(item.getString("id")!=contentId)throw IllegalArgumentException("Título não corresponde ao pedido")
                    val full=if(item.optString("kind")=="series")Gate.request(app,"GET","/api/connect/catalog/$contentId",null,true) else item
                    playable.put("episodes",full.optJSONArray("episodes")?:JSONArray())
                    lastId=contentId;lastTitle=item.getString("title")
                    app.getSharedPreferences("guhflix-connect",0).edit().putString("contentId",lastId).putString("contentTitle",lastTitle).commit()
                    main.post {
                        if(activity.isDestroyed||activity.isFinishing)return@post
                        val target=Alvo(item.getString("title"),item.getString("kind")=="series",item.optString("letter", "L"),item.optString("year"),tmdbId=item.optInt("tmdbId"))
                        val intent=target.em(Intent(activity,PlayerActivity::class.java))
                            .putExtra("player.temporada",command.optInt("season"))
                            .putExtra("player.episodio",command.optInt("episode"))
                            .putExtra("guhflix.remote",playable.toString())
                            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        activity.startActivity(intent)
                    }
                }
                "pause","resume","seek","seekRelative","stop"->{
                    val done=java.util.concurrent.CountDownLatch(1)
                    var failure:Exception?=null
                    main.post {try{
                        val live=current.get()?:throw IllegalStateException()
                        if(command.getString("type")=="stop"){
                            if(live is PlayerActivity||live.javaClass.simpleName=="MainActivity")live.finish()
                        }else{
                            val listener=live as? Connect.Listener?:throw IllegalStateException()
                            val playing=listener.obterEstado().optBoolean("playing")
                            when(command.getString("type")){
                                "pause"->if(playing)listener.pausarOuRetomarVod()
                                "resume"->if(!playing)listener.pausarOuRetomarVod()
                                "seek"->listener.avancarOuVoltarVod(command.getLong("positionMs"),null)
                                "seekRelative"->listener.avancarOuVoltarVod(0,command.getLong("deltaMs"))
                            }
                        }
                    }catch(e:Exception){failure=e}finally{done.countDown()}}
                    if(!done.await(3,TimeUnit.SECONDS)||failure!=null)throw IllegalStateException()
                }
                else->throw IllegalArgumentException()
            }
            JSONObject().put("success",true).put("message","Comando recebido pela TV")
        }catch(e:Exception){JSONObject().put("success",false).put("message",if(e is Gate.ApiError)e.message?.take(240)?:"Falha no serviço" else "Não foi possível executar. Abra o player na televisão e tente novamente.")}
    }
    @JvmStatic fun liveCatalog(offset:Int):JSONObject=Gate.request(app,"GET","/api/connect/live?offset=${offset.coerceAtLeast(0)}",null,true)
    @JvmStatic fun playLive(activity:Activity,contentId:String){worker.execute{execute(activity,JSONObject().put("type","play").put("contentId",contentId).put("live",true))}}
    @JvmStatic fun playEpisode(activity:Activity,contentId:String,season:Int,episode:Int){worker.execute{execute(activity,JSONObject().put("type","play").put("contentId",contentId).put("season",season).put("episode",episode))}}
    @JvmStatic fun related(id:Int,series:Boolean):JSONObject?=try{Gate.request(app,"GET","/api/connect/catalog-related/"+(if(series)"series" else "movie")+"/$id",null,true)}catch(_:Exception){null}
    @JvmStatic fun metadata(path:String):JSONObject?=try{Gate.request(app,"GET","/api/connect/metadata/"+path,null,true)}catch(_:Exception){null}
    @JvmStatic fun application():Application=app
}
