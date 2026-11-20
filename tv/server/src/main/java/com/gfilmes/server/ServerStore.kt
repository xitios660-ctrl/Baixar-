package com.gfilmes.server
import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import org.json.JSONArray
import com.gfilmes.license.*
import java.security.Signature
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class ServerStore(context:Context):SQLiteOpenHelper(context,"gfilmes-authority.db",null,1){
 override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE records(kind TEXT NOT NULL,id TEXT NOT NULL,owner TEXT NOT NULL,data BLOB NOT NULL,PRIMARY KEY(kind,id))");db.execSQL("CREATE INDEX records_owner ON records(kind,owner)")}
 override fun onUpgrade(db:SQLiteDatabase,old:Int,next:Int){error("Explicit migration required")}
 @Synchronized fun put(kind:String,id:String,value:JSONObject,owner:String=""){val v=android.content.ContentValues().apply{put("kind",kind);put("id",id);put("owner",owner);put("data",ServerKeys.seal(value.toString().toByteArray(Charsets.UTF_8)))};writableDatabase.insertWithOnConflict("records",null,v,SQLiteDatabase.CONFLICT_REPLACE)}
 @Synchronized fun get(kind:String,id:String):JSONObject?=readableDatabase.rawQuery("SELECT data FROM records WHERE kind=? AND id=?",arrayOf(kind,id)).use{if(it.moveToFirst())JSONObject(String(ServerKeys.open(it.getBlob(0)),Charsets.UTF_8)) else null}
 @Synchronized fun list(kind:String,owner:String?=null):List<JSONObject>{val sql="SELECT data FROM records WHERE kind=?"+(if(owner!=null)" AND owner=?" else "");return readableDatabase.rawQuery(sql,if(owner!=null)arrayOf(kind,owner) else arrayOf(kind)).use{c->val rows=mutableListOf<JSONObject>();while(c.moveToNext())rows.add(JSONObject(String(ServerKeys.open(c.getBlob(0)),Charsets.UTF_8)));rows}}
 fun userByName(name:String)=list("user",Crypto.sha(name.trim().lowercase(Locale.ROOT).toByteArray())).firstOrNull()
 fun saveUser(u:JSONObject)=put("user",u.getString("id"),u,Crypto.sha(u.getString("username").trim().lowercase(Locale.ROOT).toByteArray()))
 fun ready()=list("user").any{it.optString("role")=="admin"}
 fun audit(actor:String,action:String,subject:String,extra:JSONObject=JSONObject()){put("audit",java.util.UUID.randomUUID().toString(),JSONObject().put("actor",actor).put("action",action).put("subject",subject).put("at",System.currentTimeMillis()).put("details",extra))}
 fun authority():String?=get("config","authority")?.optString("grant")
 fun installGrant(value:String){val a=Crypto.verifyJws(value,Crypto.publicKey(RootTrust.PUBLIC_KEY));require(a.getString("kind")=="gfilmes-authority"&&a.getString("issuerId")==ServerKeys.id()&&a.getString("publicKey")==Crypto.encode(ServerKeys.publicKey().encoded)&&a.getLong("maxLicenseMs") in 1..MAX_LICENSE_MS);put("config","authority",JSONObject().put("grant",value))}
 @Synchronized fun importSnapshot(envelope:JSONObject){
  require(!ready()){ "O banco já foi importado. Use a recuperação autenticada, sem sobrescrever renovações." }
  val snapshot=openTransfer(envelope)
  require(snapshot.getInt("v")==1&&snapshot.getString("database")=="guhflix");val collections=snapshot.getJSONObject("collections");val users=collections.getJSONArray("users");require(users.length()>0&&users.length()<=100000);val count=snapshot.getJSONObject("counts");val names=collections.keys();while(names.hasNext()){val name=names.next();require(collections.getJSONArray(name).length()==count.getInt(name))}
  val db=writableDatabase;db.beginTransaction();try{
   // Keep every original byte without exceeding Android CursorWindow row limits.
   val original=snapshot.toString().toByteArray(Charsets.UTF_8);val chunkSize=128*1024
   put("migration","manifest",JSONObject().put("sha256",Crypto.sha(original)).put("chunks",(original.size+chunkSize-1)/chunkSize).put("counts",count))
   for(offset in original.indices step chunkSize){val chunk=original.copyOfRange(offset,minOf(offset+chunkSize,original.size));put("migration_chunk",(offset/chunkSize).toString(),JSONObject().put("bytes",Crypto.encode(chunk)))}
   for(i in 0 until users.length()){val raw=users.getJSONObject(i);val id=raw.getJSONObject("_id").getString("$"+"oid");val user=JSONObject().put("id",id).put("username",raw.getString("username")).put("passwordHash",raw.getString("passwordHash")).put("role",raw.getString("role")).put("status",raw.getString("status")).put("expiresAt",dateMillis(raw.opt("expiresAt"))).put("sourceDoc",raw);saveUser(user)}
   val devices=collections.optJSONArray("offline_devices")?:JSONArray()
   for(i in 0 until devices.length()){val d=devices.getJSONObject(i);val owner=d.getString("userId");val id=owner+":"+d.getString("deviceId");put("device",id,JSONObject().put("id",id).put("deviceId",d.getString("deviceId")).put("label",d.optString("label","Android TV")).put("retired",d.optBoolean("retired")).put("expiresAt",dateMillis(d.opt("expiresAt"))),owner)}
   val licenses=collections.optJSONArray("offline_licenses")?:JSONArray()
   for(i in 0 until licenses.length()){val l=licenses.getJSONObject(i);l.put("expiresAt",dateMillis(l.opt("expiresAt"))).put("issuedAt",dateMillis(l.opt("issuedAt")));put("license",l.getString("licenseId"),l,l.getString("userId"))}
   require(ready()){ "O snapshot não contém administrador existente" };audit("migration","import_snapshot","guhflix",count);db.setTransactionSuccessful()
  }finally{db.endTransaction()}
 }
 private fun openTransfer(envelope:JSONObject):JSONObject {
  val encoded=envelope.getString("data");require(encoded.length<=32*1024*1024);val bytes=decodeLarge(encoded);val signature=Signature.getInstance("SHA256withRSA");signature.initVerify(Crypto.publicKey(RootTrust.PUBLIC_KEY));signature.update(bytes);require(signature.verify(Crypto.decode(envelope.getString("signature")))){"Backup não foi autorizado pelo titular"}
  val pack=JSONObject(String(bytes,Charsets.UTF_8));require(pack.getString("deviceId")==ServerKeys.id());val aes=ServerKeys.unwrap(Crypto.decode(pack.getString("key")));val cipher=javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");cipher.init(javax.crypto.Cipher.DECRYPT_MODE,javax.crypto.spec.SecretKeySpec(aes,"AES"),javax.crypto.spec.GCMParameterSpec(128,Crypto.decode(pack.getString("iv"))));val snapshot=try{JSONObject(String(cipher.doFinal(decodeLarge(pack.getString("encrypted"))),Charsets.UTF_8))}finally{aes.fill(0)}
  return snapshot
 }
 @Synchronized fun importConfiguration(envelope:JSONObject){
  val payload=openTransfer(envelope);require(payload.getInt("v")==1&&payload.getString("kind")=="gfilmes-server-config")
  val tls=payload.getJSONObject("tls");val bytes=decodeLarge(tls.getString("p12"));require(bytes.size<=128*1024)
  val password=tls.getString("password").toCharArray();try{val ks=java.security.KeyStore.getInstance("PKCS12");ks.load(java.io.ByteArrayInputStream(bytes),password);require(ks.aliases().toList().any{ks.isKeyEntry(it)});put("config","tls",tls);audit("owner-root","import_https_config","server")}finally{password.fill('\u0000')}
 }
 @Synchronized fun exportRows():JSONObject {val out=JSONArray();readableDatabase.rawQuery("SELECT kind,id,owner,data FROM records",null).use{c->while(c.moveToNext())out.put(JSONObject().put("kind",c.getString(0)).put("id",c.getString(1)).put("owner",c.getString(2)).put("data",JSONObject(String(ServerKeys.open(c.getBlob(3)),Charsets.UTF_8))))};return JSONObject().put("v",1).put("records",out)}
 @Synchronized fun restoreRows(snapshot:JSONObject){require(snapshot.getInt("v")==1);val rows=snapshot.getJSONArray("records");val db=writableDatabase;db.beginTransaction();try{db.delete("records",null,null);for(i in 0 until rows.length()){val r=rows.getJSONObject(i);if(r.getString("kind")=="config"&&r.getString("id")=="authority")continue;put(r.getString("kind"),r.getString("id"),r.getJSONObject("data"),r.getString("owner"))};require(ready());audit("recovery","restore_backup","local");db.setTransactionSuccessful()}finally{db.endTransaction()}}
 companion object {
  fun decodeLarge(v:String):ByteArray {require(v.length<=32*1024*1024);return android.util.Base64.decode(v,android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)}
  fun dateMillis(value:Any?):Long {if(value==null||value===JSONObject.NULL)return 0;if(value is Number)return value.toLong();if(value is JSONObject){val d=value.opt("$"+"date");if(d is JSONObject)return d.getString("$"+"numberLong").toLong();return dateMillis(d)};val text=value.toString();return try{java.time.Instant.parse(text).toEpochMilli()}catch(_:Exception){0}}
 }
}
