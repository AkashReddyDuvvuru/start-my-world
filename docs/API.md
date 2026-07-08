# Native JNI API Specification

## Overview

StealthStream uses JNI to interface with libsodium for cryptographic operations. This document specifies the C++ interface, argument contracts, and error handling.

## JNI Bridge Header (NativeBridge.h)

```cpp
#ifndef STEALTHSTREAM_NATIVE_BRIDGE_H
#define STEALTHSTREAM_NATIVE_BRIDGE_H

#include <jni.h>
#include <sodium.h>

class NativeBridge {
public:
    // Initialization
    static jint initLibsodium();
    
    // Key Management
    static jlong generateKey();
    static void zeroizeKeyMemory(jlong keyPtr);
    
    // Encryption
    static jbyteArray encryptFrame(
        JNIEnv* env,
        jobject obj,
        jbyteArray plaintext,
        jlong keyPtr,
        jbyteArray nonce,
        jbyteArray aad);
    
    // Decryption (receiver side)
    static jbyteArray decryptFrame(
        JNIEnv* env,
        jobject obj,
        jbyteArray ciphertext,
        jlong keyPtr,
        jbyteArray nonce,
        jbyteArray aad,
        jbyteArray tag);
    
    // Nonce Generation
    static jbyteArray generateNonce();
    
    // Cleanup
    static void cleanup();
};

#endif // STEALTHSTREAM_NATIVE_BRIDGE_H
```

## JNI Method Specifications

### initLibsodium()

**Purpose**: Initialize libsodium library.

**Signature**: 
```java
private external fun initLibsodium(): Int
```

**Returns**:
- `0` on success
- `-1` on failure (libsodium already initialized or out of memory)

**Throws**: None

**Preconditions**: None

**Postconditions**: libsodium is ready for use

**Thread Safety**: Idempotent (safe to call multiple times)

**Implementation Note**:
```cpp
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    if (sodium_init() < 0) {
        return -1; // Failure
    }
    return JNI_VERSION_1_6;
}
```

### generateKey()

**Purpose**: Generate a secure 32-byte XChaCha20-Poly1305 key.

**Signature**:
```java
private external fun generateKey(): Long
```

**Returns**: Pointer to key buffer (as jlong) or 0 on failure

**Throws**: None

**Preconditions**: `initLibsodium()` already called

**Postconditions**: Key allocated in secure memory (mlocked)

**Memory Contract**: 
- Caller must call `zeroizeKeyMemory(keyPtr)` to free
- Key persists until explicitly cleared

**Implementation Note**:
```cpp
jlong Java_com_stealthstream_crypto_NativeBridge_generateKey(
    JNIEnv* env, jclass clazz) {
    unsigned char* key = (unsigned char*)sodium_malloc(
        crypto_aead_xchacha20poly1305_IETF_KEYBYTES);
    if (key == nullptr) return 0;
    
    sodium_mlock(key, crypto_aead_xchacha20poly1305_IETF_KEYBYTES);
    crypto_aead_xchacha20poly1305_ietf_keygen(key);
    
    return reinterpret_cast<jlong>(key);
}
```

### zeroizeKeyMemory(keyPtr)

**Purpose**: Securely erase and free a key.

**Signature**:
```java
private external fun zeroizeKeyMemory(keyPtr: Long)
```

**Parameters**:
- `keyPtr`: Pointer to key (returned from `generateKey()`)

**Returns**: void

**Throws**: None

**Preconditions**: `keyPtr` is valid (from `generateKey()`)

**Postconditions**: Key is zeroed and freed; pointer is invalid

**Implementation Note**:
```cpp
void Java_com_stealthstream_crypto_NativeBridge_zeroizeKeyMemory(
    JNIEnv* env, jclass clazz, jlong keyPtr) {
    if (keyPtr == 0) return;
    
    unsigned char* key = reinterpret_cast<unsigned char*>(keyPtr);
    sodium_memzero(key, crypto_aead_xchacha20poly1305_IETF_KEYBYTES);
    sodium_free(key);
}
```

### encryptFrame(plaintext, keyPtr, nonce, aad)

**Purpose**: Encrypt a camera frame using XChaCha20-Poly1305.

**Signature**:
```java
private external fun encryptFrame(
    plaintext: ByteArray,
    keyPtr: Long,
    nonce: ByteArray,
    aad: ByteArray
): ByteArray?
```

**Parameters**:
- `plaintext`: H.265 NAL unit (variable length, ≤ 1 MB)
- `keyPtr`: From `generateKey()`
- `nonce`: 24 bytes, random (from `generateNonce()`)
- `aad`: Additional authenticated data (e.g., frame sequence number, 8 bytes)

**Returns**: 
- ByteArray containing `[ciphertext | tag]` (plaintext_len + 16 bytes)
- null on error

**Throws**: None

