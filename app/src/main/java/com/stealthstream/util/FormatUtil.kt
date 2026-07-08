package com.stealthstream.util

import java.util.concurrent.TimeUnit

/**
 * Utilities for formatting and time calculations.
 */
object FormatUtil {

    /**
     * Format bytes as human-readable size.
     */
    fun formatBytes(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var size = bytes.toDouble()
        var unitIndex = 0
        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }
        return String.format("%.1f %s", size, units[unitIndex])
    }

    /**
     * Format bitrate as human-readable.
     */
    fun formatBitrate(bitsPerSecond: Long): String {
        val kbps = bitsPerSecond / 1000
        val mbps = kbps / 1000
        return if (mbps > 0) {
            "$mbps Mbps"
        } else {
            "$kbps kbps"
        }
    }

    /**
     * Format duration as HH:MM:SS.
     */
    fun formatDuration(milliseconds: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        val minutes =
            TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60
        val seconds =
            TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Format percentage.
     */
    fun formatPercent(value: Float): String {
        return String.format("%.1f%%", value * 100)
    }
}
