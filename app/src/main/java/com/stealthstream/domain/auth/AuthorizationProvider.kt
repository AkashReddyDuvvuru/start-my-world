package com.stealthstream.domain.auth

import com.stealthstream.domain.model.PermissionStatus
import com.stealthstream.domain.usecase.PermissionUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authorization interface for checking access control.
 */
interface AuthorizationProvider {
    /**
     * Check if user is authorized to start streaming.
     */
    fun canStartStreaming(): Boolean

    /**
     * Check if user is authorized to access camera.
     */
    fun canAccessCamera(): Boolean

    /**
     * Check if user is authorized to access network.
     */
    fun canAccessNetwork(): Boolean
}

/**
 * Implementation of authorization provider.
 */
@Singleton
class AuthorizationProviderImpl @Inject constructor(
    private val permissionUseCase: PermissionUseCase
) : AuthorizationProvider {

    override fun canStartStreaming(): Boolean {
        return canAccessCamera() && canAccessNetwork()
    }

    override fun canAccessCamera(): Boolean {
        return permissionUseCase.checkPermission(
            android.Manifest.permission.CAMERA
        ) == PermissionStatus.GRANTED
    }

    override fun canAccessNetwork(): Boolean {
        return permissionUseCase.checkPermission(
            android.Manifest.permission.INTERNET
        ) == PermissionStatus.GRANTED
    }
}
