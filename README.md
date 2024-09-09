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

1. Download the [latest release](https://github.com/algorandfoundation/liquid-auth-android/releases) on an Android device
2. You may need to [enable permissions](https://www.androidauthority.com/how-to-install-apks-31494/) to install the app


### Liquid Auth Service

[Start a demo liquid-auth](https://liquidauth.com/server/running-locally/) application or navigate to a [liquid-auth enabled service](https://liquidauth.com/#get-connected) in your browser to test the FIDO2 feature.

# Building

Clone the repository and open in Android Studio
```bash
git clone git@github.com:algorandfoundation/liquid-auth-android.git
```

Connect a device and run the `demo` target on a device, it is recommended to use a physical device for testing.

Make sure to also [start the liquid-auth service](https://liquidauth.com/server/running-locally/) on your local machine or a remote server.

# Integration Guide

See the [integration guide](https://liquidauth.com/clients/android/introduction/) for more information on how to integrate the liquid-auth service into your application.
