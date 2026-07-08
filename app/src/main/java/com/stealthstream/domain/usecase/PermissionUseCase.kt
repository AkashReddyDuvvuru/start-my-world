package com.stealthstream.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.stealthstream.domain.model.PermissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case for permission management.
 */
interface PermissionUseCase {
    /**
     * Check permission status.
     */
    fun checkPermission(permission: String): PermissionStatus

    /**
     * Get permission status flow.
     */
    fun getPermissionStatusFlow(permission: String): Flow<PermissionStatus>

    /**
     * Update permission status (called from activity).
     */
    fun updatePermissionStatus(permission: String, status: PermissionStatus)
}

/**
 * Implementation of permission use case.
 */
@Singleton
class PermissionUseCaseImpl @Inject constructor(
    private val context: Context
) : PermissionUseCase {

    private val permissionStatusMap =
        mutableMapOf<String, MutableStateFlow<PermissionStatus>>()

    override fun checkPermission(permission: String): PermissionStatus {
        return when {
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED -> PermissionStatus.GRANTED
            else -> PermissionStatus.DENIED
        }
    }

    override fun getPermissionStatusFlow(permission: String): Flow<PermissionStatus> {
        if (!permissionStatusMap.containsKey(permission)) {
            permissionStatusMap[permission] =
                MutableStateFlow(checkPermission(permission))
        }
        return permissionStatusMap[permission]!!.asStateFlow()
    }

    override fun updatePermissionStatus(permission: String, status: PermissionStatus) {
        if (!permissionStatusMap.containsKey(permission)) {
            permissionStatusMap[permission] = MutableStateFlow(status)
        } else {
            permissionStatusMap[permission]?.value = status
        }
    }
}
