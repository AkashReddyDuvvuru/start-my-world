package com.stealthstream.security

import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Cryptographic utilities for key management.
 */
object CryptoUtils {

    /**
     * Generate a secure random nonce.
     */
    fun generateNonce(size: Int = 24): ByteArray {
        val nonce = ByteArray(size)
        SecureRandom().nextBytes(nonce)
        return nonce
    }

    /**
     * Generate a secure random key.
     */
    fun generateKey(size: Int = 256): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(size)
        return keyGenerator.generateKey()
    }

    /**
     * Securely wipe memory.
     */
    fun secureWipe(array: ByteArray) {
        array.fill(0)
    }

    /**
     * Check nonce uniqueness (in production, use a database).
     */
    private val usedNonces = mutableSetOf<ByteArray>()

    fun isNonceUnique(nonce: ByteArray): Boolean {
        return usedNonces.add(nonce)
    }

    fun recordNonce(nonce: ByteArray) {
        usedNonces.add(nonce)
    }
}
