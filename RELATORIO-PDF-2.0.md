# GUHFLIX TV 2.0.1 — entrega corrigida

O download foi atualizado para o APK 2.0.1 (com.guhflix.tv, versionCode 2000001), com tela nativa de usuário/senha, sessão persistente, validação de acesso e autorização do player. A versão 1.0.1 anterior não incluía esse acesso e foi mantida somente como arquivo de recuperação.

API correta: https://guhflix-marley-api.onrender.com
Painel correto: https://guhflix-marley-admin.onrender.com
Workspace Render: Marley's workspace, confirmado pelo proprietário. MongoDB: conta Guh Flix / Cluster0; a implantação atual usa o banco existente em minúsculas guhflix. Nenhum banco foi apagado ou migrado nesta correção.

## Correção realizada

A candidata Android apontava para a API antiga, enquanto a configuração pronta do usuário estava nos serviços Marley. O endereço foi corrigido no código nativo e o APK foi recompilado e assinado com a mesma chave da versão pública anterior. O build agora força reconstrução para aplicar corretamente versionName/versionCode, evitando metadados antigos do cache.

A página, o arquivo principal e o link de versão foram atualizados. O APK 2.0.1 contém a tela de acesso; não é apenas a versão visual 1.0.1 renomeada. Para entrar, usar a conta de cliente do painel com assinatura ativa. Não usar credenciais do MongoDB no aplicativo.

## Recursos presentes no código

Identidade GUHFLIX e abertura animada; interface Android TV com foco por controle; busca, detalhes, temporadas, episódios, favoritos e retomada da base nativa. Login GUHFLIX, refresh criptografado no AndroidKeyStore, conta/logout, bloqueio de assinatura, contato do atendimento, limite de três autorizações de reprodução e gestão de sessões. Player original preservado e inicializado após autorização da API. Preview silencioso quando o catálogo fornece fonte autorizada, com espera ajustada para aproximadamente 1,5 segundo. Sem download offline inventado ou contorno de DRM.

Servidor atual: renovação manual R$ 30 / 30 dias, idempotência, pagamentos/auditoria, bloqueio manual, expiração, heartbeat e revogação. As configurações do usuário recebidas do repositório (login administrativo simplificado e senhas de no mínimo oito caracteres) foram preservadas. MFA não está ativo nessa configuração atual; os testes antigos de MFA não comprovam MFA na implantação atual.

## Verificações realmente executadas nesta entrega

- API Marley /health retornou HTTP 200, ok:true, indicando conexão com o banco.
- Painel Marley /health retornou HTTP 200.
- Rota autenticada sem sessão retornou HTTP 401.
- POST de login de TV com credencial inválida de teste retornou HTTP 401 INVALID_CREDENTIALS, compatível com o cliente Android; nenhuma conta real foi acessada nesse teste.
- Compilação Android, assinatura, pacote, versão 2.0.1/code2000001, manifestos e DEX verificados.
- Mesmo certificado da versão 1.0.1, permitindo atualização do pacote existente.
- DEX contém somente a API de produção Marley esperada; não contém a API antiga nem endpoints locais de teste.
- Testes locais com MongoDB replica set real descartável: autenticação/RBAC, renovação concorrente idempotente, limite 3/4 de leases, parada/timeout, bloqueio, refresh, isolamento entre contas, auditoria, backup criptografado e prontidão HTTP passaram após alinhar os testes ao banco em minúsculas.

Em testes anteriores do mesmo cliente nativo: login no emulador Android9, persistência após force-stop, bloqueio por vencimento, reativação, tela de limite e regressão de Voltar antes de inicializar o player. Esses testes locais anteriores não equivalem a autenticar uma conta real nesta nova implantação.

## Limites honestos

O login com conta real, o catálogo carregado dentro da TCL, reprodução de vídeo real, legendas/áudio, troca de episódio, reinício físico da TV, desempenho 60 FPS e 4K precisam de validação no aparelho. Um teste anterior com título sintético indisponível comprovou tratamento de erro, não reprodução. Não há garantia de transferência do histórico ou credenciais do Saimo para o pacote separado.

A exportação criptografada de backup existe, mas agendamento, armazenamento externo, alertas e ensaio operacional de restauração continuam pendentes. A mudança do banco para minúsculas também foi alinhada ao utilitário de backup. Recursos gratuitos do Render estão sujeitos a cold start. Não se declara o PDF integralmente homologado enquanto esses itens não forem executados.

O código Android entregue é Java novo, Smali e recursos reconstruíveis; não é o Kotlin original de alto nível. Chaves de assinatura e segredos não fazem parte do fonte público.
