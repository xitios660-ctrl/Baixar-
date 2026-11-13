# GUHFLIX | Migração segura para AppDeploy (staging)

> **NÃO É PRODUÇÃO.** Esta branch é separada de `guhflix-platform`, que continua sendo a origem das instalações atuais. Não publicar nem alterar os endpoints Android sem validação.

## Diagnóstico em 13/11/2026
- Render workspace confirmado: Marley's workspace.
- `guhflix-marley-api` e `guhflix-marley-admin`: status **suspended**, suspender **billing**. Não são apenas casos de cold start.
- Repositório de origem: `xitios660-ctrl/Baixar-`, branch `guhflix-platform`.
- Banco MongoDB Atlas: Guh Flix / Project 0 / Cluster0 / database `guhflix` (minúsculas); coleções observadas: `users`, `audit_logs`, `connect_links`, `connect_state`, `sessions`, `payments`, `subscriptions`, `idempotency_keys`, `devices`, `web_playbacks`, `connect_commands`, `connect_pairings`. **Nunca inicializar do zero nem renomear esse banco.**

## Contrato a manter
1. Contas atuais e hashes Argon2id; autenticação cliente/admin, sessões de 30 dias, JWT curto e rotação de refresh.
2. R$ 30,00 por 30 dias corridos, renovação somente com pagamento confirmado, idempotência e auditoria. Sem cobrança automática.
3. Máximo de 3 *reproduções simultâneas* por conta, não limite de 3 dispositivos cadastrados; leasing e heartbeat no servidor; quarta reprodução negada.
4. Bloqueio e vencimento conferidos no servidor; renovação não remove bloqueio manual.
5. Endpoints dos apps TCL/Android, web/PWA, painel e fluxo Connect/TV ao vivo; conservar permissões por cliente.
6. Download de APK e OTA com pacote, certificado, versão e hash válidos. Troca de domínio **não** atualiza automaticamente um APK já instalado.
7. Não alterar os projetos GANOH, RR Motos ou TV DO GUH AI.

## Segurança
- Obter `MONGODB_URI` e `JWT_SECRET` **somente pelo formulário de segredos do AppDeploy**. Não registrar em Git, frontend, conversa ou logs.
- A API substituta deve usar o driver de MongoDB e transações sem ler/escrever dados fora do database `guhflix`. Rodar primeiro testes **somente leitura**: conexão, `ping`, metadados das coleções.
- Até os testes passarem, não criar usuário, renovar, bloquear, migrar sessão ou executar bootstrap na base real.
- Em cada mutação, validar identidade e função do operador, escopo da conta, período da assinatura e idempotência.
- Atenção: `@appdeploy/sdk` usa seu banco nativo separado; ele NÃO acessa automaticamente o Atlas existente. Um novo painel vazio não significa migração.

## Ordem de implementação e testes
- [ ] 1. Implantar backend AppDeploy em ambiente paralelo; `GET /api/_healthcheck`, confirmação de `MONGODB_URI` e `ping` sem expor segredos.
- [ ] 2. Portar autenticação/refresh/roles com hashes e sessões existentes; testar login válido, inválido, bloqueado e logout com banco isolado.
- [ ] 3. Portar assinaturas, pagamentos, auditoria, renovação de 30 dias/idempotência; testes transacionais.
- [ ] 4. Portar sessões de reprodução, lease de 120 s, heartbeat, liberação e limite 3/4; testar concorrência.
- [ ] 5. Portar pareamento, catálogo autorizado, TV ao vivo, comandos e controle remoto.
- [ ] 6. Portar painel administrativo e PWA, comparar contratos HTTP e renderização.
- [ ] 7. Testar ponta a ponta em homologação e depois na TCL/Android físico; validar assinatura OTA, instalação e transmissão real.
- [ ] 8. Criar backup cifrado verificável; definir retorno/rollback e só então redirecionar serviços e atualizar URLs dos apps.

## Estado da publicação
Ainda **não existe cutover** nem URL nova pronta para os clientes. Render suspenso não deve ser usado como proxy provisório: isso manteria a dependência que a migração pretende eliminar.
