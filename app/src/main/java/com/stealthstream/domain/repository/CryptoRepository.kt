package com.stealthstream.domain.repository

import com.stealthstream.domain.model.EncryptedPacket

/**
 * Repository interface for cryptographic operations.
 */
interface CryptoRepository {
    /**
     * Generate a new encryption key.
     * @return Pointer to key (as Long) or 0 on failure
     */
    fun generateKey(): Long

    /**
     * Securely erase and free a key.
     */
    fun zeroizeKey(keyPtr: Long)

    /**
     * Generate a random 24-byte nonce.
     */
    fun generateNonce(): ByteArray

    /**
     * Encrypt a frame.
     * @param plaintext Camera frame data
     * @param keyPtr Key pointer from generateKey()
     * @param nonce 24-byte nonce
     * @param aad Additional authenticated data (frame sequence)
     * @return Ciphertext with tag appended, or null on error
     */
    fun encryptFrame(
        plaintext: ByteArray,
        keyPtr: Long,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray?

    /**
     * Decrypt and verify a frame.
     * @return Plaintext on success, null if verification fails
     */
    fun decryptFrame(
        ciphertext: ByteArray,
        keyPtr: Long,
        nonce: ByteArray,
        aad: ByteArray,
        tag: ByteArray
    ): ByteArray?
}
