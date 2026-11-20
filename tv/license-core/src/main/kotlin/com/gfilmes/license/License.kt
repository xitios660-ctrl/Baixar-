package com.gfilmes.license
import java.security.PublicKey
import org.json.JSONObject

const val MAX_LICENSE_MS=30L*24*60*60*1000

data class LicenseClaims(val id:String,val customerId:String,val username:String,val deviceId:String,val issuedAt:Long,val expiresAt:Long,val subscriptionExpiresAt:Long,val version:Int)
class LicenseVerifier(private val root:PublicKey){
 fun verify(envelope:JSONObject,deviceId:String):LicenseClaims {
  val authority=Crypto.verifyJws(envelope.getString("authority"),root)
  require(authority.getInt("v")==1&&authority.getString("kind")=="gfilmes-authority")
  val key=Crypto.publicKey(authority.getString("publicKey"));val issuer=Crypto.sha(key.encoded)
  require(authority.getString("issuerId")==issuer&&authority.getLong("maxLicenseMs") in 1..MAX_LICENSE_MS)
  val p=Crypto.verifyJws(envelope.getString("license"),key)
  require(p.getInt("v")==1&&p.getString("kind")=="gfilmes-offline-license"&&p.getString("issuerId")==issuer)
  require(p.getString("deviceId")==deviceId&&deviceId.matches(Regex("[a-f0-9]{64}"))){"License belongs to another device"}
  val issued=p.getLong("issuedAt");val expiry=p.getLong("expiresAt");val subscription=p.getLong("subscriptionExpiresAt")
  require(issued>=authority.getLong("notBefore")&&expiry<=authority.getLong("expiresAt"))
  require(issued>0&&expiry>issued&&expiry-issued<=authority.getLong("maxLicenseMs")&&expiry<=subscription){"Invalid license lifetime"}
  require(p.getString("customerId").isNotBlank()&&p.getString("licenseId").isNotBlank())
  return LicenseClaims(p.getString("licenseId"),p.getString("customerId"),p.getString("username"),deviceId,issued,expiry,subscription,1)
 }
}
/** State is encrypted and Keystore-bound on Android. UTC never moves backwards. */
data class ClockCheckpoint(val trustedMs:Long,val wallMs:Long,val elapsedMs:Long,val boot:String,val blocked:Boolean=false){
 fun json()=JSONObject().put("trusted",trustedMs).put("wall",wallMs).put("elapsed",elapsedMs).put("boot",boot).put("blocked",blocked)
 companion object {fun parse(j:JSONObject)=ClockCheckpoint(j.getLong("trusted"),j.getLong("wall"),j.getLong("elapsed"),j.getString("boot"),j.optBoolean("blocked"))}
}
object LicenseClock {
 fun advance(c:ClockCheckpoint,wall:Long,elapsed:Long,boot:String):ClockCheckpoint {
  if(c.blocked)return c
  val sameBoot=boot==c.boot&&elapsed>=c.elapsedMs
  val elapsedDelta=if(sameBoot)elapsed-c.elapsedMs else 0L
  val wallDelta=wall-c.wallMs
  // Same boot: monotonic time drives validity; clock changes beyond 2 min require online trust.
  // New boot: rollback is rejected. Powered-off time still relies on the non-root device RTC.
  val tampered=if(sameBoot)kotlin.math.abs(wallDelta-elapsedDelta)>120000 else wallDelta<0
  val delta=if(sameBoot)elapsedDelta else wallDelta.coerceAtLeast(0)
  return ClockCheckpoint(c.trustedMs+delta,wall,elapsed,boot,tampered)
 }
 fun remaining(claims:LicenseClaims,c:ClockCheckpoint):Long {if(c.blocked)return 0;return (claims.expiresAt-c.trustedMs).coerceAtLeast(0)}
}
