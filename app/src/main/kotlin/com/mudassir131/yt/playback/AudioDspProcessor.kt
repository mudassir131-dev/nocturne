/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import com.mudassir131.yt.constants.AudioDspPreset
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * High-Quality Audio DSP AudioProcessor for Media3.
 *
 * Implements:
 * 1. Dynamic Pre-Gain / Headroom management to prevent digital clipping before EQ boosts
 * 2. Transposed Direct Form II Biquad Filters with denormal protection
 * 3. Parametric Bass Enhancement (Low-Shelf @ 90 Hz, Q=0.707 - deep sub-bass & punch, no vocal mud)
 * 4. Vocal / Presence Clarity Enhancement (Peaking @ 3000 Hz, Q=1.0 - crisp vocals, no sibilance)
 * 5. Treble Detail Enhancement (High-Shelf @ 10000 Hz, Q=0.707 - airy detail, no harshness)
 * 6. Stereo-Linked Soft-Knee Limiter (Prevents clipping & preserves stereo imaging)
 * 7. Conservative Loudness Normalization with auto-peak headroom scaling
 * 8. Comprehensive PCM support: 16-bit PCM, 24-bit PCM, 32-bit Float PCM
 *
 * CRITICAL LOSSLESS & CODEC INTEGRITY:
 * This processor operates purely on post-decoded PCM audio samples.
 * It NEVER modifies format metadata, codec tags, MIME types, sample rate, bit depth, or lossless classification.
 */
@UnstableApi
class AudioDspProcessor : AudioProcessor {

    data class DspConfig(
        val enabled: Boolean = false,
        val preset: AudioDspPreset = AudioDspPreset.BALANCED,
        val bassGainDb: Float = 2.0f,       // Range: -6.0 dB to +6.0 dB
        val clarityGainDb: Float = 1.0f,    // Range: -3.0 dB to +4.0 dB
        val trebleGainDb: Float = 0.5f,     // Range: -4.0 dB to +4.0 dB
        val loudnessEnabled: Boolean = false,
    )

    private var inputFormat = AudioFormat.NOT_SET
    private var outputFormat = AudioFormat.NOT_SET

    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    @Volatile
    var config: DspConfig = DspConfig()
        set(value) {
            field = value
            recomputeFilters()
        }

    // Filter states per channel (Stereo/Mono/Multichannel)
    private var biquadBass = arrayOf(BiquadFilter(), BiquadFilter())
    private var biquadClarity = arrayOf(BiquadFilter(), BiquadFilter())
    private var biquadTreble = arrayOf(BiquadFilter(), BiquadFilter())

    // Dynamic pre-gain factor (linear)
    @Volatile
    private var preGainFactor: Float = 1.0f

    // Limiter threshold (linear, -0.2 dBFS = 0.9772)
    private val limiterThreshold = 0.9772f

