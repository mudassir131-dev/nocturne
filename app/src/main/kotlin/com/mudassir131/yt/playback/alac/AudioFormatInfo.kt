/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import com.mudassir131.yt.db.entities.FormatEntity

/**
 * Single canonical source of truth for audio stream/playback quality and format details.
 */
data class AudioFormatInfo(
    val codec: String, // e.g. "ALAC", "Opus", "AAC", "FLAC", "MP3"
    val mimeType: String?,
    val isLossless: Boolean,
    val isHiRes: Boolean,
    val sampleRate: Int?,
    val bitDepth: Int?,
    val channelCount: Int?,
    val bitrate: Int?, // in bps, or null if unavailable/dynamic lossless
    val contentLength: Long?,
    val itag: Int?,
    val source: String?, // e.g. "saavn", "youtube", "local", "alac"
    val decoderName: String?,
    val outputInfo: com.mudassir131.yt.playback.nativeaudio.AudioOutputInfo? = null,
) {
    val isBitPerfect: Boolean
        get() = outputInfo?.isBitPerfect == true

    val qualityLabel: String
        get() {
            if (outputInfo != null) {
                if (outputInfo.isBitPerfect) {
                    return "Bit-perfect"
                }
                if (isLossless) {
                    return if (outputInfo.isResampled) {
                        "Resampled (${com.mudassir131.yt.playback.nativeaudio.AudioOutputInfo.formatSampleRate(outputInfo.outputSampleRate)})"
                    } else if (isHiRes) {
                        "Hi-Res Lossless"
                    } else {
                        "Lossless"
                    }
                }
            }
            if (isLossless) {
                return if (isHiRes) "Hi-Res Lossless" else "Lossless"
            }
            val sourcePrefix = when {
                source?.contains("saavn", ignoreCase = true) == true -> "SAAVN"
                else -> codec.uppercase()
            }
            val kbps = bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" }
            return listOfNotNull(sourcePrefix, kbps).joinToString(" ")
        }

    val displayCodec: String
        get() = codec.uppercase()

    val displayBitDepth: String?
        get() = bitDepth?.takeIf { it > 0 }?.let { "$it-bit" }

    val displaySampleRate: String?
        get() = sampleRate?.takeIf { it > 0 }?.let { "$it Hz" }

    val displayBitrate: String?
        get() = when {
            bitrate != null && bitrate > 0 -> "${bitrate / 1000} Kbps"
            isLossless -> "Lossless (Variable)"
            else -> null
        }

    companion object {
        fun resolve(
            media3Format: Format? = null,
            decoderName: String? = null,
            formatEntity: FormatEntity? = null,
            playbackUrl: String? = null,
        ): AudioFormatInfo? {
            if (media3Format == null && formatEntity == null) return null

            val rawMime = media3Format?.sampleMimeType
                ?: media3Format?.containerMimeType
                ?: formatEntity?.mimeType.orEmpty()

            val rawCodecs = media3Format?.codecs
                ?: formatEntity?.codecs.orEmpty()

            val resolvedUrl = playbackUrl ?: formatEntity?.playbackUrl.orEmpty()

            // 1. Check for ALAC (Apple Lossless)
            val isAacCodec = rawCodecs.contains("mp4a", ignoreCase = true) ||
                    rawCodecs.contains("aac", ignoreCase = true) ||
                    decoderName?.contains("aac", ignoreCase = true) == true

            val isAlac = !isAacCodec && (
                    rawMime.contains("alac", ignoreCase = true) ||
                    rawMime.contains(MimeTypes.AUDIO_ALAC, ignoreCase = true) ||
                    rawCodecs.contains("alac", ignoreCase = true) ||
                    decoderName?.contains("Alac", ignoreCase = true) == true
            )

            if (isAlac) {
                // Parse ALAC specific config if available
                var alacConfig: AlacConfig? = null
                if (media3Format != null && media3Format.initializationData.isNotEmpty()) {
                    alacConfig = runCatching { AlacConfig.parse(media3Format.initializationData[0]) }.getOrNull()
                }

                val sampleRate = media3Format?.sampleRate?.takeIf { it > 0 }
                    ?: alacConfig?.sampleRate
                    ?: formatEntity?.sampleRate?.takeIf { it > 0 }
                    ?: 44100

                val bitDepth = alacConfig?.bitDepth
                    ?: when (media3Format?.pcmEncoding) {
                        C.ENCODING_PCM_24BIT -> 24
                        C.ENCODING_PCM_32BIT -> 32
                        C.ENCODING_PCM_16BIT -> 16
                        else -> {
                            if (rawCodecs.contains("24") || sampleRate > 48000) 24 else 16
                        }
                    }

                val channelCount = media3Format?.channelCount?.takeIf { it > 0 }
                    ?: alacConfig?.numChannels
                    ?: 2

                // Genuine bitrate if known for ALAC, otherwise null (never show fake/stale Opus bitrate)
                val bitrate = alacConfig?.avgBitRate?.takeIf { it > 0 }
                    ?: media3Format?.bitrate?.takeIf { it > 0 && it > 320000 }
                    ?: formatEntity?.bitrate?.takeIf { it > 320000 && formatEntity.codecs.contains("alac", ignoreCase = true) }

                val isHiRes = sampleRate > 48000 || bitDepth > 16

                return AudioFormatInfo(
                    codec = "ALAC",
                    mimeType = "audio/alac",
                    isLossless = true,
                    isHiRes = isHiRes,
                    sampleRate = sampleRate,
                    bitDepth = bitDepth,
                    channelCount = channelCount,
                    bitrate = bitrate,
                    contentLength = formatEntity?.contentLength?.takeIf { it > 0 },
                    itag = formatEntity?.itag,
                    source = if (resolvedUrl.contains("saavn", ignoreCase = true)) "saavn" else "alac",
                    decoderName = decoderName ?: "AlacDecoder",
                )
            }

            // 2. Check for FLAC
            val isFlac = rawCodecs.contains("flac", ignoreCase = true) ||
                    rawMime.contains("flac", ignoreCase = true) ||
                    rawMime.contains(MimeTypes.AUDIO_FLAC, ignoreCase = true) ||
                    decoderName?.contains("Flac", ignoreCase = true) == true

            if (isFlac) {
                val sampleRate = media3Format?.sampleRate?.takeIf { it > 0 }
                    ?: formatEntity?.sampleRate?.takeIf { it > 0 }
                    ?: 44100
                val bitDepth = when (media3Format?.pcmEncoding) {
                    C.ENCODING_PCM_24BIT -> 24
                    C.ENCODING_PCM_32BIT -> 32
                    else -> if (rawCodecs.contains("24") || sampleRate > 48000) 24 else 16
                }
                val isHiRes = sampleRate > 48000 || bitDepth > 16
                return AudioFormatInfo(
                    codec = "FLAC",
                    mimeType = "audio/flac",
                    isLossless = true,
                    isHiRes = isHiRes,
                    sampleRate = sampleRate,
                    bitDepth = bitDepth,
                    channelCount = media3Format?.channelCount?.takeIf { it > 0 } ?: 2,
                    bitrate = media3Format?.bitrate?.takeIf { it > 0 },
                    contentLength = formatEntity?.contentLength?.takeIf { it > 0 },
                    itag = formatEntity?.itag,
                    source = if (resolvedUrl.contains("saavn", ignoreCase = true)) "saavn" else "flac",
                    decoderName = decoderName ?: "FlacDecoder",
                )
            }

            // 3. Check for AAC / MP4A
            val isAac = rawCodecs.contains("mp4a", ignoreCase = true) ||
                    rawCodecs.contains("aac", ignoreCase = true) ||
                    rawMime.contains("aac", ignoreCase = true) ||
                    (rawMime.contains("mp4", ignoreCase = true) && !rawCodecs.contains("opus", ignoreCase = true))

            if (isAac) {
                val sampleRate = media3Format?.sampleRate?.takeIf { it > 0 }
                    ?: formatEntity?.sampleRate?.takeIf { it > 0 }
                    ?: 44100
                val bitrate = media3Format?.bitrate?.takeIf { it > 0 }
                    ?: formatEntity?.bitrate?.takeIf { it > 0 }
                    ?: 160000

                return AudioFormatInfo(
                    codec = "AAC",
                    mimeType = "audio/mp4",
                    isLossless = false,
                    isHiRes = false,
                    sampleRate = sampleRate,
                    bitDepth = 16,
                    channelCount = media3Format?.channelCount?.takeIf { it > 0 } ?: 2,
                    bitrate = bitrate,
                    contentLength = formatEntity?.contentLength?.takeIf { it > 0 },
                    itag = formatEntity?.itag,
                    source = if (resolvedUrl.contains("saavn", ignoreCase = true)) "saavn" else "youtube",
                    decoderName = decoderName ?: "c2.android.aac.decoder",
                )
            }

            // 4. Check for Opus
            val isOpus = rawCodecs.contains("opus", ignoreCase = true) ||
                    rawMime.contains("opus", ignoreCase = true) ||
                    rawMime.contains("webm", ignoreCase = true)

            if (isOpus) {
                val sampleRate = media3Format?.sampleRate?.takeIf { it > 0 }
                    ?: formatEntity?.sampleRate?.takeIf { it > 0 }
                    ?: 48000
                val bitrate = media3Format?.bitrate?.takeIf { it > 0 }
                    ?: formatEntity?.bitrate?.takeIf { it > 0 }
                    ?: 160000

                return AudioFormatInfo(
                    codec = "Opus",
                    mimeType = "audio/webm",
                    isLossless = false,
                    isHiRes = false,
                    sampleRate = sampleRate,
                    bitDepth = 16,
                    channelCount = media3Format?.channelCount?.takeIf { it > 0 } ?: 2,
                    bitrate = bitrate,
                    contentLength = formatEntity?.contentLength?.takeIf { it > 0 },
                    itag = formatEntity?.itag,
                    source = if (resolvedUrl.contains("saavn", ignoreCase = true)) "saavn" else "youtube",
                    decoderName = decoderName ?: "c2.android.opus.decoder",
                )
            }

            // 5. Fallback for other formats
            val detectedCodec = rawCodecs.ifBlank { rawMime.substringAfter("/").ifBlank { "Audio" } }
            return AudioFormatInfo(
                codec = detectedCodec,
                mimeType = rawMime.ifBlank { null },
                isLossless = false,
                isHiRes = false,
                sampleRate = media3Format?.sampleRate ?: formatEntity?.sampleRate,
                bitDepth = 16,
                channelCount = media3Format?.channelCount ?: 2,
                bitrate = media3Format?.bitrate?.takeIf { it > 0 } ?: formatEntity?.bitrate?.takeIf { it > 0 },
                contentLength = formatEntity?.contentLength?.takeIf { it > 0 },
                itag = formatEntity?.itag,
                source = if (resolvedUrl.contains("saavn", ignoreCase = true)) "saavn" else "unknown",
                decoderName = decoderName,
            )
        }
    }
}

/**
 * Convert FormatEntity to AudioFormatInfo directly.
 */
fun FormatEntity.toAudioFormatInfo(
    media3Format: Format? = null,
    decoderName: String? = null,
): AudioFormatInfo {
    return AudioFormatInfo.resolve(
        media3Format = media3Format,
        decoderName = decoderName,
        formatEntity = this,
        playbackUrl = playbackUrl,
    ) ?: AudioFormatInfo(
        codec = codecs.ifBlank { "Audio" },
        mimeType = mimeType,
        isLossless = codecs.contains("alac", ignoreCase = true) || codecs.contains("flac", ignoreCase = true),
        isHiRes = (sampleRate != null && sampleRate > 48000) || codecs.contains("24"),
        sampleRate = sampleRate,
        bitDepth = if (codecs.contains("24") || (sampleRate != null && sampleRate > 48000)) 24 else 16,
        channelCount = 2,
        bitrate = bitrate.takeIf { it > 0 },
        contentLength = contentLength.takeIf { it > 0 },
        itag = itag,
        source = playbackUrl,
        decoderName = decoderName,
    )
}
