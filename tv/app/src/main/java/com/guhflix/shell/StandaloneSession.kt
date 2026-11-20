package com.guhflix.shell
import org.json.JSONObject
import org.json.JSONArray

/** Local profile, not a customer account or a signed subscription. Only enabled in a separate APK. */
object StandaloneSession {
 class Unavailable:IllegalStateException("Recurso indisponível nesta edição local")
 @JvmStatic fun reply(method:String,path:String):JSONObject? {
  if(path=="/api/auth/me"&&method=="GET")return JSONObject().put("id","this-tv").put("username","Minha TV").put("role","local").put("local",true).put("expiresAt",JSONObject.NULL)
  if(path=="/api/app/access"&&method=="GET")return JSONObject().put("state","active").put("local",true).put("expiresAt",JSONObject.NULL).put("maxStreams",JSONObject.NULL)
  if(method=="POST"&&path in listOf("/api/app/playback/start","/api/app/playback/heartbeat","/api/app/playback/stop","/api/auth/logout"))return JSONObject().put("success",true).put("local",true)
  if(path=="/api/app/sessions"&&method=="GET")return JSONObject().put("items",JSONArray())
  if(path.startsWith("/api/connect/catalog-files/")||path=="/api/connect/home"||path.startsWith("/api/connect/catalog")||path.startsWith("/api/connect/live"))return null
  throw Unavailable()
 }
}
