package com.stealthstream.test

import com.stealthstream.domain.repository.CryptoRepository
import com.stealthstream.security.CryptoUtils
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

/**
 * Unit tests for crypto operations.
 */
class CryptoUtilsTest {

    @Test
    fun testGenerateNonce() {
        val nonce1 = CryptoUtils.generateNonce()
        val nonce2 = CryptoUtils.generateNonce()

        assertEquals(24, nonce1.size)
        assertEquals(24, nonce2.size)
        // Nonces should be different (with overwhelming probability)
        assertTrue(nonce1.contentEquals(nonce2) == false)
    }

    @Test
    fun testGenerateKey() {
        val key = CryptoUtils.generateKey()
        assertNotNull(key)
        assertEquals("AES", key.algorithm)
    }

    @Test
    fun testSecureWipe() {
        val array = ByteArray(32) { 42 }
        CryptoUtils.secureWipe(array)
        assertTrue(array.all { it == 0.toByte() })
    }

    @Test
    fun testNonceUniqueness() {
        val nonce1 = CryptoUtils.generateNonce()
        CryptoUtils.recordNonce(nonce1)
        val isUnique = CryptoUtils.isNonceUnique(nonce1)
        // Second attempt should fail
        assertTrue(!isUnique)
    }
}
