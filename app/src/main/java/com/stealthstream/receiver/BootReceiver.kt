package com.stealthstream.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.stealthstream.service.StreamingService
import com.stealthstream.util.SecureLogger

/**
 * Boot receiver for handling device boot (currently disabled by default for Android 14+ compliance).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            SecureLogger.debug("BootReceiver", "Boot completed received")
            // Disabled by default - requires explicit user action on Android 14+
            // To enable: Set android:enabled="true" in AndroidManifest.xml
            // Then uncomment below:
            // val serviceIntent = Intent(context, StreamingService::class.java)
            // context?.startForegroundService(serviceIntent)
        }
    }
}
