package com.gfilmes.server

import android.app.*
import android.content.*
import android.os.*
import com.gfilmes.license.Crypto
import org.json.JSONObject
import java.net.NetworkInterface
import java.net.Inet4Address
import java.security.SecureRandom

/** User-started, private IPv4 only, fifteen-minute configuration window. */
class SetupTransferService:Service(){
 private var portal:SetupPortal?=null;private var starting=false;private var destroyed=false;private val lifecycleLock=Any()
 private val handler=Handler(Looper.getMainLooper())
 override fun onBind(intent:Intent?)=null
 override fun onCreate(){super.onCreate();val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel("setup","Configuração local Gfilmes",NotificationManager.IMPORTANCE_LOW));val pending=PendingIntent.getActivity(this,1,Intent(this,ServerActivity::class.java),PendingIntent.FLAG_IMMUTABLE);startForeground(2,Notification.Builder(this,"setup").setContentTitle("Gfilmes · configuração pela rede local").setContentText("Janela temporária de 15 minutos. Feche ao terminar.").setSmallIcon(android.R.drawable.ic_lock_lock).setContentIntent(pending).build());handler.postDelayed({stopSelf()},15*60*1000L)}
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(portal!=null||starting)return START_NOT_STICKY;starting=true
  Thread {try {
   val address=NetworkInterface.getNetworkInterfaces().toList().filter{it.isUp&&!it.isLoopback}.flatMap{it.inetAddresses.toList()}.filterIsInstance<Inet4Address>().firstOrNull{it.isSiteLocalAddress}?:error("Conecte a TV ao Wi-Fi ou cabo de rede local")
   val token=Crypto.sha(ByteArray(32).also{SecureRandom().nextBytes(it)})
   val store=ServerStore(this)
   val server=SetupPortal(address,17861,token,Crypto.encode(ServerKeys.publicKey().encoded).toByteArray(),{kind,bytes->
    when(kind){
     "grant"->store.installGrant(String(bytes,Charsets.UTF_8).trim())
     "migration"->store.importSnapshot(JSONObject(String(bytes,Charsets.UTF_8)))
     "configuration"->store.importConfiguration(JSONObject(String(bytes,Charsets.UTF_8)))
     "restore"->{val operation=takeRecovery()?:error("Prepare a recuperação na TV primeiro");operation(bytes)}
    }
   },{exportedBytes})
   synchronized(lifecycleLock){if(destroyed){server.close();return@Thread};portal=server;server.start();addressUrl="http://${address.hostAddress}:17861/$token/";status="Configuração local aberta por até 15 minutos"}
  }catch(_:Exception){status="Não foi possível abrir a configuração. Confira a rede local e tente novamente.";stopSelf()}}.start()
  return START_NOT_STICKY
 }
 override fun onTimeout(startId:Int,fgsType:Int){stopSelf()}
 override fun onDestroy(){handler.removeCallbacksAndMessages(null);synchronized(lifecycleLock){destroyed=true;portal?.close();portal=null;addressUrl="";status="Configuração local fechada"};exportedBytes=null;clearRecovery();super.onDestroy()}
 companion object {
  @Volatile var addressUrl="";@Volatile var status="Configuração local fechada"
  @Volatile var exportedBytes:ByteArray?=null
  private var recovery:((ByteArray)->Unit)?=null;private var wipe:(()->Unit)?=null
  @Synchronized fun armRecovery(operation:(ByteArray)->Unit,cleanup:()->Unit){clearRecovery();recovery=operation;wipe=cleanup}
  @Synchronized private fun takeRecovery():((ByteArray)->Unit)? {val op=recovery?:return null;val cleanup=wipe;recovery=null;wipe=null;return {bytes->try{op(bytes)}finally{cleanup?.invoke()}}}
  @Synchronized private fun clearRecovery(){wipe?.invoke();wipe=null;recovery=null}
 }
}
