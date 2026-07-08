package com.stealthstream.logging

import android.util.Log
import com.stealthstream.util.SecureLogger
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audit logger for tracking events.
 */
interface AuditLogger {
    /**
     * Log an audit event.
     */
    fun log(event: AuditEvent)

    /**
     * Get all audit events.
     */
    fun getEvents(): List<AuditEvent>

    /**
     * Clear audit log.
     */
    fun clear()
}

/**
 * Audit event.
 */
data class AuditEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val action: String,
    val result: String,
    val details: String = "",
    val userId: String? = null
) {
    fun toLogLine(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        val timeStr = formatter.format(Date(timestamp))
        return "[$timeStr] $action - $result - $details"
    }
}

/**
 * Implementation of audit logger.
 */
@Singleton
class AuditLoggerImpl @Inject constructor() : AuditLogger {

    private val events = mutableListOf<AuditEvent>()
    private val maxEvents = 1000

    companion object {
        private const val TAG = "AuditLogger"
    }

    override fun log(event: AuditEvent) {
        events.add(event)
        SecureLogger.info(TAG, event.toLogLine())

        // Keep size bounded
        if (events.size > maxEvents) {
            events.removeAt(0)
        }
    }

    override fun getEvents(): List<AuditEvent> = events.toList()

    override fun clear() {
        events.clear()
    }
}
