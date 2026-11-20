package com.gfilmes.server
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject
class ServerSecurityTest {
 @Test fun existingNodeArgon2idHashesRemainUsable(){val hash="\$argon2id\$v=19\$m=8192,t=2,p=1\$Zml4dHVyZS1zYWx0LTEyMzQ\$gxYrpdKnRTI9XZDECcWlQ65iyzCRMG2X57p/yWWRmTI";assertTrue(PasswordHash.verify(hash,"offline-fixture-password"));assertFalse(PasswordHash.verify(hash,"wrong-password"));assertFalse(PasswordHash.verify(hash.replace("m=8192","m=999999999"),"offline-fixture-password"));assertFalse(PasswordHash.verify("not-a-hash","offline-fixture-password"));assertFalse(PasswordHash.verifyKnownOrDummy(null,"offline-fixture-password"))}
 @Test fun backupRoundtripRejectsWrongPasswordAndTampering(){val data=ByteArray(300000){(it%251).toByte()};val password="fixture-backup-password".toCharArray();val encrypted=BackupCodec.encrypt(data,password);assertArrayEquals(data,BackupCodec.decrypt(encrypted,password));try{BackupCodec.decrypt(encrypted,"incorrect-password".toCharArray());fail()}catch(_:Exception){};val tampered=JSONObject(encrypted.toString());val encoded=tampered.getString("encrypted");tampered.put("encrypted",(if(encoded.first()=='A')"B" else "A")+encoded.drop(1));try{BackupCodec.decrypt(tampered,password);fail()}catch(_:Exception){};try{BackupCodec.encrypt(data,"short".toCharArray());fail()}catch(_:Exception){}}
}
