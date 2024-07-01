plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("dagger.hilt.android.plugin")
    `maven-publish`
}

android {
    namespace = "foundation.algorand.auth"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    buildFeatures {
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://github.com/algorandfoundation/liquid-auth-android")
        }
    }
    publications {
        register<MavenPublication>("release") {
            groupId = "foundation.algorand"
            artifactId = "auth"
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
dependencies {
    // AlgoSDK
    implementation("com.algorand:algosdk:2.4.0")
    // FIDO2
    implementation("com.google.android.gms:play-services-fido:21.0.0")
    // Barcode Scanner
    implementation("com.google.mlkit:barcode-scanning-common:17.0.0")
    implementation("com.google.mlkit:camera:16.0.0-beta3")
    // Signaling Service
    implementation("io.socket:socket.io-client:2.1.0")
    implementation("org.webrtc:google-webrtc:1.0.32006")
    // QR Code Generator
    implementation("io.github.g0dkar:qrcode-kotlin:4.1.1")

    implementation("androidx.core:core-ktx:1.12.0")

    val coroutineVersion by extra { "1.7.1" }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutineVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:$coroutineVersion")

    val hiltVersion by extra { "2.48" }
    implementation("com.google.dagger:hilt-android:$hiltVersion")
    kapt("com.google.dagger:hilt-compiler:$hiltVersion")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    val okhttpVersion by extra { "4.12.0" }
    implementation("com.squareup.okhttp3:okhttp:$okhttpVersion")
    implementation("ru.gildor.coroutines:kotlin-coroutines-okhttp:1.0")

    // Dev Dependencies
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.9.21")
    androidTestImplementation("org.webrtc:google-webrtc:1.0.32006")
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("androidx.test:monitor:1.6.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutineVersion")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
