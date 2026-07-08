# System Architecture

## High-Level Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Android Application                  │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   MainActivity │  │  StreamService │  │ BootReceiver  │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│               Business Logic Layer                      │
│  ┌──────────────────────────────────────────────────┐  │
│  │        StreamingCoordinator                      │  │
│  │  - Permission management                        │  │
│  │  - Service lifecycle                            │  │
│  │  - Error handling                               │  │
│  └──────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│               Repository Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Camera Repo  │  │ Network Repo │  │ Crypto Repo │  │
│  └──────────────┘  └──────────────┘  └─────────────┘  │
├─────────────────────────────────────────────────────────┤
│               Integration Layer                        │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │  Android API │  │   UDP Socket │  │ JNI Bridge  │  │
│  │  (Camera2)   │  │   (Network)  │  │  (Crypto)   │  │
│  └──────────────┘  └──────────────┘  └─────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   Native Layer                         │
│  ┌──────────────────────────────────────────────────┐  │
│  │     NativeBridge (C++)                          │  │
│  │  - XChaCha20-Poly1305 encryption               │  │
│  │  - Secure memory management                    │  │
│  │  - Nonce generation                            │  │
│  │  - libsodium integration                        │  │
│  └──────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
         │
         ├─ Camera Hardware
         ├─ Network (UDP/40001)
         └─ System Services (Permissions, Notifications)
```

## Core Modules

### 1. Presentation Layer (UI)
- `MainActivity` - Primary user interface
- Permission request flows
- Start/Stop streaming controls
- Status display (FPS, bytes sent, packet loss)

### 2. Service Layer
- `StreamingService` - Foreground service
- Lifecycle management
- Camera frame capture
- Encoding coordination

### 3. Domain Layer
- Use cases and business logic
- Domain models
- Port definitions (interfaces)

### 4. Data Layer
- Repositories for each domain
- Adapter implementations
- External service integration

### 5. Native Layer
- JNI bindings
- Cryptographic operations
- Secure memory handling

## Data Flow

```
User Action (Start Button)
        ↓
    MainActivity
        ↓
    Check CAMERA permission
        ↓
    StreamingCoordinator.startStreaming()
        ↓
    StreamingService.onStartCommand()
        ↓
    Display notification + request foreground
        ↓
    CameraRepository.openCamera()
        ↓
    ImageReader.setOnImageAvailableListener()
        ↓
    Frame Available Event
        ↓
    ┌─────────────────────────────┐
    │ Encode frame (MediaCodec)   │
    │ Get encoded NAL unit        │
    └─────────────────────────────┘
        ↓
    ┌─────────────────────────────┐
    │ Generate random nonce       │
    │ (via JNI randombytes_buf)   │
    └─────────────────────────────┘
        ↓
    ┌─────────────────────────────┐
    │ Encrypt (XChaCha20-Poly)    │
    │ (via JNI encryptFrame)      │
    └─────────────────────────────┘
        ↓
    ┌─────────────────────────────┐
    │ Construct UDP packet:       │
    │ [nonce(24B) | ciphertext]   │
    │ [frame_seq(8B) | nonce(24B) │
    │  | ciphertext | tag(16B)]   │
    └─────────────────────────────┘
        ↓
    NetworkRepository.sendUDP(packet)
        ↓
    UDP Socket.send(packet, addr:port)
        ↓
    Remote server receives & decrypts
```

## Concurrency & Threading

- **Main Thread**: UI, permission dialogs
- **Background Thread (Service)**: Camera capture loop
- **Encoding Thread**: MediaCodec callbacks
- **Network Thread**: UDP send operations
- **JNI**: Direct memory access (carefully synchronized)

## Error Handling Strategy

```
Exception occurs
        ↓
    Try to recover
        ↓
    Log (non-sensitive) to audit log
        ↓
    Notify user if critical
        ↓
    Stop service if unrecoverable
        ↓
    Clean up resources (close camera, socket, keys)
```

## Security Boundaries

1. **Application Boundary**: Permissions, SELinux, app sandboxing
2. **Encryption Boundary**: XChaCha20-Poly1305 over UDP
3. **Memory Boundary**: Secure allocation, zeroization on exit
4. **Network Boundary**: UDP encryption, no plaintext transmission

## Dependency Graph

```
AndroidManifest.xml
    ↓
StealthStreamApplication (Hilt setup)
    ├─ Di Module (provides repositories, use cases)
    ├─ MainActivity
    │   ├─ StreamingCoordinator
    │   └─ PermissionHelper
    │
    └─ StreamingService
        ├─ StreamingCoordinator
        ├─ CameraRepository
        ├─ NetworkRepository
        ├─ CryptoRepository
        └─ AuditLogger

Native Layer
    ├─ NativeBridge.h (JNI declarations)
    ├─ NativeBridge.cpp (implementations)
    └─ CMakeLists.txt (build)
```
