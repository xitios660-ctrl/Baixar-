package com.gfilmes.server
import javax.crypto.*
import javax.crypto.spec.*
import java.security.SecureRandom
import org.json.JSONObject
import com.gfilmes.license.Crypto
object BackupCodec {
 private fun key(password:CharArray,salt:ByteArray):javax.crypto.SecretKey {require(password.size>=14){"Use uma senha de backup com ao menos 14 caracteres"};val spec=PBEKeySpec(password,salt,310000,256);try{return SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES")}finally{spec.clearPassword()}}
 fun encrypt(bytes:ByteArray,password:CharArray):JSONObject {val salt=ByteArray(16).also{SecureRandom().nextBytes(it)};val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key(password,salt));return JSONObject().put("v",1).put("salt",Crypto.encode(salt)).put("iv",Crypto.encode(c.iv)).put("encrypted",java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(c.doFinal(bytes)))}
 fun decrypt(value:JSONObject,password:CharArray):ByteArray {require(value.getInt("v")==1);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(password,Crypto.decode(value.getString("salt"))),GCMParameterSpec(128,Crypto.decode(value.getString("iv"))));return c.doFinal(java.util.Base64.getUrlDecoder().decode(value.getString("encrypted")))}
}
