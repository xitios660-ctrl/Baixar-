#!/usr/bin/env python3
"""Portable package: explicit binaries only, never reads user data/session files."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import argparse,hashlib,json,struct
p=argparse.ArgumentParser();p.add_argument('--exe',type=Path,required=True);p.add_argument('--mpv-dir',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--report',type=Path);a=p.parse_args()
files={'GUHFLIX.exe':a.exe,'libmpv-2.dll':a.mpv_dir/'libmpv-2.dll','vulkan-1.dll':a.mpv_dir/'vulkan-1.dll'}
checks={}
for name,path in files.items():
 with path.open('rb') as f:
  dos=f.read(64);assert dos[:2]==b'MZ',name;f.seek(struct.unpack_from('<I',dos,60)[0]);pe=f.read(6);assert pe[:4]==b'PE\0\0' and pe[4:]==b'\x64\x86',name
  f.seek(0);h=hashlib.sha256()
  for block in iter(lambda:f.read(1024*1024),b''):h.update(block)
  checks[name]=h.hexdigest()
text='''GUHFLIX Windows3.0.0 — Windows10/11 x64\n\n1. Extraia TODOS os arquivos do ZIP para uma pasta.\n2. Abra GUHFLIX.exe. Não abra o executável de dentro da janela do ZIP.\n3. Entre com usuário e senha da conta de CLIENTE ativa criada no painel GUHFLIX. É a mesma conta usada na TV. Não use credenciais do MongoDB ou administrador.\n4. A conta permite ATÉ3 reproduções simultâneas, somando PCs e TVs. A multitelas conta cada vídeo separadamente.\n5. Conta permite ver/encerrar sessões e sair. Revogar um dispositivo encerra todas as reproduções dele e exige novo login.\n\nÉ um programa nativo recompilado com a integração GUHFLIX; não é o antigo modo rápido por CMD. Nenhum launcher especial ou DLL auxiliar é necessário.\n\nNão mova o EXE sozinho: libmpv-2.dll e vulkan-1.dll precisam acompanhar a pasta. Dados novos em %APPDATA%\\GUHFLIX; sessão protegida por DPAPI do Windows. Dados antigos %APPDATA%\\SaimoTV não são apagados nem importados automaticamente.\n\nO programa não instala serviços nem altera firewall/antivírus. Não precisa executar como administrador. Não foi fornecido certificado Authenticode, portanto não há alegação de assinatura digital do EXE. Não é necessário desativar proteção do Windows.\n\nServidor pode precisar iniciar após inatividade; aguarde na tela de login. Catálogo e reprodução dependem das fontes autorizadas e da internet. O programa não aumenta a banda contratada.\n\nCréditos/base: SaimoWin2.6.7, código público de SaimoTV/SaimoWin, personalização autorizada pelo usuário. Motor original libmpv e DLL Vulkan fornecidos pelo usuário, preservados byte a byte.\n\nVeja RELATORIO-TESTES.md para distinguir testes executados de validações pendentes, especialmente vídeo/áudio/legendas reais no PC.\n'''
a.out.parent.mkdir(parents=True,exist_ok=True)
with ZipFile(a.out,'w',ZIP_DEFLATED,compresslevel=6) as z:
 for name,path in files.items():z.write(path,'GUHFLIX-Windows/'+name)
 z.writestr('GUHFLIX-Windows/LEIA-PRIMEIRO.txt',text.encode('utf-8-sig'))
 z.writestr('GUHFLIX-Windows/SHA256.json',json.dumps(checks,indent=2))
 if a.report:z.write(a.report,'GUHFLIX-Windows/RELATORIO-TESTES.md')
with ZipFile(a.out) as z:assert z.testzip() is None
h=hashlib.sha256(a.out.read_bytes()).hexdigest();a.out.with_suffix(a.out.suffix+'.sha256').write_text(h+'  '+a.out.name+'\n');print(json.dumps({'zip':a.out.name,'bytes':a.out.stat().st_size,'sha256':h,'files':checks}))
