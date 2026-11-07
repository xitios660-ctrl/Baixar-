package com.guhflix.shell

import org.json.JSONObject
import java.net.URI
import java.io.File
import java.security.MessageDigest

/** Pure policy, shared by downloader and JVM tests. No trust in manifest-provided certificates. */
object OtaPolicy {
 data class Release(val code:Long,val version:String,val url:String,val hash:String,val bytes:Long,val notes:String,val mandatory:Boolean)
 fun parse(json:JSONObject,packageName:String):Release {
  require(json.getString("package")==packageName){"Pacote de atualização incompatível"}
  val url=json.optString("apkUrl").ifBlank { URI("https://guhflix-tv-download.onrender.com/").resolve(json.getString("file")).toString() }
  val uri=URI(url)
  require(uri.scheme=="https"&&uri.host!=null&&uri.userInfo==null&&uri.fragment==null){"Endereço de atualização inválido"}
  val hash=json.getString("sha256").lowercase();require(hash.matches(Regex("[a-f0-9]{64}")))
  val size=json.optLong("bytes");require(size in 1..150_000_000)
  return Release(json.getLong("versionCode"),json.getString("version"),url,hash,size,json.optString("notes").take(3000),json.optBoolean("mandatory"))
 }
 fun hash(file:File):String {val md=MessageDigest.getInstance("SHA-256");file.inputStream().use{input->val buf=ByteArray(65536);while(true){val n=input.read(buf);if(n<0)break;md.update(buf,0,n)}};return md.digest().joinToString(""){"%02x".format(it)}}
 fun remainingSpace(total:Long,partial:Long,available:Long):Boolean = total>0&&partial in 0..total&&available>=total-partial+8*1024*1024
 fun canAppend(code:Int,range:String?,offset:Long):Boolean=code==206&&range?.startsWith("bytes $offset-")==true
}