    fun updateConfig(
        enabled: Boolean,
        preset: AudioDspPreset = AudioDspPreset.BALANCED,
        bassGainDb: Float = 2.0f,
        clarityGainDb: Float = 1.0f,
        trebleGainDb: Float = 0.5f,
        loudnessEnabled: Boolean = false,
    ) {
        val (effectiveBass, effectiveClarity, effectiveTreble, effectiveLoudness) = when (preset) {
            AudioDspPreset.PURE -> Quad(0.0f, 0.0f, 0.0f, false)
            AudioDspPreset.BALANCED -> Quad(2.0f, 1.0f, 0.5f, false)
            AudioDspPreset.BASS_BOOST -> Quad(4.0f, 0.5f, 0.0f, false)
            AudioDspPreset.VOCAL -> Quad(0.0f, 2.5f, 1.0f, false)
            AudioDspPreset.LOUD_CLEAN -> Quad(2.5f, 1.0f, 0.5f, true)
            AudioDspPreset.CUSTOM -> Quad(bassGainDb, clarityGainDb, trebleGainDb, loudnessEnabled)
        }

        config = DspConfig(
            enabled = enabled,
            preset = preset,
            bassGainDb = effectiveBass.coerceIn(-6.0f, 6.0f),
            clarityGainDb = effectiveClarity.coerceIn(-3.0f, 4.0f),
            trebleGainDb = effectiveTreble.coerceIn(-4.0f, 4.0f),
            loudnessEnabled = effectiveLoudness,
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    private fun recomputeFilters() {
        val sampleRate = if (inputFormat.sampleRate > 0) inputFormat.sampleRate.toDouble() else 44100.0

        if (!config.enabled) {
            preGainFactor = 1.0f
            return
        }

        // 1. Calculate Headroom / Negative Pre-Gain to prevent clipping
        val maxBoostDb = max(0.0f, max(config.bassGainDb, max(config.clarityGainDb, config.trebleGainDb)))
        val headroomDb = if (config.loudnessEnabled) {
            // In Loud & Clean, conservative makeup gain with dynamic limiter safety
            -maxBoostDb + 1.2f
        } else {
            -maxBoostDb
        }
        preGainFactor = 10.0f.pow(headroomDb / 20.0f)

        // 2. Recompute Bass Filter (Low Shelf @ 90 Hz, Q = 0.707)
        val bassCoeffs = BiquadFilter.calculateLowShelf(
            sampleRate = sampleRate,
            cutoffHz = 90.0,
            gainDb = config.bassGainDb.toDouble(),
            q = 0.707,
        )

        // 3. Recompute Clarity / Vocal Filter (Peaking @ 3000 Hz, Q = 1.0)
        val clarityCoeffs = BiquadFilter.calculatePeaking(
            sampleRate = sampleRate,
            centerHz = 3000.0,
            gainDb = config.clarityGainDb.toDouble(),
            q = 1.0,
        )

        // 4. Recompute Treble Filter (High Shelf @ 10000 Hz, Q = 0.707)
        val trebleCoeffs = BiquadFilter.calculateHighShelf(
            sampleRate = sampleRate,
            cutoffHz = 10000.0,
            gainDb = config.trebleGainDb.toDouble(),
            q = 0.707,
        )

        for (ch in biquadBass.indices) {
            biquadBass[ch].setCoefficients(bassCoeffs)
            biquadClarity[ch].setCoefficients(clarityCoeffs)
            biquadTreble[ch].setCoefficients(trebleCoeffs)
        }

        Timber.tag("AudioDsp").d("[DSP] Recomputed filters: preset=${config.preset}, bass=${config.bassGainDb}dB, clarity=${config.clarityGainDb}dB, treble=${config.trebleGainDb}dB, preGain=${headroomDb}dB (factor=$preGainFactor)")
    }

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_24BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }

        inputFormat = inputAudioFormat
        outputFormat = inputAudioFormat

        // Ensure filter channel count is allocated
        val channelCount = inputAudioFormat.channelCount.coerceAtLeast(1)
        if (biquadBass.size < channelCount) {
            biquadBass = Array(channelCount) { BiquadFilter() }
            biquadClarity = Array(channelCount) { BiquadFilter() }
            biquadTreble = Array(channelCount) { BiquadFilter() }
        }

        recomputeFilters()
        return outputFormat
    }

