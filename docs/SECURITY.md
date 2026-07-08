# Security Hardening & Compliance

## Executive Summary

StealthStream implements defense-in-depth across all layers:
- **Cryptography**: XChaCha20-Poly1305 AEAD
- **Memory Safety**: libsodium secure allocation
- **Runtime Safety**: Android 14+ permissions & SELinux
- **Transport**: Encrypted-only UDP
- **Audit**: Comprehensive logging without sensitive data leaks

## Threat Model

### Adversaries

1. **Local Malware**: Other apps attempting to exfiltrate keys or video
2. **Network Eavesdropper**: Passive Wi-Fi/cellular snooping
3. **Network Active Attacker**: Packet injection/replay
4. **Device Compromise**: Rooted device or kernel exploits
5. **Legal/Regulatory**: Law enforcement, privacy regulation

### Attack Vectors & Mitigations

| Attack Vector | Threat | Mitigation |
|---|---|---|
| Key Theft | Rooted device reads memory | Secure allocation (sodium_malloc), mlock, frequent rotation |
| Logic Bypass | Modify APK to disable permissions | Code signing, runtime integrity checks |
| Permission Elevation | Exported components abused | exported=false, restricted intent filters |
| Denial of Service | Crash via malformed data | Input validation, exception handling, rate limiting |
| Replay Attacks | Duplicate frames injected | Frame sequence numbers in AAD |
| Side Channels | Timing attacks on crypto | Use libsodium (constant-time ops), no key logging |
| Covert Recording | Legal/ethical violations | Explicit consent UI, visible notification, auto-stop |

## Implementation Details

### 1. Cryptography

#### Key Generation
```cpp
unsigned char* key = (unsigned char*)sodium_malloc(crypto_aead_xchacha20poly1305_IETF_KEYBYTES);
sodium_mlock(key, crypto_aead_xchacha20poly1305_IETF_KEYBYTES);
crypto_aead_xchacha20poly1305_ietf_keygen(key);
```

**Rationale**: 
- `sodium_malloc()` uses `madvise(MADV_DONTDUMP)` to prevent core dumps
- `sodium_mlock()` pins memory, prevents swap-to-disk
- `keygen()` uses system CSPRNG

#### Nonce Management
```cpp
unsigned char nonce[crypto_aead_xchacha20poly1305_IETF_NPUBBYTES];
randombytes_buf(nonce, sizeof(nonce));
// Send nonce with ciphertext; never reuse for same key
```

**Rationale**:
- Fresh random nonce per frame prevents nonce reuse
- Nonce sent in plaintext (40 bytes overhead acceptable)
- Receiver verifies nonce is never repeated (optional)

#### Encryption
```cpp
unsigned long long clen;
unsigned char ad[16]; // frame_seq(8) + flags(8)
memcpy(ad, &frame_seq, 8);

int res = crypto_aead_xchacha20poly1305_ietf_encrypt(
    ciphertext, &clen,
    plaintext, plaintext_len,
    ad, 16,
    NULL,
    nonce,
    key
);
```

**Rationale**:
- AAD includes frame sequence number to prevent replay
- Tag (16 bytes) authenticates both data and metadata
- Ciphertext is deterministic under (key, nonce, plaintext)

#### Key Zeroization
```cpp
void NativeBridge::zeroizeKeyMemory(jlong keyPtr) {
    unsigned char* key = reinterpret_cast<unsigned char*>(keyPtr);
    sodium_memzero(key, crypto_aead_xchacha20poly1305_IETF_KEYBYTES);
    sodium_free(key); // Also zeroes internally
}
```

**Rationale**:
- `sodium_memzero()` uses volatile write, not optimized away
- `sodium_free()` additionally clears and deallocates
- Called immediately before service shutdown

### 2. Android Permissions (API 34+)

#### Manifest Declarations
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<service
    android:name=".service.StreamingService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="camera|dataSync" />

<receiver
    android:name=".receiver.BootReceiver"
    android:enabled="true"
    android:exported="true"
    android:directBootAware="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
    </intent-filter>
</receiver>
```

#### Runtime Permission Flow
```kotlin
// 1. Check if permission granted
if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
    != PackageManager.PERMISSION_GRANTED) {
    // 2. Request permission
    ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), CAMERA_PERM_REQUEST)
} else {
    // 3. Permission already granted, proceed
    startStreaming()
}

