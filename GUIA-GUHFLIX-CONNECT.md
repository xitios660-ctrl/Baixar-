# GUHFLIX Connect TV 2.5.1

Pacote: com.guhflix.tv.connect. VersionCode: 2050001. App Android TV nativo, independente do GUHFLIX 2.4.2 (com.guhflix.tv), com o mesmo servidor e catálogo autorizado. O novo APK usa a chave privada enviada pelo usuário em 2026-11-08. Seu certificado é fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b e não coincide com o certificado da 2.4.2. Por isso esta edição instala ao lado, com autoridades e links próprios; não substitui a antiga nem transfere seus dados automaticamente.

## Instalar e conectar

1. Na TV, abra https://guhflix-tv-download.onrender.com/connect.html pelo Downloader e baixe GUHFLIX-Connect-TV-2.5.1.apk.
2. Autorize a instalação para o aplicativo Downloader, se o Android pedir. Instale e abra GUHFLIX Connect. Não é necessário remover o GUHFLIX antigo.
3. Entre com seu usuário e senha de cliente GUHFLIX. A conta precisa estar ativa.
4. No iPhone, abra https://guhflix-marley-api.onrender.com/pwa/ e use essa mesma conta.
5. Na TV: Conta / iPhone → Gerar código de vínculo. No site: Conectar TV → digite o código. Escolha a TV que deve receber a reprodução.
6. O site consulta o estado enviado pela TV e permite escolher filme/episódio, pausar, retomar, avançar, retroceder e parar. A TV precisa estar com o GUHFLIX Connect aberto e conectada à internet.

## Atualizações desta edição

A chave está fora do Git, separada em /workspace/private/guhflix/uploaded-signing. O arquivo original ZIP fornecido pelo usuário é o backup privado e não deve ser publicado no site de downloads. Nunca mude a chave desta edição. Configure localmente tv/keystore-connect.properties (ignorado pelo Git) e compile:

    cd tv
    ./gradlew -PguhflixConnect=true :app:assembleRelease

O projeto inclui apenas instruções e o certificado público; a chave e suas credenciais não são distribuídas com o código. Guarde o ZIP privado recebido separadamente.

O build normal, sem essa propriedade, continua sendo a edição original com.guhflix.tv e exige a chave antiga. Não use a chave nova para assinar esse pacote original. O manifesto Connect é https://guhflix-tv-download.onrender.com/release-connect.json. O manifesto antigo release.json permanece separado.

Para nova publicação: aumente versionCode/versionName da edição Connect, valide assinatura e gere o manifesto com tools/ota_manifest.py --package-name com.guhflix.tv.connect --expected-signer fe13bf8634b76a4450b70833a7a6eb6aa01429729371a33dccc75c8eaa72b76b. Publique primeiro o APK versionado e confira seu hash; depois atualize release-connect.json. O Android sempre exige confirmação para instalar.

## Verificações e limites

A API e o site passaram pelos testes de isolamento de contas, seleção explícita de destino e troca de conta registrados em TESTES.md. O fluxo com TVs nesses testes usa simuladores da API. Compilação release, otimização R8 e assinatura verificadas. Os oito testes release da política OTA passaram. O verificador do APK conferiu pacote, versão, launcher, activities, provedores, links próprios, URL de atualização e certificado. Veja APK-2.5.1-verificado.json. Não há acesso à TCL real; reprodução, navegação e instalação real continuam pendentes de validação no aparelho. Não foi modificado o Windows.
