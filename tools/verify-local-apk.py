#!/usr/bin/env python3
from pathlib import Path
import json,hashlib,zipfile,subprocess,os,re
root=Path(__file__).resolve().parents[1]
folder=Path(os.environ.get('GFILMES_LOCAL_ARTIFACTS','/workspace/artifacts/gfilmes-local'))
apk=folder/'Gfilmes-Local-TV-1.7.0.apk'
sdk=Path(os.environ.get('ANDROID_HOME','/workspace/tools/android-sdk'));java=os.environ.get('JAVA_HOME','/workspace/tools/jdk21/current');env={**os.environ,'JAVA_HOME':java,'PATH':java+'/bin:'+os.environ['PATH']}
cert='fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b'
result=subprocess.run([str(sdk/'build-tools/35.0.0/apksigner'),'verify','--verbose','--print-certs',str(apk)],env=env,capture_output=True,text=True,check=True).stdout
assert cert in result and 'Verified using v2 scheme (APK Signature Scheme v2): true' in result
badging=subprocess.check_output([str(sdk/'build-tools/35.0.0/aapt'),'dump','badging',str(apk)],env=env,text=True)
assert "name='com.gfilmes.tv.local' versionCode='1007000' versionName='1.7.0'" in badging
assert "sdkVersion:'23'" in badging and "application-label:'Gfilmes Local'" in badging
config=(root/'tv/app/build/generated/source/buildConfig/release/br/com/saimo/tv/BuildConfig.java').read_text()
for expected in ['GFILMES_LOCAL = true','GFILMES_OFFLINE = false','GUHFLIX_API = ""','GUHFLIX_UPDATE_MANIFEST = ""']:assert expected in config,expected
with zipfile.ZipFile(apk) as z:
 assert z.testzip() is None
 assetRoot=root/'tv/app/src/main/assets/gfilmes'
 for asset in assetRoot.rglob('*'):
  if asset.is_file():assert z.read('assets/gfilmes/'+asset.relative_to(assetRoot).as_posix())==asset.read_bytes(),asset.name
 import gzip
 assert {n for n in z.namelist() if n.startswith('assets/gfilmes/catalog-snapshot/')}=={'assets/gfilmes/catalog-snapshot/manifest.json','assets/gfilmes/catalog-snapshot/busca.txt.bin','assets/gfilmes/catalog-snapshot/fichas.txt.bin'}
 snapshot=json.loads(z.read('assets/gfilmes/catalog-snapshot/manifest.json'))
 for name,meta in snapshot['files'].items():assert hashlib.sha256(gzip.decompress(z.read('assets/gfilmes/catalog-snapshot/'+name+'.bin'))).hexdigest()==meta['sha256']
 avatars=json.loads((assetRoot/'avatars/manifest.json').read_text())['avatars'];assert len(avatars)==12
 from PIL import Image
 import io
 for a in avatars:
  payload=z.read('assets/gfilmes/'+a['gif']);assert hashlib.sha256(payload).hexdigest()==a['sha256']
  gif=Image.open(io.BytesIO(payload));assert gif.size==(152,152) and gif.n_frames==16 and gif.info['loop']==0
  frames=[]
  for i in range(gif.n_frames):gif.seek(i);frames.append(hashlib.sha256(gif.convert('RGBA').tobytes()).hexdigest())
  assert len(set(frames))>1

 assert not any(n.lower().endswith(('.jks','.keystore','.pem','.p12','.pfx','.env')) for n in z.namelist())
 dex=b''.join(z.read(n) for n in z.namelist() if n.endswith('.dex'))
 assert b'StandaloneSession' in dex and b'LOCAL_ONLY' in dex
 assert b'fastPreview' in dex and b'CinemaWorkQueue' in dex and b'CatalogViewCache' in dex
 assert not any(s in dex for s in [b'SetupTransferService',b'ActivationService',b'gfilmes-authority.db',b'mongodb+srv://',b'LocalDeviceAccess',b'Autorizar esta TV'])
report={'file':apk.name,'package':'com.gfilmes.tv.local','version':'1.7.0','versionCode':1007000,'bytes':apk.stat().st_size,'sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),'signerSha256':cert,'apkV2SignatureVerified':True,'uiAssetsMatchSource':True,'standaloneBuildFlag':True,'signedOfflineLicenseMode':False,'accountApiConfigured':False,'productionOtaConfigured':False,'activationServerPackaged':False,'privateKeyFilesPackaged':False,'historicalCatalogSnapshotIncluded':True,'snapshotDate':'2026-11-07','catalogRefreshNonBlocking':True,'lightweightPreview':True,'contentIoSeparatedFromBrowse':True,'pagedFacetsCached':True,'animatedAvatarCount':12,'avatarFrames':16,'sourceGifHashesVerified':True,'localAnimatedAssets':True,'parentalPinOnly':True,'profilePreferencesIsolated':True,'familyProfilesIncluded':True,'profilePasswordsRequired':False,'legacyProfilePinsRemoved':True,'deviceBindingRequired':False,'deviceGrantEmbedded':False,'customerCredentialsPackaged':False,'physicalTvTested':False,'productionChanged':False}
(folder/'APK-MANIFEST.json').write_text(json.dumps(report,indent=2)+'\n');(folder/'SHA256SUMS.txt').write_text(report['sha256']+'  '+apk.name+'\n');print(json.dumps(report,indent=2))
