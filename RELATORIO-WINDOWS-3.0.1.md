# GUHFLIX Windows 3.0.1 — tentativa de correção da inicialização

## Problema observado no computador do usuário

Windows 10 22H2 de 64 bits. A versão 3.0.0 encerrou na abertura com
0xC0000005 (-1073741819). O Windows registrou BEX64/StackHash e uma
falha de acesso à memória. A ausência anterior da pasta de dados sugere
uma falha antes do registro de inicialização, mas não comprova a causa.
Não foi obtido um dump de memória; a causa exata ainda não foi estabelecida.

## Alterações

- Compilação nativa x64 MSVC em Windows, substituindo a compilação GNU distribuída.
- Runtime C vinculado estaticamente ao executável.
- Recursos de ícone e versão compilados pelo Windows SDK rc.exe.
- Registro de entrada no main antes da verificação de instância única.
- Mantidos catálogo, identidade visual, API, autenticação e limite de telas.
- Mantidas as DLLs originais do player, sem alteração.
- Sem alteração de antivírus, políticas, registro ou privilégios do usuário.

## Limites da validação

A confirmação de abertura no computador do usuário ainda é necessária.
Testes de CI não validam o driver gráfico, áudio e reprodução real desse PC.
Não é alegada resolução definitiva da falha com base apenas na recompilação.

## Validação executada em 06/11/2026

Workflow Windows nativo, commit 72ae294, execução 42283740071:
- Etapa cargo test --locked concluída com sucesso no Windows.
- Build release MSVC x64 concluído com sucesso.
- Executável iniciado pelo Windows: alcançou main, registrou a versão,
  reservou a instância e tentou criar o contexto OpenGL.
- O runner sem aceleração gráfica recusou OpenGL 2.0+, gerando mensagem
  e registro de erro controlado. A interface não foi validada nesse runner.
- A exceção nativa 0xC0000005 não se repetiu nesse teste.
- Importações PE conferidas: sem dependência de VCRUNTIME140.dll,
  MSVCP, libgcc ou libwinpthread externos.
- Nenhuma reprodução real de conteúdo ou teste no PC do usuário foi feito.

Evidências: arquivos inicio.txt e falhas.txt no artefato
GUHFLIX-startup-evidence da execução acima.
