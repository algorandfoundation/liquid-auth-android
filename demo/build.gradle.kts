val NODELY_TURN_USERNAME = "liquid-auth"
val NODELY_TURN_CREDENTIAL = "sqmcP4MiTKMT4TGEDSk9jgHY"
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.devtools.ksp)
}

android {
    namespace = "foundation.algorand.demo"
    compileSdk = 35
    extracted()
    defaultConfig {
        applicationId = "foundation.algorand.demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "NODELY_TURN_USERNAME", "\"$NODELY_TURN_USERNAME\"")
        buildConfigField("String", "NODELY_TURN_CREDENTIAL", "\"$NODELY_TURN_CREDENTIAL\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue(
            "string", "asset_statements", """
           [{
             "include": "https://nest-fido2.onrender.com/.well-known/assetlinks.json"
           }]
        """)
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("../debug.jks")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(mapOf("path" to ":liquid")))
    implementation(files("libs/provider-debug.aar"))
    implementation(files("libs/crypto-debug.aar"))
    implementation(libs.java.uuid.generator)


    implementation(libs.jackson.annotations)
    implementation(libs.jackson.dataformat.msgpack)
    implementation(libs.jackson.dataformat.cbor)
    implementation(libs.json.kotlin.schema)

    // Algorand SDK
    implementation(libs.algosdk)
    implementation(libs.bcprov.jdk15on)

    // FIDO2 - Deprecated
    implementation(libs.play.services.fido)

    // Barcode Reader
    implementation(libs.play.services.code.scanner)

    // Credentials
    // MUST BE PINNED!
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")

    // Deterministic Passkeys
    implementation(files("libs/dP256.jar"))
    implementation(libs.kotlin.bip39)

    // Kotlin Coroutine
    val coroutineVersion by extra { "1.7.1" }
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)


    implementation(libs.camera)
    implementation(libs.barcode.scanning.common)
    implementation(libs.androidx.biometric.ktx)

    // Dagger/Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)
    // Rooms
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    // HTTP Requests
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlin.coroutines.okhttp)
    implementation(libs.socket.io.client)
    implementation(libs.stream.webrtc.android)

    // Core
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Dev Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

fun Build_gradle.extracted() {
    configurations {
        all {
            exclude("org.bouncycastle", "bcprov-jdk15to18")
        }
    }
}
