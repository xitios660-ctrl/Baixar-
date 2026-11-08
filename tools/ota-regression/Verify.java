import java.io.File;
import java.security.MessageDigest;
import com.android.apksig.ApkVerifier;
/** Runs against the same library after R8, not only against an unoptimized test dependency. */
public class Verify {
 public static void main(String[] args) throws Exception {
  ApkVerifier.Result result=new ApkVerifier.Builder(new File(args[0])).setMinCheckedPlatformVersion(23).build().verify();
  boolean trusted=result.isVerified()&&result.getSignerCertificates().size()==1;
  if(trusted){byte[] digest=MessageDigest.getInstance("SHA-256").digest(result.getSignerCertificates().get(0).getEncoded());StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format("%02x",b));trusted=args[1].equals(hex.toString());}
  System.out.println(trusted?"TRUSTED":"REJECTED");
  if(!result.isVerified())for(ApkVerifier.Result.V1SchemeSignerInfo signer:result.getV1SchemeSigners())System.out.println(signer.getErrors());
 }
}
