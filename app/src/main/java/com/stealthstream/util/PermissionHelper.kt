package com.stealthstream.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.stealthstream.domain.model.PermissionStatus

/**
 * Helper class for permission checking and requesting.
 */
class PermissionHelper(private val context: Context) {

    /**
     * Check if a permission is granted.
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if a permission should be shown rationale.
     */
    fun shouldShowRationale(activity: android.app.Activity, permission: String): Boolean {
        return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            permission
        )
    }

    /**
     * Map permission status.
     */
    fun getPermissionStatus(permission: String): PermissionStatus {
        return if (isPermissionGranted(permission)) {
            PermissionStatus.GRANTED
        } else {
            PermissionStatus.DENIED
        }
    }
}
