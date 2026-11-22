package com.guhflix.shell
import java.util.concurrent.Executors
/** Provider/episode IO must not monopolize browsing or profile controls. */
class CinemaWorkQueue:AutoCloseable {
 private val fast=Executors.newSingleThreadExecutor{r->Thread(r,"gflix-browse").apply{isDaemon=true}}
 private val content=Executors.newFixedThreadPool(2){r->Thread(r,"gflix-content").apply{isDaemon=true}}
 fun submit(op:String,action:()->Unit){(when(op){"detail","anime","live","play"->content;else->fast}).execute(action)}
 override fun close(){fast.shutdownNow();content.shutdownNow()}
}
