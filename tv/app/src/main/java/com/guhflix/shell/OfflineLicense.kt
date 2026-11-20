package com.guhflix.shell
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import br.com.saimo.tv.BuildConfig
import com.gfilmes.license.*
import org.json.JSONObject
import java.security.*
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/** Private device key + encrypted license/checkpoint. No issuer private key is in this app. */
object OfflineLicense {
 private const val KEY="gfilmes-offline-device-v1"
 private fun pair():java.security.KeyStore {val k=KeyStore.getInstance("AndroidKeyStore");k.load(null);if(!k.containsAlias(KEY)){val generator=KeyPairGenerator.getInstance("RSA","AndroidKeyStore");generator.initialize(KeyGenParameterSpec.Builder(KEY,KeyProperties.PURPOSE_SIGN).setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256).setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).build());generator.generateKeyPair()};return k}
 private fun publicKey()=pair().getCertificate(KEY).publicKey
 private fun privateKey()=pair().getKey(KEY,null) as PrivateKey
 fun deviceId()=Crypto.sha(publicKey().encoded)
 private fun boot(context:Context):String=try{Settings.Global.getInt(context.contentResolver,"boot_count").toString()}catch(_:Exception){"elapsed-fallback"}
 private fun read(context:Context):JSONObject? {val raw=context.getSharedPreferences("gfilmes-offline",0).getString("sealed",null)?:return null;return try{val a=raw.split(':');val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,Gate.key(),GCMParameterSpec(128,Crypto.decode(a[0])));JSONObject(String(cipher.doFinal(Crypto.decode(a[1])),Charsets.UTF_8))}catch(_:Exception){null}}
 private fun write(context:Context,value:JSONObject){val cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,Gate.key());val sealed=Crypto.encode(cipher.iv)+":"+Crypto.encode(cipher.doFinal(value.toString().toByteArray(Charsets.UTF_8)));check(context.getSharedPreferences("gfilmes-offline",0).edit().putString("sealed",sealed).commit()){"Não foi possível guardar a licença"}}
 @JvmStatic @Synchronized fun remaining(context:Context):Long {return try{val value=read(context)?:return 0;val claims=LicenseVerifier(Crypto.publicKey(RootTrust.PUBLIC_KEY)).verify(value.getJSONObject("envelope"),deviceId());val next=LicenseClock.advance(ClockCheckpoint.parse(value.getJSONObject("clock")),System.currentTimeMillis(),SystemClock.elapsedRealtime(),boot(context));value.put("clock",next.json());write(context,value);LicenseClock.remaining(claims,next)}catch(_:Exception){0}}
 @JvmStatic fun valid(context:Context)=remaining(context)>0
 @JvmStatic fun claims(context:Context):LicenseClaims {val value=read(context)?:error("Ative este dispositivo");return LicenseVerifier(Crypto.publicKey(RootTrust.PUBLIC_KEY)).verify(value.getJSONObject("envelope"),deviceId())}
 @JvmStatic fun endpoint(context:Context):String {val origin=context.getSharedPreferences("gfilmes-offline",0).getString("endpoint",null)?:BuildConfig.GUHFLIX_API;val u=java.net.URI(origin);require(u.scheme=="https"&&u.host!=null&&u.userInfo==null&&u.rawQuery==null&&u.fragment==null&&u.path.orEmpty() in listOf("","/"));return origin.trimEnd('/')}
 @JvmStatic fun setEndpoint(context:Context,value:String){val previous=context.getSharedPreferences("gfilmes-offline",0);val u=java.net.URI(value.trim());require(u.scheme=="https"&&u.host!=null&&u.userInfo==null&&u.rawQuery==null&&u.fragment==null&&u.path.orEmpty() in listOf("","/")){"Use apenas a origem HTTPS do servidor"};previous.edit().putString("endpoint",value.trim().trimEnd('/')).commit();Gate.access="";context.getSharedPreferences("guhflix-session",0).edit().remove("refresh").commit()}
 @JvmStatic fun activate(context:Context){
  val challenge=Gate.request(context,"GET","/api/offline/challenge",null,true);val nonce=challenge.getString("nonce");val signer=Signature.getInstance("SHA256withRSA");signer.initSign(privateKey());signer.update(("gfilmes-device-proof-v1|"+nonce+"|"+deviceId()).toByteArray(Charsets.UTF_8));
  val body=JSONObject().put("nonce",nonce).put("devicePublicKey",Crypto.encode(publicKey().encoded)).put("proof",Crypto.encode(signer.sign())).put("label",(android.os.Build.MANUFACTURER+" "+android.os.Build.MODEL).take(100));val envelope=Gate.request(context,"POST","/api/offline/activate",body,true)
  val claims=LicenseVerifier(Crypto.publicKey(RootTrust.PUBLIC_KEY)).verify(envelope,deviceId());val trusted=envelope.getLong("serverTime");require(trusted>=claims.issuedAt&&trusted<claims.expiresAt&&trusted-claims.issuedAt<60000)
  write(context,JSONObject().put("envelope",envelope).put("clock",ClockCheckpoint(trusted,System.currentTimeMillis(),SystemClock.elapsedRealtime(),boot(context)).json()))
 }
 @JvmStatic fun ensureActivated(context:Context){if(valid(context))return;try{activate(context)}catch(e:Gate.ApiError){throw e}catch(_:Exception){throw Gate.ApiError(403,"INVALID_LICENSE","Não foi possível validar a licença assinada. Confira o servidor de ativação.")}}
 @JvmStatic fun clear(context:Context){context.getSharedPreferences("gfilmes-offline",0).edit().remove("sealed").commit()}
}
