package com.stealthstream.api

import com.stealthstream.domain.coordinator.StreamingCoordinator
import com.stealthstream.domain.model.CameraFrame
import com.stealthstream.domain.model.EncryptedPacket
import com.stealthstream.domain.repository.CryptoRepository
import com.stealthstream.domain.repository.NetworkRepository
import com.stealthstream.util.SecureLogger
import com.stealthstream.util.BinaryUtil
import com.stealthstream.util.Validators
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encryption and transmission pipeline.
 */
interface EncryptionPipeline {
    /**
     * Process frames: encode -> encrypt -> send.
     */
    fun processFrames(frameFlow: Flow<CameraFrame>): Flow<EncryptedPacket>
}

/**
 * Implementation of encryption pipeline.
 */
@Singleton
class EncryptionPipelineImpl @Inject constructor(
    private val cryptoRepository: CryptoRepository,
    private val networkRepository: NetworkRepository
) : EncryptionPipeline {

    companion object {
        private const val TAG = "EncryptionPipeline"
    }

    private var keyPtr = 0L
    private var frameSequence = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun processFrames(frameFlow: Flow<CameraFrame>): Flow<EncryptedPacket> {
        return frameFlow
            .catch { error ->
                SecureLogger.error(TAG, "Error in frame flow", error)
            }
            .filterNotNull()
            .also { flow ->
                scope.launch {
                    // Initialize key if needed
                    if (keyPtr == 0L) {
                        keyPtr = cryptoRepository.generateKey()
                        SecureLogger.debug(TAG, "Encryption key initialized")
                    }

                    // Process each frame
                    flow.collect { frame ->
                        try {
                            val encryptedPacket = encryptFrame(frame)
                            if (encryptedPacket != null) {
                                networkRepository.sendPacket(encryptedPacket)
                            }
                        } catch (e: Exception) {
                            SecureLogger.error(TAG, "Failed to process frame", e)
                        }
                    }
                }
            }
    }

    private fun encryptFrame(frame: CameraFrame): EncryptedPacket? {
        if (keyPtr == 0L) return null

        // Generate nonce
        val nonce = cryptoRepository.generateNonce()
        Validators.validateNonce(nonce).onFailure {
            SecureLogger.error(TAG, "Invalid nonce", it)
            return null
        }

        // Create AAD with frame sequence
        val aad = BinaryUtil.encodeLong(frameSequence)

        // Encrypt frame data
        val ciphertext = cryptoRepository.encryptFrame(
            plaintext = frame.data,
            keyPtr = keyPtr,
            nonce = nonce,
            aad = aad
        ) ?: return null

        // Extract tag (last 16 bytes)
        val tag = ciphertext.takeLast(16).toByteArray()
        val ciphertextOnly = ciphertext.dropLast(16).toByteArray()

        Validators.validateTag(tag).onFailure {
            SecureLogger.error(TAG, "Invalid tag", it)
            return null
        }

        val packet = EncryptedPacket(
            frameSequence = frameSequence++,
            nonce = nonce,
            ciphertext = ciphertextOnly,
            tag = tag,
            timestamp = System.currentTimeMillis()
        )

        SecureLogger.debug(
            TAG,
            "Frame encrypted: seq=${ frameSequence - 1}, size=${ciphertextOnly.size}"
        )

        return packet
    }

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        if (keyPtr != 0L) {
            cryptoRepository.zeroizeKey(keyPtr)
            keyPtr = 0L
        }
        scope.cancel()
    }
}
