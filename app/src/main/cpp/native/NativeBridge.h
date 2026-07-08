#ifndef STEALTHSTREAM_NATIVE_BRIDGE_H
#define STEALTHSTREAM_NATIVE_BRIDGE_H

#include <jni.h>
#include <sodium.h>
#include <cstddef>
#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Initialize libsodium.
 * Safe to call multiple times (idempotent).
 * @return 0 on success, -1 on failure
 */
jint Java_com_stealthstream_crypto_NativeBridge_initLibsodium(
    JNIEnv* env,
    jclass clazz);

/**
 * Generate a secure XChaCha20-Poly1305 key.
 * @return Pointer to key (as jlong) or 0 on failure
 */
jlong Java_com_stealthstream_crypto_NativeBridge_generateKey(
    JNIEnv* env,
    jclass clazz);

/**
 * Securely erase and free a key.
 * @param keyPtr Pointer to key from generateKey()
 */
void Java_com_stealthstream_crypto_NativeBridge_zeroizeKeyMemory(
    JNIEnv* env,
    jclass clazz,
    jlong keyPtr);

/**
 * Generate a random 24-byte nonce.
 * @return ByteArray with nonce, or null on failure
 */
jbyteArray Java_com_stealthstream_crypto_NativeBridge_generateNonce(
    JNIEnv* env,
    jclass clazz);

/**
 * Encrypt a frame using XChaCha20-Poly1305 AEAD.
 * @param plaintext H.265 NAL unit
 * @param keyPtr Key pointer from generateKey()
 * @param nonce 24-byte random nonce
 * @param aad Additional authenticated data (e.g., frame sequence)
 * @return ByteArray [ciphertext | tag] or null on error
 */
jbyteArray Java_com_stealthstream_crypto_NativeBridge_encryptFrame(
    JNIEnv* env,
    jclass clazz,
    jbyteArray plaintext,
    jlong keyPtr,
    jbyteArray nonce,
    jbyteArray aad);

/**
 * Decrypt and verify a frame.
 * @param ciphertext Encrypted data
 * @param keyPtr Key pointer
 * @param nonce 24-byte nonce
 * @param aad Additional authenticated data
 * @param tag 16-byte authentication tag
 * @return ByteArray plaintext or null if verification fails
 */
jbyteArray Java_com_stealthstream_crypto_NativeBridge_decryptFrame(
    JNIEnv* env,
    jclass clazz,
    jbyteArray ciphertext,
    jlong keyPtr,
    jbyteArray nonce,
    jbyteArray aad,
    jbyteArray tag);

/**
 * Optional: Cleanup (called at app shutdown).
 */
void Java_com_stealthstream_crypto_NativeBridge_cleanup(
    JNIEnv* env,
    jclass clazz);

#ifdef __cplusplus
}
#endif

#endif // STEALTHSTREAM_NATIVE_BRIDGE_H
