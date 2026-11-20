package com.guhflix.shell
import br.com.saimo.tv.BuildConfig
import br.com.saimo.tv.Atualizacao
import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
@androidx.media3.common.util.UnstableApi
class StandaloneUpdaterTest {
 @Test fun localEditionDoesNotReadContextOrFetchTheLegacyUpdateManifest()=runBlocking {
  assumeTrue(BuildConfig.GFILMES_LOCAL)
  assertNull(Atualizacao.procurar(ContextWrapper(null),true))
  assertEquals("",BuildConfig.GUHFLIX_API)
  assertEquals("",BuildConfig.GUHFLIX_UPDATE_MANIFEST)
 }
}
