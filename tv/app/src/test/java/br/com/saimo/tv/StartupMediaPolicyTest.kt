package br.com.saimo.tv
import org.junit.Assert.*
import org.junit.Test
class StartupMediaPolicyTest {
 @Test fun videoReadyCannotProveAnImage(){assertFalse(StartupMediaPolicy.audioOnlyReady(true,true));assertFalse(StartupMediaPolicy.audioOnlyReady(false,true))}
 @Test fun radioDoesNotWaitForVideo(){assertTrue(StartupMediaPolicy.audioOnlyReady(true,false));assertFalse(StartupMediaPolicy.audioOnlyReady(false,false))}
}
