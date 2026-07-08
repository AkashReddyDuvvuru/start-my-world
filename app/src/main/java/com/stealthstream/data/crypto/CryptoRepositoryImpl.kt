package com.stealthstream.data.crypto

import com.stealthstream.domain.repository.CryptoRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JNI bridge to native libsodium functions.
 */
object NativeBridge {
    init {
        System.loadLibrary("stealthstream-native")
    }

    external fun initLibsodium(): Int
    external fun generateKey(): Long
    external fun zeroizeKeyMemory(keyPtr: Long)
    external fun generateNonce(): ByteArray
    external fun encryptFrame(
        plaintext: ByteArray,
        keyPtr: Long,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray?

    external fun decryptFrame(
        ciphertext: ByteArray,
        keyPtr: Long,
        nonce: ByteArray,
        aad: ByteArray,
        tag: ByteArray
    ): ByteArray?

    external fun cleanup()
}

/**
 * Implementation of crypto repository using JNI.
 */
@Singleton
class CryptoRepositoryImpl @Inject constructor() : CryptoRepository {

    init {
        val result = NativeBridge.initLibsodium()
        if (result != 0) {
            throw RuntimeException("Failed to initialize libsodium")
        }
    }

    override fun generateKey(): Long = NativeBridge.generateKey()

    override fun zeroizeKey(keyPtr: Long) = NativeBridge.zeroizeKeyMemory(keyPtr)

    override fun generateNonce(): ByteArray = NativeBridge.generateNonce()

    override fun encryptFrame(
        plaintext: ByteArray,
        keyPtr: Long,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray? = NativeBridge.encryptFrame(plaintext, keyPtr, nonce, aad)

    override fun decryptFrame(
        ciphertext: ByteArray,
        keyPtr: Long,
        nonce: ByteArray,
        aad: ByteArray,
        tag: ByteArray
    ): ByteArray? = NativeBridge.decryptFrame(ciphertext, keyPtr, nonce, aad, tag)
}
