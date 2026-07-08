package com.stealthstream.domain.repository

import com.stealthstream.domain.model.CameraFrame
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for camera operations.
 */
interface CameraRepository {
    /**
     * Check if camera permission is granted.
     */
    fun isCameraPermissionGranted(): Boolean

    /**
     * Open the camera and start capturing frames.
     */
    suspend fun openCamera(width: Int = 1280, height: Int = 720): Result<Unit>

    /**
     * Close the camera.
     */
    suspend fun closeCamera(): Result<Unit>

    /**
     * Stream of camera frames.
     */
    fun getFrameStream(): Flow<CameraFrame>

    /**
     * Check if camera is currently open.
     */
    fun isCameraOpen(): Boolean
}
