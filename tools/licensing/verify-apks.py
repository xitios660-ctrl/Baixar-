#!/usr/bin/env python3
"""Checks actual signed artifacts; never reads private signing configuration."""
from pathlib import Path
import hashlib,json,subprocess,zipfile,os
root=Path(__file__).resolve().parents[2]
sdk=Path(os.environ.get('ANDROID_HOME','/workspace/tools/android-sdk'))
java=os.environ.get('JAVA_HOME','/workspace/tools/jdk21/current')
env={**os.environ,'JAVA_HOME':java,'PATH':java+'/bin:'+os.environ['PATH']}
cert='fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b'
folder=Path(os.environ.get('GFILMES_ARTIFACTS','/workspace/artifacts/gfilmes-offline-30d'))
rows=[]
for name,pkg,version,code in [('Gfilmes-Cliente-3.0.0-homologacao.apk','com.guhflix.tv.connect','3.0.0',3000000),('Gfilmes-Servidor-1.0.0-homologacao.apk','com.gfilmes.activationserver','1.0.0',1000000),('Gfilmes-Servidor-1.0.1-homologacao.apk','com.gfilmes.activationserver','1.0.1',1000001),('Gfilmes-Recuperacao-Online-3.0.1.apk','com.guhflix.tv.connect','3.0.1',3000001)]:
 p=folder/name
 if not p.exists():continue
 result=subprocess.run([str(sdk/'build-tools/35.0.0/apksigner'),'verify','--verbose','--print-certs',str(p)],env=env,text=True,capture_output=True,check=True).stdout
 assert cert in result and 'Verified using v2 scheme (APK Signature Scheme v2): true' in result
 badging=subprocess.check_output([str(sdk/'build-tools/35.0.0/aapt'),'dump','badging',str(p)],env=env,text=True)
 assert f"package: name='{pkg}' versionCode='{code}' versionName='{version}'" in badging
 with zipfile.ZipFile(p) as z:
  assert z.testzip() is None
  assert not any(n.lower().endswith(('.jks','.keystore','.p12','.pfx','.pem','.env')) for n in z.namelist())
  if pkg=='com.guhflix.tv.connect':
   for asset in (root/'tv/app/src/main/assets/gfilmes').iterdir():assert z.read('assets/gfilmes/'+asset.name)==asset.read_bytes(),asset.name
 row={'name':name,'package':pkg,'version':version,'versionCode':code,'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'signerSha256':cert,'apkV2SignatureVerified':True,'zipIntegrityVerified':True,'privateKeyFilesPackaged':False,'physicalInstallationTested':False,'productionReleased':False}
 rows.append(row)
(folder/'APK-MANIFEST.json').write_text(json.dumps({'stage':'homologation','artifacts':rows},indent=2)+'\n')
(folder/'SHA256SUMS.txt').write_text(''.join(x['sha256']+'  '+x['name']+'\n' for x in rows))
print(json.dumps(rows,indent=2))
