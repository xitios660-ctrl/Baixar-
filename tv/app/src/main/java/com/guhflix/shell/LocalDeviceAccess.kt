package com.guhflix.shell
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import br.com.saimo.tv.BuildConfig
import com.gfilmes.license.*
import org.json.JSONObject
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
object LocalDeviceAccess {
 fun code()=OfflineLicense.deviceId()
 private fun boot(c:Context)=try{Settings.Global.getInt(c.contentResolver,"boot_count").toString()}catch(_:Exception){"fallback"}
 @JvmStatic @Synchronized fun remaining(c:Context):Long {
  if(!BuildConfig.GFILMES_LOCAL)return Long.MAX_VALUE
  require(BuildConfig.GFILMES_LOCAL_GRANT.isNotBlank()){"Esta edição precisa de um APK autorizado para o código desta TV"}
  val p=DeviceGrant.verify(BuildConfig.GFILMES_LOCAL_GRANT,code());val prefs=c.getSharedPreferences("local-device-grant",0);val wall=System.currentTimeMillis();val elapsed=SystemClock.elapsedRealtime();val boot=boot(c)
  val raw=prefs.getString("checkpoint",null)
  val old=if(raw==null){require(wall>=p.getLong("issuedAt")-120000){"Confira a data da TV"};ClockCheckpoint(maxOf(wall,p.getLong("issuedAt")),wall,elapsed,boot)}else{val parts=raw.split(':');val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,Gate.key(),GCMParameterSpec(128,Crypto.decode(parts[0])));ClockCheckpoint.parse(JSONObject(String(cipher.doFinal(Crypto.decode(parts[1])),Charsets.UTF_8)))}
  val next=LicenseClock.advance(old,wall,elapsed,boot)
  val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,Gate.key());check(prefs.edit().putString("checkpoint",Crypto.encode(cipher.iv)+":"+Crypto.encode(cipher.doFinal(next.json().toString().toByteArray()))).commit())
  require(!next.blocked){"Data da TV alterada. Solicite suporte para conferir o acesso"}
  return (p.getLong("expiresAt")-next.trustedMs).coerceAtLeast(0)
 }
 fun requireAccess(c:Context){require(remaining(c)>0){"Acesso vencido. Solicite um novo APK autorizado"}}
 fun status(c:Context):JSONObject {val code=code();return try{requireAccess(c);JSONObject().put("authorized",true).put("deviceCode",code)}catch(e:Exception){JSONObject().put("authorized",false).put("deviceCode",code).put("message",e.message?:"Acesso indisponível")}}
}
