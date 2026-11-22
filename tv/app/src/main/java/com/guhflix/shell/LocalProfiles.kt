package com.guhflix.shell
import android.content.Context
import br.com.saimo.tv.BuildConfig
import org.json.JSONObject
object LocalProfiles {
 @Volatile private var engine:FamilyProfiles?=null
 @Synchronized fun store(c:Context):FamilyProfiles {engine?.let{return it};val p=c.applicationContext.getSharedPreferences("gfilmes-family",0);return FamilyProfiles({p.getString("profiles",null)},{check(p.edit().putString("profiles",it).commit()){"Não foi possível salvar o perfil"}}).also{engine=it}}
 @Volatile private var parent:ParentalControl?=null
 @Synchronized fun guardian(c:Context):ParentalControl {parent?.let{return it};val p=c.applicationContext.getSharedPreferences("gfilmes-parental",0);return ParentalControl({p.getString("state",null)},{check(p.edit().putString("state",it).commit())}).also{parent=it}}
 fun isKids(c:Context):Boolean {if(!BuildConfig.GFILMES_LOCAL)return false;val s=store(c);val id=s.activeId();return if(id==null)guardian(c).locked() else s.person(id).optBoolean("kids")}
 fun allowed(c:Context,key:String)=!isKids(c)||guardian(c).allowed(key)
 fun key(c:Context):String {if(!BuildConfig.GFILMES_LOCAL)return "";return "profile-"+(store(c).activeId()?:error("Escolha um perfil para continuar"))}
 fun me(c:Context):JSONObject {val s=store(c);val id=s.activeId()?:error("Escolha um perfil para continuar");val rows=s.list().getJSONArray("items");for(i in 0 until rows.length()){val p=rows.getJSONObject(i);if(p.getString("id")==id)return JSONObject().put("id",id).put("username",key(c)).put("displayName",p.getString("name")).put("role","local").put("local",true)};error("Perfil não encontrado")}
 fun requireSelected(c:Context){if(BuildConfig.GFILMES_LOCAL){key(c)}}
}
