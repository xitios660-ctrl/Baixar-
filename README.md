# GUHFLIX — plataforma atual Marley

API: https://guhflix-marley-api.onrender.com
Painel: https://guhflix-marley-admin.onrender.com
Download: https://guhflix-tv-download.onrender.com/GUHFLIX-TV-2.0.1.apk

Render: Marley's workspace, conexão Conta Para Tv Dog Marley, confirmado pelo proprietário. Serviços guhflix-marley-api e guhflix-marley-admin. MongoDB: conexão Guh Flix, Project0 / Cluster0, banco EXISTENTE `guhflix` em minúsculas. Preservar coleções e dados; não mudar capitalização nem voltar para os serviços antigos sem Marley.

## Estado e acesso

Health da API e do painel retornaram200. APK2.0.1 com.guhflix.tv aponta para a API Marley e inclui tela nativa de usuário/senha. O aplicativo recebe a conta de cliente cadastrada no painel, com assinatura ativa. Login MongoDB e login da TV são coisas distintas.

O proprietário configurou acesso administrativo somente com usuário e senha, sem autenticador/MFA, e senhas de no mínimo8 caracteres. Essas mudanças remotas foram preservadas. Não declarar MFA ativo nem reintroduzir campos de autenticador sem considerar as escolhas atuais do proprietário. Argon2id, JWT curto, hash/rotação de refresh e role no servidor continuam presentes.

## Compilar/executar

Node22+, MongoDB replica set/Atlas. API: `npm ci --prefix api --include=dev && npm run build --prefix api`; start `node api/dist/server.js`. Painel: `node admin/server.mjs`, com API_URL para a API Marley. Proxy de mesma origem e cookie HttpOnly/Secure/SameSite Strict; não há refresh/senha em localStorage.

Variáveis MONGODB_URI, JWT_SECRET e ADMIN_ORIGIN são privadas no Render. Não imprimir ou versionar valores. ADMIN_ORIGIN corresponde ao painel Marley. O token de bootstrap, quando presente, é segredo de primeiro acesso; não recriar administrador se já existir. O serviço só ativa rotas autenticadas depois de conectar ao Mongo e criar índices. Antes disso retorna503; /health200 não significa que todas as funções de vídeo da TV foram homologadas.

## Regras de acesso

Plano BRL3000 centavos/30 dias exatos. Renovação parte do maior entre agora e vencimento atual, usa relógio UTC do servidor e exige confirmação manual do pagamento. Não há cobrança automática ou cartão/webhook ativados. Registrar método, período e operador. Transação grava assinatura, pagamento, vencimento, auditoria e chave idempotente. Renovação não apaga bloqueio manual.

Vários dispositivos podem estar cadastrados; máximo3 leases simultâneos de reprodução por conta. Lease120s, heartbeat35s e watchdog monotônico local antes de110s; quarta tentativa recusada sem derrubar outro usuário. Parada, timeout e revogação liberam/invalidam sessões. Cada cliente só gerencia suas sessões. O servidor deve continuar aplicando as regras, independentemente do relógio da TV.

JWT10min, refresh rotativo com hash no banco e detecção de reutilização, sessão renovável de30 dias separada do vencimento da assinatura. O app Android usa AES-GCM/AndroidKeyStore e dados sem backup automático. Não é proxy/DRM de filmes nem substitui autorização do provedor.

## Testes e limites

`cd api && npm test`: testes em MongoDB replica set real descartável `guhflix_test`, sem acessar clientes reais. Autenticação/RBAC, isolamento de sessão, renovação concorrente/idempotência, limite3/4, parada/timeout, bloqueio, refresh/revogação/logout, auditoria/receita, backup e prontidão HTTP passaram. Testes foram alinhados ao banco em minúsculas e a exportação de backup usa a mesma capitalização.

Painel atual testado localmente no Chromium: login usuário/senha, criar cliente, confirmar pagamento de teste isolado, renovar, consultar telas, bloquear/desbloquear e restaurar sessão após reload; sem erros JavaScript. Em produção foram verificados health e o contrato de login inválido de TV. Não houve acesso a conta real nesse teste.

A reprodução e login de cliente real na TCL, reinício físico, áudio/legendas, episódios, desempenho/4K ainda exigem validação no aparelho. Não apresentar testes sintéticos ou de licença como vídeos efetivamente tocando.

## Backups e operação

Exportação de snapshot com AES-256-GCM em api/src/run-backup.ts. Usar MONGODB_URI, BACKUP_KEY privado (64 caracteres hexadecimais) e BACKUP_FILE privado. Formato GUHFLIX1 + IV12 bytes + gzip cifrado + tag16 bytes. Guardar chave separada e autenticar tag antes de restaurar EJSON em ambiente isolado. Script e criptografia foram testados localmente; agendamento, armazenamento externo/retencão, alertas e ensaio operacional de restauração permanecem pendentes. Não guardar backup na pasta pública de downloads ou disco efêmero do Render.

Planos gratuitos sujeitos a cold start. Nenhuma configuração desta plataforma autoriza modificar GANOH, RR Motos, TV DO GUH AI ou outros projetos.
