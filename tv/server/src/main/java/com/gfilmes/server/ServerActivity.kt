package com.gfilmes.server
import android.app.*
import android.os.Bundle
import android.content.Intent
import android.graphics.Color
import android.text.InputType
import android.widget.*
import com.gfilmes.license.Crypto
import org.json.JSONObject
import java.time.Instant
import java.util.concurrent.Executors

class ServerActivity:Activity(){
 private lateinit var store:ServerStore;private lateinit var engine:AuthorityEngine;private lateinit var root:LinearLayout
 private val worker=Executors.newSingleThreadExecutor();private var actor:String?=null;private var until=0L;private var pendingWrite:ByteArray?=null;private var importKind="";private var importPassword:CharArray?=null;private val uiHandler=android.os.Handler(android.os.Looper.getMainLooper());private var setupTicker:Runnable?=null
 override fun onCreate(state:Bundle?){super.onCreate(state);store=ServerStore(this);engine=AuthorityEngine(store);home()}
 private fun screen(title:String){setupTicker?.let{uiHandler.removeCallbacks(it)};setupTicker=null;val scroll=ScrollView(this);root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(48,24,48,24);setBackgroundColor(Color.rgb(8,9,14))};scroll.addView(root);setContentView(scroll);text("Gfilmes Servidor · "+BuildConfig.VERSION_NAME,30);text(title,22)}
 private fun text(value:String,size:Int=18){root.addView(TextView(this).apply{this.text=value;textSize=size.toFloat();setTextColor(Color.WHITE);setPadding(0,8,0,8)})}
 private fun input(hint:String,password:Boolean=false)=EditText(this).apply{this.hint=hint;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);inputType=if(password)InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD else InputType.TYPE_CLASS_TEXT;root.addView(this)}
 private fun button(label:String,action:()->Unit){root.addView(Button(this).apply{text=label;setOnClickListener{action()}})}
 private fun message(value:String){AlertDialog.Builder(this).setMessage(value).setPositiveButton("OK",null).show()}
 private fun job(action:()->Unit){worker.execute{try{action()}catch(e:Exception){runOnUiThread{message(e.message?:"Operação não concluída")}}}}
 private fun requireAdmin():String {val id=actor;check(id!=null&&android.os.SystemClock.elapsedRealtime()<until){"Entre novamente como administrador"};val u=store.get("user",id);check(u?.optString("role")=="admin"&&u.optString("status")=="enabled");until=android.os.SystemClock.elapsedRealtime()+900000;return id}
 private fun home(){screen("Ativação offline com prazo limitado")
  text("A TV não é um servidor público comprovado. Reinicialização, suspensão, CGNAT e limite de execução do Android podem interromper ativações. Clientes já licenciados continuam até o vencimento.")
  text(ActivationService.status)
  button("1 · Ver chave PÚBLICA / configurar pelo celular"){localSetup()}
  button("Atualizar esta tela após importar arquivos"){home()}
  if(!store.ready()){
   button("2 · Importar autorização pública assinada"){pick("grant")};button("3 · Importar migração criptografada e assinada"){pick("migration")};button("Recuperar backup com senha"){passwordPrompt("Senha do backup"){p->importPassword=p;pick("restore")}}
   text("Não cria outro administrador. Importe o snapshot do MongoDB original, sem modificar a origem.")
  }else{
   val name=input("Usuário administrativo existente");val password=input("Senha administrativa",true)
   button("Entrar no painel"){val n=name.text.toString();val pw=password.text.toString();password.setText("");job{actor=engine.adminLogin(n,pw);until=android.os.SystemClock.elapsedRealtime()+900000;runOnUiThread{dashboard()}}}
  }
 }
 private fun dashboard(){try{requireAdmin()}catch(_:Exception){home();return};screen("Administração local · ambiente de homologação")
  text("Renovações aqui não se sincronizam automaticamente com o MongoDB. Faça o corte administrativo somente após conciliação e backup; evite administrar duas cópias ao mesmo tempo.")
  text("Clientes: "+store.list("user").count{it.optString("role")=="customer"}+" · "+ActivationService.status)
  val query=input("Buscar cliente pelo usuário");button("Buscar"){val q=query.text.toString().trim();screen("Clientes");store.list("user").filter{it.optString("role")=="customer"&&it.optString("username").contains(q,true)}.take(200).forEach{u->button(u.getString("username")+" · "+date(u.optLong("expiresAt"))+" · "+u.optString("status")){customer(u.getString("id"))}};button("Voltar"){dashboard()}}
  button("Configuração e arquivos pelo celular (sem seletor)"){requireAdmin();localSetup()}
  button("Importar autorização pública"){requireAdmin();pick("grant")}
  button("Configurar certificado HTTPS PKCS12"){requireAdmin();passwordPrompt("Senha do certificado PKCS12"){p->importPassword=p;pick("tls")}}
  button("Iniciar janela de ativação HTTPS"){requireAdmin();startForegroundService(Intent(this,ActivationService::class.java));message("Início solicitado. Porta 17862. Certificado precisa ser confiável no Android. CGNAT exige acesso externo próprio; não foi comprovado.")}
  button("Parar janela de ativação"){stopService(Intent(this,ActivationService::class.java));dashboard()}
  button("Backup criptografado"){requireAdmin();passwordPrompt("Nova senha de backup (mínimo 14 caracteres)"){password->job{try{val data=BackupCodec.encrypt(store.exportRows().toString().toByteArray(),password).toString().toByteArray();runOnUiThread{export("gfilmes-backup.json",data)}}finally{password.fill('\u0000')}}}}
  button("Recuperar backup (substitui banco LOCAL)"){requireAdmin();AlertDialog.Builder(this).setMessage("Faça um backup atual antes. A recuperação troca somente os dados locais e exige reaprovar a chave do servidor. Continuar?").setPositiveButton("Recuperar"){_,_->passwordPrompt("Senha do backup"){p->importPassword=p;pick("restore")}}.setNegativeButton("Cancelar",null).show()}
  button("Últimas operações"){requireAdmin();screen("Auditoria local");store.list("audit").sortedByDescending{it.optLong("at")}.take(100).forEach{text(date(it.optLong("at"))+" · "+it.optString("action")+" · "+it.optString("subject"))};button("Voltar"){dashboard()}}
  button("Diagnóstico de HTTPS externo"){requireAdmin();screen("Teste público · sem credenciais");val url=input("https://seu-endereco/health");button("Testar"){val value=url.text.toString();job{val u=java.net.URI(value);require(u.scheme=="https"&&u.host!=null&&u.userInfo==null);val c=u.toURL().openConnection() as javax.net.ssl.HttpsURLConnection;c.connectTimeout=10000;c.readTimeout=10000;try{val code=c.responseCode;runOnUiThread{message("HTTPS respondeu $code nesta rede. Isso não comprova acesso de um cliente em outra rede.")}}finally{c.disconnect()}}};button("Voltar"){dashboard()}}
  button("Sair"){actor=null;until=0;home()}
 }
 private fun customer(id:String){requireAdmin();val u=store.get("user",id)?:return;screen(u.getString("username"));text("Vencimento: "+date(u.optLong("expiresAt"))+" · "+u.optString("status"))
  text("Renovação do período existente: +30 dias, sem reduzir saldo atual.");val confirmed=CheckBox(this).apply{text="Confirmo que o pagamento foi verificado fora do aplicativo";setTextColor(Color.WHITE);root.addView(this)}
  button("Confirmar renovação local"){val admin=requireAdmin();if(!confirmed.isChecked){message("Confirme o pagamento verificado");return@button};val expiry=maxOf(System.currentTimeMillis(),u.optLong("expiresAt"))+30*86400000L;AlertDialog.Builder(this).setMessage("Renovar até "+date(expiry)+"? Não efetua cobrança nem altera o MongoDB original.").setPositiveButton("Confirmar"){_,_->job{synchronized(store){val db=store.writableDatabase;db.beginTransaction();try{u.put("expiresAt",expiry);store.saveUser(u);val record=JSONObject().put("customerId",id).put("verifiedAt",System.currentTimeMillis()).put("verifiedBy",admin).put("expiresAt",expiry);store.put("payment_confirmation",java.util.UUID.randomUUID().toString(),record,id);store.audit(admin,"renewal_payment_confirmed",id,record);db.setTransactionSuccessful()}finally{db.endTransaction()}};runOnUiThread{customer(id)}}}.setNegativeButton("Cancelar",null).show()}
  button(if(u.optString("status")=="enabled")"Bloquear novas ativações" else "Desbloquear novas ativações"){val admin=requireAdmin();u.put("status",if(u.optString("status")=="enabled")"blocked" else "enabled");store.saveUser(u);store.audit(admin,"account_status",id,JSONObject().put("status",u.getString("status")));message("Aplicado localmente. Licenças offline já emitidas continuam válidas até vencer.");customer(id)}
  text("Dispositivos licenciados (máximo 3 ainda válidos)");store.list("device",id).forEach{d->text(d.optString("label")+" · "+date(d.optLong("expiresAt"))+if(d.optBoolean("retired"))" · removido" else "");if(!d.optBoolean("retired"))button("Impedir renovação deste dispositivo "+d.optString("deviceId").take(10)){val admin=requireAdmin();d.put("retired",true);store.put("device",d.getString("id"),d,id);store.audit(admin,"device_retire",id,JSONObject().put("deviceId",d.optString("deviceId")));message("A licença atual continua ocupando uma vaga até vencer.");customer(id)}}
  text("Últimas licenças");store.list("license",id).sortedByDescending{it.optLong("issuedAt")}.take(20).forEach{text(it.optString("licenseId").take(12)+" · "+date(it.optLong("expiresAt")))};button("Voltar"){dashboard()}
 }
 private fun localSetup(){
  screen("Configurar pelo celular · mesma rede Wi-Fi")
  text("Não precisa de seletor de arquivos na TV. Abra o endereço abaixo no navegador do celular ou computador conectado à mesma rede. Não encaminhe a porta no roteador.")
  val state=TextView(this).apply{setTextColor(Color.WHITE);textSize=19f;setTextIsSelectable(true);root.addView(this)}
  val fingerprint=TextView(this).apply{setTextColor(Color.WHITE);textSize=17f;setTextIsSelectable(true);root.addView(this)}
  job{val hash=ServerKeys.id();runOnUiThread{fingerprint.text="Impressão digital da chave PÚBLICA (confira no celular):\n"+hash}}
  try{startForegroundService(Intent(this,SetupTransferService::class.java))}catch(_:Exception){message("Não foi possível iniciar a configuração local. Mantenha o aplicativo aberto e tente novamente.")}
  val ticker=object:Runnable{override fun run(){state.text=SetupTransferService.status+"\n"+SetupTransferService.addressUrl;uiHandler.postDelayed(this,750)}};setupTicker=ticker;uiHandler.post(ticker)
  text("1 · No celular, baixe a chave pública e envie ao responsável.\n2 · Depois envie a autorização e a migração cifrada recebidas.\nSomente arquivos autorizados são aceitos. Senhas e chaves privadas não são digitadas nessa página.")
  button("Mostrar chave pública na tela"){job{val value=Crypto.encode(ServerKeys.publicKey().encoded);runOnUiThread{screen("Chave PÚBLICA — pode compartilhar");text(value.chunked(64).joinToString("\n"),18);text("SHA256: "+ServerKeys.id(),16);button("Voltar à configuração pelo celular"){localSetup()}}}}
  button("Encerrar configuração local"){stopService(Intent(this,SetupTransferService::class.java));home()}
  button("Voltar e atualizar estado"){home()}
 }
 override fun onDestroy(){setupTicker?.let{uiHandler.removeCallbacks(it)};super.onDestroy()}
 private fun date(ms:Long)=if(ms<=0)"sem vencimento válido" else Instant.ofEpochMilli(ms).toString()
 private fun passwordPrompt(title:String,done:(CharArray)->Unit){val field=EditText(this).apply{inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD};AlertDialog.Builder(this).setTitle(title).setView(field).setPositiveButton("Continuar"){_,_->val p=field.text.toString().toCharArray();field.setText("");done(p)}.setNegativeButton("Cancelar",null).show()}
 private fun pick(kind:String){
  if(kind=="restore"){val password=importPassword;importPassword=null;require(password!=null);SetupTransferService.armRecovery({bytes->if(store.ready())requireAdmin();stopService(Intent(this,ActivationService::class.java));store.restoreRows(JSONObject(String(BackupCodec.decrypt(JSONObject(String(bytes,Charsets.UTF_8)),password),Charsets.UTF_8)));actor=null;until=0},{password.fill('\u0000')});localSetup();return}
  importKind=kind;try{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE)},2)}catch(_:android.content.ActivityNotFoundException){importPassword?.fill('\u0000');importPassword=null;message("Esta TV não tem seletor de documentos compatível. A importação precisa de um provedor de arquivos SAF; valide essa configuração antes de usar como servidor.")}}
 private fun export(name:String,bytes:ByteArray){
  if(name=="gfilmes-backup.json"){SetupTransferService.exportedBytes=bytes;localSetup();return}
  pendingWrite=bytes;try{startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/octet-stream";addCategory(Intent.CATEGORY_OPENABLE);putExtra(Intent.EXTRA_TITLE,name)},1)}catch(_:android.content.ActivityNotFoundException){pendingWrite=null;if(name.endsWith(".txt"))message("Chave PÚBLICA (sem segredo):\n"+String(bytes)) else message("Esta TV não tem seletor de documentos compatível. Configure um provedor SAF para salvar backups.")}}
 @Deprecated("Platform API") override fun onActivityResult(request:Int,result:Int,data:Intent?){super.onActivityResult(request,result,data);if(result!=RESULT_OK){importPassword?.fill('\u0000');return};val uri=data?.data?:return;val bytes=pendingWrite;val kind=importKind;val password=importPassword;pendingWrite=null;importPassword=null;job{try{if(request==1){require(bytes!=null);contentResolver.openOutputStream(uri,"wt")!!.use{it.write(bytes)}}else{val input=contentResolver.openInputStream(uri)!!.use{stream->val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(16384);while(true){val n=stream.read(buffer);if(n<0)break;require(out.size()+n<=32*1024*1024);out.write(buffer,0,n)};out.toByteArray()};require(input.size<=32*1024*1024);when(kind){"grant"->store.installGrant(String(input).trim());"migration"->store.importSnapshot(JSONObject(String(input)));"restore"->{require(password!=null);if(store.ready())requireAdmin();stopService(Intent(this,ActivationService::class.java));store.restoreRows(JSONObject(String(BackupCodec.decrypt(JSONObject(String(input)),password))));actor=null;until=0};"tls"->{requireAdmin();require(input.size<=128*1024){"PKCS12 deve ter no máximo 128 KiB"};require(password!=null);val ks=java.security.KeyStore.getInstance("PKCS12");ks.load(java.io.ByteArrayInputStream(input),password);require(ks.aliases().toList().any{ks.isKeyEntry(it)});store.put("config","tls",JSONObject().put("p12",android.util.Base64.encodeToString(input,11)).put("password",String(password)))}}};runOnUiThread{message("Operação concluída localmente");if(actor!=null)dashboard() else home()}}finally{password?.fill('\u0000')}}}
}
