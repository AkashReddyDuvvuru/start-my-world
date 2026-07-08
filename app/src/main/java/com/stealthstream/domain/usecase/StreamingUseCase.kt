package com.stealthstream.domain.usecase

import com.stealthstream.domain.model.StreamingConfig
import com.stealthstream.domain.model.StreamingError
import com.stealthstream.domain.model.StreamingStats
import com.stealthstream.domain.repository.AuditRepository
import com.stealthstream.domain.repository.CameraRepository
import com.stealthstream.domain.repository.CryptoRepository
import com.stealthstream.domain.repository.NetworkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for streaming operations.
 */
interface StreamingUseCase {
    /**
     * Start streaming.
     */
    suspend fun startStreaming(config: StreamingConfig): Result<Unit>

    /**
     * Stop streaming.
     */
    suspend fun stopStreaming(): Result<Unit>

    /**
     * Get current streaming status flow.
     */
    fun getStatusFlow(): Flow<StreamingStatus>

    /**
     * Get current statistics.
     */
    fun getCurrentStats(): StreamingStats
}

/**
 * Streaming status with error context.
 */
data class StreamingStatus(
    val isStreaming: Boolean,
    val error: StreamingError? = null,
    val stats: StreamingStats = StreamingStats()
)

/**
 * Implementation of streaming use case.
 */
@Singleton
class StreamingUseCaseImpl @Inject constructor(
    private val cameraRepository: CameraRepository,
    private val cryptoRepository: CryptoRepository,
    private val networkRepository: NetworkRepository,
    private val auditRepository: AuditRepository
) : StreamingUseCase {

    private val statusFlow = MutableSharedFlow<StreamingStatus>()
    private var currentStats = StreamingStats()
    private var keyPtr = 0L
    private var frameSequence = 0L
    private var isStreaming = false

    override suspend fun startStreaming(config: StreamingConfig): Result<Unit> =
        runCatching {
            if (isStreaming) {
                throw IllegalStateException("Already streaming")
            }

            // Check permission
            if (!cameraRepository.isCameraPermissionGranted()) {
                throw SecurityException("Camera permission not granted")
            }

            auditRepository.logServiceStart("User initiated streaming")

            // Open camera
            cameraRepository.openCamera().onFailure {
                auditRepository.logError("CAMERA_OPEN_FAILED", it.message ?: "Unknown")
                throw it
            }

            // Generate encryption key
            keyPtr = cryptoRepository.generateKey()
            if (keyPtr == 0L) {
                throw RuntimeException("Failed to generate encryption key")
            }

            // Connect to network
            networkRepository.connect(config.targetIp, config.targetPort).onFailure {
                cryptoRepository.zeroizeKey(keyPtr)
                cameraRepository.closeCamera()
                auditRepository.logError("NETWORK_CONNECT_FAILED", it.message ?: "Unknown")
                throw it
            }

            isStreaming = true
            frameSequence = 0L
            currentStats = StreamingStats()
            statusFlow.emit(StreamingStatus(isStreaming = true, stats = currentStats))
        }.also {
            if (it.isFailure) {
                isStreaming = false
                statusFlow.emit(StreamingStatus(isStreaming = false))
            }
        }

    override suspend fun stopStreaming(): Result<Unit> =
        runCatching {
            if (!isStreaming) return@runCatching

            isStreaming = false
            auditRepository.logServiceStop("User stopped streaming")

            // Disconnect network
            networkRepository.disconnect()

            // Close camera
            cameraRepository.closeCamera()

            // Clean up key
            if (keyPtr != 0L) {
                cryptoRepository.zeroizeKey(keyPtr)
                keyPtr = 0L
            }

            statusFlow.emit(StreamingStatus(isStreaming = false, stats = currentStats))
        }

    override fun getStatusFlow(): Flow<StreamingStatus> = statusFlow.asSharedFlow()

    override fun getCurrentStats(): StreamingStats = currentStats.copy()
}
