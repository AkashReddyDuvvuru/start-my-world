package com.stealthstream.api

import com.stealthstream.domain.coordinator.StreamingCoordinator
import com.stealthstream.domain.model.EncryptedPacket
import com.stealthstream.domain.model.StreamingConfig
import com.stealthstream.domain.repository.CameraRepository
import com.stealthstream.domain.repository.CryptoRepository
import com.stealthstream.domain.repository.NetworkRepository
import com.stealthstream.util.SecureLogger
import com.stealthstream.util.Validators
import com.stealthstream.util.BinaryUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level API for streaming operations.
 */
interface StreamingApi {
    /**
     * Start streaming.
     */
    suspend fun startStreaming(config: StreamingConfig): Result<Unit>

    /**
     * Stop streaming.
     */
    suspend fun stopStreaming(): Result<Unit>

    /**
     * Get streaming statistics.
     */
    suspend fun getStreamingStats(): Result<StreamingStats>

    /**
     * Is currently streaming.
     */
    fun isStreaming(): Boolean
}

/**
 * Streaming statistics DTO.
 */
data class StreamingStats(
    val framesEncoded: Long,
    val bytesSent: Long,
    val packetLossPercent: Float,
    val currentFps: Int,
    val uptimeSeconds: Long
)

/**
 * Implementation of streaming API.
 */
@Singleton
class StreamingApiImpl @Inject constructor(
    private val streamingCoordinator: StreamingCoordinator,
    private val cameraRepository: CameraRepository,
    private val cryptoRepository: CryptoRepository,
    private val networkRepository: NetworkRepository
) : StreamingApi {

    companion object {
        private const val TAG = "StreamingApi"
    }

    override suspend fun startStreaming(config: StreamingConfig): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                SecureLogger.info(TAG, "Starting stream via API")
                streamingCoordinator.startStreaming(config).getOrThrow()
            }.onFailure { error ->
                SecureLogger.error(TAG, "Failed to start streaming", error)
            }
        }

    override suspend fun stopStreaming(): Result<Unit> =
        withContext(Dispatchers.Default) {
            runCatching {
                SecureLogger.info(TAG, "Stopping stream via API")
                streamingCoordinator.stopStreaming().getOrThrow()
            }.onFailure { error ->
                SecureLogger.error(TAG, "Failed to stop streaming", error)
            }
        }

    override suspend fun getStreamingStats(): Result<StreamingStats> =
        withContext(Dispatchers.Default) {
            runCatching {
                val currentStats = streamingCoordinator.getCurrentStats()
                StreamingStats(
                    framesEncoded = currentStats.framesEncoded,
                    bytesSent = currentStats.bytesSent,
                    packetLossPercent = currentStats.packetLossPercent,
                    currentFps = currentStats.currentFps,
                    uptimeSeconds = currentStats.uptimeSeconds
                )
            }
        }

    override fun isStreaming(): Boolean = streamingCoordinator.isStreaming()
}
