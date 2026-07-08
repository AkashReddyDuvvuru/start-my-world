package com.stealthstream.data.camera

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import androidx.core.content.ContextCompat
import com.stealthstream.domain.model.CameraFrame
import com.stealthstream.domain.repository.CameraRepository
import com.stealthstream.util.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of camera repository using Camera2 API.
 */
@Singleton
class CameraRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CameraRepository {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val frameFlow = MutableSharedFlow<CameraFrame>()
    private var cameraDevice: CameraDevice? = null
    private var imageReader: ImageReader? = null
    private var captureSession: CameraCaptureSession? = null
    private var frameNumber = 0L
    private var isCameraOpen = false

    override fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun openCamera(
        width: Int,
        height: Int
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            if (isCameraOpen) {
                throw IllegalStateException("Camera already open")
            }

            if (!isCameraPermissionGranted()) {
                throw SecurityException("Camera permission not granted")
            }

            val cameraId = getCameraId() ?: throw RuntimeException("No camera device found")

            // Create ImageReader for frame capture
            imageReader = ImageReader.newInstance(
                width,
                height,
                android.graphics.ImageFormat.YUV_420_888,
                2  // Max images in queue
            ).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireNextImage() ?: return@setOnImageAvailableListener
                    try {
                        val frame = convertImageToFrame(image)
                        frameFlow.tryEmit(frame)
                    } catch (e: Exception) {
                        SecureLogger.error("CameraRepository", "Failed to convert image", e)
                    } finally {
                        image.close()
                    }
                }, null)
            }

            // Open camera device
            withContext(Dispatchers.Main) {
                cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        cameraDevice = camera
                        startCaptureSession()
                    }

                    override fun onDisconnected(camera: CameraDevice) {
                        cameraDevice = null
                        isCameraOpen = false
                    }

                    override fun onError(camera: CameraDevice, error: Int) {
                        SecureLogger.error(
                            "CameraRepository",
                            "Camera device error: $error"
                        )
                        isCameraOpen = false
                    }
                }, null)
            }

            isCameraOpen = true
            SecureLogger.debug("CameraRepository", "Camera opened: $width x $height")
        }
    }

    override suspend fun closeCamera(): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            isCameraOpen = false
            frameNumber = 0L
            SecureLogger.debug("CameraRepository", "Camera closed")
        }
    }

    override fun getFrameStream(): Flow<CameraFrame> = frameFlow.asSharedFlow()

    override fun isCameraOpen(): Boolean = isCameraOpen

    private fun getCameraId(): String? {
        val cameraIds = cameraManager.cameraIdList
        for (cameraId in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId
            }
        }
        return cameraIds.firstOrNull()
    }

    private fun startCaptureSession() {
        val device = cameraDevice ?: return
        val reader = imageReader ?: return
        val surface = Surface(reader.surfaceTexture)

        try {
            val captureRequestBuilder = device.createCaptureRequest(
                CameraDevice.TEMPLATE_RECORD
            ).apply {
                addTarget(surface)
                set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
            }

            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        try {
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                null
                            )
                            SecureLogger.debug(
                                "CameraRepository",
                                "Capture session started"
                            )
                        } catch (e: Exception) {
                            SecureLogger.error(
                                "CameraRepository",
                                "Failed to start capture",
                                e
                            )
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        SecureLogger.error(
                            "CameraRepository",
                            "Capture session configuration failed"
                        )
                    }
                },
                null
            )
        } catch (e: Exception) {
            SecureLogger.error(
                "CameraRepository",
                "Failed to start capture session",
                e
            )
        }
    }

    private fun convertImageToFrame(image: Image): CameraFrame {
        val planes = image.planes
        val pixelStride = planes[0].pixelStride
        val ySize = planes[0].buffer.remaining()
        val uvSize = planes[1].buffer.remaining()
        val nv21 = ByteArray(ySize + uvSize)

        planes[0].buffer.get(nv21, 0, ySize)
        planes[1].buffer.get(nv21, ySize, uvSize)

        return CameraFrame(
            frameNumber = frameNumber++,
            timestamp = image.timestamp,
            width = image.width,
            height = image.height,
            data = nv21,
            isKeyFrame = false
        )
    }
}
