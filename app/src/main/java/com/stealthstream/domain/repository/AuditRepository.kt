package com.stealthstream.domain.repository

/**
 * Repository interface for audit logging and analytics.
 */
interface AuditRepository {
    /**
     * Log a service lifecycle event.
     */
    fun logServiceStart(reason: String)

    fun logServiceStop(reason: String)

    /**
     * Log permission events.
     */
    fun logPermissionRequested(permission: String)

    fun logPermissionGranted(permission: String)

    fun logPermissionDenied(permission: String)

    /**
     * Log streaming statistics.
     */
    fun logStreamingStats(
        framesEncoded: Long,
        bytesSent: Long,
        packetLoss: Float
    )

    /**
     * Log errors (non-sensitive).
     */
    fun logError(
        errorType: String,
        message: String
    )

    /**
     * Retrieve recent audit logs (for debugging).
     */
    suspend fun getRecentLogs(limitDays: Int = 7): List<String>
}
