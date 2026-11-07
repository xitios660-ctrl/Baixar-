package br.com.saimo.tv

import androidx.appcompat.app.AppCompatActivity
import com.guhflix.shell.OtaManager

class OfertaDeAtualizacao(private val tela:AppCompatActivity, private val status:(String)->Unit){
 val baixando:Boolean get()=OtaManager.downloading
 fun ofertar(){OtaManager.check(tela,false)}
 fun avisarSemInterromper(){OtaManager.check(tela,false)}
 fun retomarSePendente(){OtaManager.resume(tela)}
}
