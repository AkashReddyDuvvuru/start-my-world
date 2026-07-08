# StealthStream - Production-Ready Encrypted Camera Stream

## Overview

StealthStream is a production-grade Android application that securely captures camera frames in the foreground and streams them over encrypted UDP channels. This implementation follows enterprise architecture patterns, OWASP Mobile Top 10 guidelines, and Android 14+ security requirements.

## Architecture

- **Multi-module design** with feature isolation
- **Clean Architecture** principles
- **Dependency Injection** via Hilt
- **Repository pattern** for data access
- **XChaCha20-Poly1305** AEAD encryption
- **Android Foreground Services** with proper permissions

## Security Features

- Runtime permission handling (Android 14+)
- Secure memory management (libsodium)
- Nonce handling and AAD for replay prevention
- Hardened native code with address sanitizers
- Audit logging for all critical operations
- No hardcoded secrets

## Project Structure

```
stealthstream/
├── app/                           # Main Android app module
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   ├── java/com/stealthstream/
│   │   │   │   ├── StealthStreamApplication.kt
│   │   │   │   ├── di/
│   │   │   │   ├── ui/
│   │   │   │   ├── service/
│   │   │   │   ├── receiver/
│   │   │   │   └── util/
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   ├── drawable/
│   │   │   │   └── navigation/
│   │   │   └── cpp/
│   │   │       └── native/
│   │   └── test/
│   └── build.gradle.kts
├── buildSrc/                      # Build logic and versions
├── gradle/
├── settings.gradle.kts
└── build.gradle.kts
```

## Build Requirements

- Android SDK 34 (Android 14)
- Android NDK r26 or higher
- libsodium (pre-built for all ABIs)
- Kotlin 1.9+
- Gradle 8.0+

## Development

### Build

```bash
./gradlew build
./gradlew buildRelease
```

### Test

```bash
./gradlew test
./gradlew connectedAndroidTest
```

### Deploy

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Documentation

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) - System design
- [SECURITY.md](docs/SECURITY.md) - Security hardening
- [API.md](docs/API.md) - Native interface specifications
- [TESTING.md](docs/TESTING.md) - Test strategy

## Compliance

- GDPR-compliant with explicit user consent
- No covert recording features
- Clear notification while streaming
- Transparent permission requests
- Data deletion capabilities

## License

Proprietary - All rights reserved
