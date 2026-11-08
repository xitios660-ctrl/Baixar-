# GUHFLIX TV 2.5.0

Código Android nativo adaptado do projeto autorizado Saímo TV Android, base a0f080c6eeadca36f503964884c8ecbf088bab82. Identidade GUHFLIX recuperada do APK 2.4.2, conexões autenticadas e OTA. Consulte os créditos e a licença original.

## Compilar
JDK 21, Android SDK 35, Gradle wrapper 8.11.1. Configure local.properties com sdk.dir.

    ./gradlew :app:assembleDebug
    ./gradlew :app:testDebugUnitTest --tests 'com.guhflix.shell.*'

Debug usa chave de teste e não deve ser distribuído como atualização.

## Publicação
Configure keystore.properties local, ignorado pelo Git, com storeFile, storePassword, keyAlias e keyPassword. Nunca versione a chave ou as senhas.

O certificado SHA-256 precisa ser 0f1c1118520d99353f57e756c5801362d12ba10e36dcf31e4d7a93b8cc762810, igual ao APK GUHFLIX TV 2.4.2. Valide com apksigner antes de publicar. Release recusa ausência da chave.

    ./gradlew :app:assembleRelease
    apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk

Pacote com.guhflix.tv, versionCode 2050000, versão 2.5.0. Sem chave correspondente não há garantia de atualização por cima.

## Edição Connect independente

A chave recebida em 2026-11-08 é válida, porém não é a chave da 2.4.2. A edição Connect usa com.guhflix.tv.connect, nome GUHFLIX Connect, links/provedores próprios e manifesto release-connect.json. Consulte docs/connect/CONNECT-2.5.1.md. A assinatura é configurada em keystore-connect.properties, nunca no Git. Compile com -PguhflixConnect=true. Sem essa propriedade, o projeto continua exigindo a assinatura antiga para o pacote original.
