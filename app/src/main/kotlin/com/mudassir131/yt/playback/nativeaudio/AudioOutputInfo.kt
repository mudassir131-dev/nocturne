/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.nativeaudio

/**
 * Encapsulates real-time source vs hardware output audio metrics.
 * Strictly separates what the source audio claims from what the hardware DAC actually receives.
 */
data class AudioOutputInfo(
    val sourceSampleRate: Int,
    val sourceBitDepth: Int,
    val sourceChannels: Int,
    val outputSampleRate: Int,
    val outputBitDepth: Int,
    val outputChannels: Int,
    val backend: String, // "AAudio", "OpenSL ES", "AudioTrack"
    val sharingMode: String, // "Exclusive", "Shared"
    val isResampled: Boolean,
    val isProcessed: Boolean,
    val isBitPerfect: Boolean,
    val routeName: String? = null,
    val codec: String = "FLAC",
) {
    val isHiRes: Boolean
        get() = (sourceSampleRate > 48000 || sourceBitDepth > 16)

    val displaySourceFormat: String
        get() = "${sourceBitDepth}-bit / ${formatSampleRate(sourceSampleRate)} $codec"

    val displayOutputFormat: String
        get() = "${outputBitDepth}-bit / ${formatSampleRate(outputSampleRate)}"

    val statusLabel: String
        get() = when {
            isBitPerfect -> "Bit-perfect"
            isResampled -> "Resampled (${formatSampleRate(outputSampleRate)})"
            isProcessed -> "Processed (DSP)"
            isHiRes -> "Hi-Res Lossless"
            else -> "Lossless"
        }

    companion object {
        fun formatSampleRate(rate: Int): String {
            return when {
                rate <= 0 -> "44.1 kHz"
                rate % 1000 == 0 -> "${rate / 1000} kHz"
                else -> String.format(java.util.Locale.US, "%.1f kHz", rate / 1000.0)
            }
        }
    }
}
