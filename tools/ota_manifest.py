#!/usr/bin/env python3
"""Generate a version manifest only after apksigner verifies the installed app's signer."""
import argparse,hashlib,json,pathlib,re,subprocess,urllib.parse
p=argparse.ArgumentParser();p.add_argument('apk',type=pathlib.Path);p.add_argument('--apksigner',default='apksigner');p.add_argument('--aapt',default='aapt');p.add_argument('--url',required=True);p.add_argument('--notes',required=True);p.add_argument('--mandatory',action='store_true');p.add_argument('--output',type=pathlib.Path,required=True);args=p.parse_args()
url=urllib.parse.urlparse(args.url)
assert url.scheme=='https' and url.hostname and not url.username and not url.fragment,'APK URL must be HTTPS'
verification=subprocess.check_output([args.apksigner,'verify','--verbose','--print-certs',str(args.apk)],text=True)
expected='0f1c1118520d99353f57e756c5801362d12ba10e36dcf31e4d7a93b8cc762810'
signers=re.findall(r'Signer #\d+ certificate SHA-256 digest: ([a-f0-9]+)',verification)
assert signers==[expected],'Signing certificate differs from installed GUHFLIX TV 2.4.2'
badging=subprocess.check_output([args.aapt,'dump','badging',str(args.apk)],text=True)
m=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
assert m and m[1]=='com.guhflix.tv' and int(m[2])>2040002,'Package/version incompatible with installed app'
value={'package':m[1],'versionCode':int(m[2]),'version':m[3],'file':args.apk.name,'apkUrl':args.url,'sha256':hashlib.file_digest(args.apk.open('rb'),'sha256').hexdigest(),'bytes':args.apk.stat().st_size,'signerSha256':expected,'notes':pathlib.Path(args.notes).read_text(),'mandatory':args.mandatory,'api':'https://guhflix-marley-api.onrender.com','admin':'https://guhflix-marley-admin.onrender.com'}
args.output.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n');print('Verified signed APK; manifest written:',args.output)
