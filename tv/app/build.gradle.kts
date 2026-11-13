import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/**
 * Chave de assinatura, fora do Git (ver keystore.properties e a pasta chaves).
 *
 * A atualização por cima só instala quando a assinatura é a mesma, então a
 * chave é a identidade do aplicativo: perdê-la obriga todo mundo a desinstalar
 * e instalar de novo.
 */
val mobileEdition = providers.gradleProperty("guhflixAndroidMobile").orNull == "true"
val connectEdition = mobileEdition || providers.gradleProperty("guhflixConnect").orNull == "true"
val chave = Properties().apply {
    val arquivo = rootProject.file(if (connectEdition) "keystore-connect.properties" else "keystore.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}

android {
    namespace = "br.com.saimo.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = if (mobileEdition) "com.guhflix.android" else if (connectEdition) "com.guhflix.tv.connect" else "com.guhflix.tv"
        // Android 6: o mínimo do Media3 e das bibliotecas androidx atuais.
        // Até a 2.0.2 era o 5 (21); quem ainda tem Android 5 fica na 2.0.2.
        minSdk = 23
        targetSdk = 35
        versionCode = if (connectEdition) 2070001 else 2050000
        versionName = if (connectEdition) "2.7.1" else "2.5.0"
        https://guhflix-tv-nova-conta-pau5pe.v2.appdeploy.ai
        val updateFile = if (mobileEdition) "release-android.json" else if (connectEdition) "release-connect.json" else "release.json"
        buildConfigField("String", "GUHFLIX_UPDATE_MANIFEST", "\"https://guhflix-tv-download.onrender.com/$updateFile\"")
        buildConfigField("String", "GUHFLIX_URI_SCHEME", if (mobileEdition) "\"guhflix-android\"" else if (connectEdition) "\"guhflix-connect\"" else "\"saimo\"")
        resValue("string", "guhflix_search_authority", if (mobileEdition) "com.guhflix.android.busca" else if (connectEdition) "com.guhflix.tv.connect.busca" else "com.guhflix.tv.busca")
        manifestPlaceholders["guhflixLeanbackRequired"] = if (mobileEdition) "false" else "true"
        manifestPlaceholders["guhflixAppLabel"] = if (mobileEdition) "Gfilmes Android" else if (connectEdition) "Gfilmes Connect" else "Gfilmes"
        manifestPlaceholders["guhflixUriScheme"] = if (mobileEdition) "guhflix-android" else if (connectEdition) "guhflix-connect" else "saimo"

    }

    signingConfigs {
        create("saimo") {
            val arquivo = chave.getProperty("storeFile")
            if (arquivo != null) {
                storeFile = file(arquivo)
                storePassword = chave.getProperty("storePassword")
                keyAlias = chave.getProperty("keyAlias")
                keyPassword = chave.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8: APK menor e código mais enxuto — abre mais rápido no TV Box.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("saimo")

        }
        debug {
            val testApi = providers.gradleProperty("guhflixTestApi").orNull
            if (testApi != null) {
                require(testApi.matches(Regex("http://(?:10\\.0\\.2\\.2|127\\.0\\.0\\.1):[0-9]+")))
                buildConfigField("String", "GUHFLIX_API", "\"$testApi\"")
            }
            // Igual à publicada: instalar o teste por cima da versão de
            // verdade, e vice-versa, sem "assinaturas não conferem".
            if (chave.getProperty("storeFile") != null) {
                signingConfig = signingConfigs.getByName("saimo")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        // A atualização compara a versão instalada com a do release.
        buildConfig = true
    }
    // O parser do guia é Kotlin puro, então roda em teste de JVM contra os
    // feeds de verdade — sem isso a única forma de validá-lo seria no aparelho.
    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    val media3 = "1.6.1"
    // HLS e DASH nativos: no Android o ExoPlayer decodifica HEVC em TS e faz
    // ClearKey sozinho, então nada de proxy nem ffmpeg como no macOS.
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")
    implementation("androidx.media3:media3-effect:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    // A sessão de mídia é o que o Assistant (Mi Box) e a Alexa (Fire TV) usam
    // para "próximo/anterior canal" e "abrir X" por voz enquanto o app está na
    // tela — sem ela o sistema não tem para quem mandar o comando de voz.
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    // Instala o perfil de compilação (src/main/baseline-prof.txt) também em
    // quem instala o APK direto, sem loja: é ele que compila o app na hora.
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")
    // "Continuar assistindo" na tela inicial do Google TV.
    implementation("androidx.tvprovider:tvprovider:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("io.coil-kt.coil3:coil:3.1.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.1.0")
    // OkHttp traz DNS-over-HTTPS pronto, que é o que contorna resolvedor
    // filtrado sem eu ter de falar DNS na mão como no macOS.
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("com.android.tools.build:apksig:8.9.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20180813")
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

configurations.all { resolutionStrategy.force("androidx.core:core:1.15.0", "androidx.core:core-ktx:1.15.0") }

tasks.matching { it.name == "packageRelease" || it.name == "validateSigningRelease" }.configureEach {
    doFirst { require(chave.getProperty("storeFile") != null) { "Release exige o arquivo de assinatura da edição selecionada; assinatura de depuração proibida." } }
}
