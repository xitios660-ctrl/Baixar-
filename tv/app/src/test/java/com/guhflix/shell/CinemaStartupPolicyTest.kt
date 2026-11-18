package com.guhflix.shell
import org.junit.Assert.*
import org.junit.Test
class CinemaStartupPolicyTest {
 @Test fun automaticRecoveryStopsAfterOneRetry(){val p=CinemaStartupPolicy();val first=p.begin();assertEquals(CinemaStartupPolicy.Failure.RETRY_COMPATIBLE,p.fail(first));val second=p.begin();repeat(50){assertEquals(CinemaStartupPolicy.Failure.SHOW_RECOVERY,p.fail(second))};assertFalse(p.markReady(second))}
 @Test fun oldPageCannotHideRecoveryOrReplaceNewPage(){val p=CinemaStartupPolicy();val old=p.begin();p.fail(old);val next=p.begin();assertFalse(p.markReady(old));assertEquals(CinemaStartupPolicy.Failure.IGNORE,p.fail(old));assertFalse(p.ready);assertTrue(p.markReady(next));assertTrue(p.ready)}
 @Test fun manualRetryStartsANewBoundedAttempt(){val p=CinemaStartupPolicy();p.fail(p.begin());p.fail(p.begin());val manual=p.begin(true);assertEquals(CinemaStartupPolicy.Failure.RETRY_COMPATIBLE,p.fail(manual))}
 @Test fun rendererDeathAfterReadyStillRecovers(){val p=CinemaStartupPolicy();val id=p.begin();assertTrue(p.markReady(id));assertEquals(CinemaStartupPolicy.Failure.RETRY_COMPATIBLE,p.fail(id));assertFalse(p.ready);assertFalse(p.markReady(id))}
}
