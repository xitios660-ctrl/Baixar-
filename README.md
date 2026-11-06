# GUHFLIX · plataforma de acesso

Implementação dedicada ao GUHFLIX; não modifica GANOH, RR Motos ou TV DO GUH AI. API Fastify/TypeScript e painel Node separado no Render. Banco MongoDB exclusivo, transações obrigatórias (replica set/Atlas). Não é gateway de vídeo, DRM ou autorização de provedores: as permissões do catálogo continuam independentes.

## Serviços

- Download publicado: https://guhflix-tv-download.onrender.com — APK 1.0.1 com novo ícone.
- Painel publicado: https://guhflix-admin.onrender.com — interface e proxy; integração de produção depende da API.
- API prevista: https://guhflix-api.onrender.com — ainda NÃO implantada sem credencial Atlas confirmada.
- Fonte: branch `guhflix-platform` em https://github.com/xitios660-ctrl/Baixar-.
- Atlas: projeto GUHFLIX / cluster gratuito `guhflix`; banco GUHFLIX. A ferramenta não confirmou a criação de `guhflix-api`; conexão de produção pendente. Nenhuma credencial de outro projeto foi reutilizada.

## Instalar e executar

Node 22+ e MongoDB replica set. `cd api && npm ci && npm run build`. Configure em variáveis de ambiente:

```
MONGODB_URI=mongodb+srv://USUARIO:SENHA@SEU_CLUSTER/GUHFLIX
JWT_SECRET=segredo-aleatorio-com-pelo-menos-48-caracteres
ADMIN_ORIGIN=https://guhflix-admin.onrender.com
BOOTSTRAP_TOKEN=segredo-privado-aleatorio-com-pelo-menos-48-caracteres
```

Não use os textos de exemplo como segredos. O usuário Mongo deve ter `readWrite` somente em GUHFLIX (e GUHFLIX_TEST para testes isolados). Libere na lista de acesso do projeto apenas os endereços de saída exibidos no serviço da API e os endereços do operador necessários; não use liberação mundial por conveniência. A implantação usa o mecanismo de variáveis do Render como fonte de verdade.

Build API: `npm ci --prefix api --include=dev && npm run build --prefix api`; start: `node api/dist/server.js`; plano gratuito inicial, região Virginia; health `/health`. O cliente Mongo é único por processo e usa os limites padrão do driver; medir conexões, fila e latência antes de ajustar o pool ou escalar. Duas instâncias exigiriam rate limiting compartilhado; o limite de reproduções já é transacional no banco.

Painel: start `node admin/server.mjs`; `API_URL` aponta para a URL HTTPS real da API. O proxy de mesma origem permite cookie HttpOnly/Secure/SameSite Strict também no Safari. Não há senha ou refresh token em localStorage; apenas identificador aleatório de instalação.

## Primeiro administrador

Após configurar o backend, abra privadamente `https://guhflix-admin.onrender.com/#setup=SEU_BOOTSTRAP_TOKEN`. O fragmento é removido do histórico visível e não é enviado no HTTP. Escolha usuário, senha >=16 caracteres e registre a chave exibida num autenticador TOTP (SHA1, 6 dígitos, 30 segundos). Confirme o código. O endpoint recusa recriar administrador, usa token privado, limite de tentativas e transação. Depois remova BOOTSTRAP_TOKEN no Render. Não publique esse link nem a chave MFA. Não existe admin/senha padrão.

Alternativa operacional, antes de existir admin: `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_TOTP_SECRET` e `npm run bootstrap` na pasta api. Senhas são Argon2id; a chave MFA precisa ser guardada pelo administrador. O reset do autenticador é operação administrativa com acesso ao banco e auditoria, não um endpoint público.

## Regras

Vários dispositivos podem ser cadastrados; no máximo três reproduções por conta. Cada sessão pode ter uma reprodução. Slots ficam num vetor limitado do usuário, atualizado com transação, para serializar concorrência entre instâncias. Lease de 120s, heartbeat do app a cada 35s e watchdog monotônico local antes de 110s desde o pedido; perda de rede não permite reprodução indefinida. Pausar fora do app/fechar player libera a sessão; sessões abandonadas expiram pelo relógio do servidor. TTL do Mongo limpa sessões de autenticação, mas a API verifica expiração sem depender da execução do TTL.

JWT de 10 min, refresh aleatório com hash no banco, rotação e detecção de reutilização anterior. Sessão renovável de 30 dias, separada do vencimento da assinatura. Bloqueio manual não é apagado por renovação. Sessões existentes podem consultar a reativação sem novo login; sessão revogada ou senha redefinida exige login. Android salva refresh com AES-GCM/AndroidKeyStore e desativa backup dos dados.

Renovação custa BRL 3000 centavos por 30 dias exatos, a partir do maior entre agora e vencimento atual. Exige confirmação explícita do recebimento e Idempotency-Key UUID. Pagamento, assinatura, vencimento, chave e auditoria são escritos na mesma transação. Repetir a mesma chave com corpo diferente falha. Não há gateway automático, cartão ou webhook ativado.

## Testes

`cd api && npm test` sobe MongoDB replica set descartável exclusivo e testa criação inicial protegida, senha/role, rejeição de objeto em usuário, expiração, renovação duplicada concorrente, preservação do bloqueio, 3/4 reproduções concorrentes, stop/timeout, rotação/reutilização de token, persistência entre instâncias da API, revogação, logout, receita e auditoria.

Painel também foi executado no Chromium com API e banco descartáveis reais: MFA, criar cliente, confirmar pagamento, renovar, consultar telas, bloquear/desbloquear e restaurar sessão após reload. As capturas locais contêm somente clientes de teste. Não confundir esses testes com homologação no Atlas de produção ou TCL física.

## Operação e backups

`/health` verifica ping no banco. A API gera logs estruturados sem corpos/senhas/tokens. Use os eventos e métricas do Render e configure alertas externos para o health após implantar. O plano gratuito pode suspender por inatividade; a primeira conexão fica sujeita ao cold start. Não há garantia de SLA comercial nesse plano.

Há exportação criptografada AES-256-GCM com snapshot consistente em `api/src/run-backup.ts`; o teste local verifica descriptografia autenticada e presença de pagamentos/auditoria. Execute com `MONGODB_URI`, `BACKUP_KEY` de 64 caracteres hexadecimais e `BACKUP_FILE` privado. Guarde a chave separada. O arquivo contém cabeçalho GUHFLIX1, IV de 12 bytes, gzip cifrado e tag final de 16 bytes; autentique a tag antes de restaurar os registros EJSON em banco vazio. A restauração operacional e o agendamento ainda precisam ser exercitados. Backup automático NÃO está ativado. O cluster gratuito não oferece os mesmos recursos de snapshots de planos pagos. Antes de cadastrar clientes reais, configure uma rotina de exportação exclusiva do banco GUHFLIX, criptografia, armazenamento externo privado, retenção e teste de restauração. Não guarde backup na pasta pública do site ou no disco efêmero do Render. Essa dependência operacional está explicitamente pendente.

Não considere concluído o aceite do PDF sem banco/API em produção, teste de ponta a ponta no APK e reprodução no hardware autorizado. A versão 2.0.0 candidata não deve substituir o download até essa conexão e validação.
