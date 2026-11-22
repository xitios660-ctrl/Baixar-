package com.guhflix.shell
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import br.com.saimo.tv.BuildConfig
import br.com.saimo.tv.Preferencias
import br.com.saimo.tv.Progresso
class ProfilePreferenceIsolationTest {
 @Test fun realPreferenceAndProgressAccessorsKeepTwoPeopleSeparateAcrossSwitchAndRename(){
  assumeTrue(BuildConfig.GFILMES_LOCAL)
  val context=ProfileMemoryContext();Preferencias.iniciar(context)
  val profiles=LocalProfiles.store(context);profiles.create("Ana",0);profiles.create("Bruno",11)
  val list=profiles.list().getJSONArray("items");val a=list.getJSONObject(0).getString("id");val b=list.getJSONObject(1).getString("id");val movie=Progresso.chaveFilme("Synthetic fixture movie")
  profiles.select(a);Preferencias.versao="leg";Preferencias.legenda=3;Preferencias.proximoAutomatico=false;Preferencias.telemetria=false;Progresso.salvar(context,movie,90000,600000)
  profiles.select(b);assertEquals("dub",Preferencias.versao);assertEquals(1,Preferencias.legenda);assertTrue(Preferencias.proximoAutomatico);assertFalse(Preferencias.telemetria);assertEquals(0L,Progresso.posicao(context,movie));Preferencias.legenda=0;Progresso.salvar(context,movie,210000,600000)
  profiles.select(a);profiles.update(a,"Ana Maria",4);assertEquals("leg",Preferencias.versao);assertEquals(3,Preferencias.legenda);assertFalse(Preferencias.proximoAutomatico);assertEquals(90000L,Progresso.posicao(context,movie))
  profiles.select(b);assertEquals(0,Preferencias.legenda);assertEquals(210000L,Progresso.posicao(context,movie));assertFalse(Preferencias.telemetria)
 }
}
