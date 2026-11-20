package com.gfilmes.server
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import com.gfilmes.license.Crypto
import java.security.MessageDigest
object PasswordHash {
 // Fixed Argon2id dummy has no account and no retained usable password.
 private const val DUMMY="\$argon2id\$v=19\$m=65536,t=3,p=4\$ZHVtbXktc2FsdC1saWMtdjE\$U//FOJX0Mk5L6ZgzUwlkzhLavWisqDAEMOYqzaZlU/s"
 fun verifyKnownOrDummy(phc:String?,password:String):Boolean {val valid=verify(phc?:DUMMY,password);return phc!=null&&valid}

 fun verify(phc:String,password:String):Boolean=try{
  val p=phc.split('$');require(p.size==6&&p[1]=="argon2id"&&p[2]=="v=19");val params=p[3].split(',').associate{val v=it.split('=');v[0] to v[1].toInt()};val memory=params.getValue("m");val time=params.getValue("t");val lanes=params.getValue("p");require(memory in 8192..131072&&time in 1..6&&lanes in 1..8&&password.length<=128)
  fun decode(v:String)=Crypto.decode(v.trimEnd('=').replace('+','-').replace('/','_'))
  val salt=decode(p[4]);val expected=decode(p[5]);require(salt.size in 8..64&&expected.size in 16..64);val out=ByteArray(expected.size);val generator=Argon2BytesGenerator();generator.init(Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withVersion(Argon2Parameters.ARGON2_VERSION_13).withMemoryAsKB(memory).withIterations(time).withParallelism(lanes).withSalt(salt).build());val input=password.toByteArray(Charsets.UTF_8);try{generator.generateBytes(input,out);MessageDigest.isEqual(out,expected)}finally{input.fill(0);out.fill(0)}
 }catch(_:Exception){false}
}
