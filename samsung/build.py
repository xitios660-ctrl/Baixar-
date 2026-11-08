from pathlib import Path
import zipfile,json,hashlib,xml.etree.ElementTree as E
root=Path(__file__).resolve().parent;site=Path('/workspace/guhflix-download-site');files=['config.xml','index.html','style.css','app.js','icon.png']
xml=E.parse(root/'config.xml').getroot();assert xml.attrib['version']=='1.0.0'
for name in files:assert (root/name).is_file()
for name in ['GUHFLIX-Samsung-1.0.0-nao-assinado.wgt','GUHFLIX-Samsung-1.0.0-fonte.zip']:
 with zipfile.ZipFile(site/name,'w',zipfile.ZIP_DEFLATED) as z:
  for file in files+(['README.md','build.py','test.cjs'] if name.endswith('.zip') else []):z.write(root/file,file)
 p=site/name;(site/(name+'.sha256')).write_text(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+name+'\n')
with zipfile.ZipFile(site/'GUHFLIX-Samsung-1.0.0-nao-assinado.wgt') as z:assert not any('signature' in n.lower() for n in z.namelist())
(site/'release-samsung.json').write_text(json.dumps({'version':'1.0.0','file':'GUHFLIX-Samsung-1.0.0-nao-assinado.wgt','signed':False,'status':'source-and-unsigned-widget','minimumTizen':'4.0','modelConfirmed':False,'physicalDeviceTested':False,'storePublished':False},indent=2)+'\n')
print('Unsigned widget, sources, XML and resources verified. Samsung signing and physical validation pending.')
