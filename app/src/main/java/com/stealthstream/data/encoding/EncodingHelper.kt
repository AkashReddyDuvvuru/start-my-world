package com.stealthstream.data.encoding

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.stealthstream.util.SecureLogger

/**
 * Helper for H.265 video encoding configuration.
 */
object EncodingHelper {

    const val MIME_TYPE_H265 = "video/hevc"
    const val MIME_TYPE_H264 = "video/avc"
    const val DEFAULT_BITRATE_KBPS = 500
    const val DEFAULT_FRAME_RATE = 30

    /**
     * Create H.265 encoder.
     */
    fun createH265Encoder(
        width: Int,
        height: Int,
        bitRateKbps: Int = DEFAULT_BITRATE_KBPS,
        frameRate: Int = DEFAULT_FRAME_RATE
    ): Result<MediaCodec> = runCatching {
        val format = MediaFormat.createVideoFormat(MIME_TYPE_H265, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)  // Keyframe every 1 second
        }

        val codec = MediaCodec.createEncoderByType(MIME_TYPE_H265)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        SecureLogger.debug(
            "EncodingHelper",
            "H.265 encoder created: $width x $height @ ${frameRate}fps, ${bitRateKbps}kbps"
        )
        codec
    }

    /**
     * Create H.264 encoder (fallback if H.265 not available).
     */
    fun createH264Encoder(
        width: Int,
        height: Int,
        bitRateKbps: Int = DEFAULT_BITRATE_KBPS,
        frameRate: Int = DEFAULT_FRAME_RATE
    ): Result<MediaCodec> = runCatching {
        val format = MediaFormat.createVideoFormat(MIME_TYPE_H264, width, height).apply {
            setInteger(MediaFormat.KEY_BIT_RATE, bitRateKbps * 1000)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val codec = MediaCodec.createEncoderByType(MIME_TYPE_H264)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        SecureLogger.debug(
            "EncodingHelper",
            "H.264 encoder created: $width x $height @ ${frameRate}fps, ${bitRateKbps}kbps"
        )
        codec
    }
}
