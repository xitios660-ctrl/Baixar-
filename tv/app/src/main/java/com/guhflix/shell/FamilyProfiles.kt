package com.guhflix.shell

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Local people, not login credentials. IDs keep each person's library stable across edits. */
class FamilyProfiles(private val read:()->String?, private val write:(String)->Unit) {
 @Volatile private var selected:String?=null
 private fun rows():JSONArray {
  val a=JSONArray(read()?:"[]");var changed=false
  for(i in 0 until a.length()){
   val p=a.getJSONObject(i)
   for(key in listOf("hash","salt","pin","password","locked"))if(p.has(key)){p.remove(key);changed=true}
  }
  // Remove the old PIN without replacing IDs, names or unrelated profile data.
  if(changed)write(a.toString())
  return a
 }
 private fun save(a:JSONArray)=write(a.toString())
 fun activeId():String?=selected
 fun lock(){selected=null}
 @Synchronized fun list():JSONObject {val out=JSONArray();val a=rows();for(i in 0 until a.length()){val p=a.getJSONObject(i);out.put(JSONObject().put("id",p.getString("id")).put("name",p.getString("name")).put("avatar",p.optInt("avatar")).put("kids",p.optBoolean("kids")))};return JSONObject().put("items",out).put("activeId",selected?:JSONObject.NULL)}
 private fun validName(name:String):String {val value=name.trim();require(value.length in 1..24){"Use um nome de até 24 caracteres"};return value}
 @Synchronized fun create(name:String,avatar:Int,kids:Boolean=false):JSONObject {val a=rows();require(a.length()<6){"Você pode criar até 6 perfis"};a.put(JSONObject().put("id",UUID.randomUUID().toString()).put("name",validName(name)).put("avatar",avatar.coerceIn(0,11)).put("kids",kids));save(a);return list()}
 @Synchronized fun select(id:String):JSONObject {val p=find(id);selected=id;return JSONObject().put("id",id).put("name",p.getString("name")).put("avatar",p.optInt("avatar")).put("kids",p.optBoolean("kids"))}
 @Synchronized fun update(id:String,name:String,avatar:Int,kids:Boolean?=null):JSONObject {val a=rows();for(i in 0 until a.length()){val p=a.getJSONObject(i);if(p.getString("id")==id){p.put("name",validName(name)).put("avatar",avatar.coerceIn(0,11));if(kids!=null)p.put("kids",kids);save(a);return list()}};error("Perfil não encontrado")}
 @Synchronized fun delete(id:String):JSONObject {find(id);val a=rows();val out=JSONArray();for(i in 0 until a.length())if(a.getJSONObject(i).getString("id")!=id)out.put(a.getJSONObject(i));save(out);if(selected==id)selected=null;return list()}
 @Synchronized fun person(id:String)=JSONObject(find(id).toString())
 private fun find(id:String):JSONObject {val a=rows();for(i in 0 until a.length())if(a.getJSONObject(i).getString("id")==id)return a.getJSONObject(i);error("Perfil não encontrado")}
}
