package com.gfilmes.server
import org.json.JSONObject
import com.gfilmes.license.*
import java.security.Signature
import java.security.SecureRandom
import java.util.UUID

class ApiFailure(val status:Int,val code:String,message:String):Exception(message)
/** No administrative write endpoints are exposed over HTTP. */
class AuthorityEngine(val store:ServerStore){
 private data class Session(val user:String,val expires:Long)
 private data class Challenge(val user:String,val token:String,val expires:Long)
 private val sessions=mutableMapOf<String,Session>();private val refresh=mutableMapOf<String,Session>();private val challenges=mutableMapOf<String,Challenge>()
 private val attempts=ArrayDeque<Long>()
 private fun token()=Crypto.encode(ByteArray(32).also{SecureRandom().nextBytes(it)})
 private fun fail(status:Int,code:String,message:String):Nothing=throw ApiFailure(status,code,message)
 private fun authorized(id:String):JSONObject {val u=store.get("user",id)?:fail(401,"INVALID_SESSION","Entre novamente");if(u.optString("status")!="enabled")fail(403,"ACCOUNT_BLOCKED","Conta bloqueada");if(u.optString("role")!="customer")fail(403,"CUSTOMER_REQUIRED","Use uma conta de cliente");if(u.optLong("expiresAt")<=System.currentTimeMillis())fail(403,"SUBSCRIPTION_EXPIRED","Assinatura vencida");return u}
 private fun user(auth:String):JSONObject {val s=sessions[auth.removePrefix("Bearer ")]?:fail(401,"INVALID_SESSION","Entre novamente");if(s.expires<=System.currentTimeMillis())fail(401,"INVALID_SESSION","Sessão expirada");return authorized(s.user)}
 private fun tokens(id:String):JSONObject {val now=System.currentTimeMillis();sessions.entries.removeAll{it.value.expires<now};refresh.entries.removeAll{it.value.expires<now};if(sessions.size>=256||refresh.size>=256)fail(429,"BUSY","Aguarde e tente novamente");val a=token();val r=token();sessions[a]=Session(id,now+900000);refresh[r]=Session(id,now+3600000);return JSONObject().put("accessToken",a).put("refreshToken",r).put("expiresIn",900)}
 @Synchronized fun handle(method:String,path:String,body:JSONObject,auth:String):JSONObject {
  if(method=="GET"&&path=="/health")return JSONObject().put("status","ok").put("service","gfilmes-activation").put("configured",store.ready()&&store.authority()!=null)
  if(method=="POST"&&path=="/api/auth/login"){
   val now=System.currentTimeMillis();while(attempts.isNotEmpty()&&attempts.first()<now-60000)attempts.removeFirst();if(attempts.size>=8)fail(429,"RATE_LIMIT","Aguarde um minuto");attempts.add(now)
   val u=store.userByName(body.optString("username"));val valid=PasswordHash.verifyKnownOrDummy(u?.optString("passwordHash"),body.optString("password"));if(u==null||!valid)fail(401,"INVALID_CREDENTIALS","Usuário ou senha incorretos");authorized(u.getString("id"));return tokens(u.getString("id"))
  }
  if(method=="POST"&&path=="/api/auth/refresh"){val s=refresh.remove(body.optString("refreshToken"))?:fail(401,"INVALID_SESSION","Entre novamente");if(s.expires<=System.currentTimeMillis())fail(401,"INVALID_SESSION","Entre novamente");authorized(s.user);return tokens(s.user)}
  val u=user(auth);val uid=u.getString("id");val now=System.currentTimeMillis()
  if(method=="GET"&&path=="/api/auth/me")return JSONObject().put("id",uid).put("username",u.getString("username")).put("role","customer").put("expiresAt",java.time.Instant.ofEpochMilli(u.getLong("expiresAt")).toString())
  if(method=="GET"&&path=="/api/app/access")return JSONObject().put("allowed",true).put("maxDevices",3)
  if(method=="GET"&&path=="/api/offline/challenge"){challenges.entries.removeAll{it.value.expires<now};if(challenges.size>=256)fail(429,"BUSY","Aguarde");val nonce=token();challenges[nonce]=Challenge(uid,auth,now+60000);return JSONObject().put("nonce",nonce).put("serverTime",now)}
  if(method!="POST"||path!="/api/offline/activate")fail(404,"NOT_FOUND","Rota indisponível no servidor de ativação")
  val nonce=body.getString("nonce");val c=challenges.remove(nonce);if(c==null||c.expires<now||c.user!=uid||c.token!=auth)fail(403,"DEVICE_CHALLENGE","Desafio expirado")
  val pub=Crypto.publicKey(body.getString("devicePublicKey"));val device=Crypto.sha(pub.encoded);val proof=Signature.getInstance("SHA256withRSA");proof.initVerify(pub);proof.update(("gfilmes-device-proof-v1|"+nonce+"|"+device).toByteArray());if(!proof.verify(Crypto.decode(body.getString("proof"))))fail(403,"DEVICE_PROOF","Identidade inválida")
  return synchronized(store){val db=store.writableDatabase;db.beginTransaction();try{
  val fresh=authorized(uid)
  val grant=store.authority()?:fail(503,"AUTHORITY_MISSING","Importe a autorização pública do titular");val a=Crypto.verifyJws(grant,Crypto.publicKey(RootTrust.PUBLIC_KEY));if(now<a.getLong("notBefore")||now>=a.getLong("expiresAt"))fail(503,"AUTHORITY_EXPIRED","Autoridade vencida")
  val id=uid+":"+device;val old=store.get("device",id);if(old?.optBoolean("retired")==true)fail(403,"DEVICE_RETIRED","Dispositivo removido");val active=store.list("device",uid).count{it.optLong("expiresAt")>now};if((old==null||old.optLong("expiresAt")<=now)&&active>=3)fail(409,"DEVICE_LIMIT","Três dispositivos ainda possuem licenças válidas")
  val expires=minOf(now+a.getLong("maxLicenseMs"),fresh.getLong("expiresAt"),a.getLong("expiresAt"));require(expires>now)
  val claims=JSONObject().put("v",1).put("kind","gfilmes-offline-license").put("issuerId",ServerKeys.id()).put("licenseId",UUID.randomUUID().toString()).put("customerId",uid).put("username",fresh.getString("username")).put("deviceId",device).put("issuedAt",now).put("expiresAt",expires).put("subscriptionExpiresAt",fresh.getLong("expiresAt"))
  val envelope=JSONObject().put("authority",grant).put("license",Crypto.signJws(claims,ServerKeys.privateKey())).put("serverTime",now)
  store.put("device",id,JSONObject().put("id",id).put("deviceId",device).put("label",body.optString("label","Android TV").take(100)).put("expiresAt",expires).put("retired",false),uid);store.put("license",claims.getString("licenseId"),claims,uid);store.audit(uid,"license_issue",uid,claims);db.setTransactionSuccessful();envelope
  }finally{db.endTransaction()}}
 }
 @Synchronized fun adminLogin(name:String,password:String):String {val now=System.currentTimeMillis();while(attempts.isNotEmpty()&&attempts.first()<now-60000)attempts.removeFirst();require(attempts.size<8){"Aguarde um minuto"};attempts.add(now);val u=store.userByName(name);require(u!=null&&u.optString("role")=="admin"&&u.optString("status")=="enabled"&&PasswordHash.verify(u.getString("passwordHash"),password)){"Credenciais administrativas inválidas"};return u.getString("id")}
}
