package com.gfilmes.server
import android.security.keystore.*
import java.security.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import com.gfilmes.license.Crypto
object ServerKeys {
 private const val ISSUER="gfilmes-server-issuer-v1";private const val STORAGE="gfilmes-server-store-v1"
 private fun store():KeyStore {val s=KeyStore.getInstance("AndroidKeyStore");s.load(null);return s}
 private fun ensure(){val s=store();if(!s.containsAlias(ISSUER)){val g=KeyPairGenerator.getInstance("RSA","AndroidKeyStore");g.initialize(KeyGenParameterSpec.Builder(ISSUER,KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_DECRYPT).setKeySize(2048).setDigests(KeyProperties.DIGEST_SHA256,KeyProperties.DIGEST_SHA1).setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP).build());g.generateKeyPair()}}
 fun privateKey():PrivateKey {ensure();return store().getKey(ISSUER,null) as PrivateKey}
 fun publicKey():PublicKey {ensure();return store().getCertificate(ISSUER).publicKey}
 fun id()=Crypto.sha(publicKey().encoded)
 private fun aes():SecretKey {val s=store();if(!s.containsAlias(STORAGE)){val g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(KeyGenParameterSpec.Builder(STORAGE,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());g.generateKey()};return store().getKey(STORAGE,null) as SecretKey}
 fun seal(bytes:ByteArray):ByteArray {val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,aes());return c.iv+c.doFinal(bytes)}
 fun open(bytes:ByteArray):ByteArray {require(bytes.size>28);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,aes(),GCMParameterSpec(128,bytes.copyOfRange(0,12)));return c.doFinal(bytes.copyOfRange(12,bytes.size))}
 fun unwrap(bytes:ByteArray):ByteArray {val c=Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding");c.init(Cipher.DECRYPT_MODE,privateKey());return c.doFinal(bytes)}
}
