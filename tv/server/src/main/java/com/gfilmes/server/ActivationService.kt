package com.gfilmes.server
import android.app.*
import android.content.*
import android.os.*
import org.json.JSONObject
import java.net.*
import java.io.*
import java.security.KeyStore
import javax.net.ssl.*
import java.util.concurrent.*

class ActivationService:Service(){
 private var listener:ServerSocket?=null
 private val pool=ThreadPoolExecutor(2,4,30,TimeUnit.SECONDS,ArrayBlockingQueue<Runnable>(16))
 override fun onBind(intent:Intent?)=null
 override fun onCreate(){super.onCreate();val nm=getSystemService(NotificationManager::class.java);nm.createNotificationChannel(NotificationChannel("activation","Ativação Gfilmes",NotificationManager.IMPORTANCE_LOW));val pending=PendingIntent.getActivity(this,0,Intent(this,ServerActivity::class.java),PendingIntent.FLAG_IMMUTABLE);startForeground(1,Notification.Builder(this,"activation").setContentTitle("Gfilmes Servidor · janela de ativação").setContentText("Mantenha a TV ligada. Acesso externo depende de HTTPS e rede.").setSmallIcon(android.R.drawable.ic_lock_lock).setContentIntent(pending).build())}
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {if(listener!=null)return START_NOT_STICKY;Thread{
  try{val store=ServerStore(this);require(store.ready()&&store.authority()!=null){"Importe banco e autorização"};val tls=store.get("config","tls")?:error("Importe certificado HTTPS PKCS12 válido");val ks=KeyStore.getInstance("PKCS12");val password=tls.getString("password").toCharArray();ks.load(ByteArrayInputStream(ServerStore.decodeLarge(tls.getString("p12"))),password);val km=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());km.init(ks,password);password.fill('\u0000');val ctx=SSLContext.getInstance("TLS");ctx.init(km.keyManagers,null,null);val server=ctx.serverSocketFactory.createServerSocket(17862,16,InetAddress.getByName("0.0.0.0")) as SSLServerSocket;server.enabledProtocols=server.supportedProtocols.filter{it=="TLSv1.2"||it=="TLSv1.3"}.toTypedArray();listener=server;running=true;status="HTTPS na porta 17862; alcance externo não comprovado";val engine=AuthorityEngine(store)
   while(!server.isClosed){val socket=server.accept();try{pool.execute{serve(socket,engine)}}catch(_:RejectedExecutionException){socket.close()}}
  }catch(_:Exception){status="Serviço parado: confira certificado, banco e autorização";stopSelf()}
 }.start();return START_NOT_STICKY}
 private fun serve(socket:Socket,engine:AuthorityEngine){socket.use{s->s.soTimeout=10000;try{val input=s.getInputStream();var size=0
  fun line():String{val out=ByteArrayOutputStream();while(true){val b=input.read();if(b<0)throw EOFException();size++;require(size<8192);if(b==10)break;if(b!=13)out.write(b)};return out.toString("US-ASCII")}
  val first=line().split(' ');require(first.size==3&&first[2]=="HTTP/1.1");val headers=mutableMapOf<String,String>();while(true){val l=line();if(l.isEmpty())break;val at=l.indexOf(':');require(at>0);val k=l.substring(0,at).lowercase();require(!headers.containsKey(k));headers[k]=l.substring(at+1).trim()};require(!headers.containsKey("transfer-encoding"));val length=headers["content-length"]?.toInt()?:0;require(length in 0..16384);val bytes=ByteArray(length);var offset=0;while(offset<length){val n=input.read(bytes,offset,length-offset);if(n<0)throw EOFException();offset+=n};val body=if(length==0)JSONObject() else JSONObject(String(bytes,Charsets.UTF_8));val result=engine.handle(first[0],first[1].substringBefore('?'),body,headers["authorization"].orEmpty());respond(s,200,result)
 }catch(e:ApiFailure){respond(s,e.status,JSONObject().put("error",e.code).put("message",e.message))}catch(_:Exception){try{respond(s,400,JSONObject().put("error","BAD_REQUEST").put("message","Requisição inválida"))}catch(_:Exception){}}}}
 private fun respond(s:Socket,status:Int,value:JSONObject){val data=value.toString().toByteArray(Charsets.UTF_8);s.getOutputStream().apply{write(("HTTP/1.1 $status Response\r\nContent-Type: application/json; charset=utf-8\r\nContent-Length: ${data.size}\r\nConnection: close\r\nCache-Control: no-store\r\n\r\n").toByteArray());write(data);flush()}}
 override fun onTimeout(startId:Int,fgsType:Int){status="Janela encerrada pelo Android; abra o app para reiniciar";stopSelf()}
 override fun onDestroy(){running=false;try{listener?.close()}catch(_:Exception){};pool.shutdownNow();super.onDestroy()}
 companion object{@Volatile var running=false;@Volatile var status="Parado; exige início manual e certificado HTTPS"}
}
class BootReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){if(intent.action==Intent.ACTION_BOOT_COMPLETED){context.getSharedPreferences("server",0).edit().putBoolean("restartNeeded",true).apply()}}}
