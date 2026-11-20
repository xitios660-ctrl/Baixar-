import java.nio.file.*;import java.security.*;import java.util.*;import java.io.*;
/** Operator-only tool. Reads existing signing properties privately; never exports the APK private key. */
public class AuthorityTool {
 static String b64(byte[] b){return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 static String sha(byte[] b)throws Exception{ return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b)); }
 static KeyStore store(Properties p,Path tv)throws Exception {KeyStore k=KeyStore.getInstance("JKS");Path f=Path.of(p.getProperty("storeFile"));if(!f.isAbsolute())f=tv.resolve("app").resolve(f);try(InputStream in=Files.newInputStream(f)){k.load(in,p.getProperty("storePassword").toCharArray());}return k;}
 public static void main(String[] args)throws Exception {
  if(args.length<2)throw new IllegalArgumentException("public <tv-dir> | approve <tv-dir> <server-public-key-base64url-file> <grant-output> | snapshot-sign <tv-dir> <snapshot-json> <signature-output>");
  Path tv=Path.of(args[1]);Properties p=new Properties();try(InputStream in=Files.newInputStream(tv.resolve("keystore-connect.properties"))){p.load(in);}KeyStore k=store(p,tv);String alias=p.getProperty("keyAlias");PublicKey root=k.getCertificate(alias).getPublicKey();
  if(args[0].equals("public")){System.out.println(b64(root.getEncoded()));return;}
  PrivateKey privateKey=(PrivateKey)k.getKey(alias,p.getProperty("keyPassword").toCharArray());
  if(args[0].equals("snapshot-sign")){Signature s=Signature.getInstance("SHA256withRSA");s.initSign(privateKey);s.update(Files.readAllBytes(Path.of(args[2])));Files.writeString(Path.of(args[3]),b64(s.sign()));return;}
  byte[] encoded=Base64.getUrlDecoder().decode(Files.readString(Path.of(args[2])).trim());PublicKey pub=KeyFactory.getInstance("RSA").generatePublic(new java.security.spec.X509EncodedKeySpec(encoded));if(! (pub instanceof java.security.interfaces.RSAPublicKey rsa)||rsa.getModulus().bitLength()<2048)throw new IllegalArgumentException("RSA 2048+ required");
  long now=System.currentTimeMillis();String payload="{\"v\":1,\"kind\":\"gfilmes-authority\",\"issuerId\":\""+sha(encoded)+"\",\"publicKey\":\""+b64(encoded)+"\",\"notBefore\":"+(now-60000)+",\"expiresAt\":"+(now+365L*86400000)+",\"maxLicenseMs\":2592000000}";
  String jws=b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8))+"."+b64(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));Signature s=Signature.getInstance("SHA256withRSA");s.initSign(privateKey);s.update(jws.getBytes(java.nio.charset.StandardCharsets.US_ASCII));Files.writeString(Path.of(args[3]),jws+"."+b64(s.sign()));System.out.println("Public authority grant saved; issuer "+sha(encoded));
 }
}
