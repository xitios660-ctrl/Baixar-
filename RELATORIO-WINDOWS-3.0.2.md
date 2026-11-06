# GUHFLIX Windows 3.0.2 — navegação e recuperação de tela

## Problema e alteração

O usuário informou que o aplicativo muda para Ao vivo enquanto navega nos
filmes. Foi encontrado no código um redirecionamento explícito: qualquer
pânico no desenho do catálogo levava a Aba::Canais. Erros sucessivos também
podiam encerrar o processo depois de três quadros.

Esse redirecionamento foi removido. Agora, uma falha capturada interrompe o
desenho daquela tela e aguarda uma ação do usuário. O aplicativo não troca
a seção por Ao vivo, não retoma um canal por esse tratamento de erro e não
repete o desenho quebrado continuamente. As ações disponíveis são Tentar
novamente, Voltar ao início e Copiar diagnóstico. A cópia lê apenas as
últimas linhas de falhas.txt; não lê credenciais, tokens ou arquivos de sessão.

Mantidas a compilação nativa MSVC x64 com runtime C estático da 3.0.1,
a API de produção, identidade visual, autenticação e bibliotecas do player.
Nenhuma mudança de política de segurança do Windows.

## Limites

O pânico exato do computador do usuário ainda não foi reproduzido ou
identificado. A correção remove um caminho comprovado no código que
causava a mudança automática. Se aparecer a tela de recuperação, seu
registro permitirá investigar a falha de desenho que a disparou.

Não foi testada a rolagem no computador físico do usuário nem reprodução
real de filmes. O runner Windows não fornece OpenGL 2.0 suficiente para
validar a interface; seu teste de início verifica chegada ao main e
tratamento controlado da indisponibilidade gráfica.

## Validação

Fonte: commit 6e34cff, branch guhflix-windows.
Workflow Windows nativo: execução 42287518389.

Os testes de regressão simulam falha no desenho da rolagem, verificam que
quadros posteriores não executam o desenho antes de uma ação explícita,
que a tentativa liberada pode funcionar e que falhas repetidas capturadas
não propagam o pânico para encerrar o processo.

Resultado executado no Windows: 88 testes passaram, 0 falharam, 8 ignorados.
Build release MSVC x64 e teste de início concluídos com sucesso.
Evidência de início: versão 3.0.2 registrada, instância reservada e tentativa
de criar o contexto OpenGL; o runner retornou o erro gráfico controlado
já descrito. Não valida a interface em um desktop com aceleração gráfica.
Importações PE e arquitetura x64 conferidas no executável empacotado.
