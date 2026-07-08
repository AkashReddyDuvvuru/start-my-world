package com.stealthstream.test

import com.stealthstream.domain.model.CameraFrame
import com.stealthstream.domain.model.EncryptedPacket
import com.stealthstream.domain.model.StreamingConfig
import java.util.concurrent.atomic.AtomicLong

/**
 * Test utilities and factories.
 */
object TestFactory {

    fun createStreamingConfig(
        targetIp: String = "192.168.1.1",
        targetPort: Int = 40001,
        targetBitrateMbps: Float = 0.5f,
        frameRateTarget: Int = 30
    ): StreamingConfig {
        return StreamingConfig(
            targetIp = targetIp,
            targetPort = targetPort,
            targetBitrateMbps = targetBitrateMbps,
            frameRateTarget = frameRateTarget
        )
    }

    fun createCameraFrame(
        data: ByteArray = ByteArray(1024),
        width: Int = 1920,
        height: Int = 1080,
        timestamp: Long = System.currentTimeMillis()
    ): CameraFrame {
        return CameraFrame(
            data = data,
            width = width,
            height = height,
            timestamp = timestamp,
            format = "HEVC"
        )
    }

    fun createEncryptedPacket(
        frameSequence: Long = 0,
        nonce: ByteArray = ByteArray(24),
        ciphertext: ByteArray = ByteArray(1024),
        tag: ByteArray = ByteArray(16),
        timestamp: Long = System.currentTimeMillis()
    ): EncryptedPacket {
        return EncryptedPacket(
            frameSequence = frameSequence,
            nonce = nonce,
            ciphertext = ciphertext,
            tag = tag,
            timestamp = timestamp
        )
    }
}
