package com.guhflix.shell
import org.junit.Assert.*
import org.junit.Test
class FamilyProfilesTest {
 private fun reject(f:()->Unit){try{f();fail("Expected rejection")}catch(_:IllegalArgumentException){}}
 @Test fun savedProfilesSurviveRestartButSelectionDoesNot(){var disk:String?=null;val s=FamilyProfiles({disk},{disk=it});s.create("Família",2,"");val id=s.list().getJSONArray("items").getJSONObject(0).getString("id");s.select(id,"");assertEquals(id,s.activeId());val reopened=FamilyProfiles({disk},{disk=it});assertNull(reopened.activeId());assertEquals("Família",reopened.list().getJSONArray("items").getJSONObject(0).getString("name"))}
 @Test fun pinsAreSaltedNeverExposedAndRequiredForEdit(){var disk:String?=null;val s=FamilyProfiles({disk},{disk=it});s.create("Ana",1,"12345678");val id=s.list().getJSONArray("items").getJSONObject(0).getString("id");assertFalse(disk!!.contains("12345678"));assertFalse(s.list().toString().contains("hash"));reject{s.select(id,"0000")};s.select(id,"12345678");reject{s.update(id,"Outro",0,"0000","")};s.update(id,"Ana",0,"12345678","4321");s.lock();reject{s.select(id,"12345678")};s.select(id,"4321");s.delete(id,"4321");assertEquals(0,s.list().getJSONArray("items").length())}
 @Test fun profilesUseDifferentKeysAndHaveCapacityAndValidation(){var disk:String?=null;val s=FamilyProfiles({disk},{disk=it});reject{s.create("",0,"")};reject{s.create("A",0,"123")};repeat(6){s.create("Perfil $it",it,"")};reject{s.create("Sétimo",0,"")};val ids=(0..5).map{s.list().getJSONArray("items").getJSONObject(it).getString("id")};assertEquals(6,ids.toSet().size)}
 @Test fun repeatedWrongPinsAreThrottled(){var disk:String?=null;var now=100L;val s=FamilyProfiles({disk},{disk=it},{now});s.create("A",0,"1234");val id=s.list().getJSONArray("items").getJSONObject(0).getString("id");repeat(5){reject{s.select(id,"0000")}};reject{s.select(id,"1234")};now+=60001;s.select(id,"1234")}
}
