#!/usr/bin/env python3
"""Read-only deployment checks: never accepts credentials or starts media."""
import argparse,json,urllib.request,urllib.parse
p=argparse.ArgumentParser();p.add_argument('origin');a=p.parse_args()
u=urllib.parse.urlsplit(a.origin)
if u.scheme!='https' or not u.hostname or u.username or u.password or u.query or u.fragment or u.path not in ('','/'):
 p.error('Use an HTTPS origin only, without credentials, path or query')
base=a.origin.rstrip('/')
checks=[('/health',200,'application/json'),('/api/connect/health',200,'application/json'),('/pwa/',200,'text/html'),('/admin/',200,'text/html'),('/api/connect/catalog',401,'application/json')]
failed=False
for path,expected,content in checks:
 try:
  try:r=urllib.request.urlopen(base+path,timeout=35)
  except urllib.error.HTTPError as e:r=e
  with r:
   body=r.read(65536);ok=r.status==expected and content in r.headers.get('Content-Type','')
   if path=='/api/connect/health' and ok:ok=json.loads(body).get('catalog',{}).get('available') is True
   failed|=not ok;print(json.dumps({'path':path,'status':r.status,'expected':expected,'passed':ok}))
 except Exception as e:failed=True;print(json.dumps({'path':path,'passed':False,'error':type(e).__name__}))
raise SystemExit(1 if failed else 0)