**Preconditions**:
- `keyPtr` valid
- `nonce.length == 24`
- `plaintext.length > 0 && plaintext.length <= 1048576`
- `aad.length > 0`

**Postconditions**: Frame encrypted; output length = plaintext.length + 16

**Error Handling**:
- Invalid keyPtr → return null
- Invalid nonce length → return null
- Plaintext too large → return null
- OOM → return null

**Implementation Note**:
```cpp
jbyteArray Java_com_stealthstream_crypto_NativeBridge_encryptFrame(
    JNIEnv* env, jclass clazz,
    jbyteArray plaintext, jlong keyPtr,
    jbyteArray nonce, jbyteArray aad) {
    
    // Validate inputs
    if (keyPtr == 0) return nullptr;
    
    jbyte* pt = env->GetByteArrayElements(plaintext, nullptr);
    if (pt == nullptr) return nullptr;
    jsize pt_len = env->GetArrayLength(plaintext);
    if (pt_len <= 0 || pt_len > 1048576) {
        env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
        return nullptr;
    }
    
    jbyte* nonce_buf = env->GetByteArrayElements(nonce, nullptr);
    if (nonce_buf == nullptr || env->GetArrayLength(nonce) != 24) {
        env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
        return nullptr;
    }
    
    jbyte* aad_buf = env->GetByteArrayElements(aad, nullptr);
    if (aad_buf == nullptr) {
        env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
        env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
        return nullptr;
    }
    jsize aad_len = env->GetArrayLength(aad);
    
    // Allocate output
    unsigned long long clen = pt_len + crypto_aead_xchacha20poly1305_IETF_ABYTES;
    unsigned char* ciphertext = new unsigned char[clen];
    if (ciphertext == nullptr) {
        env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
        env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
        env->ReleaseByteArrayElements(aad, aad_buf, JNI_ABORT);
        return nullptr;
    }
    
    // Encrypt
    unsigned char* key = reinterpret_cast<unsigned char*>(keyPtr);
    int res = crypto_aead_xchacha20poly1305_ietf_encrypt(
        ciphertext, &clen,
        (const unsigned char*)pt, pt_len,
        (const unsigned char*)aad_buf, aad_len,
        nullptr, // nsec (null)
        (const unsigned char*)nonce_buf,
        key
    );
    
    // Release input
    env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
    env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
    env->ReleaseByteArrayElements(aad, aad_buf, JNI_ABORT);
    
    // Return result
    if (res != 0) {
        delete[] ciphertext;
        return nullptr;
    }
    
    jbyteArray result = env->NewByteArray(clen);
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, clen, (const jbyte*)ciphertext);
    }
    delete[] ciphertext;
    
    return result;
}
```

### decryptFrame(ciphertext, keyPtr, nonce, aad, tag)

**Purpose**: Decrypt and verify a frame (for receiving side).

**Signature**:
```java
private external fun decryptFrame(
    ciphertext: ByteArray,
    keyPtr: Long,
    nonce: ByteArray,
    aad: ByteArray,
    tag: ByteArray
): ByteArray?
```

**Returns**: Plaintext on success, null if authentication fails

**Preconditions**:
- `tag.length == 16`
- `nonce.length == 24`
- `ciphertext` excludes tag (so total msg = ciphertext.length + tag.length)

### generateNonce()

**Purpose**: Generate a random 24-byte nonce.

**Signature**:
```java
private external fun generateNonce(): ByteArray
```

**Returns**: 24-byte array with random data

**Implementation**:
```cpp
jbyteArray Java_com_stealthstream_crypto_NativeBridge_generateNonce(
    JNIEnv* env, jclass clazz) {
    unsigned char nonce[crypto_aead_xchacha20poly1305_IETF_NPUBBYTES];
    randombytes_buf(nonce, sizeof(nonce));
    
    jbyteArray result = env->NewByteArray(sizeof(nonce));
    if (result != nullptr) {
        env->SetByteArrayRegion(result, 0, sizeof(nonce), (const jbyte*)nonce);
    }
    
    return result;
}
```

### cleanup()

**Purpose**: Finalize libsodium (optional, called at app shutdown).

**Signature**:
```java
private external fun cleanup()
```

## Error Codes & Handling

| Code | Meaning |
|---|---------|
| -1 | Generic failure (invalid input, OOM) |
| -2 | Cryptographic operation failed |
| -3 | Authentication tag verification failed |
| -4 | Memory allocation failed |

## Thread Safety

- All functions are thread-safe (no global state)
- Key pointers must not be accessed concurrently
- Use mutexes to protect key access

## Performance Characteristics

- `generateKey()`: ~1-2 ms
- `generateNonce()`: <1 ms
- `encryptFrame()`: ~5-10 ms for 1 MB frame (depends on CPU)
- `decryptFrame()`: ~5-10 ms
- Memory: ~1 MB for key + temp buffers
