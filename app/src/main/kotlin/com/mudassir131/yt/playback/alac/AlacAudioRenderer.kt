/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.decoder.CryptoConfig
import androidx.media3.exoplayer.RendererCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DecoderAudioRenderer

/**
 * Media3 DecoderAudioRenderer extension that routes ALAC (Apple Lossless Audio Codec)
 * streams through the native ALAC decoder into PCM output for AudioSink.
 */
@UnstableApi
class AlacAudioRenderer(
    eventHandler: Handler? = null,
    eventListener: AudioRendererEventListener? = null,
    private val audioSink: AudioSink,
) : DecoderAudioRenderer<AlacDecoder>(
    eventHandler,
    eventListener,
    audioSink,
) {
    override fun getName(): String = TAG

    override fun supportsFormatInternal(format: Format): Int {
        val mimeType = format.sampleMimeType
        if (MimeTypes.AUDIO_ALAC.equals(mimeType, ignoreCase = true) ||
            "audio/alac".equals(mimeType, ignoreCase = true)
        ) {
            val pcmEncoding = when (format.pcmEncoding) {
                C.ENCODING_PCM_24BIT -> C.ENCODING_PCM_24BIT
                C.ENCODING_PCM_32BIT -> C.ENCODING_PCM_32BIT
                else -> C.ENCODING_PCM_16BIT
            }
            return if (!audioSink.supportsFormat(
                    Format.Builder()
                        .setSampleMimeType(MimeTypes.AUDIO_RAW)
                        .setChannelCount(format.channelCount.coerceAtLeast(2))
                        .setSampleRate(format.sampleRate.coerceAtLeast(44100))
                        .setPcmEncoding(pcmEncoding)
                        .build()
                )
            ) {
                RendererCapabilities.create(C.FORMAT_UNSUPPORTED_SUBTYPE)
            } else {
                RendererCapabilities.create(C.FORMAT_HANDLED)
            }
        }
        return RendererCapabilities.create(C.FORMAT_UNSUPPORTED_TYPE)
    }

    override fun createDecoder(format: Format, mediaCrypto: CryptoConfig?): AlacDecoder {
        val config = if (format.initializationData.isNotEmpty()) {
            AlacConfig.parse(format.initializationData[0])
        } else {
            AlacConfig(
                frameLength = 4096,
                compatibleVersion = 0,
                bitDepth = 16,
                pb = 40,
                mb = 10,
                kb = 14,
                numChannels = format.channelCount.takeIf { it > 0 } ?: 2,
                maxRun = 255,
                maxFrameBytes = 0,
                avgBitRate = format.bitrate.takeIf { it > 0 } ?: 0,
                sampleRate = format.sampleRate.takeIf { it > 0 } ?: 44100,
            )
        }
        return AlacDecoder(config)
    }

    override fun getOutputFormat(decoder: AlacDecoder): Format {
        return Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_RAW)
            .setChannelCount(decoder.config.numChannels)
            .setSampleRate(decoder.config.sampleRate)
            .setPcmEncoding(decoder.pcmEncoding)
            .build()
    }

    companion object {
        private const val TAG = "AlacAudioRenderer"
    }
}
