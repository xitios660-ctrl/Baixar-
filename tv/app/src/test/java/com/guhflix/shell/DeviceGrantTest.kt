package com.guhflix.shell
import com.gfilmes.license.Crypto
import java.security.KeyPairGenerator
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
class DeviceGrantTest {
 private val keys=KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair()
 private val device="a".repeat(64)
 private fun payload()=JSONObject().put("v",1).put("kind","gfilmes-local-device").put("customerRef","b".repeat(64)).put("deviceId",device).put("issuedAt",1000).put("expiresAt",2000).put("subscriptionExpiresAt",2000)
 private fun reject(f:()->Unit){try{f();fail("Should reject")}catch(_:IllegalArgumentException){}}
 @Test fun signedGrantContainsOnlyDeviceAndOpaqueCustomerReference(){val p=DeviceGrant.verify(Crypto.signJws(payload(),keys.private),device,keys.public);assertEquals(device,p.getString("deviceId"));assertFalse(p.has("username"));assertFalse(p.has("password"))}
 @Test fun anotherDeviceCannotUseCopiedAuthorization(){val j=Crypto.signJws(payload(),keys.private);reject{DeviceGrant.verify(j,"c".repeat(64),keys.public)}}
 @Test fun tamperedAndUntrustedGrantsAreRejected(){val p=payload();val j=Crypto.signJws(p,keys.private);val x=j.split('.');val modified=x[0]+"."+Crypto.encode(p.put("deviceId","c".repeat(64)).toString().toByteArray())+"."+x[2];reject{DeviceGrant.verify(modified,device,keys.public)};reject{DeviceGrant.verify(j,device,KeyPairGenerator.getInstance("RSA").apply{initialize(2048)}.generateKeyPair().public)}}
 @Test fun authorizationCannotExtendBeyondRealSubscription(){reject{DeviceGrant.verify(Crypto.signJws(payload().put("expiresAt",2001),keys.private),device,keys.public)}}
}
