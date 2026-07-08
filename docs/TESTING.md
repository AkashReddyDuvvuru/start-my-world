# Testing Strategy

## Test Pyramid

```
      ┌─────┐
      │  E2E│  End-to-end (3-5 tests)
      ├─────┤
      │ Integ│  Integration (15-20 tests)
      ├─────┤
      │Unit │  Unit (50+ tests)
      └─────┘
```

## Unit Tests

### Crypto Layer
- ✓ Key generation (libsodium keygen)
- ✓ Nonce generation (random, non-repeating)
- ✓ Encryption/decryption round-trip
- ✓ Tag verification (altered ciphertext fails)
- ✓ AAD validation (sequence number)
- ✓ Key zeroization (memory cleared)

### Network Layer
- ✓ UDP packet construction
- ✓ Packet serialization
- ✓ Anti-replay check
- ✓ Sequence number monotonicity

### Permission Layer
- ✓ Permission check logic
- ✓ Permission request flow
- ✓ Permission state transitions

### Buffer Management
- ✓ Buffer allocation
- ✓ Buffer reuse
- ✓ Buffer overflow prevention
- ✓ Memory leaks (Valgrind)

## Integration Tests

### Service Lifecycle
- ✓ Service start (with notification)
- ✓ Service stop (key cleanup)
- ✓ Service crash recovery
- ✓ Multiple start attempts

### Camera Integration
- ✓ Camera open
- ✓ Frame capture
- ✓ ImageReader callback
- ✓ Camera release
- ✓ Permission denied (camera fails gracefully)

### Encoding Integration
- ✓ MediaCodec initialization
- ✓ NAL unit extraction
- ✓ Frame buffering
- ✓ Codec error handling

### Network Integration
- ✓ UDP socket creation
- ✓ Packet send
- ✓ Network error handling
- ✓ Connection lost scenarios

### Permission Integration
- ✓ Runtime permission request
- ✓ Permission grant/deny
- ✓ Service starts only with permission
- ✓ Service stops on permission revoked

## End-to-End Tests

### Real Device Tests (Android 14+ Device)
- ✓ Boot device, reboot receiver disabled
- ✓ Open app, tap "Start Streaming"
- ✓ Camera preview visible
- ✓ Notification shows (low importance)
- ✓ Frames sent to remote server
- ✓ Stop streaming
- ✓ Notification clears
- ✓ Service stops cleanly

### Network Tests
- ✓ Send 1000 frames, verify all arrive
- ✓ Capture with tcpdump, verify encrypted
- ✓ Modify packet, receiver rejects
- ✓ Replay packet, receiver detects duplicate

### Security Tests
- ✓ Attempt to access camera without permission (fails)
- ✓ Attempt to access keys via reflection (fails)
- ✓ Check no plaintext frames in memory
- ✓ Verify key zeroized after stop

## Performance Tests

### Metrics
- Frames per second (FPS)
- Bytes sent per second (throughput)
- CPU usage (should be <30%)
- Memory footprint (should be <100 MB)
- Battery drain over 1 hour

### Tools
- Android Studio Profiler
- adb shell top
- Perfetto / systrace

## Continuous Integration

### GitHub Actions
- Run on every PR
- Build debug APK
- Run Lint, SpotBugs
- Run unit tests
- Upload coverage to Codecov

### Deployment
- Tag releases
- Build release APK
- Sign with keystore
- Upload to Play Store (internal testing track)

## Test Files

```
app/src/test/
├── java/com/stealthstream/
│   ├── crypto/
│   │   ├── CryptoRepositoryTest.kt
│   │   ├── NativeEncryptionTest.kt
│   │   └── KeyManagementTest.kt
│   ├── network/
│   │   ├── PacketTest.kt
│   │   ├── UdpNetworkTest.kt
│   │   └── AntiReplayTest.kt
│   ├── permission/
│   │   └── PermissionHelperTest.kt
│   └── util/
│       └── BufferManagementTest.kt
│
app/src/androidTest/
├── java/com/stealthstream/
│   ├── service/
│   │   ├── StreamingServiceTest.kt
│   │   └── ServiceLifecycleTest.kt
│   ├── ui/
│   │   └── MainActivityTest.kt
│   └── integration/
│       └── EndToEndTest.kt
```
