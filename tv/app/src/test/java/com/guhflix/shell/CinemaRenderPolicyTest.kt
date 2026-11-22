package com.guhflix.shell
import org.junit.Assert.*
import org.junit.Test
class CinemaRenderPolicyTest {
 @Test fun localUpgradeTriesGpuOnceEvenWhenOldSoftwareDefaultWasSaved(){assertFalse(CinemaRenderPolicy.compatible(true,false,true,true))}
 @Test fun recoveryAndManualSoftwareChoiceSurviveLaterLaunches(){assertTrue(CinemaRenderPolicy.compatible(true,true,true,false));assertFalse(CinemaRenderPolicy.compatible(true,true,false,true))}
 @Test fun nonLocalRendererChoiceDoesNotChange(){assertTrue(CinemaRenderPolicy.compatible(false,false,null,true));assertTrue(CinemaRenderPolicy.compatible(false,false,true,false));assertFalse(CinemaRenderPolicy.compatible(false,false,false,true))}
}
