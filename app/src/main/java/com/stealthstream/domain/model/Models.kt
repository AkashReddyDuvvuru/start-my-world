package com.stealthstream.domain.model

/**
 * Represents the streaming status.
 */
enum class StreamStatus {
    IDLE,
    INITIALIZING,
    STREAMING,
    PAUSED,
    STOPPING,
    ERROR,
    STOPPED
}

/**
 * Represents permission status.
 */
enum class PermissionStatus {
    GRANTED,
    DENIED,
    PENDING_REQUEST,
    NEVER_ASK_AGAIN
}

/**
 * Error types for detailed error handling.
 */
enum class ErrorType {
    PERMISSION_DENIED,
    CAMERA_UNAVAILABLE,
    CAMERA_IN_USE,
    NETWORK_UNAVAILABLE,
    ENCRYPTION_FAILED,
    SERVICE_FAILED,
    UNKNOWN
}

/**
 * Represents a streaming error with context.
 */
data class StreamingError(
    val type: ErrorType,
    val message: String,
    val cause: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Throwable(message, cause)

/**
 * Streaming configuration.
 */
data class StreamingConfig(
    val targetIp: String = "203.0.113.42",  // RFC 5737 documentation IP
    val targetPort: Int = 40001,
    val targetBitrateMbps: Float = 0.5f,
    val frameRateTarget: Int = 30,
    val enableAudio: Boolean = false,
    val autoRestart: Boolean = false
)

/**
 * Real-time statistics for streaming.
 */
data class StreamingStats(
    val framesEncoded: Long = 0,
    val bytesSent: Long = 0,
    val packetsDropped: Long = 0,
    val currentFps: Int = 0,
    val averageBitrate: Float = 0f,
    val packetLossPercent: Float = 0f,
    val uptimeSeconds: Long = 0,
    val lastFrameTimestamp: Long = 0
)

/**
 * Stream session metadata.
 */
data class StreamSession(
    val sessionId: String,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: StreamStatus = StreamStatus.IDLE,
    val config: StreamingConfig = StreamingConfig(),
    val stats: StreamingStats = StreamingStats(),
    val lastError: StreamingError? = null
) {
    val duration: Long
        get() = (endTime ?: System.currentTimeMillis()) - startTime

    val isActive: Boolean
        get() = status == StreamStatus.STREAMING
}

/**
 * Camera frame metadata.
 */
data class CameraFrame(
    val frameNumber: Long,
    val timestamp: Long,
    val width: Int,
    val height: Int,
    val data: ByteArray,
    val isKeyFrame: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CameraFrame

        if (frameNumber != other.frameNumber) return false
        if (timestamp != other.timestamp) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!data.contentEquals(other.data)) return false
        if (isKeyFrame != other.isKeyFrame) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameNumber.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + data.contentHashCode()
        result = 31 * result + isKeyFrame.hashCode()
        return result
    }
}

/**
 * Encrypted packet ready for transmission.
 */
data class EncryptedPacket(
    val frameSequence: Long,
    val nonce: ByteArray,
    val ciphertext: ByteArray,
    val tag: ByteArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(nonce.size == 24) { "Nonce must be 24 bytes" }
        require(tag.size == 16) { "Tag must be 16 bytes" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EncryptedPacket

        if (frameSequence != other.frameSequence) return false
        if (!nonce.contentEquals(other.nonce)) return false
        if (!ciphertext.contentEquals(other.ciphertext)) return false
        if (!tag.contentEquals(other.tag)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = frameSequence.hashCode()
        result = 31 * result + nonce.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        result = 31 * result + tag.contentHashCode()
        return result
    }
}
