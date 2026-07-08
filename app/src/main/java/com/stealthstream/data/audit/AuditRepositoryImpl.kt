package com.stealthstream.data.audit

import android.content.Context
import android.util.Log
import com.stealthstream.domain.repository.AuditRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of audit repository using Timber for logging.
 */
@Singleton
class AuditRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AuditRepository {

    companion object {
        private const val TAG = "AuditLog"
        private val dateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    }

    private val logBuffer = mutableListOf<String>()
    private val maxBufferSize = 1000

    override fun logServiceStart(reason: String) {
        val message = "[SERVICE_START] reason=$reason"
        addLog(message)
        Timber.tag(TAG).i(message)
    }

    override fun logServiceStop(reason: String) {
        val message = "[SERVICE_STOP] reason=$reason"
        addLog(message)
        Timber.tag(TAG).i(message)
    }

    override fun logPermissionRequested(permission: String) {
        val message = "[PERMISSION_REQUESTED] permission=$permission"
        addLog(message)
        Timber.tag(TAG).i(message)
    }

    override fun logPermissionGranted(permission: String) {
        val message = "[PERMISSION_GRANTED] permission=$permission"
        addLog(message)
        Timber.tag(TAG).i(message)
    }

    override fun logPermissionDenied(permission: String) {
        val message = "[PERMISSION_DENIED] permission=$permission"
        addLog(message)
        Timber.tag(TAG).w(message)
    }

    override fun logStreamingStats(
        framesEncoded: Long,
        bytesSent: Long,
        packetLoss: Float
    ) {
        val message = "[STREAMING_STATS] frames=$framesEncoded bytes=$bytesSent loss=$packetLoss"
        // Only add to buffer (don't spam logs)
        addLog(message)
    }

    override fun logError(
        errorType: String,
        message: String
    ) {
        val logMessage = "[ERROR] type=$errorType msg=$message"
        addLog(logMessage)
        Timber.tag(TAG).e(logMessage)
    }

    override suspend fun getRecentLogs(limitDays: Int): List<String> {
        // Return the last N logs from buffer
        return logBuffer.takeLast(100)
    }

    private fun addLog(message: String) {
        val timestamp = dateFormat.format(Date())
        val logEntry = "[$timestamp] $message"
        synchronized(logBuffer) {
            logBuffer.add(logEntry)
            if (logBuffer.size > maxBufferSize) {
                logBuffer.removeAt(0)
            }
        }
    }
}
