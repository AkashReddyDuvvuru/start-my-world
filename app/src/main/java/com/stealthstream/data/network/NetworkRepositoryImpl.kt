package com.stealthstream.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.stealthstream.domain.model.EncryptedPacket
import com.stealthstream.domain.repository.NetworkRepository
import com.stealthstream.util.BinaryUtil
import com.stealthstream.util.SecureLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Implementation of network repository using UDP sockets.
 */
@Singleton
class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkRepository {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var socket: DatagramSocket? = null
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0
    private var packetsSent: Long = 0
    private var packetsLost: Long = 0
    private var isConnected = false

    companion object {
        private const val MAX_DATAGRAM_SIZE = 1200  // UDP payload limit (MTU-safe)
    }

    override fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override suspend fun connect(
        host: String,
        port: Int
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (isConnected) {
                throw IllegalStateException("Already connected")
            }

            if (!isNetworkAvailable()) {
                throw RuntimeException("Network is not available")
            }

            remoteAddress = InetAddress.getByName(host)
            remotePort = port
            socket = DatagramSocket()
            isConnected = true
            packetsSent = 0
            packetsLost = 0

            SecureLogger.info(
                "NetworkRepository",
                "Connected to $host:$port"
            )
        }
    }

    override suspend fun disconnect(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            socket?.close()
            socket = null
            remoteAddress = null
            isConnected = false
            SecureLogger.info("NetworkRepository", "Disconnected")
        }
    }

    override suspend fun sendPacket(packet: EncryptedPacket): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!isConnected) {
                    throw IllegalStateException("Not connected")
                }

                val socket = socket ?: throw RuntimeException("Socket is null")
                val address = remoteAddress ?: throw RuntimeException("Remote address is null")

                // Build packet: [frame_seq (8B)] [nonce (24B)] [ciphertext] [tag (16B)]
                val frameSeqBytes = BinaryUtil.encodeLong(packet.frameSequence)
                val payload = BinaryUtil.concat(
                    frameSeqBytes,
                    packet.nonce,
                    packet.ciphertext,
                    packet.tag
                )

                if (payload.size > MAX_DATAGRAM_SIZE) {
                    throw RuntimeException(
                        "Packet too large: ${payload.size} > $MAX_DATAGRAM_SIZE"
                    )
                }

                val datagramPacket = DatagramPacket(
                    payload,
                    payload.size,
                    address,
                    remotePort
                )

                try {
                    socket.send(datagramPacket)
                    packetsSent++
                    SecureLogger.debug(
                        "NetworkRepository",
                        "Packet sent: ${payload.size} bytes, seq=${packet.frameSequence}"
                    )
                } catch (e: Exception) {
                    packetsLost++
                    SecureLogger.error(
                        "NetworkRepository",
                        "Failed to send packet",
                        e
                    )
                    throw e
                }
            }
        }

    override fun isConnected(): Boolean = isConnected

    override fun getPacketLossRate(): Float {
        if (packetsSent == 0L) return 0f
        return (packetsLost.toFloat() / (packetsSent + packetsLost))
            .coerceIn(0f, 1f)
    }
}
