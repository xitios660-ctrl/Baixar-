package com.guhflix.shell
import org.junit.Test
import org.junit.Assert.*
import com.gfilmes.license.CatalogRow
class ParentalControlTest {
 private fun reject(f:()->Unit){try{f();fail("Expected rejection")}catch(_:IllegalArgumentException){}}
 @Test fun childLockSurvivesRestartAndOnlyTheParentCanLeaveOrApprove(){var disk:String?=null;val p=ParentalControl({disk},{disk=it});reject{p.select(true,"")};p.configure("831725");assertFalse(disk!!.contains("831725"));p.select(true,"");val reopened=ParentalControl({disk},{disk=it});assertTrue(reopened.locked());reject{reopened.select(false,"")};reject{reopened.select(false,"000000")};val key="vod:"+"a".repeat(32);reject{reopened.approve(key,true,"")};reopened.approve(key,true,"831725");assertTrue(reopened.allowed(key));reopened.select(false,"831725");assertFalse(reopened.locked())}
 @Test fun unknownRatingsAndGenresNeverImplicitlyPermitATitle(){val cartoon=CatalogRow("a".repeat(32),"Animação sem classificação","movie",genres=listOf("Animação"));val approved=CatalogRow("b".repeat(32),"Título aprovado","movie");assertEquals(emptyList<CatalogRow>(),KidsCatalog.visible(listOf(cartoon,approved),true,emptySet()));assertEquals(listOf(approved),KidsCatalog.visible(listOf(cartoon,approved),true,setOf("vod:"+approved.id)));assertEquals(2,KidsCatalog.visible(listOf(cartoon,approved),false,emptySet()).size)}
 @Test fun throttlingAlsoSurvivesProcessRestart(){var disk:String?=null;var time=100L;val p=ParentalControl({disk},{disk=it},{time});p.configure("1432");p.select(true,"");repeat(5){reject{p.select(false,"0000")}};val reopened=ParentalControl({disk},{disk=it},{time});reject{reopened.select(false,"1432")};time+=60001;reopened.select(false,"1432");assertFalse(reopened.locked())}
 @Test fun approvalsCanBeRemovedWithoutAffectingAnotherTitle(){var disk:String?=null;val p=ParentalControl({disk},{disk=it});val a="vod:"+"a".repeat(32);val b="anime:"+"b".repeat(32);p.approve(a,true,"");p.approve(b,true,"");p.approve(a,false,"");assertEquals(setOf(b),p.approvedKeys());assertFalse(p.summary().has("hash"))}
}
