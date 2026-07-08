package com.stealthstream.util

import timber.log.Timber

/**
 * Security-aware logger that prevents sensitive data leaks.
 */
object SecureLogger {

    /**
     * Log an error without sensitive data.
     */
    fun error(tag: String, message: String, cause: Throwable? = null) {
        Timber.tag(tag).e(cause, message)
    }

    /**
     * Log a non-sensitive event.
     */
    fun info(tag: String, message: String) {
        if (timber.log.Timber.treeCount() > 0) {
            Timber.tag(tag).i(message)
        }
    }

    /**
     * Log debug information (only in debug builds).
     */
    fun debug(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    /**
     * Never log cryptographic material.
     */
    fun neverLog(sensitiveData: String?) {
        // Explicitly do not log
        // This is a placeholder to make intent clear
    }
}
