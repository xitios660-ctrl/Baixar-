package com.guhflix.shell
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.*
class CinemaWorkQueueTest {
 @Test fun blockedEpisodesDoNotBlockBrowsingOrProfileControls(){val q=CinemaWorkQueue();val held=CountDownLatch(1);val release=CountDownLatch(1);val browse=CountDownLatch(1);val profile=CountDownLatch(1);try{q.submit("detail"){held.countDown();release.await(3,TimeUnit.SECONDS)};assertTrue(held.await(1,TimeUnit.SECONDS));q.submit("catalog"){browse.countDown()};q.submit("profileSelect"){profile.countDown()};assertTrue(browse.await(500,TimeUnit.MILLISECONDS));assertTrue(profile.await(500,TimeUnit.MILLISECONDS))}finally{release.countDown();q.close()}}
 @Test fun profileMutationsKeepTheirSubmissionOrder(){val q=CinemaWorkQueue();val done=CountDownLatch(3);val ids=java.util.Collections.synchronizedList(mutableListOf<Int>());try{for(i in 1..3)q.submit("profileSelect"){ids.add(i);done.countDown()};assertTrue(done.await(1,TimeUnit.SECONDS));assertEquals(listOf(1,2,3),ids)}finally{q.close()}}
}
