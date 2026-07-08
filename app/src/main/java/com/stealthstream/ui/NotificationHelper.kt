package com.stealthstream.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Helper for creating notification channels.
 */
object NotificationHelper {

    const val CHANNEL_ID_STREAMING = "stealthstream_streaming"
    const val CHANNEL_NAME_STREAMING = "StealthStream Streaming"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Streaming channel (low importance)
            val streamingChannel = NotificationChannel(
                CHANNEL_ID_STREAMING,
                CHANNEL_NAME_STREAMING,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for active streaming sessions"
                enableVibration(false)
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(streamingChannel)
        }
    }
}
