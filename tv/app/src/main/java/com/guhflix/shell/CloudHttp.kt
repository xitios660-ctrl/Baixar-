package com.guhflix.shell

import br.com.saimo.tv.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

object CloudHttp {
 @JvmStatic fun catalogFile(name:String):String? {
  // Local source IO must never wait on Gate while its caller awaits a coroutine.
  if(BuildConfig.GFILMES_OFFLINE||BuildConfig.GFILMES_LOCAL)return try{val ctx=CloudRemote.application();if(BuildConfig.GFILMES_OFFLINE)check(OfflineLicense.valid(ctx));LocalCatalog.file(ctx,name.replace("%23","#"))}catch(_:Exception){null}
  return synchronized(Gate::class.java){
  val safeName=name.replace("%23","#")
  require(safeName.matches(Regex("(?:[A-Za-z0-9_#-]+\\.txt|redeflix/links-(?:animes|doramas)\\.txt)")))
  try {
   fun fetch(retry:Boolean):String {
    if(Gate.access.isEmpty())Gate.renew(CloudRemote.application())
    val token=Gate.access
    val connection=URL(BuildConfig.GUHFLIX_API+"/api/connect/catalog-files/"+safeName.split("/").joinToString("/"){android.net.Uri.encode(it)}).openConnection() as HttpURLConnection
    connection.connectTimeout=15000;connection.readTimeout=30000;connection.instanceFollowRedirects=false
    connection.setRequestProperty("Authorization","Bearer $token")
    try {
     if(connection.responseCode==401&&retry){Gate.renew(CloudRemote.application());return fetch(false)}
     check(connection.responseCode==200){"Catálogo indisponível"}
     return connection.inputStream.use { input->
      val limit=if(safeName.startsWith("redeflix/links-"))32*1024*1024 else 12*1024*1024
      val out=java.io.ByteArrayOutputStream();val buf=ByteArray(16384)
      while(true){val n=input.read(buf);if(n<0)break;check(out.size()+n<=limit);out.write(buf,0,n)}
      out.toString("UTF-8")
     }
    } finally {connection.disconnect()}
   }
   fetch(true)
  }catch(_:Exception){null}
 }
}
}
