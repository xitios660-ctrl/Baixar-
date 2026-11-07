package com.guhflix.shell

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import org.json.JSONObject
import java.util.UUID

/** A second decoder consumes its own existing account playback slot. */
class SecondLease(private val a:Activity,private val lost:()->Unit){
 private val id=UUID.randomUUID().toString()
 private val handler=Handler(Looper.getMainLooper())
 private var stopped=false
 private var until=0L
 private val beat=object:Runnable{override fun run(){
  if(stopped)return
  Gate.work.execute {try {Gate.request(a,"POST","/api/app/playback/heartbeat",JSONObject().put("playbackId",id),true);handler.post {if(!stopped){until=SystemClock.elapsedRealtime()+110000;handler.postDelayed(this,35000)}}}catch(_:Exception){handler.post{if(!stopped){if(SystemClock.elapsedRealtime()>=until){stop();lost()}else handler.postDelayed(this,10000)}}}}
 }}
 private val watchdog=object:Runnable{override fun run(){if(stopped)return;if(SystemClock.elapsedRealtime()>=until){stop();lost()}else handler.postDelayed(this,1000)}}
 fun start(ready:()->Unit){
  Gate.work.execute{try{Gate.request(a,"POST","/api/app/playback/start",JSONObject().put("playbackId",id),true);handler.post{if(stopped||a.isFinishing||a.isDestroyed){stopRemote()}else{until=SystemClock.elapsedRealtime()+110000;handler.postDelayed(beat,35000);handler.post(watchdog);ready()}}}catch(e:Exception){handler.post{stopped=true;Toast.makeText(a,if(e is Gate.ApiError)e.message else "Não foi possível liberar a segunda tela.",Toast.LENGTH_LONG).show();lost()}}}
 }
 fun stop(){if(stopped)return;stopped=true;handler.removeCallbacks(beat);handler.removeCallbacks(watchdog);stopRemote()}
 private fun stopRemote(){Gate.work.execute{try{Gate.request(a,"POST","/api/app/playback/stop",JSONObject().put("playbackId",id),true)}catch(_:Exception){}}}
}
