# Operação GUHFLIX Connect

O PWA e a API usam o serviço existente guhflix-marley-api, branch guhflix-platform. O diretório pwa é servido em /pwa/ pelo próprio Fastify. Não há nova instância paga, IPA ou alteração no Windows.

## iPhone e TV

1. No Safari, abra https://guhflix-marley-api.onrender.com/pwa/ e use a conta de cliente GUHFLIX, não o administrador.
2. Compartilhar → Adicionar à Tela de Início.
3. No APK 2.5.0 da TV: Conta / iPhone → Gerar código de vínculo.
4. No iPhone: Conectar TV → informar o código de cinco minutos. A mesma conta precisa estar ativa em ambos.
5. Escolha o título e Assistir na TV. Séries permitem escolher temporada/episódio. O player roda na TV; o telefone mostra o estado recebido dela.
6. Revogue pelo telefone ou por Desvincular iPhones desta TV. Uma sessão explicitamente encerrada exige novo vínculo; renovação normal mantém o vínculo.

## Configuração existente

MONGODB_URI, JWT_SECRET e ADMIN_ORIGIN permanecem no Render. TMDB_API_KEY contém a credencial da integração de metadados autorizada. Não colocar credenciais nos APKs, páginas, manifestos ou no Git. CATALOG_BASE_URL e CATALOG_AUTH_TOKEN são opcionais e somente do servidor. O catálogo padrão usa o formato original autorizado; erros de acesso não são contornados.

As coleções connect_pairings, connect_links, connect_commands e connect_state são aditivas. Códigos são armazenados como hash, com TTL. TTL não substitui a validação de expiração em cada comando. A liberação e validade existentes não foram alteradas; os testes incluem cliente ativo por três dias e expiração/bloqueio. Nenhuma regra nova de concessão de acesso foi inventada.

## Novo APK / OTA

1. Guarde a chave original fora do repositório e configure tv/keystore.properties local. Confira o certificado informado em tv/README.md.
2. Aumente versionCode/versionName no projeto. Execute os testes e compile assembleRelease. Nunca publique o debug.
3. Execute tools/ota_manifest.py com o APK assinado, URL HTTPS e notas. Ele recusa certificado/pacote incompatível e calcula bytes e SHA-256 reais.
4. Publique o APK com nome versionado no site de downloads existente. Confirme download público, SHA-256 e certificado. Só então atualize release.json. Preserve os campos antigos file/package/api/admin e todos os arquivos Windows.
5. Manifesto: https://guhflix-tv-download.onrender.com/release.json. Versão 2.5.0 consulta ao abrir e a cada seis horas enquanto a tela inicial está ativa; há verificação manual em Conta / iPhone e Ajustes.
6. A TV baixa com progresso, guarda parcial ao cancelar, valida bytes/hash e assinatura apksig e compara o certificado com o instalado. O instalador do Android pede confirmação. Android 8+ pode pedir autorização de fonte externa para GUHFLIX. Não há instalação silenciosa.
7. Teste primeiro numa TV/emulador com a versão anterior: instalar por cima, conferir login/configurações/favoritos/progresso e vínculo. Não desinstale antes de validar a atualização.

## Cache e novas versões do site

Atualizações de catálogo/metadados passam pela API sem reinstalar. Cache do catálogo no servidor: até cinco minutos e 32 MiB; arquivos nativos também usam cache local. Use Recarregar catálogo na TV quando precisar invalidar o cache local. Alterações do site exigem publicar os arquivos pwa e incrementar a versão do service worker/URLs dos assets. Somente o shell público entra no cache; nenhuma resposta /api nem vídeo é guardado. Alteração nativa exige APK/OTA.

## Rollback / dados

Baseline de rollback do servidor: e2d0e35fce522cc6c00e5d05d9b3c7832b7e5923. A versão anterior ignora as novas coleções; não apague dados, usuários ou coleções para reverter o código. Serviços existentes, regras administrativas e campos de usuários permanecem compatíveis. Nenhuma alteração destrutiva foi aplicada. Para futuras migrações destrutivas, execute antes o backup cifrado existente e valide restauração em ambiente separado.

Reverter o manifesto não rebaixa APKs instalados: Android recusa versionCode menor. Corrija por nova versão com código maior e a mesma chave. Mantenha APK anterior arquivado e não mude assinaturas.
