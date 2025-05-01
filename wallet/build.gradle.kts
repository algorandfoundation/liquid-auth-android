val NODELY_TURN_USERNAME = "liquid-auth"
val NODELY_TURN_CREDENTIAL = "sqmcP4MiTKMT4TGEDSk9jgHY"
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.devtools.ksp)
}

android {
    namespace = "co.algorand.liquid.wallet"
    compileSdk = 35
    configurations {
        all {
            exclude("org.bouncycastle", "bcprov-jdk15to18")
        }
    }
    defaultConfig {
        applicationId = "co.algorand.liquid.wallet"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "NODELY_TURN_USERNAME", "\"$NODELY_TURN_USERNAME\"")
        buildConfigField("String", "NODELY_TURN_CREDENTIAL", "\"$NODELY_TURN_CREDENTIAL\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Algorand Foundation Integration
    implementation(files("libs/dP256.jar"))
    implementation(files("libs/provider-debug.aar"))
    implementation(files("libs/crypto-debug.aar"))
    implementation("net.java.dev.jna:jna:5.14.0@aar")
    implementation(libs.java.uuid.generator)
    implementation(libs.xhdwalletapi.android)
    implementation(libs.kotlin.bip39)
    implementation("org.bouncycastle:bcprov-jdk15on:1.61")
    implementation(libs.algosdk)

    // Liquid Auth
    implementation(project(mapOf("path" to ":liquid")))
    // HTTP/Webrtc
    implementation(libs.okhttp)
    implementation(libs.kotlin.coroutines.okhttp)
    implementation(libs.stream.webrtc.android)

    // Credentials and Scanner
    implementation("androidx.credentials:credentials:1.5.0")
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.androidx.credential.manager)
    implementation(libs.play.services.code.scanner)
    implementation(libs.androidx.biometrics)

    // Android Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecyle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    // UI
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Preferences
    implementation("androidx.datastore:datastore-preferences:1.1.4")
    implementation("androidx.datastore:datastore:1.1.4")

    // Database
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.navigation.compose)
    ksp(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler)

    // Test Resources
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}