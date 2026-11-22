package com.guhflix.shell
import org.json.JSONObject
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
/** Parent-only PIN. The child latch persists across process/TV restarts; titles require explicit approval. */
class ParentalControl(private val read:()->String?,private val write:(String)->Unit,private val now:()->Long={System.currentTimeMillis()}){
 private fun state()=JSONObject(read()?:"{}")
 private fun save(s:JSONObject)=write(s.toString())
 private fun hex(bytes:ByteArray)=bytes.joinToString(""){"%02x".format(it)}
 private fun derive(pin:String,salt:String):String {val p=PBEKeySpec(pin.toCharArray(),salt.chunked(2).map{it.toInt(16).toByte()}.toByteArray(),210000,256);return try{hex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(p).encoded)}finally{p.clearPassword()}}
 fun configured()=state().has("hash")
 fun locked()=state().optBoolean("childLock")
 @Synchronized fun configure(pin:String){require(!configured()){"O controle infantil já está configurado"};require(pin.matches(Regex("[0-9]{4,8}"))){"Defina um PIN de responsável com 4 a 8 números"};val s=state();val salt=hex(ByteArray(16).also{SecureRandom().nextBytes(it)});s.put("salt",salt).put("hash",derive(pin,salt));save(s)}
 @Synchronized fun verify(pin:String){val s=state();require(s.has("hash")){"Configure o controle infantil"};require(now()>=s.optLong("retryAt")){"RESPONSAVEL:Aguarde um minuto antes de tentar novamente"};val match=pin.matches(Regex("[0-9]{4,8}"))&&MessageDigest.isEqual(derive(pin,s.getString("salt")).toByteArray(),s.getString("hash").toByteArray());if(!match){val count=s.optInt("attempts")+1;s.put("attempts",if(count>=5)0 else count);if(count>=5)s.put("retryAt",now()+60000);save(s);throw IllegalArgumentException("RESPONSAVEL:PIN do responsável incorreto")};s.put("attempts",0).put("retryAt",0);save(s)}
 fun requireAdult(pin:String){if(locked()){if(pin.isEmpty())throw IllegalArgumentException("RESPONSAVEL:Digite o PIN do responsável");verify(pin)}}
 @Synchronized fun select(kids:Boolean,pin:String){if(!kids)requireAdult(pin) else require(configured()){"Configure o PIN do responsável antes do perfil infantil"};val s=state();s.put("childLock",kids);save(s)}
 fun allowed(key:String):Boolean {val a=state().optJSONArray("approved")?:return false;for(i in 0 until a.length())if(a.optString(i)==key)return true;return false}
 @Synchronized fun approve(key:String,enabled:Boolean,pin:String){require(key.matches(Regex("(?:vod|anime):[a-f0-9]{32}")));requireAdult(pin);val s=state();val old=s.optJSONArray("approved")?:JSONArray();val next=JSONArray();for(i in 0 until old.length())if(old.getString(i)!=key)next.put(old.getString(i));if(enabled){require(next.length()<1000){"Limite de títulos aprovados atingido"};next.put(key)};s.put("approved",next);save(s)}
 fun approvedKeys():Set<String> {val a=state().optJSONArray("approved")?:JSONArray();return (0 until a.length()).map{a.getString(it)}.toSet()}
 fun summary()=JSONObject().put("configured",configured()).put("childLock",locked())
}
