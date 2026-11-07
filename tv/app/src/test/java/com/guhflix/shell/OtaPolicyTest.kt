package com.guhflix.shell

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class OtaPolicyTest {
 private fun manifest()=JSONObject().put("package","com.guhflix.tv").put("versionCode",2050000).put("version","2.5.0").put("apkUrl","https://example.com/tv.apk").put("sha256","a".repeat(64)).put("bytes",5000000)
 @Test fun validRelease(){val r=OtaPolicy.parse(manifest(),"com.guhflix.tv");assertEquals(2050000L,r.code);assertFalse(r.mandatory)}
 @Test fun rejectWrongPackage(){assertThrows(IllegalArgumentException::class.java){OtaPolicy.parse(manifest().put("package","other"),"com.guhflix.tv")}}
 @Test fun rejectInsecureUrl(){assertThrows(IllegalArgumentException::class.java){OtaPolicy.parse(manifest().put("apkUrl","http://example.com/a.apk"),"com.guhflix.tv")}}
 @Test fun rejectCredentialsInUrl(){assertThrows(IllegalArgumentException::class.java){OtaPolicy.parse(manifest().put("apkUrl","https://user:pass@example.com/a.apk"),"com.guhflix.tv")}}
 @Test fun rejectOversizeAndBadHash(){assertThrows(IllegalArgumentException::class.java){OtaPolicy.parse(manifest().put("bytes",200000000),"com.guhflix.tv")};assertThrows(IllegalArgumentException::class.java){OtaPolicy.parse(manifest().put("sha256","xx"),"com.guhflix.tv")}}
 @Test fun resumeRequiresCorrectRange(){assertTrue(OtaPolicy.canAppend(206,"bytes 1024-2047/4096",1024));assertFalse(OtaPolicy.canAppend(206,"bytes 0-2047/4096",1024));assertFalse(OtaPolicy.canAppend(200,null,1024))}
 @Test fun spaceAccountsForPartialAndReserve(){assertTrue(OtaPolicy.remainingSpace(10000000,9000000,10000000));assertFalse(OtaPolicy.remainingSpace(10000000,0,10000000));assertFalse(OtaPolicy.remainingSpace(10000000,11000000,100000000))}
 @Test fun hashChecksActualBytes(){val file=File.createTempFile("ota",".part");try{file.writeText("abc");assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",OtaPolicy.hash(file));file.appendText("x");assertNotEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",OtaPolicy.hash(file))}finally{file.delete()}}
}