// 4. Handle result
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == CAMERA_PERM_REQUEST) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startStreaming()
        } else {
            showError("Camera permission denied")
        }
    }
}
```

#### Foreground Service Start
```kotlin
val intent = Intent(context, StreamingService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    ServiceCompat.startForeground(
        context,
        SERVICE_NOTIFICATION_ID,
        notification,
        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
    )
}
```

**Rationale**:
- `FOREGROUND_SERVICE_CAMERA` required on Android 14+
- Mismatch throws `SecurityException`
- Must call within 5 seconds of `startService()`

### 3. Memory Safety

#### JNI Buffer Handling
```cpp
jbyteArray NativeBridge::encryptFrame(
    JNIEnv* env,
    jobject obj,
    jbyteArray plaintext,
    jlong keyPtr,
    jlong noncePtr)
{
    jbyte* pt = env->GetByteArrayElements(plaintext, nullptr);
    if (pt == nullptr) return nullptr; // OOM
    
    jsize pt_len = env->GetArrayLength(plaintext);
    if (pt_len > MAX_FRAME_SIZE) {
        env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
        return nullptr; // Size validation
    }
    
    // Process...
    
    env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
    return ciphertext_array; // Return must be new array
}
```

**Rationale**:
- `GetByteArrayElements` may pin or copy; check for nullptr
- Always release before returning
- `JNI_ABORT` discards changes, doesn't copy back
- Allocate output as new array (avoids overwrites)

#### Frame Buffer Reuse
```cpp
class EncryptionBuffer {
    std::vector<uint8_t> buffer;
    std::mutex mutex;
    
public:
    void ensureCapacity(size_t size) {
        std::lock_guard<std::mutex> lock(mutex);
        if (buffer.capacity() < size) {
            buffer.clear();
            buffer.reserve(size);
        }
    }
    
    void secure_clear() {
        std::lock_guard<std::mutex> lock(mutex);
        sodium_memzero(buffer.data(), buffer.size());
        buffer.clear();
    }
};
```

**Rationale**:
- Reuse buffers to reduce GC pressure
- Thread-safe with mutex
- Secure erase before reuse

### 4. Network Security

#### UDP Packet Structure
```
Packet Format:
┌────────────────────────────────────────────────┐
│ Frame Sequence (8 bytes, big-endian)           │ <- AAD (authenticated)
├────────────────────────────────────────────────┤
│ Nonce (24 bytes, random)                       │ <- Sent in plaintext
├────────────────────────────────────────────────┤
│ Ciphertext (variable, ≤ 1200 - 24 = 1176 B)  │ <- Encrypted H.265 NAL
├────────────────────────────────────────────────┤
│ Authentication Tag (16 bytes)                  │ <- MAC
└────────────────────────────────────────────────┘
Total ≤ 1216 bytes (fits in UDP MTU)
```

#### Anti-Replay
```kotlin
class PacketValidator {
    private var lastSeqNum = -1L
    private val lock = Mutex()
    
    suspend fun validateSequence(seqNum: Long): Boolean = lock.withLock {
        if (seqNum <= lastSeqNum) return@withLock false
        lastSeqNum = seqNum
        return@withLock true
    }
}
```

### 5. Audit Logging

#### Events Logged
- Service start/stop (with timestamp)
- Permission request/grant/deny
- Frame capture rate
- Encryption errors
- Network send failures
- Service crash/restart

#### Non-Logged (Sensitive Data)
- Frame contents
- Encryption keys
- Nonces
- User data in AAD

#### Implementation
```kotlin
class AuditLogger(private val context: Context) {
    fun logServiceStart(reason: String) {
        val event = AuditEvent(
            timestamp = System.currentTimeMillis(),
            event = "SERVICE_START",
            reason = reason,
            userId = getCurrentUserId()
        )
        writeAuditLog(event)
    }
    
    private fun writeAuditLog(event: AuditEvent) {
        // Write to local encrypted database or Firebase
        // Never include sensitive data
    }
}
```

## Compliance

### GDPR
- **Lawful Basis**: Explicit user consent (shown in UI)
- **Purpose Limitation**: Camera used only for streaming
- **Data Subject Rights**: User can stop/delete anytime
- **Privacy Policy**: Link in app manifest

### CCPA
- **Consumer Rights**: Opt-out via UI toggle
- **Data Deletion**: Ensure no local storage
- **Transparency**: Clear disclosure

### Android Policy
- No covert recording ("Stealth" is misleading)
- Visible notification while active
- Clear permission requests
- Complies with Google Play policies

## Testing Strategy

See [TESTING.md](TESTING.md) for comprehensive test plans.

## References

- [libsodium Docs](https://doc.libsodium.org/)
- [OWASP Mobile Top 10](https://owasp.org/www-project-mobile-top-10/)
- [Android Security Docs](https://developer.android.com/training/articles/security-overview)
- [RFC 7748 - ECC](https://tools.ietf.org/html/rfc7748)
