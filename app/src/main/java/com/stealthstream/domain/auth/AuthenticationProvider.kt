package com.stealthstream.domain.auth

import com.stealthstream.domain.model.PermissionStatus
import com.stealthstream.domain.usecase.PermissionUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication interface for checking permissions and access.
 */
interface AuthenticationProvider {
    /**
     * Check if user has granted required permissions.
     */
    fun isAuthenticated(): Boolean

    /**
     * Check specific permission.
     */
    fun hasPermission(permission: String): Boolean
}

/**
 * Implementation of authentication provider.
 */
@Singleton
class AuthenticationProviderImpl @Inject constructor(
    private val permissionUseCase: PermissionUseCase
) : AuthenticationProvider {

    companion object {
        private val REQUIRED_PERMISSIONS = listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.INTERNET
        )
    }

    override fun isAuthenticated(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            permissionUseCase.checkPermission(permission) == PermissionStatus.GRANTED
        }
    }

    override fun hasPermission(permission: String): Boolean {
        return permissionUseCase.checkPermission(permission) == PermissionStatus.GRANTED
    }
}
