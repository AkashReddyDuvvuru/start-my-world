package com.stealthstream.util

import java.nio.ByteBuffer

/**
 * Utilities for binary data handling.
 */
object BinaryUtil {

    /**
     * Encode 64-bit integer as big-endian bytes.
     */
    fun encodeLong(value: Long): ByteArray {
        val bytes = ByteArray(8)
        for (i in 0 until 8) {
            bytes[i] = ((value shr (56 - i * 8)) and 0xFF).toByte()
        }
        return bytes
    }

    /**
     * Decode 64-bit integer from big-endian bytes.
     */
    fun decodeLong(bytes: ByteArray): Long {
        require(bytes.size >= 8) { "Bytes must be at least 8 bytes" }
        var value = 0L
        for (i in 0 until 8) {
            value = (value shl 8) or (bytes[i].toInt() and 0xFF).toLong()
        }
        return value
    }

    /**
     * Concatenate byte arrays.
     */
    fun concat(vararg arrays: ByteArray): ByteArray {
        val totalSize = arrays.sumOf { it.size }
        val result = ByteArray(totalSize)
        var offset = 0
        for (array in arrays) {
            System.arraycopy(array, 0, result, offset, array.size)
            offset += array.size
        }
        return result
    }

    /**
     * Slice a portion of byte array.
     */
    fun slice(data: ByteArray, start: Int, length: Int): ByteArray {
        return data.copyOfRange(start, start + length)
    }
}
