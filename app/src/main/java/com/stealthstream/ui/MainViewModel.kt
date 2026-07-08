package com.stealthstream.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stealthstream.api.StreamingApi
import com.stealthstream.api.StreamingStats
import com.stealthstream.domain.auth.AuthorizationProvider
import com.stealthstream.domain.coordinator.StreamingCoordinator
import com.stealthstream.domain.model.PermissionStatus
import com.stealthstream.domain.model.StreamingConfig
import com.stealthstream.domain.model.StreamStatus
import com.stealthstream.domain.usecase.PermissionUseCase
import com.stealthstream.util.SecureLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainActivity.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val streamingApi: StreamingApi,
    private val streamingCoordinator: StreamingCoordinator,
    private val authorizationProvider: AuthorizationProvider,
    private val permissionUseCase: PermissionUseCase
) : ViewModel() {

    private val _streamingStatus = MutableStateFlow(StreamStatus.IDLE)
    val streamingStatus: StateFlow<StreamStatus> = _streamingStatus.asStateFlow()

    private val _streamingStats = MutableStateFlow(
        StreamingStats(
            framesEncoded = 0,
            bytesSent = 0,
            packetLossPercent = 0f,
            currentFps = 0,
            uptimeSeconds = 0
        )
    )
    val streamingStats: StateFlow<StreamingStats> = _streamingStats.asStateFlow()

    init {
        observeStreamingStatus()
    }

    fun startStreaming(config: StreamingConfig) {
        if (!authorizationProvider.canStartStreaming()) {
            SecureLogger.error("MainViewModel", "Not authorized to start streaming")
            _streamingStatus.update { StreamStatus.ERROR }
            return
        }

        _streamingStatus.update { StreamStatus.INITIALIZING }

        viewModelScope.launch(Dispatchers.Default) {
            streamingApi.startStreaming(config).onSuccess {
                _streamingStatus.update { StreamStatus.STREAMING }
                SecureLogger.info("MainViewModel", "Streaming started")
            }.onFailure { error ->
                _streamingStatus.update { StreamStatus.ERROR }
                SecureLogger.error(
                    "MainViewModel",
                    "Failed to start streaming",
                    error
                )
            }
        }
    }

    fun stopStreaming() {
        _streamingStatus.update { StreamStatus.STOPPING }

        viewModelScope.launch(Dispatchers.Default) {
            streamingApi.stopStreaming().onSuccess {
                _streamingStatus.update { StreamStatus.STOPPED }
                SecureLogger.info("MainViewModel", "Streaming stopped")
            }.onFailure { error ->
                _streamingStatus.update { StreamStatus.ERROR }
                SecureLogger.error(
                    "MainViewModel",
                    "Failed to stop streaming",
                    error
                )
            }
        }
    }

    fun onPermissionGranted(permission: String) {
        permissionUseCase.updatePermissionStatus(
            permission,
            PermissionStatus.GRANTED
        )
    }

    fun onPermissionDenied(permission: String) {
        permissionUseCase.updatePermissionStatus(
            permission,
            PermissionStatus.DENIED
        )
    }

    private fun observeStreamingStatus() {
        viewModelScope.launch {
            streamingCoordinator.getStatusFlow().collect { status ->
                _streamingStatus.update { status.isStreaming.toStreamStatus() }
                _streamingStats.update {
                    StreamingStats(
                        framesEncoded = status.stats.framesEncoded,
                        bytesSent = status.stats.bytesSent,
                        packetLossPercent = status.stats.packetLossPercent,
                        currentFps = status.stats.currentFps,
                        uptimeSeconds = status.stats.uptimeSeconds
                    )
                }
            }
        }
    }

    private fun Boolean.toStreamStatus(): StreamStatus {
        return if (this) StreamStatus.STREAMING else StreamStatus.IDLE
    }
}
