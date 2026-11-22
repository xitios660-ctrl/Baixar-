package com.guhflix.shell
import com.gfilmes.license.Crypto
import com.gfilmes.license.RootTrust
import java.security.PublicKey
/** Public authorization only. Never customer credentials or database exports. */
object DeviceGrant {
 fun verify(jws:String,device:String,root:PublicKey=Crypto.publicKey(RootTrust.PUBLIC_KEY)):org.json.JSONObject {
  val p=Crypto.verifyJws(jws,root)
  require(p.getString("kind")=="gfilmes-local-device"&&p.getInt("v")==1){"Autorização incompatível"}
  require(device.matches(Regex("[a-f0-9]{64}"))&&p.getString("deviceId")==device){"Este aplicativo foi autorizado para outra TV"}
  require(p.getString("customerRef").matches(Regex("[a-f0-9]{64}"))){"Referência de acesso inválida"}
  require(p.getLong("issuedAt")>0&&p.getLong("expiresAt")>p.getLong("issuedAt")&&p.getLong("expiresAt")<=p.getLong("subscriptionExpiresAt")){"Prazo de acesso inválido"}
  return p
 }
}
