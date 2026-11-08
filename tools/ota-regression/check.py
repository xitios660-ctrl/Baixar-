#!/usr/bin/env python3
"""Verify a real release using R8-processed apksig; detect stripped ASN.1 reflection metadata."""
import argparse,pathlib,subprocess,tempfile,struct,zipfile
p=argparse.ArgumentParser();p.add_argument('--apk',type=pathlib.Path,required=True);p.add_argument('--signer',required=True);p.add_argument('--jdk',type=pathlib.Path,required=True);p.add_argument('--r8',required=True);p.add_argument('--apksig',required=True);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];rules=root/'tv/app/proguard-rules.pro'
def run(cmd):return subprocess.run(list(map(str,cmd)),check=True,capture_output=True,text=True).stdout
with tempfile.TemporaryDirectory(prefix='guhflix-ota-regression-') as tmp:
 work=pathlib.Path(tmp);classes=work/'classes';classes.mkdir();run([a.jdk/'bin/javac','-cp',a.apksig,'-d',classes,root/'tools/ota-regression/Verify.java'])
 base='-keep class Verify { public static void main(java.lang.String[]); }\n'
 for name,keep in [('before',base),('after',base+'\n'.join(line for line in rules.read_text().splitlines() if 'apksig' in line or '*Annotation*' in line)+'\n')]:
  config=work/(name+'.pro');config.write_text(keep);jar=work/(name+'.jar')
  run([a.jdk/'bin/java','-cp',a.r8,'com.android.tools.r8.R8','--classfile','--release','--lib',a.jdk,'--pg-conf',config,'--output',jar,classes/'Verify.class',a.apksig])
  result=run([a.jdk/'bin/java','-cp',jar,'Verify',a.apk,a.signer]);assert ('TRUSTED' in result) == (name=='after'),(name,result)
  print(name+': '+('TRUSTED' if 'TRUSTED' in result.splitlines() else 'REJECTED'))
  if name=='after':
   wrong=run([a.jdk/'bin/java','-cp',jar,'Verify',a.apk,'0'*64]);assert wrong.splitlines()[0]=='REJECTED'
   bad=work/'tampered.apk';data=bytearray(a.apk.read_bytes())
   with zipfile.ZipFile(a.apk) as z:entry=next(e for e in z.infolist() if e.filename.endswith('.dex'))
   offset=entry.header_offset;name_len,extra_len=struct.unpack_from('<HH',data,offset+26);data[offset+30+name_len+extra_len+100]^=1;bad.write_bytes(data)
   tampered=run([a.jdk/'bin/java','-cp',jar,'Verify',bad,a.signer]);assert tampered.splitlines()[0]=='REJECTED'
   print('Wrong signer: REJECTED; modified APK: REJECTED')
print('R8 signature regression: PASS')
