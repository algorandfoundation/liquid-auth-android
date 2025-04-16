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
    implementation(project(mapOf("path" to ":liquid")))
    // Android Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecyle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // CredentialProviderService
    implementation(libs.androidx.credential.manager)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    annotationProcessor(libs.androidx.room.compiler)
    implementation(libs.androidx.biometrics)

    // Algorand Foundation Integration
//    implementation(files("libs/dP256.jar"))
    implementation(libs.jna)
    implementation(libs.xhdwalletapi.android)
    implementation(libs.kotlin.bip39)

    // HTTP/Webrtc
    implementation(libs.okhttp)
    implementation(libs.stream.webrtc.android)

    // Test Resources
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}