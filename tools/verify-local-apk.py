#!/usr/bin/env python3
from pathlib import Path
import json,hashlib,zipfile,subprocess,os,re
root=Path(__file__).resolve().parents[1]
folder=Path(os.environ.get('GFILMES_LOCAL_ARTIFACTS','/workspace/artifacts/gfilmes-local'))
apk=folder/'Gfilmes-Local-TV-1.0.0.apk'
sdk=Path(os.environ.get('ANDROID_HOME','/workspace/tools/android-sdk'));java=os.environ.get('JAVA_HOME','/workspace/tools/jdk21/current');env={**os.environ,'JAVA_HOME':java,'PATH':java+'/bin:'+os.environ['PATH']}
cert='fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b'
result=subprocess.run([str(sdk/'build-tools/35.0.0/apksigner'),'verify','--verbose','--print-certs',str(apk)],env=env,capture_output=True,text=True,check=True).stdout
assert cert in result and 'Verified using v2 scheme (APK Signature Scheme v2): true' in result
badging=subprocess.check_output([str(sdk/'build-tools/35.0.0/aapt'),'dump','badging',str(apk)],env=env,text=True)
assert "name='com.gfilmes.tv.local' versionCode='1000000' versionName='1.0.0'" in badging
assert "sdkVersion:'23'" in badging and "application-label:'Gfilmes Local'" in badging
config=(root/'tv/app/build/generated/source/buildConfig/release/br/com/saimo/tv/BuildConfig.java').read_text()
for expected in ['GFILMES_LOCAL = true','GFILMES_OFFLINE = false','GUHFLIX_API = ""','GUHFLIX_UPDATE_MANIFEST = ""']:assert expected in config,expected
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 for asset in (root/'tv/app/src/main/assets/gfilmes').iterdir():assert z.read('assets/gfilmes/'+asset.name)==asset.read_bytes(),asset.name
 assert not any(n.lower().endswith(('.jks','.keystore','.pem','.p12','.pfx','.env')) for n in z.namelist())
 dex=b''.join(z.read(n) for n in z.namelist() if n.endswith('.dex'))
 assert b'StandaloneSession' in dex and b'LOCAL_ONLY' in dex
 assert not any(s in dex for s in [b'SetupTransferService',b'ActivationService',b'gfilmes-authority.db',b'mongodb+srv://'])
report={'file':apk.name,'package':'com.gfilmes.tv.local','version':'1.0.0','versionCode':1000000,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'signerSha256':cert,'apkV2SignatureVerified':True,'uiAssetsMatchSource':True,'standaloneBuildFlag':True,'signedOfflineLicenseMode':False,'accountApiConfigured':False,'productionOtaConfigured':False,'activationServerPackaged':False,'privateKeyFilesPackaged':False,'physicalTvTested':False,'productionChanged':False}
(folder/'APK-MANIFEST.json').write_text(json.dumps(report,indent=2)+'\n');(folder/'SHA256SUMS.txt').write_text(report['sha256']+'  '+apk.name+'\n');print(json.dumps(report,indent=2))
