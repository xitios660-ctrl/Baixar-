# GFILMES: preparação de atualização OTA após catálogo e botão Voltar

**Status: NÃO PUBLICADO AOS CLIENTES.** A branch prepara apenas versões que precisam ser compiladas, assinadas, verificadas e colocadas no servidor de download antes da mudança de qualquer manifesto.

## Conteúdo
- Catálogo amplo de filmes e séries por gênero, ano e ordenação; busca com filtros combinados.
- Botão visível **← Voltar** na lista de Filmes/Séries/Animes para retornar ao seletor; suporte ao controle remoto.
- Backend já publicado na conta Render MARY, em `https://gfilmes-mary-api.onrender.com`.

## Identidades instaladas e versões a preparar

| Edição | packageName | Antiga no código | Nova nesta branch | Chave necessária |
| --- | --- | --- | --- | --- |
| TV Connect | `com.guhflix.tv.connect` | `2070006 / 2.7.6` | `2070007 / 2.7.7` | Assinatura Connect atual (`keystore-connect.properties`) |
| Android mobile | `com.guhflix.android` | `2070006 / 2.7.6` | `2070007 / 2.7.7` | Assinatura Mobile/Connect compatível |
| TV legada | `com.guhflix.tv` | `2050000 / 2.5.0` | `2050001 / 2.5.1` | Certificado legado SHA-256 `0f1c1118520d99353f57e756c5801362d12ba10e36dcf31e4d7a93b8cc762810` |

A versão instalada em TVs específicas pode ser diferente. Antes de distribuir, verificar `versionCode`, `applicationId`, assinatura real instalada e compatibilidade com o pacote de destino. **Nunca instalar por cima APK com assinatura distinta**, nem pedir para o cliente desinstalar o aplicativo para forçar a migração.

## Bloqueios identificados nesta preparação

- O repositório contém o código-fonte e um mecanismo OTA, mas **não há APK novo assinado** nesta sessão.
- Não há `.jks`/ `.keystore` de assinatura disponível no ambiente desta execução.
- Não foi possível confirmar acesso ao servidor atual de downloads `guhflix-tv-download.onrender.com`, nem editar seu conteúdo.
- Não há pipeline CI de Android configurado no repositório para produzir automaticamente o release com a assinatura original.
- Não publicar JSON de release com números novos antes de verificar que os APKs corretos estão efetivamente disponíveis via HTTPS.

## Procedimento para liberar aos dispositivos

1. No ambiente de assinatura original, configurar `tv/keystore-connect.properties` e, se existir chave legada válida, `tv/keystore.properties`, **fora do Git**, com permissões restritas.
2. Executar a compilação da edição adequada, por exemplo: `cd tv && ./gradlew -PguhflixConnect=true :app:assembleRelease`. Para Android mobile, usar `-PguhflixAndroidMobile=true`. Para a edição legada, `./gradlew :app:assembleRelease`.
3. Validar os arquivos com `apksigner verify --verbose --print-certs` e `aapt dump badging`, comparando `packageName`, certificado, versionCode e assinatura instalada. Rodar testes JVM/Playwright e instalar sobre o APK anterior em dispositivo de homologação.
4. Calcular SHA-256 e tamanho exatos dos APKs realmente gerados. Publicar em URLs HTTPS estáveis e acessíveis.
5. Gerar separadamente `release-connect.json`, `release-android.json` e `release.json` com `tools/ota_manifest.py`, contendo `package`, `versionCode`, `version`, `apkUrl`, `sha256`, `bytes`, `notes` e `mandatory=false`, evitando dados fictícios.
6. Confirmar HTTP 200, `Content-Length`, integridade, certificado e validade de cada manifesto; só então disponibilizar cada manifesto para a edição correspondente.
7. Abrir o aplicativo em uma TV de teste e usar **Verificar atualização**, aceitar a instalação do Android, confirmar que preservou login/dados e que o catálogo/botão Voltar aparecem e reproduzem mídia autorizada.
8. Após homologação, liberar as mesmas URLs/manifestos para as TVs clientes. O mecanismo OTA consulta em intervalos, exibe uma oferta e depende de confirmação do usuário Android para instalar. **Não é atualização forçada silenciosa.**
9. Se um cliente estiver em versão antiga com verificador OTA defeituoso, poderá ser necessária instalação de reparo por cima do APK anterior, mantendo a assinatura (ver docs/RELEASE-2.6.2-OTA.md).

## Não executar nesta etapa
- Não criar outra aplicação ou serviço Render.
- Não alterar cadastros, pagamentos, assinaturas ou banco MongoDB.
- Não mexer em manifestos de produção, chaves, APKs já publicados ou clientes antes de gerar e verificar os arquivos.
- Não substituir o arquivo de instalação por uma versão não assinada ou incompatível.
