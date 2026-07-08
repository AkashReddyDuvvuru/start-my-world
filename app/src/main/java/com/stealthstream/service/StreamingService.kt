package com.stealthstream.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.stealthstream.domain.coordinator.StreamingCoordinator
import com.stealthstream.domain.model.StreamingConfig
import com.stealthstream.util.SecureLogger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service for streaming.
 */
@AndroidEntryPoint
class StreamingService : LifecycleService() {

    @Inject
    lateinit var streamingCoordinator: StreamingCoordinator

    private val binder = StreamingBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isStreaming = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "stealthstream_streaming"
        const val ACTION_START_STREAM = "com.stealthstream.START_STREAM"
        const val ACTION_STOP_STREAM = "com.stealthstream.STOP_STREAM"
    }

    override fun onCreate() {
        super.onCreate()
        SecureLogger.debug("StreamingService", "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_STREAM -> {
                scope.launch {
                    startStreaming()
                }
            }
            ACTION_STOP_STREAM -> {
                scope.launch {
                    stopStreaming()
                }
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder = binder

    override fun onDestroy() {
        scope.launch {
            stopStreaming()
        }
        SecureLogger.debug("StreamingService", "Service destroyed")
        super.onDestroy()
    }

    private suspend fun startStreaming() {
        if (isStreaming) return

        val config = StreamingConfig(
            targetIp = "203.0.113.42",
            targetPort = 40001,
            targetBitrateMbps = 0.5f,
            frameRateTarget = 30
        )

        streamingCoordinator.startStreaming(config).onSuccess {
            isStreaming = true
            showNotification()
            SecureLogger.info("StreamingService", "Streaming started")
        }.onFailure { error ->
            SecureLogger.error("StreamingService", "Failed to start streaming", error)
        }
    }

    private suspend fun stopStreaming() {
        if (!isStreaming) return

        streamingCoordinator.stopStreaming().onSuccess {
            isStreaming = false
            stopForeground(STOP_FOREGROUND_REMOVE)
            SecureLogger.info("StreamingService", "Streaming stopped")
        }.onFailure { error ->
            SecureLogger.error("StreamingService", "Failed to stop streaming", error)
        }
    }

    private fun showNotification() {
        val notification = android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(com.stealthstream.R.string.notification_title))
            .setContentText(getString(com.stealthstream.R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(false)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        )
    }

    /**
     * Binder for local binding.
     */
    inner class StreamingBinder : Binder() {
        fun getService(): StreamingService = this@StreamingService
    }
}
