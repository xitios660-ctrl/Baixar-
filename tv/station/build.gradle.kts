plugins { id("com.android.application") }
android {
    namespace = "com.guhflix.station"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.guhflix.station"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-dev"
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main").java.srcDir("../../licensing/station-core/src")
}
// Intentional: debug-only proof of signing key storage. No production release
// signing configuration and no unattended remote issuance endpoint.
