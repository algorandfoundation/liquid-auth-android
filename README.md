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

[Start a demo liquid-auth](https://github.com/algorandfoundation/liquid-auth?tab=readme-ov-file#getting-started) application or navigate to the liquid-auth enabled service in your browser to test the FIDO2 feature.

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

Connect a device and run the `app` target on a device, it is recommended to use a physical device for testing.

Make sure to also [start the liquid-auth service](https://github.com/algorandfoundation/liquid-auth?tab=readme-ov-file#getting-started) on your local machine or a remote server.
