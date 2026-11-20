package com.guhflix.shell
import org.junit.Assert.*
import org.junit.Test
class PlaybackLeasePolicyTest {
 @Test fun localPlaybackHasNoActivationTimerRegardlessOfClockOrOldLicenseState(){for(elapsed in listOf(0L,120000,86400000,30L*86400000))for(remaining in listOf(-1L,0L,5000L,30L*86400000))assertEquals(Long.MAX_VALUE,PlaybackLeasePolicy.deadline(true,false,elapsed,remaining))}
 @Test fun subscriptionEditionsKeepTheOriginalBoundedLease(){assertEquals(210000,PlaybackLeasePolicy.deadline(false,false,100000,-1));assertEquals(105000,PlaybackLeasePolicy.deadline(false,true,100000,5000));assertEquals(100000,PlaybackLeasePolicy.deadline(false,true,100000,-1));assertEquals(210000,PlaybackLeasePolicy.deadline(false,true,100000,30L*86400000))}
}
