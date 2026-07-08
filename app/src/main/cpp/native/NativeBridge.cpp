#include "NativeBridge.h"
#include <android/log.h>
#include <cstring>
#include <stdexcept>

#define LOG_TAG "StealthStream"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

// Constants
const size_t MAX_FRAME_SIZE = 1048576; // 1 MB
const size_t MAX_PACKET_SIZE = 1200;   // UDP payload limit

jint Java_com_stealthstream_crypto_NativeBridge_initLibsodium(
    JNIEnv* env,
    jclass clazz) {
    if (sodium_init() < 0) {
        LOGE("Failed to initialize libsodium");
        return -1;
    }
    LOGD("libsodium initialized successfully");
    return 0;
}

jlong Java_com_stealthstream_crypto_NativeBridge_generateKey(
    JNIEnv* env,
    jclass clazz) {
    try {
        unsigned char* key = (unsigned char*)sodium_malloc(
            crypto_aead_xchacha20poly1305_IETF_KEYBYTES);
        if (key == nullptr) {
            LOGE("Failed to allocate key buffer");
            return 0;
        }

        // Lock in memory (prevent swapping to disk)
        if (sodium_mlock(key, crypto_aead_xchacha20poly1305_IETF_KEYBYTES) != 0) {
            LOGE("Failed to lock key memory");
            sodium_free(key);
            return 0;
        }

        // Generate key using secure RNG
        crypto_aead_xchacha20poly1305_ietf_keygen(key);
        LOGD("Key generated and locked in memory");

        return reinterpret_cast<jlong>(key);
    } catch (const std::exception& e) {
        LOGE("Exception in generateKey: %s", e.what());
        return 0;
    }
}

void Java_com_stealthstream_crypto_NativeBridge_zeroizeKeyMemory(
    JNIEnv* env,
    jclass clazz,
    jlong keyPtr) {
    if (keyPtr == 0) {
        LOGE("Invalid key pointer");
        return;
    }

    try {
        unsigned char* key = reinterpret_cast<unsigned char*>(keyPtr);
        size_t key_len = crypto_aead_xchacha20poly1305_IETF_KEYBYTES;

        // Secure erase (volatile write, not optimized away)
        sodium_memzero(key, key_len);
        LOGD("Key memory zeroed");

        // Unlock and free
        sodium_munlock(key, key_len);
        sodium_free(key);
        LOGD("Key memory freed");
    } catch (const std::exception& e) {
        LOGE("Exception in zeroizeKeyMemory: %s", e.what());
    }
}

jbyteArray Java_com_stealthstream_crypto_NativeBridge_generateNonce(
    JNIEnv* env,
    jclass clazz) {
    try {
        size_t nonce_len = crypto_aead_xchacha20poly1305_IETF_NPUBBYTES;
        unsigned char nonce[nonce_len];

        // Generate random nonce
        randombytes_buf(nonce, nonce_len);
        LOGD("Nonce generated (24 bytes)");

        // Convert to Java ByteArray
        jbyteArray result = env->NewByteArray(nonce_len);
        if (result != nullptr) {
            env->SetByteArrayRegion(result, 0, nonce_len, (const jbyte*)nonce);
        }

        // Erase local buffer
        sodium_memzero(nonce, nonce_len);

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in generateNonce: %s", e.what());
        return nullptr;
    }
}

jbyteArray Java_com_stealthstream_crypto_NativeBridge_encryptFrame(
    JNIEnv* env,
    jclass clazz,
    jbyteArray plaintext,
    jlong keyPtr,
    jbyteArray nonce,
    jbyteArray aad) {
    if (keyPtr == 0) {
        LOGE("Invalid key pointer");
        return nullptr;
    }

    jbyte* pt = nullptr;
    jbyte* nonce_buf = nullptr;
    jbyte* aad_buf = nullptr;
    unsigned char* ciphertext = nullptr;

    try {
        // Get plaintext
        pt = env->GetByteArrayElements(plaintext, nullptr);
        if (pt == nullptr) {
            LOGE("Failed to get plaintext elements");
            return nullptr;
        }
        jsize pt_len = env->GetArrayLength(plaintext);
        if (pt_len <= 0 || pt_len > (jsize)MAX_FRAME_SIZE) {
            LOGE("Invalid plaintext length: %d", pt_len);
            env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
            return nullptr;
        }

        // Get nonce
        nonce_buf = env->GetByteArrayElements(nonce, nullptr);
        if (nonce_buf == nullptr) {
            LOGE("Failed to get nonce elements");
            env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
            return nullptr;
        }
        jsize nonce_len = env->GetArrayLength(nonce);
        if (nonce_len != (jsize)crypto_aead_xchacha20poly1305_IETF_NPUBBYTES) {
            LOGE("Invalid nonce length: %d (expected %zu)", nonce_len,
                 crypto_aead_xchacha20poly1305_IETF_NPUBBYTES);
            env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
            env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
            return nullptr;
        }

        // Get AAD
        aad_buf = env->GetByteArrayElements(aad, nullptr);
        if (aad_buf == nullptr) {
            LOGE("Failed to get AAD elements");
            env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
            env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
            return nullptr;
        }
        jsize aad_len = env->GetArrayLength(aad);

        // Allocate output buffer
        unsigned long long clen =
            pt_len + crypto_aead_xchacha20poly1305_IETF_ABYTES;
        ciphertext = new unsigned char[clen];
        if (ciphertext == nullptr) {
            LOGE("Failed to allocate ciphertext buffer");
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
            nullptr,  // nsec
            (const unsigned char*)nonce_buf,
            key);

        // Clean up input buffers
        env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
        env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
        env->ReleaseByteArrayElements(aad, aad_buf, JNI_ABORT);

        if (res != 0) {
            LOGE("Encryption failed");
            sodium_memzero(ciphertext, clen);
            delete[] ciphertext;
            return nullptr;
        }

        LOGD("Frame encrypted: %zu -> %llu bytes", pt_len, clen);

        // Create Java byte array
        jbyteArray result = env->NewByteArray(clen);
        if (result != nullptr) {
            env->SetByteArrayRegion(result, 0, clen, (const jbyte*)ciphertext);
        }

        sodium_memzero(ciphertext, clen);
        delete[] ciphertext;

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in encryptFrame: %s", e.what());
        if (pt != nullptr) env->ReleaseByteArrayElements(plaintext, pt, JNI_ABORT);
        if (nonce_buf != nullptr) env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
        if (aad_buf != nullptr) env->ReleaseByteArrayElements(aad, aad_buf, JNI_ABORT);
        if (ciphertext != nullptr) {
            sodium_memzero(ciphertext, MAX_FRAME_SIZE);
            delete[] ciphertext;
        }
        return nullptr;
    }
}

