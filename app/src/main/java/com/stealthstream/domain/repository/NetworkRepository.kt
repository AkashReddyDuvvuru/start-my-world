package com.stealthstream.domain.repository

import com.stealthstream.domain.model.EncryptedPacket

/**
 * Repository interface for network operations.
 */
interface NetworkRepository {
    /**
     * Check if network is available.
     */
    fun isNetworkAvailable(): Boolean

    /**
     * Connect to the remote server.
     */
    suspend fun connect(host: String, port: Int): Result<Unit>

    /**
     * Disconnect from the remote server.
     */
    suspend fun disconnect(): Result<Unit>

    /**
     * Send an encrypted packet.
     */
    suspend fun sendPacket(packet: EncryptedPacket): Result<Unit>

    /**
     * Check if connected.
     */
    fun isConnected(): Boolean

    /**
     * Get current packet loss rate (0.0 to 1.0).
     */
    fun getPacketLossRate(): Float
}
