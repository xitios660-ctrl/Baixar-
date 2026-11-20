package com.gfilmes.license
import java.security.*
import java.security.spec.X509EncodedKeySpec
import java.security.interfaces.RSAPublicKey
import org.json.JSONObject

object Crypto {
 private const val alphabet="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
 fun encode(bytes:ByteArray):String {val out=StringBuilder();var bits=0;var value=0;for(b in bytes){value=(value shl 8) or (b.toInt() and 255);bits+=8;while(bits>=6){bits-=6;out.append(alphabet[(value ushr bits) and 63])}};if(bits>0)out.append(alphabet[(value shl (6-bits)) and 63]);return out.toString()}
 fun decode(text:String):ByteArray {require(text.length<=131072&&text.length%4!=1&&text.all{alphabet.indexOf(it)>=0}){"Invalid base64url"};val out=java.io.ByteArrayOutputStream();var bits=0;var value=0;for(c in text){value=(value shl 6) or alphabet.indexOf(c);bits+=6;if(bits>=8){bits-=8;out.write((value ushr bits) and 255)}};require(bits==0||(value and ((1 shl bits)-1))==0){"Noncanonical base64url"};return out.toByteArray()}
 fun sha(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
 fun publicKey(encoded:String):PublicKey {val key=KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(decode(encoded)));require((key as RSAPublicKey).modulus.bitLength() in 2048..4096);return key}
 fun verifyJws(jws:String,key:PublicKey):JSONObject {require(jws.length<=32768);val parts=jws.split('.');require(parts.size==3);val h=JSONObject(String(decode(parts[0]),Charsets.UTF_8));require(h.getString("alg")=="RS256"&&!h.has("crit")&&!h.has("jku")&&!h.has("jwk"));val verify=Signature.getInstance("SHA256withRSA");verify.initVerify(key);verify.update((parts[0]+"."+parts[1]).toByteArray(Charsets.US_ASCII));require(verify.verify(decode(parts[2]))){"Invalid signature"};return JSONObject(String(decode(parts[1]),Charsets.UTF_8))}
 fun signJws(payload:JSONObject,key:PrivateKey):String {val body=encode("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".toByteArray())+"."+encode(payload.toString().toByteArray(Charsets.UTF_8));val signer=Signature.getInstance("SHA256withRSA");signer.initSign(key);signer.update(body.toByteArray(Charsets.US_ASCII));return body+"."+encode(signer.sign())}
}
