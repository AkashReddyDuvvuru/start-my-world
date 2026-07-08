package com.stealthstream.domain.coordinator

import com.stealthstream.domain.model.StreamingConfig
import com.stealthstream.domain.model.StreamingError
import com.stealthstream.domain.model.StreamingStats
import com.stealthstream.domain.model.ErrorType
import com.stealthstream.domain.repository.AuditRepository
import com.stealthstream.domain.repository.CameraRepository
import com.stealthstream.domain.repository.CryptoRepository
import com.stealthstream.domain.repository.NetworkRepository
import com.stealthstream.domain.usecase.StreamingStatus
import com.stealthstream.domain.usecase.StreamingUseCase
import com.stealthstream.util.SecureLogger
import com.stealthstream.util.Validators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates all streaming operations with proper error handling and resource management.
 */
interface StreamingCoordinator {
    /**
     * Start streaming with configuration.
     */
    suspend fun startStreaming(config: StreamingConfig): Result<Unit>

    /**
     * Stop streaming.
     */
    suspend fun stopStreaming(): Result<Unit>

    /**
     * Get streaming status flow.
     */
    fun getStatusFlow(): Flow<StreamingStatus>

    /**
     * Get current statistics.
     */
    fun getCurrentStats(): StreamingStats

    /**
     * Check if currently streaming.
     */
    fun isStreaming(): Boolean
}

/**
 * Implementation of streaming coordinator.
 */
@Singleton
class StreamingCoordinatorImpl @Inject constructor(
    private val streamingUseCase: StreamingUseCase,
    private val cameraRepository: CameraRepository,
    private val cryptoRepository: CryptoRepository,
    private val networkRepository: NetworkRepository,
    private val auditRepository: AuditRepository
) : StreamingCoordinator {

    private val statusFlow = MutableSharedFlow<StreamingStatus>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isCurrentlyStreaming = false
    private var currentConfig: StreamingConfig? = null
    private var currentStats = StreamingStats()
    private var keyPtr = 0L
    private var frameSequence = 0L

    override suspend fun startStreaming(config: StreamingConfig): Result<Unit> = runCatching {
        if (isCurrentlyStreaming) {
            throw IllegalStateException("Already streaming")
        }

        // Validate configuration
        Validators.validateStreamingConfig(config).onFailure { error ->
            auditRepository.logError("CONFIG_VALIDATION_FAILED", error.message ?: "Unknown")
            throw error
        }

        // Check camera permission
        if (!cameraRepository.isCameraPermissionGranted()) {
            val error = StreamingError(
                type = ErrorType.PERMISSION_DENIED,
                message = "Camera permission not granted"
            )
            auditRepository.logError("PERMISSION_DENIED", "CAMERA")
            statusFlow.emit(StreamingStatus(isStreaming = false, error = error))
            throw error
        }

        SecureLogger.info("StreamingCoordinator", "Starting stream with config: ${config.targetIp}:${config.targetPort}")
        auditRepository.logServiceStart("User initiated streaming")

        currentConfig = config
        isCurrentlyStreaming = true
        frameSequence = 0L

        // Start streaming through use case
        streamingUseCase.startStreaming(config).onFailure { error ->
            isCurrentlyStreaming = false
            val streamError = StreamingError(
                type = ErrorType.SERVICE_FAILED,
                message = error.message ?: "Unknown error",
                cause = error
            )
            statusFlow.emit(StreamingStatus(isStreaming = false, error = streamError))
            throw error
        }

        statusFlow.emit(StreamingStatus(isStreaming = true, stats = currentStats))
    }.onFailure { error ->
        isCurrentlyStreaming = false
        SecureLogger.error("StreamingCoordinator", "Failed to start streaming", error)
    }

    override suspend fun stopStreaming(): Result<Unit> = runCatching {
        if (!isCurrentlyStreaming) {
            return@runCatching
        }

        SecureLogger.info("StreamingCoordinator", "Stopping stream")
        auditRepository.logServiceStop("User stopped streaming")

        isCurrentlyStreaming = false

        streamingUseCase.stopStreaming().onFailure { error ->
            SecureLogger.error("StreamingCoordinator", "Error stopping streaming", error)
        }

        statusFlow.emit(StreamingStatus(isStreaming = false, stats = currentStats))
    }

    override fun getStatusFlow(): Flow<StreamingStatus> = statusFlow.asSharedFlow()

    override fun getCurrentStats(): StreamingStats = currentStats.copy()

    override fun isStreaming(): Boolean = isCurrentlyStreaming

    /**
     * Clean up resources on app shutdown.
     */
    fun cleanup() {
        scope.cancel()
        if (keyPtr != 0L) {
            cryptoRepository.zeroizeKey(keyPtr)
            keyPtr = 0L
        }
    }
}