    override fun isActive(): Boolean {
        return config.enabled && inputFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        if (!config.enabled) {
            // Passthrough without processing
            val directBuffer = replaceOutputBuffer(remaining)
            directBuffer.put(inputBuffer)
            directBuffer.flip()
            outputBuffer = directBuffer
            return
        }

        val out = replaceOutputBuffer(remaining)
        val channelCount = inputFormat.channelCount.coerceAtLeast(1)
        val gain = preGainFactor
        val bassFilters = biquadBass
        val clarityFilters = biquadClarity
        val trebleFilters = biquadTreble

        when (inputFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                val frames = remaining / (2 * channelCount)
                val tempSamples = FloatArray(channelCount)

                for (f in 0 until frames) {
                    var maxPeakInFrame = 0.0f

                    for (ch in 0 until channelCount) {
                        val filterIndex = min(ch, bassFilters.size - 1)
                        val sampleShort = inputBuffer.short
                        var sample = (sampleShort / 32768.0f) * gain

                        // Apply Filter Pipeline
                        sample = bassFilters[filterIndex].process(sample.toDouble()).toFloat()
                        sample = clarityFilters[filterIndex].process(sample.toDouble()).toFloat()
                        sample = trebleFilters[filterIndex].process(sample.toDouble()).toFloat()

                        tempSamples[ch] = sample
                        val peak = abs(sample)
                        if (peak > maxPeakInFrame) maxPeakInFrame = peak
                    }

                    // Stereo-Linked Limiting (preserves stereo imaging across all channels)
                    val limiterScale = computeStereoLimiterScale(maxPeakInFrame)

                    for (ch in 0 until channelCount) {
                        val limited = tempSamples[ch] * limiterScale
                        val clamped = (limited * 32767.0f).coerceIn(-32768.0f, 32767.0f).toInt().toShort()
                        out.putShort(clamped)
                    }
                }
            }

            C.ENCODING_PCM_24BIT -> {
                val frames = remaining / (3 * channelCount)
                val tempSamples = FloatArray(channelCount)

                for (f in 0 until frames) {
                    var maxPeakInFrame = 0.0f

                    for (ch in 0 until channelCount) {
                        val filterIndex = min(ch, bassFilters.size - 1)
                        val b0 = inputBuffer.get().toInt() and 0xFF
                        val b1 = inputBuffer.get().toInt() and 0xFF
                        val b2 = inputBuffer.get().toInt()
                        val sampleInt = (b2 shl 16) or (b1 shl 8) or b0
                        var sample = (sampleInt / 8388608.0f) * gain

                        sample = bassFilters[filterIndex].process(sample.toDouble()).toFloat()
                        sample = clarityFilters[filterIndex].process(sample.toDouble()).toFloat()
                        sample = trebleFilters[filterIndex].process(sample.toDouble()).toFloat()

                        tempSamples[ch] = sample
                        val peak = abs(sample)
                        if (peak > maxPeakInFrame) maxPeakInFrame = peak
                    }

                    val limiterScale = computeStereoLimiterScale(maxPeakInFrame)

                    for (ch in 0 until channelCount) {
                        val limited = tempSamples[ch] * limiterScale
                        val clamped = (limited * 8388607.0f).coerceIn(-8388608.0f, 8388607.0f).toInt()
                        out.put((clamped and 0xFF).toByte())
                        out.put(((clamped shr 8) and 0xFF).toByte())
                        out.put(((clamped shr 16) and 0xFF).toByte())
                    }
                }
            }

            C.ENCODING_PCM_FLOAT -> {
                val frames = remaining / (4 * channelCount)
                val tempSamples = FloatArray(channelCount)

                for (f in 0 until frames) {
                    var maxPeakInFrame = 0.0f

                    for (ch in 0 until channelCount) {
                        val filterIndex = min(ch, bassFilters.size - 1)
                        var sample = inputBuffer.float * gain

                        sample = bassFilters[filterIndex].process(sample.toDouble()).toFloat()
                        sample = clarityFilters[filterIndex].process(sample.toDouble()).toFloat()
                        sample = trebleFilters[filterIndex].process(sample.toDouble()).toFloat()

                        tempSamples[ch] = sample
                        val peak = abs(sample)
                        if (peak > maxPeakInFrame) maxPeakInFrame = peak
                    }

                    val limiterScale = computeStereoLimiterScale(maxPeakInFrame)

                    for (ch in 0 until channelCount) {
                        val limited = (tempSamples[ch] * limiterScale).coerceIn(-1.0f, 1.0f)
                        out.putFloat(limited)
                    }
                }
            }

            else -> {
                out.put(inputBuffer)
            }
        }