jbyteArray Java_com_stealthstream_crypto_NativeBridge_decryptFrame(
    JNIEnv* env,
    jclass clazz,
    jbyteArray ciphertext,
    jlong keyPtr,
    jbyteArray nonce,
    jbyteArray aad,
    jbyteArray tag) {
    if (keyPtr == 0) {
        LOGE("Invalid key pointer");
        return nullptr;
    }

    // Similar structure to encryptFrame but for decryption
    // Implementation follows the same pattern with error handling
    // For brevity, showing the critical path:

    try {
        jbyte* ct = env->GetByteArrayElements(ciphertext, nullptr);
        if (ct == nullptr) return nullptr;
        jsize ct_len = env->GetArrayLength(ciphertext);

        jbyte* nonce_buf = env->GetByteArrayElements(nonce, nullptr);
        if (nonce_buf == nullptr) {
            env->ReleaseByteArrayElements(ciphertext, ct, JNI_ABORT);
            return nullptr;
        }

        jbyte* aad_buf = env->GetByteArrayElements(aad, nullptr);
        if (aad_buf == nullptr) {
            env->ReleaseByteArrayElements(ciphertext, ct, JNI_ABORT);
            env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
            return nullptr;
        }

        jbyte* tag_buf = env->GetByteArrayElements(tag, nullptr);
        if (tag_buf == nullptr) {
            env->ReleaseByteArrayElements(ciphertext, ct, JNI_ABORT);
            env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
            env->ReleaseByteArrayElements(aad, aad_buf, JNI_ABORT);
            return nullptr;
        }
        jsize aad_len = env->GetArrayLength(aad);

        unsigned char* plaintext =
            new unsigned char[ct_len + 1];
        if (plaintext == nullptr) {
            env->ReleaseByteArrayElements(ciphertext, ct, JNI_ABORT);
            env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
            env->ReleaseByteArrayElements(aad, aad_buf, JNI_ABORT);
            env->ReleaseByteArrayElements(tag, tag_buf, JNI_ABORT);
            return nullptr;
        }

        unsigned char* key = reinterpret_cast<unsigned char*>(keyPtr);
        unsigned long long pt_len = 0;
        int res = crypto_aead_xchacha20poly1305_ietf_decrypt(
            plaintext, &pt_len,
            nullptr,  // nsec
            (const unsigned char*)ct, ct_len,
            (const unsigned char*)aad_buf, aad_len,
            (const unsigned char*)tag_buf,
            (const unsigned char*)nonce_buf,
            key);

        env->ReleaseByteArrayElements(ciphertext, ct, JNI_ABORT);
        env->ReleaseByteArrayElements(nonce, nonce_buf, JNI_ABORT);
        env->ReleaseByteArrayElements(aad, aad_buf, JNI_ABORT);
        env->ReleaseByteArrayElements(tag, tag_buf, JNI_ABORT);

        if (res != 0) {
            LOGE("Decryption/verification failed");
            sodium_memzero(plaintext, ct_len);
            delete[] plaintext;
            return nullptr;
        }

        LOGD("Frame decrypted and verified: %zu bytes", pt_len);

        jbyteArray result = env->NewByteArray(pt_len);
        if (result != nullptr) {
            env->SetByteArrayRegion(result, 0, pt_len, (const jbyte*)plaintext);
        }

        sodium_memzero(plaintext, pt_len);
        delete[] plaintext;

        return result;
    } catch (const std::exception& e) {
        LOGE("Exception in decryptFrame: %s", e.what());
        return nullptr;
    }
}

void Java_com_stealthstream_crypto_NativeBridge_cleanup(
    JNIEnv* env,
    jclass clazz) {
    LOGD("Native cleanup called");
    // Any final cleanup here
}
