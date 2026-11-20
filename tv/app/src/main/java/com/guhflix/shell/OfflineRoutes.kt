package com.guhflix.shell
import android.content.Context
import br.com.saimo.tv.BuildConfig
import org.json.JSONObject

/** Only the explicit offline APK changes access rules. Online 2.x remains unchanged. */
object OfflineRoutes {
 @JvmStatic fun handle(context:Context,method:String,path:String,body:JSONObject?):JSONObject? {
  if(!BuildConfig.GFILMES_OFFLINE||path.startsWith("/api/auth/login")||path.startsWith("/api/auth/refresh")||path.startsWith("/api/offline/"))return null
  if(path=="/api/auth/logout"){OfflineLicense.clear(context);return JSONObject().put("success",true)}
  if(!OfflineLicense.valid(context))throw Gate.ApiError(403,"OFFLINE_EXPIRED","Licença ausente, vencida ou relógio alterado. Conecte ao servidor para autorizar novamente.")
  val license=OfflineLicense.claims(context)
  if(path=="/api/app/access")return JSONObject().put("state","active").put("expiresAt",license.expiresAt).put("offline",true).put("maxDevices",3).put("maxStreams",JSONObject.NULL)
  if(path=="/api/auth/me")return JSONObject().put("id",license.customerId).put("username",license.username).put("role","customer").put("expiresAt",license.expiresAt)
  if(path.startsWith("/api/app/playback/"))return JSONObject().put("success",true).put("offline",true)
  if(path=="/api/app/sessions")return JSONObject().put("items",org.json.JSONArray())
  if(path.startsWith("/api/connect/catalog-files/"))return JSONObject().put("text",LocalCatalog.file(context,path.substringAfter("/api/connect/catalog-files/")))
  if(path.startsWith("/api/connect/metadata/"))throw Gate.ApiError(503,"OFFLINE_METADATA","Informações extras exigem um serviço de metadados; catálogo e player locais continuam disponíveis")
  if(path.startsWith("/api/connect/"))return LocalCatalog.route(context,method,path,body)
  throw Gate.ApiError(503,"OFFLINE_SERVICE","Este recurso precisa de um serviço online separado")
 }
}
