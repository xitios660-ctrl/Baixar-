package br.com.saimo.tv

import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import coil3.load
import com.guhflix.shell.Gate
import com.guhflix.shell.CloudRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Real authenticated web-compatible channels; no illustrative programmes or schedules. */
@UnstableApi
class LiveHomeActivity : TelaComMenu() {
 override val aba = Aba.AO_VIVO
 private lateinit var title:TextView
 private lateinit var status:TextView
 private lateinit var cards:LinearLayout
 private lateinit var filters:LinearLayout
 private var channels=listOf<JSONObject>()
 private var chosen:JSONObject?=null
 private var offset=0
 private var total=0
 private var loading=false
 private var category="Todos"
 private fun dp(n:Int)=(resources.displayMetrics.density*n).toInt()
 private fun label(text:String,size:Float)=TextView(this).apply{this.text=text;textSize=size;setTextColor(Color.WHITE);setPadding(0,dp(4),0,dp(6))}
 private fun button(text:String,action:()->Unit)=Button(this).apply{this.text=text;textSize=15f;setTextColor(Color.WHITE);setBackgroundResource(R.drawable.aba_menu);setPadding(dp(18),dp(8),dp(18),dp(8));setOnClickListener{action()}}
 override fun onCreate(savedInstanceState:Bundle?){
  super.onCreate(savedInstanceState)
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(40),dp(12),dp(40),dp(30));setBackgroundColor(Color.rgb(9,9,12))}
  root.addView(MenuGlobal.criar(this,Aba.AO_VIVO))
  val scroll=ScrollView(this);scroll.addView(root);setContentView(scroll)
  root.addView(label("TV AO VIVO  ━━━",13f).apply{setTextColor(Color.rgb(240,20,39))})
  root.addView(label("NO AR AGORA",18f))
  title=label("Escolha seu canal",34f).apply{typeface=Typeface.DEFAULT_BOLD};root.addView(title)
  status=label("Carregando canais da sua conta…",15f);root.addView(status)
  val actions=LinearLayout(this);root.addView(actions)
  actions.addView(button("▶ Assistir agora"){chosen?.let{CloudRemote.playLive(this,it.getString("id"))}})
  actions.addView(button("☷ Guia de canais"){cards.getChildAt(0)?.requestFocus()})
  val hs=HorizontalScrollView(this);filters=LinearLayout(this);hs.addView(filters);root.addView(hs)
  root.addView(label("Canais em destaque",23f).apply{typeface=Typeface.DEFAULT_BOLD})
  val row=HorizontalScrollView(this).apply{clipToPadding=false};cards=LinearLayout(this);row.addView(cards);root.addView(row)
  root.addView(button("Carregar mais canais"){load()})
  root.addView(label("Programação",22f))
  root.addView(label("Horários e próximos programas não estão disponíveis neste catálogo.",14f).apply{setTextColor(Color.LTGRAY)})
  load()
 }
 private fun load(){if(loading||offset>0&&offset>=total)return;loading=true;lifecycleScope.launch{
  try{
   val page=withContext(Dispatchers.IO){CloudRemote.liveCatalog(offset)}
   val a=page.getJSONArray("items");channels=channels+(0 until a.length()).map{a.getJSONObject(it)};offset+=a.length();total=page.getInt("total")
   status.text="$total canais · A reprodução depende da fonte e da conexão."
   filters.removeAllViews();val categories=linkedSetOf("Todos");channels.forEach{c->val g=c.optJSONArray("genres");if(g!=null)for(i in 0 until g.length())categories.add(g.getString(i))}
   categories.forEach{g->filters.addView(button(g){category=g;render()})};render()
  }catch(e:Exception){status.text=e.message?:"Não foi possível carregar. Tente novamente."}finally{loading=false}
 }}
 private fun render(){cards.removeAllViews();channels.filter{c->category=="Todos"||c.optJSONArray("genres")?.toString()?.contains("\"$category\"")==true}.forEach{c->
  val tile=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;isFocusable=true;setPadding(dp(8),dp(8),dp(8),dp(8));setBackgroundResource(R.drawable.row_focus);layoutParams=LinearLayout.LayoutParams(dp(195),dp(175)).apply{rightMargin=dp(12)}}
  tile.addView(label("● AO VIVO",11f).apply{setTextColor(Color.rgb(240,20,39))})
  tile.addView(ImageView(this).apply{scaleType=ImageView.ScaleType.CENTER_INSIDE;val poster=c.optString("poster");if(poster.startsWith("https://"))load(poster) else setImageResource(R.mipmap.ic_launcher)},LinearLayout.LayoutParams(-1,dp(80)))
  tile.addView(label(c.getString("title"),17f).apply{maxLines=2;typeface=Typeface.DEFAULT_BOLD})
  tile.setOnFocusChangeListener{_,focus->if(focus){chosen=c;title.text=c.getString("title")}}
  tile.setOnClickListener{chosen=c;CloudRemote.playLive(this,c.getString("id"))};cards.addView(tile)
 };if(chosen==null){chosen=channels.firstOrNull();chosen?.let{title.text=it.getString("title")}}}
}
