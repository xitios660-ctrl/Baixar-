# Verificação da preparação — 17/11/2026

- Compose: `docker compose ... config --quiet` aprovado com origem e valores fictícios, sem ler segredos de produção.
- Caddy 2: `caddy validate` em container aprovado; isso valida a configuração e não emite certificado real nem comprova DNS de uma VM.
- API: `npm run build --prefix api` aprovado; código da aplicação não foi modificado nesta preparação.
- Verificador público executado contra o Render MARY existente: /health 200, /api/connect/health 200 com catálogo disponível, /pwa/ 200, /admin/ 200, /api/connect/catalog sem autenticação 401. São cinco verificações somente leitura, não teste de uma Oracle e não teste de streaming.
- Build completo da imagem API: tentativa interrompida por falta de espaço no ambiente local durante instalação das ferramentas de compilação. A imagem não foi aprovada nem enviada para um registro. Repetir na VM com espaço suficiente e na arquitetura ARM64 de destino; o Dockerfile compila as dependências nessa arquitetura.
- Oracle: conta inexistente conforme informado pelo titular. Nenhuma VM, rede, certificado, domínio, IP ou regra Atlas criada. Nenhum gasto ou alteração de plano.
- Produção: Render, MongoDB, contas, pagamentos, assinaturas, JWT, chaves Android, downloads e OTA não alterados. Arquivos ficam em branch separada `ops/oracle-free-staging`, sem deploy automático dessa branch.

Pendências: cadastro do titular, acesso autorizado à conta/VM, capacidade Always Free, hostname HTTPS de teste, transferência privada das variáveis existentes, liberação restrita do IP no Atlas, build ARM64 e testes reais de login/reprodução/pareamento antes de qualquer corte de produção.
