package br.com.saimo.tv
/** Video READY must keep its first-frame deadline; audio-only streams have no frame. */
object StartupMediaPolicy {
 fun audioOnlyReady(audioSelected:Boolean,videoSelected:Boolean)=audioSelected&&!videoSelected
}
