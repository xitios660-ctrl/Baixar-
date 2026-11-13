# GUHFLIX AppDeploy (homologação)

Nova infraestrutura **paralela e incompleta**, isolada da produção atual.

**Situação:** backend de diagnóstico somente leitura, interface de status responsiva e testes de homologação. Nenhuma conta, assinatura, pagamento, senha ou sessão de reprodução foi migrada para este novo backend.

## Execução

O diretório `appdeploy/` é uma aplicação Vite/React. O backend `backend/index.ts` utiliza a interface oficial `router`, `secrets`, `json` de `@appdeploy/sdk`. A plataforma AppDeploy injeta `@appdeploy/sdk` e `@appdeploy/client`, que não devem ser listados no package.json.

O projeto exige a dependência `mongodb` no backend e os segredos **`MONGODB_URI`** e **`JWT_SECRET`** fornecidos através do coletor protegido de secrets. A ausência das configurações mantém o painel em modo pendente, sem conexão falsificada.

Chamadas disponibilizadas:
- `GET /api/_healthcheck`: confirma que o novo processo está ativo.
- `GET /api/migration/status`: verifica nomes dos segredos, faz `ping` ao banco `guhflix` e valida a presença das 12 coleções originais **sem ler registros de clientes**.

Os endpoints de login de cliente, administração, pagamentos, catálogo, playback, TV Connect e OTA **não estão implementados nesta etapa**. Evitar apresentar a URL de homologação como substituição do serviço de produção.

## Validação de migração

Ver `MIGRACAO.md`. Nunca trocar URLs de aplicativos ou desativar a origem antes de homologação de autenticação, assinaturas, limite de reprodução, catálogos e OTA no hardware real.

O código desta pasta deve ser publicado como raiz de projeto AppDeploy React/Vite + backend. Em uma criação nova por template, enviar apenas os arquivos novos e diffs de template, além de `tests/tests.json` completo.
