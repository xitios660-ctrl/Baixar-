import java.util.Properties
plugins {id("com.android.application");id("org.jetbrains.kotlin.android")}
val signing=Properties().apply{val f=rootProject.file("keystore-connect.properties");if(f.exists())f.inputStream().use{load(it)}}
android {
 namespace="com.gfilmes.server";compileSdk=35
 defaultConfig {applicationId="com.gfilmes.activationserver";minSdk=26;targetSdk=35;versionCode=1000001;versionName="1.0.1"}
 signingConfigs {create("owner"){if(signing.getProperty("storeFile")!=null){storeFile=rootProject.file("app").resolve(signing.getProperty("storeFile"));storePassword=signing.getProperty("storePassword");keyAlias=signing.getProperty("keyAlias");keyPassword=signing.getProperty("keyPassword")}}}
 buildTypes {release{isMinifyEnabled=true;signingConfig=signingConfigs.getByName("owner");proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),"proguard-rules.pro")}}
 sourceSets["main"].java.srcDir("../license-core/src/main/kotlin")
 sourceSets["test"].java.srcDir("../license-core/src/test/kotlin")
 compileOptions{sourceCompatibility=JavaVersion.VERSION_17;targetCompatibility=JavaVersion.VERSION_17}
 testOptions{unitTests.isReturnDefaultValues=true}
 buildFeatures{buildConfig=true}
}
kotlin{compilerOptions{jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)}}
dependencies {implementation("org.bouncycastle:bcprov-jdk18on:1.79");testImplementation("junit:junit:4.13.2");testImplementation("org.json:json:20180813")}
