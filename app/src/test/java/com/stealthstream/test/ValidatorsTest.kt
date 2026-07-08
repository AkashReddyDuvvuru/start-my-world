package com.stealthstream.test

import com.stealthstream.domain.model.StreamingConfig
import com.stealthstream.util.Validators
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.assertFails

/**
 * Unit tests for validators.
 */
class ValidatorsTest {

    @Test
    fun testValidStreamingConfig() {
        val config = TestFactory.createStreamingConfig()
        val result = Validators.validateStreamingConfig(config)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testInvalidStreamingConfigBadIp() {
        val config = TestFactory.createStreamingConfig(targetIp = "999.999.999.999")
        val result = Validators.validateStreamingConfig(config)
        assertTrue(result.isFailure)
    }

    @Test
    fun testInvalidStreamingConfigBadPort() {
        val config = TestFactory.createStreamingConfig(targetPort = -1)
        val result = Validators.validateStreamingConfig(config)
        assertTrue(result.isFailure)
    }

    @Test
    fun testValidNonce() {
        val nonce = ByteArray(24) { it.toByte() }
        val result = Validators.validateNonce(nonce)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testInvalidNonceSize() {
        val nonce = ByteArray(16) { it.toByte() }
        val result = Validators.validateNonce(nonce)
        assertTrue(result.isFailure)
    }

    @Test
    fun testValidTag() {
        val tag = ByteArray(16) { it.toByte() }
        val result = Validators.validateTag(tag)
        assertTrue(result.isSuccess)
    }

    @Test
    fun testInvalidTagSize() {
        val tag = ByteArray(8) { it.toByte() }
        val result = Validators.validateTag(tag)
        assertTrue(result.isFailure)
    }
}
