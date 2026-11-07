package com.guhflix.shell

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import br.com.saimo.tv.BuildConfig
import org.json.JSONObject

class HubActivity:Activity(){
 private lateinit var message:TextView
 private lateinit var code:TextView
 private val red=Color.rgb(229,9,20)
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(55),dp(30),dp(55),dp(30));setBackgroundColor(Color.rgb(8,8,8))}
  val scroll=ScrollView(this);scroll.addView(root);setContentView(scroll)
  root.addView(TextView(this).apply{text="GUHFLIX · CONEXÕES";textSize=28f;setTextColor(Color.WHITE);typeface=Typeface.DEFAULT_BOLD})
  root.addView(TextView(this).apply{text="Abra o site no iPhone e vincule usando o código desta televisão.";textSize=18f;setTextColor(Color.LTGRAY);setPadding(0,dp(12),0,dp(10))})
  code=TextView(this).apply{text="Conectar iPhone";textSize=42f;typeface=Typeface.MONOSPACE;setTextColor(Color.WHITE)};root.addView(code)
  message=TextView(this).apply{text="https://guhflix-marley-api.onrender.com/pwa/";textSize=17f;setTextColor(Color.LTGRAY);setPadding(0,dp(5),0,dp(20))};root.addView(message)
  add(root,"GERAR CÓDIGO DE VÍNCULO"){
   message.text="Gerando código seguro…"
   Gate.work.execute{try{val result=Gate.request(this,"POST","/api/connect/pairing",JSONObject(),true);runOnUiThread{code.text=result.getString("code");message.text="Válido por cinco minutos e de uso único. No iPhone, abra GUHFLIX → Conectar TV. Use a mesma conta."}}catch(e:Exception){runOnUiThread{message.text=e.message?:"Não foi possível conectar."}}}
  }.requestFocus()
  add(root,"MINHA CONTA"){startActivity(Intent(this,AccessActivity::class.java).putExtra("manage",true))}
  add(root,"VERIFICAR ATUALIZAÇÃO"){OtaManager.check(this,true)}
  add(root,"DESVINCULAR IPHONES DESTA TV"){
   AlertDialog.Builder(this).setMessage("Encerrar os vínculos de controle desta televisão?").setPositiveButton("Desvincular"){_,_->Gate.work.execute{try{Gate.request(this,"DELETE","/api/connect/tv/devices",null,true);runOnUiThread{message.text="Vínculos encerrados. Gere outro código para conectar novamente."}}catch(e:Exception){runOnUiThread{message.text=e.message}}}}.setNegativeButton("Cancelar",null).show()
  }
  root.addView(TextView(this).apply{text="GUHFLIX TV ${BuildConfig.VERSION_NAME}";textSize=14f;setTextColor(Color.GRAY);setPadding(0,dp(18),0,0)})
 }
 override fun onResume(){super.onResume();OtaManager.resume(this)}
 private fun add(root:LinearLayout,label:String,click:()->Unit):Button {val b=Button(this);b.text=label;b.textSize=16f;b.setTextColor(Color.WHITE);b.backgroundTintList=android.content.res.ColorStateList.valueOf(red);b.setOnClickListener{click()};b.setOnFocusChangeListener{view,focus->view.animate().scaleX(if(focus)1.03f else 1f).scaleY(if(focus)1.03f else 1f).setDuration(120).start()};root.addView(b,LinearLayout.LayoutParams(-1,dp(54)).apply{bottomMargin=dp(8)});return b}
 private fun dp(v:Int)=(resources.displayMetrics.density*v).toInt()
}
