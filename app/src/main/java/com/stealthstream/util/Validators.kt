package com.stealthstream.util

import com.stealthstream.domain.model.StreamingConfig
import java.net.InetAddress

/**
 * Validators for various inputs.
 */
object Validators {

    /**
     * Validate streaming configuration.
     */
    fun validateStreamingConfig(config: StreamingConfig): Result<Unit> = runCatching {
        require(config.targetPort in 1..65535) {
            "Port must be between 1 and 65535"
        }
        require(config.targetBitrateMbps > 0f) {
            "Bitrate must be positive"
        }
        require(config.frameRateTarget > 0) {
            "Frame rate must be positive"
        }
        try {
            InetAddress.getByName(config.targetIp)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid IP address: ${config.targetIp}")
        }
    }

    /**
     * Validate nonce length.
     */
    fun validateNonce(nonce: ByteArray): Result<Unit> = runCatching {
        require(nonce.size == 24) {
            "Nonce must be exactly 24 bytes, got ${nonce.size}"
        }
    }

    /**
     * Validate tag length.
     */
    fun validateTag(tag: ByteArray): Result<Unit> = runCatching {
        require(tag.size == 16) {
            "Tag must be exactly 16 bytes, got ${tag.size}"
        }
    }
}