        out.flip()
        outputBuffer = out
    }

    /**
     * Stereo-linked soft-knee anti-clipping limiter scaling factor.
     * When max peak in frame exceeds threshold, scales down all channels uniformly
     * to preserve the spatial stereo image without distortion or pumping.
     */
    private fun computeStereoLimiterScale(maxPeak: Float): Float {
        if (maxPeak <= limiterThreshold) {
            return 1.0f
        }
        val margin = 1.0f - limiterThreshold
        val compressedPeak = limiterThreshold + margin * tanh((maxPeak - limiterThreshold) / margin)
        return min(1.0f, compressedPeak / maxPeak)
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer == AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        for (f in biquadBass) f.reset()
        for (f in biquadClarity) f.reset()
        for (f in biquadTreble) f.reset()
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
    }

    private fun replaceOutputBuffer(size: Int): ByteBuffer {
        if (buffer.capacity() < size) {
            buffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }
        return buffer
    }

    /**
     * Transposed Direct Form II Biquad Filter with denormal flushing.
     */
    class BiquadFilter {
        private var a1 = 0.0
        private var a2 = 0.0
        private var b0 = 1.0
        private var b1 = 0.0
        private var b2 = 0.0

        private var s1 = 0.0
        private var s2 = 0.0

        data class Coefficients(
            val b0: Double, val b1: Double, val b2: Double,
            val a1: Double, val a2: Double,
        )

        fun setCoefficients(coeffs: Coefficients) {
            this.b0 = coeffs.b0
            this.b1 = coeffs.b1
            this.b2 = coeffs.b2
            this.a1 = coeffs.a1
            this.a2 = coeffs.a2
        }

        fun process(x: Double): Double {
            val y = b0 * x + s1
            s1 = b1 * x - a1 * y + s2
            s2 = b2 * x - a2 * y

            // Anti-denormal flushing for silence/low-level numerical stability
            if (abs(s1) < 1e-15) s1 = 0.0
            if (abs(s2) < 1e-15) s2 = 0.0

            return y
        }

        fun reset() {
            s1 = 0.0
            s2 = 0.0
        }

        companion object {
            fun calculateLowShelf(sampleRate: Double, cutoffHz: Double, gainDb: Double, q: Double): Coefficients {
                val a = 10.0.pow(gainDb / 40.0)
                val w0 = 2.0 * PI * (cutoffHz / sampleRate)
                val alpha = sin(w0) / (2.0 * q)
                val cosW0 = cos(w0)
                val sqrtA = sqrt(a)

                val a0 = (a + 1.0) + (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                val b0 = a * ((a + 1.0) - (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha) / a0
                val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0) / a0
                val b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha) / a0
                val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0) / a0
                val a2 = ((a + 1.0) + (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha) / a0

                return Coefficients(b0, b1, b2, a1, a2)
            }

            fun calculatePeaking(sampleRate: Double, centerHz: Double, gainDb: Double, q: Double): Coefficients {
                val a = 10.0.pow(gainDb / 40.0)
                val w0 = 2.0 * PI * (centerHz / sampleRate)
                val alpha = sin(w0) / (2.0 * q)
                val cosW0 = cos(w0)

                val a0 = 1.0 + alpha / a
                val b0 = (1.0 + alpha * a) / a0
                val b1 = (-2.0 * cosW0) / a0
                val b2 = (1.0 - alpha * a) / a0
                val a1 = (-2.0 * cosW0) / a0
                val a2 = (1.0 - alpha / a) / a0

                return Coefficients(b0, b1, b2, a1, a2)
            }

            fun calculateHighShelf(sampleRate: Double, cutoffHz: Double, gainDb: Double, q: Double): Coefficients {
                val a = 10.0.pow(gainDb / 40.0)
                val w0 = 2.0 * PI * (cutoffHz / sampleRate)
                val alpha = sin(w0) / (2.0 * q)
                val cosW0 = cos(w0)
                val sqrtA = sqrt(a)

                val a0 = (a + 1.0) - (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha
                val b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + 2.0 * sqrtA * alpha) / a0
                val b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0) / a0
                val b2 = a * ((a + 1.0) + (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha) / a0
                val a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0) / a0
                val a2 = ((a + 1.0) - (a - 1.0) * cosW0 - 2.0 * sqrtA * alpha) / a0

                return Coefficients(b0, b1, b2, a1, a2)
            }
        }
    }
}
