val TURN_USERNAME: String = "fc7708976bf5d60be20c5a1d"
val TURN_CREDENTIAL: String = "sVpEREQGGhXOw4gX"

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "foundation.algorand.demo"
    compileSdk = 34
    extracted()
    defaultConfig {
        applicationId = "foundation.algorand.demo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "TURN_USERNAME", "\"$TURN_USERNAME\"")
        buildConfigField("String", "TURN_CREDENTIAL", "\"$TURN_CREDENTIAL\"")

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(mapOf("path" to ":liquid")))

    // Algorand SDK
    implementation("com.algorand:algosdk:2.4.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.67")

    // FIDO2 - Deprecated
    implementation("com.google.android.gms:play-services-fido:20.1.0")

    // Barcode Reader
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Credentials
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")

    // Kotlin Coroutine
    val coroutineVersion by extra { "1.7.1" }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutineVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    // Dagger/Hilt
    val hiltVersion by extra { "2.48" }
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    implementation("com.google.mlkit:camera:16.0.0-beta3")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("com.google.mlkit:barcode-scanning-common:17.0.0")
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    kapt("androidx.hilt:hilt-compiler:1.2.0")
    // Rooms
    val room_version = "2.6.1"
    implementation("androidx.room:room-ktx:$room_version")
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    // HTTP Requests
    val okhttpVersion by extra { "4.12.0" }
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttpVersion")
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("org.webrtc:google-webrtc:1.0.32006")

    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.7.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Dev Dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

fun Build_gradle.extracted() {
    configurations {
        all {
            exclude("org.bouncycastle", "bcprov-jdk15to18")
        }
    }
}
