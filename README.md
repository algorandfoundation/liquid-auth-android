# Liquid Auth Client

This is a client library for the Liquid Auth service. It provides a simple way to authenticate users and create peer connections

# Overview

This project has two main components:

1. **foundation.algorand.auth**: A client library for the liquid-auth service
2. **foundation.algorand.demo**: A demo application that uses the client library

# Quick Start

This app is a simple android application that demonstrates the use of the liquid-auth service. It allows users to authenticate with the service and create peer connections.

### Installation

#### Prebuild APK

1. Download the [latest release](https://github.com/algorandfoundation/liquid-auth-android/releases/download/v0.2.0/fido2-debug.apk) on an Android device
2. You may need to [enable permissions](https://www.androidauthority.com/how-to-install-apks-31494/) to install the app


### Liquid Auth Service

[Start a demo liquid-auth](https://github.com/algorandfoundation/liquid-auth?tab=readme-ov-file#getting-started) application or navigate to the [liquid-auth enabled service](https://liquid-auth.onrender.com) in your browser to test the FIDO2 feature.

## User Guide

### Example Dapp
![Step-1.png](.docs%2FStep-1.png)


### QR Connect

Open the Connect Modal on the website and scan the QR code using the "Connect" button on the Android device.
Follow the instructions on the Android device to register a credential.


![Step-1-QRCode.png](.docs%2FStep-1-QRCode.png)


### Peer to Peer

Once the credential is registered, you can send messages over the peer connection.

![Step-2.png](.docs%2FStep-2.png)


# Building

Clone the repository and open in Android Studio
```bash
git clone git@github.com:algorandfoundation/liquid-auth-android.git
```

Connect a device and run the `demo` target on a device, it is recommended to use a physical device for testing.

Make sure to also [start the liquid-auth service](https://github.com/algorandfoundation/liquid-auth?tab=readme-ov-file#getting-started) on your local machine or a remote server.

# Integration Guide

To integrate the library into your own project, you can build the library and include it in your project. This is useful while
the library is not yet published to a public repository.

Add `aar` files in your project:
```bash
cp ./liquid/build/outputs/*-release.aar <ANDROID_APP_LOCATION>/libs
```

Add jcenter() to your `settings.gradle.kts` file in the project root:
```kotlin
dependencyResolutionManagement {
    //...
    repositories {
        //...
        jcenter()
    }
}
```

Update the `build.gradle.kts` file in your app module to include the library and dependencies:
```kotlin
// Liquid Auth Library
api(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
// AlgoSDK
implementation(libs.algosdk)
// FIDO2
implementation(libs.play.services.fido)
// Barcode Scanner
implementation(libs.barcode.scanning.common)
implementation(libs.camera)
// Signaling Service
implementation(libs.socket.io.client)
implementation(libs.google.webrtc)
// QR Code Generator
implementation(libs.qrcode.kotlin)
// HTTP Client
implementation(libs.okhttp)
```

Basic Usage of creating the Offer client:

```kotlin
class OfferActivity : ComponentActivity() {
    private var signalClient: SignalClient? = null
    // Third Party APIs
    private var httpClient = OkHttpClient.Builder()
        .cookieJar(Cookies())
        .build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create Client
        signalClient = SignalClient("https://liquid-auth.onrender.com", this@OfferActivity, httpClient)
        lifecycleScope.launch {
            // Generate UUID
            val requestId = signalClient!!.generateRequestId()
            // Create Bitmap for QR Code
            val qrBitmap = signalClient!!.qrCode(requestId)

            // <ADD QR Code View Handling>

            // Wait for offer from peer
            val dc = signalClient?.peer(requestId, "offer")
            // Handle the data-channel
            signalClient!!.handleDataChannel(dc!!, {
                // On Message
                Toast.makeText(this@OfferActivity, it, Toast.LENGTH_SHORT).show()
            }, {
                // On State Change
                it?.let {
                    Toast.makeText(this@OfferActivity, "onStateChanged($it)", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}
```

See the [AnswerActivity](demo/src/main/java/foundation/algorand/demo/AnswerActivity.kt) in the demo app for an example of how to create a passkey and complete the handshake.
