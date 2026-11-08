package com.guhflix.shell

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.FileProvider
import br.com.saimo.tv.BuildConfig
import com.android.apksig.ApkVerifier
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

/** Signed, resumable updates. Installation always belongs to Android's confirmation UI. */
object OtaManager {
 val MANIFEST=BuildConfig.GUHFLIX_UPDATE_MANIFEST
 private val worker=Executors.newSingleThreadExecutor()
 private val main=Handler(Looper.getMainLooper())
 @Volatile private var checking=false
 @Volatile var downloading=false;private set
 @Volatile private var cancel=false
 private var offered=0L
 private var lastCheck=0L
 private fun valid(a:Activity)=!a.isFinishing&&!a.isDestroyed
 @JvmStatic fun check(a:Activity,manual:Boolean){
  if(checking||downloading)return
  if(!manual&&System.currentTimeMillis()-lastCheck<6*60*60*1000)return
  checking=true;lastCheck=System.currentTimeMillis()
  worker.execute {
   try {
    val c=connection(MANIFEST);val text=try{check(c.responseCode==200);c.inputStream.use{val bytes=it.readBytesLimited(32768);String(bytes,Charsets.UTF_8)}}finally{c.disconnect()}
    val manifest=JSONObject(text)
    require(manifest.getString("package")==a.packageName)
    if(manifest.getLong("versionCode")<=BuildConfig.VERSION_CODE){if(manual)notify(a,"Seu GUHFLIX está atualizado.");return@execute}
    val release=OtaPolicy.parse(manifest,a.packageName)
    main.post {if(valid(a)&&(manual||offered!=release.code)){
     offered=release.code
     val builder=AlertDialog.Builder(a).setTitle("GUHFLIX ${release.version}").setMessage((if(release.mandatory)"Atualização necessária.\n\n" else "")+release.notes+"\n\nO Android pedirá sua confirmação para instalar.").setPositiveButton("Atualizar"){_,_->download(a,release)}
     if(!release.mandatory)builder.setNegativeButton("Depois",null)
     builder.setCancelable(!release.mandatory).show()
    }}
   }catch(_:Exception){if(manual)notify(a,"Não foi possível verificar a atualização. Confira a internet e tente novamente.")}
   finally{checking=false}
  }
 }
 private fun connection(url:String):HttpURLConnection {require(URL(url).protocol=="https");return (URL(url).openConnection() as HttpURLConnection).apply{connectTimeout=15000;readTimeout=20000;instanceFollowRedirects=false;setRequestProperty("Accept-Encoding","identity")}}
 private fun java.io.InputStream.readBytesLimited(max:Int):ByteArray {val out=java.io.ByteArrayOutputStream();val b=ByteArray(8192);while(true){val n=read(b);if(n<0)break;require(out.size()+n<=max);out.write(b,0,n)};return out.toByteArray()}
 private fun download(a:Activity,r:OtaPolicy.Release){
  if(downloading)return;downloading=true;cancel=false
  val progress=ProgressDialog(a).apply{setTitle("Atualizando GUHFLIX");setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);max=100;setCancelable(false);setButton(AlertDialog.BUTTON_NEGATIVE,"Cancelar"){_,_->cancel=true};show()}
  worker.execute {
   val folder=File(a.filesDir,"updates").apply{mkdirs()};val part=File(folder,"${r.hash}.part");val apk=File(folder,"${r.hash}.apk")
   try {
    folder.listFiles()?.filter{it.name!=part.name&&it.name!=apk.name}?.forEach{it.delete()}
    if(!apk.exists()){
     if(part.length()>r.bytes)part.delete()
     var offset=part.length()
     require(OtaPolicy.remainingSpace(r.bytes,offset,folder.usableSpace)){"Espaço insuficiente para atualizar. Libere espaço nas configurações da TV."}
     val c=connection(r.url)
     if(offset>0){c.setRequestProperty("Range","bytes=$offset-");a.getSharedPreferences("guhflix-ota",0).getString("etag-${r.hash}",null)?.let{c.setRequestProperty("If-Range",it)}}
     try {
      val code=c.responseCode
      require(code==200||OtaPolicy.canAppend(code,c.getHeaderField("Content-Range"),offset)){"O servidor não permitiu continuar o download. Tente novamente."}
      c.getHeaderField("ETag")?.takeIf{!it.startsWith("W/")}?.let{a.getSharedPreferences("guhflix-ota",0).edit().putString("etag-${r.hash}",it).apply()}
      if(code==200)offset=0
      RandomAccessFile(part,"rw").use {out->if(offset==0L)out.setLength(0);out.seek(offset)
       c.inputStream.use {input->val b=ByteArray(65536);var total=offset;while(true){if(cancel)throw InterruptedException();val n=input.read(b);if(n<0)break;require(total+n<=r.bytes){"Tamanho inesperado na atualização"};out.write(b,0,n);total+=n;val percent=(100*total/r.bytes).toInt();main.post{if(valid(a))progress.progress=percent}}
       require(total==r.bytes){"Download incompleto. Toque em Atualizar para continuar."}}
       out.fd.sync()
      }
     }finally{c.disconnect()}
     verify(a,part,r)
     require(part.renameTo(apk)){"Não foi possível salvar a atualização"}
    }else verify(a,apk,r)
    a.getSharedPreferences("guhflix-ota",0).edit().putString("pending",apk.name).putString("manifest",JSONObject().put("package",a.packageName).put("versionCode",r.code).put("version",r.version).put("apkUrl",r.url).put("sha256",r.hash).put("bytes",r.bytes).toString()).commit()
    main.post{if(valid(a))install(a,apk)}
   }catch(_:InterruptedException){notify(a,"Download cancelado. O progresso foi guardado para continuar depois.")}
   catch(e:Exception){if(part.length()==r.bytes)part.delete();notify(a,e.message?.take(200)?:"Falha ao baixar atualização.")}
   finally{downloading=false;main.post{if(valid(a))progress.dismiss()}}
  }
 }
 private fun verify(a:Activity,file:File,r:OtaPolicy.Release){
  require(file.length()==r.bytes&&OtaPolicy.hash(file)==r.hash){"Integridade inválida. Baixe novamente."}
  val result=ApkVerifier.Builder(file).setMinCheckedPlatformVersion(23).build().verify()
  require(result.isVerified){"A assinatura do APK não é válida."}
  @Suppress("DEPRECATION") val installed=if(Build.VERSION.SDK_INT>=28)a.packageManager.getPackageInfo(a.packageName,PackageManager.GET_SIGNING_CERTIFICATES).signingInfo!!.apkContentsSigners.map{it.toByteArray()} else a.packageManager.getPackageInfo(a.packageName,PackageManager.GET_SIGNATURES).signatures!!.map{it.toByteArray()}
  fun digest(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).toList()
  require(result.signerCertificates.map{digest(it.encoded)}.toSet()==installed.map{digest(it)}.toSet()){ "A chave não corresponde à versão instalada. Atualização recusada." }
  @Suppress("DEPRECATION") val info=a.packageManager.getPackageArchiveInfo(file.path,0)?:error("APK inválido")
  val code=if(Build.VERSION.SDK_INT>=28)info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
  require(info.packageName==a.packageName&&code==r.code&&code>BuildConfig.VERSION_CODE){"Pacote ou versão incompatível"}
 }
 private fun install(a:Activity,apk:File){
  if(Build.VERSION.SDK_INT>=26&&!a.packageManager.canRequestPackageInstalls()){
   AlertDialog.Builder(a).setTitle("Permitir atualização do GUHFLIX").setMessage("Autorize este aplicativo a instalar a atualização. Depois volte para concluir.").setPositiveButton("Abrir permissão"){_,_->try{a.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,android.net.Uri.parse("package:${a.packageName}")))}catch(_:Exception){notify(a,"Abra Configurações → Instalar apps desconhecidos → GUHFLIX.")}}.setNegativeButton("Depois",null).show();return
  }
  try {
   val uri=FileProvider.getUriForFile(a,a.packageName+".guhflix.updates",apk)
   a.startActivity(Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
   a.getSharedPreferences("guhflix-ota",0).edit().remove("pending").apply()
  }catch(_:Exception){notify(a,"O instalador não abriu. Tente novamente pelo menu Atualização.")}
 }
 @JvmStatic fun resume(a:Activity){
  if(downloading)return
  if(Build.VERSION.SDK_INT>=26&&!a.packageManager.canRequestPackageInstalls())return
  val prefs=a.getSharedPreferences("guhflix-ota",0);val name=prefs.getString("pending",null)?:return
  worker.execute{try{val r=OtaPolicy.parse(JSONObject(prefs.getString("manifest","")),a.packageName);val file=File(a.filesDir,"updates/$name");require(file.name==r.hash+".apk");verify(a,file,r);main.post{if(valid(a))install(a,file)}}catch(_:Exception){prefs.edit().remove("pending").apply();notify(a,"Atualização pendente inválida. Verifique novamente.")}}
 }
 private fun notify(a:Activity,text:String){main.post{if(valid(a))android.widget.Toast.makeText(a,text,android.widget.Toast.LENGTH_LONG).show()}}
}
