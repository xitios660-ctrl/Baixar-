package com.guhflix.shell

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local family profiles. PINs protect casual access, not a rooted/repackaged device. */
class FamilyProfiles(private val read:()->String?, private val write:(String)->Unit, private val now:()->Long={System.currentTimeMillis()}) {
 @Volatile private var selected:String?=null
 private var attempts=0
 private var retryAt=0L
 private fun rows()=JSONArray(read()?:"[]")
 private fun save(a:JSONArray)=write(a.toString())
 fun activeId():String?=selected
 fun lock(){selected=null}
 fun list():JSONObject {val out=JSONArray();val a=rows();for(i in 0 until a.length()){val p=a.getJSONObject(i);out.put(JSONObject().put("id",p.getString("id")).put("name",p.getString("name")).put("avatar",p.optInt("avatar")).put("locked",p.has("hash")))};return JSONObject().put("items",out).put("activeId",selected?:JSONObject.NULL)}
 private fun pin(pin:String){require(pin.matches(Regex("[0-9]{4,8}"))){"Use um PIN com 4 a 8 números"}}
 private fun derive(pin:String,salt:String):ByteArray {val spec=PBEKeySpec(pin.toCharArray(),salt.chunked(2).map{it.toInt(16).toByte()}.toByteArray(),210000,256);return try{SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded}finally{spec.clearPassword()}}
 private fun hex(b:ByteArray)=b.joinToString(""){"%02x".format(it)}
 private fun authorize(p:JSONObject,input:String){
  require(now()>=retryAt){"Aguarde um minuto antes de tentar novamente"}
  val valid=!p.has("hash")||MessageDigest.isEqual(derive(input,p.getString("salt")),p.getString("hash").chunked(2).map{it.toInt(16).toByte()}.toByteArray())
  if(!valid){attempts++;if(attempts>=5){retryAt=now()+60000;attempts=0};throw IllegalArgumentException("PIN incorreto")};attempts=0
 }
 private fun protect(p:JSONObject,value:String){if(value.isEmpty()){p.remove("hash");p.remove("salt")}else{pin(value);val salt=hex(ByteArray(16).also{SecureRandom().nextBytes(it)});p.put("salt",salt).put("hash",hex(derive(value,salt)))}}
 @Synchronized fun create(name:String,avatar:Int,input:String):JSONObject {val a=rows();require(a.length()<6){"Você pode criar até 6 perfis"};require(name.trim().length in 1..24){"Use um nome de até 24 caracteres"};val p=JSONObject().put("id",UUID.randomUUID().toString()).put("name",name.trim()).put("avatar",avatar.coerceIn(0,5));protect(p,input);a.put(p);save(a);return list()}
 @Synchronized fun select(id:String,input:String):JSONObject {val p=find(id);authorize(p,input);selected=id;return JSONObject().put("id",id).put("name",p.getString("name"))}
 @Synchronized fun update(id:String,name:String,avatar:Int,oldPin:String,newPin:String):JSONObject {require(selected==id){"Entre no perfil para editar"};val a=rows();for(i in 0 until a.length()){val p=a.getJSONObject(i);if(p.getString("id")==id){authorize(p,oldPin);require(name.trim().length in 1..24){"Use um nome de até 24 caracteres"};p.put("name",name.trim()).put("avatar",avatar.coerceIn(0,5));protect(p,newPin);save(a);return list()}};error("Perfil não encontrado")}
 @Synchronized fun delete(id:String,input:String):JSONObject {require(selected==id){"Entre no perfil para excluir"};authorize(find(id),input);val a=rows();val out=JSONArray();for(i in 0 until a.length())if(a.getJSONObject(i).getString("id")!=id)out.put(a.getJSONObject(i));save(out);selected=null;return list()}
 private fun find(id:String):JSONObject {val a=rows();for(i in 0 until a.length())if(a.getJSONObject(i).getString("id")==id)return a.getJSONObject(i);error("Perfil não encontrado")}
}
