plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    alias(libs.plugins.devtools.ksp)
    `maven-publish`
}

android {
    namespace = "foundation.algorand.auth"
    compileSdk = 35

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
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
    implementation(libs.algosdk)
    // FIDO2
    implementation(libs.play.services.fido)
    // Barcode Scanner
    implementation(libs.barcode.scanning.common)
    implementation(libs.camera)
    // Signaling Service
    implementation(libs.socket.io.client)
    implementation(libs.stream.webrtc.android)
    // QR Code Generator
    implementation(libs.qrcode.kotlin)

    implementation(libs.androidx.core.ktx)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.okhttp)
    implementation(libs.kotlin.coroutines.okhttp)

    // UUID Generator
    implementation(libs.java.uuid.generator)

    // Dev Dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
