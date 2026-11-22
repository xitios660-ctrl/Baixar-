package com.guhflix.shell
import com.gfilmes.license.CatalogRow
import com.gfilmes.license.Crypto
import java.security.MessageDigest
/** Listing-only parser: no network, source resolution, fuzzy-search normalization or per-byte Formatter. */
object CatalogSnapshotParser {
 private val year=Regex("\\s*\\(\\d{4}\\)\\s*$")
 private data class Meta(val poster:String,val id:Int,val genres:List<String>)
 fun validate(name:String,data:String){
  if(name=="busca.txt")parse(data)
  if(name=="fichas.txt")require(data.lineSequence().any{(it.startsWith("f\t")||it.startsWith("s\t"))&&it.count{c->c=='\t'}>=4}){"Metadados da fonte inválidos"}
 }
 fun parse(search:String,metadata:String=""):List<CatalogRow>{
  val maps=HashMap<String,Meta>();val genrePool=HashMap<String,List<String>>();var imageBase=""
  for(line in metadata.lineSequence()){
   if(line.startsWith("capa:")){imageBase=line.substringAfter("capa:").trim().takeIf{it.startsWith("https://")}.orEmpty();continue}
   if(line.startsWith("#"))continue
   val c=line.split('\t');if(c.size<5||c[0] !in listOf("f","s"))continue
   val poster=if(c[3].startsWith("https://"))c[3] else if(c[3].isNotBlank())imageBase+c[3] else ""
   val genres=genrePool.getOrPut(c[4]){c[4].split(',').filter{it.isNotBlank()}}
   maps[c[0]+"\u0000"+c[1]]=Meta(poster,c[2].toIntOrNull()?:0,genres)
  }
  val digest=MessageDigest.getInstance("SHA-256");val out=ArrayList<CatalogRow>();val seen=HashSet<String>()
  for(line in search.lineSequence()){
   val c=line.split('\t');if(c.size<3||c[1] !in listOf("f","s")||c[0].replace(year,"").isBlank())continue
   val kind=if(c[1]=="s")"series" else "movie";val y=c.getOrNull(3).orEmpty()
   val id=Crypto.hex(digest.digest((kind+"\u0000"+c[0]+"\u0000"+c[2]+"\u0000"+y).toByteArray())).take(32)
   if(!seen.add(id))continue
   val m=maps[c[1]+"\u0000"+c[0]]?:maps[c[1]+"\u0000"+c[0].replace(year,"").trim()]
   out.add(CatalogRow(id,c[0],kind,c[2],y,m?.genres?:emptyList(),m?.poster?:"",m?.id?:0))
  }
  require(out.isNotEmpty()){"O índice do catálogo está vazio ou inválido"}
  return out
 }
}
