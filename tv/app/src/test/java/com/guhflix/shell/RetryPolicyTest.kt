package com.guhflix.shell
import org.junit.Assert.*
import org.junit.Test
class RetryPolicyTest {
 @Test fun failuresBackOffAndResetAfterSuccess(){val p=RetryPolicy();assertEquals(listOf(5000L,10000L,20000L,30000L,30000L),List(5){p.nextDelayMs()});repeat(1000){assertEquals(30000L,p.nextDelayMs())};p.reset();assertEquals(5000L,p.nextDelayMs())}
}
