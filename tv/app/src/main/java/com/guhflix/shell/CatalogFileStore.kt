package com.guhflix.shell
import java.io.File
import java.util.concurrent.ConcurrentHashMap
/** Per-file locks: one slow download never monopolizes unrelated cached files. */
class CatalogFileStore(private val folder:File,private val prefix:String,private val fetch:(String)->String,private val now:()->Long={System.currentTimeMillis()}){
 private val locks=ConcurrentHashMap<String,Any>()
 init{require(prefix.matches(Regex("[a-zA-Z0-9_-]+")));folder.mkdirs()}
 private fun path(name:String):File {require(name.matches(Regex("(?:[A-Za-z0-9_#-]+\\.txt|redeflix/links-(?:animes|doramas)\\.txt)")));return File(folder,prefix+"_"+name.replace('/','_'))}
 fun cached(name:String):String? {val f=path(name);return if(f.isFile)runCatching{f.readText()}.getOrNull()?.takeIf{it.isNotBlank()&&!it.take(300).contains("<html",true)} else null}
 fun read(name:String,force:Boolean=false,staleOnFailure:Boolean=true):String=synchronized(locks.getOrPut(name){Any()}){
  val f=path(name);val previous=cached(name)
  if(!force&&previous!=null&&now()-f.lastModified()<6*3600000L)return@synchronized previous
  try{val data=fetch(name);require(data.isNotBlank()&&!data.take(300).contains("<html",true)){"Resposta inválida da fonte"};val tmp=File(f.path+".tmp");tmp.writeText(data);check(tmp.renameTo(f)){"Não foi possível atualizar o cache"};trim(f);data}catch(e:Exception){if(previous!=null&&staleOnFailure)previous else throw e}
 }
 private fun trim(current:File){val files=folder.listFiles()?.filter{it.isFile}?:return;var bytes=files.sumOf{it.length()};for(f in files.sortedBy{it.lastModified()}){if(bytes<=96*1024*1024L)break;if(f!=current){bytes-=f.length();f.delete()}}}
}
