/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.enhancement.analyzer

import com.mudassir131.yt.playback.enhancement.model.AudioCharacteristics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Real-Time Audio Spectral and Dynamics Analyzer.
 *
 * Designed specifically for mobile Android audio engines:
 * - Lock-free, preallocated PCM ring buffer to accept samples from the realtime audio thread.
 * - Zero memory allocations in the audio callback path.
 * - Background mathematical feature extraction on [Dispatchers.Default].
 * - Calculates RMS, Peak, Crest Factor, Dynamic Range, Spectral Centroid, Rolloff,
 *   Multi-band Energy ratios (Sub-bass, Bass, Low-Mid, Mid, Presence, Air),
 *   Clipping %, Compression Index, and Transient Density.
 */
class AudioAnalyzer(
    private val bufferCapacitySamples: Int = 44100 * 8, // 8 seconds of audio buffer at 44.1kHz
) {
    // Lock-free ring buffer for PCM samples
    private val ringBuffer = FloatArray(bufferCapacitySamples)
    private val writeIndex = AtomicInteger(0)
    private val totalSamplesWritten = AtomicInteger(0)

    @Volatile
    private var currentSampleRate: Int = 44100

    @Volatile
    private var currentChannelCount: Int = 2

    // Reusable buffers for analysis (reused in background coroutine only)
    private val fftSize = 2048
    private val halfFft = fftSize / 2
    private val windowBuffer = FloatArray(fftSize)
    private val realBuffer = DoubleArray(fftSize)
    private val imagBuffer = DoubleArray(fftSize)
    private val magnitudeSpectrum = FloatArray(halfFft)
    private val hannWindow = FloatArray(fftSize) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
    }

    /**
     * Called by the real-time audio thread.
     * Must be non-blocking, lock-free, and allocate NO memory.
     */
    fun pushPcmSamples(samples: FloatArray, length: Int, sampleRate: Int, channels: Int) {
        if (length <= 0) return
        currentSampleRate = if (sampleRate > 0) sampleRate else 44100
        currentChannelCount = if (channels > 0) channels else 2

        var writePos = writeIndex.get()
        val capacity = bufferCapacitySamples

        for (i in 0 until length) {
            ringBuffer[writePos] = samples[i]
            writePos++
            if (writePos >= capacity) {
                writePos = 0
            }
        }

        writeIndex.set(writePos)
        totalSamplesWritten.addAndGet(length)
    }

    /**
     * Resets the ring buffer state when starting a new track.
     */
    fun reset() {
        writeIndex.set(0)
        totalSamplesWritten.set(0)
    }

    /**
     * Returns true if enough samples have accumulated for reliable initial analysis.
     */
    fun hasSufficientSamples(minDurationMs: Long = 2500L): Boolean {
        val requiredSamples = ((currentSampleRate * currentChannelCount) * (minDurationMs / 1000.0)).toInt()
        return totalSamplesWritten.get() >= requiredSamples
    }

    /**
     * Extracts compact audio characteristics over the recent audio window.
     * Executes asynchronously on [Dispatchers.Default] without blocking audio playback.
     */
    suspend fun analyzeSnapshot(windowDurationMs: Long = 3000L): AudioCharacteristics = withContext(Dispatchers.Default) {
        val sampleRate = currentSampleRate
        val channels = currentChannelCount
        val sampleCountToRead = min(
            bufferCapacitySamples,
            ((sampleRate * channels) * (windowDurationMs / 1000.0)).toInt()
        ).coerceAtLeast(fftSize * channels)

        val localSnapshot = FloatArray(sampleCountToRead)
        val currentWrite = writeIndex.get()
        val capacity = bufferCapacitySamples

        // Copy backwards from recent write head
        var readHead = (currentWrite - sampleCountToRead) % capacity
        if (readHead < 0) readHead += capacity

        for (i in 0 until sampleCountToRead) {
            localSnapshot[i] = ringBuffer[readHead]
            readHead++
            if (readHead >= capacity) readHead = 0
        }

        return@withContext computeCharacteristicsFromBuffer(localSnapshot, sampleRate, channels)
    }

    /**
     * Computes full audio characteristics from a mono/stereo PCM sample slice.
     */
    fun computeCharacteristicsFromBuffer(
        samples: FloatArray,
        sampleRate: Int,
        channels: Int,
    ): AudioCharacteristics {
        if (samples.isEmpty()) return AudioCharacteristics()

        val monoLength = samples.size / channels
        if (monoLength < 256) return AudioCharacteristics()

        // 1. Convert to mono & Compute RMS / Peak / Clipping
        var sumSquares = 0.0
        var peakAbs = 0.0f
        var clippedSampleCount = 0
        var prevAbs = 0.0f
        var flatTopCount = 0

        val monoSamples = FloatArray(monoLength)
        var monoIdx = 0
        var i = 0
        while (i < samples.size && monoIdx < monoLength) {
            var monoVal = 0.0f
            for (ch in 0 until channels) {
                monoVal += samples[i + ch]
            }
            monoVal /= channels.toFloat()
            monoSamples[monoIdx++] = monoVal

            val absVal = abs(monoVal)
            if (absVal > peakAbs) peakAbs = absVal
            sumSquares += (monoVal * monoVal).toDouble()

            // Detect digital clipping near 0 dBFS or flat tops
            if (absVal >= 0.985f) {
                clippedSampleCount++
                if (abs(absVal - prevAbs) < 0.0001f) {
                    flatTopCount++
                }
            }
            prevAbs = absVal

            i += channels
        }

        val rms = sqrt(sumSquares / monoLength).toFloat().coerceAtLeast(1e-6f)
        val rmsDb = (20.0 * log10(rms.toDouble())).toFloat().coerceIn(-96.0f, 0.0f)
        val peakDb = (20.0 * log10(peakAbs.coerceAtLeast(1e-6f).toDouble())).toFloat().coerceIn(-96.0f, 0.0f)
        val crestFactorDb = (peakDb - rmsDb).coerceAtLeast(0.0f)

        // Estimated Clipping ratio
        val clippingRatio = ((clippedSampleCount + flatTopCount * 2).toFloat() / monoLength).coerceIn(0.0f, 1.0f)

        // Dynamic Range estimate
        val dynamicRangeDb = (crestFactorDb * 1.5f + (peakDb + 24.0f).coerceAtLeast(0.0f) * 0.5f).coerceIn(4.0f, 40.0f)

        // Estimated Compression index (low crest factor + high RMS = heavily compressed)
        val compressionEstimate = when {
            crestFactorDb < 6.0f -> 0.85f + (6.0f - crestFactorDb) * 0.025f
            crestFactorDb < 10.0f -> 0.50f + (10.0f - crestFactorDb) * 0.08f
            crestFactorDb < 14.0f -> 0.25f + (14.0f - crestFactorDb) * 0.06f
            else -> (20.0f - crestFactorDb).coerceAtLeast(0.0f) / 40.0f
        }.coerceIn(0.0f, 1.0f)

        // 2. Frequency Spectral Analysis via Windowed FFT Frames
        val frameStep = fftSize / 2
        var frameCount = 0
        var totalSpectralCentroid = 0.0
        var totalSpectralRolloff = 0.0
        var prevFrameMagnitudes: FloatArray? = null
        var totalSpectralFlux = 0.0f
        var transientCount = 0

        // Sub-band energy accumulators
        var sumSubBass = 0.0     // 20-60 Hz
        var sumBass = 0.0        // 60-250 Hz
        var sumLowMid = 0.0      // 250-500 Hz
        var sumMid = 0.0         // 500-2000 Hz
        var sumPresence = 0.0    // 2000-6000 Hz
        var sumHigh = 0.0        // 6000-20000 Hz
        var sumAir = 0.0         // 10000-20000 Hz
        var sumTotalSpectrum = 0.0

        val freqBinResolution = sampleRate.toDouble() / fftSize

        var frameStart = 0
        while (frameStart + fftSize <= monoLength) {
            // Apply Hann window
            for (k in 0 until fftSize) {
                val s = monoSamples[frameStart + k] * hannWindow[k]
                realBuffer[k] = s.toDouble()
                imagBuffer[k] = 0.0
            }

            // Perform in-place Cooley-Tukey Radix-2 FFT
            computeFft(realBuffer, imagBuffer, fftSize)

            // Compute magnitude spectrum
            var frameTotalEnergy = 0.0
            var frameWeightedFreqSum = 0.0

            for (k in 0 until halfFft) {
                val r = realBuffer[k]
                val im = imagBuffer[k]
                val mag = sqrt(r * r + im * im).toFloat()
                magnitudeSpectrum[k] = mag
                val energy = mag * mag
                frameTotalEnergy += energy

                val freq = k * freqBinResolution
                frameWeightedFreqSum += freq * energy

                // Accumulate multi-band energies
                when {
                    freq in 20.0..60.0 -> sumSubBass += energy
                    freq in 60.0..250.0 -> sumBass += energy
                    freq in 250.0..500.0 -> sumLowMid += energy
                    freq in 500.0..2000.0 -> sumMid += energy
                    freq in 2000.0..6000.0 -> sumPresence += energy
                    freq in 6000.0..20000.0 -> sumHigh += energy
                }
                if (freq in 10000.0..20000.0) {
                    sumAir += energy
                }
                sumTotalSpectrum += energy
            }

            // Spectral Centroid for this frame
            if (frameTotalEnergy > 1e-8) {
                val frameCentroid = frameWeightedFreqSum / frameTotalEnergy
                totalSpectralCentroid += frameCentroid

                // Spectral Rolloff (85% power frequency)
                val targetRolloffEnergy = frameTotalEnergy * 0.85
                var cumulativeEnergy = 0.0
                var rolloffFreq = 10000.0
                for (k in 0 until halfFft) {
                    cumulativeEnergy += magnitudeSpectrum[k] * magnitudeSpectrum[k]
                    if (cumulativeEnergy >= targetRolloffEnergy) {
                        rolloffFreq = k * freqBinResolution
                        break
                    }
                }
                totalSpectralRolloff += rolloffFreq
            }

            // Spectral Flux & Transient Detection
            if (prevFrameMagnitudes != null) {
                var flux = 0.0f
                for (k in 0 until halfFft) {
                    val diff = magnitudeSpectrum[k] - prevFrameMagnitudes[k]
                    if (diff > 0.0f) {
                        flux += diff
                    }
                }
                totalSpectralFlux += flux
                if (flux > 8.0f) {
                    transientCount++
                }
            }

            if (prevFrameMagnitudes == null) {
                prevFrameMagnitudes = FloatArray(halfFft)
            }
            System.arraycopy(magnitudeSpectrum, 0, prevFrameMagnitudes, 0, halfFft)

            frameCount++
            frameStart += frameStep
        }

        val effectiveFrameCount = max(1, frameCount)
        val avgCentroid = (totalSpectralCentroid / effectiveFrameCount).toFloat().coerceIn(100.0f, 20000.0f)
        val avgRolloff = (totalSpectralRolloff / effectiveFrameCount).toFloat().coerceIn(500.0f, 20000.0f)
        val avgFlux = totalSpectralFlux / effectiveFrameCount

        val totalTimeSec = monoLength.toFloat() / sampleRate
        val transientDensity = (transientCount.toFloat() / totalTimeSec.coerceAtLeast(0.1f)).coerceIn(0.0f, 20.0f)

        // Normalize band energies to ratios
        val grandTotal = max(1e-8, sumTotalSpectrum)
        val subBassRatio = (sumSubBass / grandTotal).toFloat().coerceIn(0.0f, 1.0f)
        val bassRatio = (sumBass / grandTotal).toFloat().coerceIn(0.0f, 1.0f)
        val lowMidRatio = (sumLowMid / grandTotal).toFloat().coerceIn(0.0f, 1.0f)
        val midRatio = (sumMid / grandTotal).toFloat().coerceIn(0.0f, 1.0f)
        val presenceRatio = (sumPresence / grandTotal).toFloat().coerceIn(0.0f, 1.0f)
        val highRatio = (sumHigh / grandTotal).toFloat().coerceIn(0.0f, 1.0f)
        val airRatio = (sumAir / grandTotal).toFloat().coerceIn(0.0f, 1.0f)

        return AudioCharacteristics(
            rmsDb = rmsDb,
            peakDb = peakDb,
            crestFactorDb = crestFactorDb,
            dynamicRangeDb = dynamicRangeDb,
            spectralCentroidHz = avgCentroid,
            spectralRolloffHz = avgRolloff,
            spectralFlux = avgFlux,
            subBassEnergy = subBassRatio,
            bassEnergy = bassRatio,
            lowMidEnergy = lowMidRatio,
            midEnergy = midRatio,
            presenceEnergy = presenceRatio,
            highEnergy = highRatio,
            airEnergy = airRatio,
            estimatedClipping = clippingRatio,
            estimatedCompression = compressionEstimate,
            transientDensity = transientDensity,
            sampleRate = sampleRate,
            channelCount = channels,
        )
    }

    /**
     * In-place Cooley-Tukey Radix-2 Fast Fourier Transform.
     */
    private fun computeFft(real: DoubleArray, imag: DoubleArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR
                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Butterfly computations
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (k in 0 until halfLen) {
                    val pos = i + k
                    val partner = pos + halfLen

                    val tr = wR * real[partner] - wI * imag[partner]
                    val ti = wR * imag[partner] + wI * real[partner]

                    real[partner] = real[pos] - tr
                    imag[partner] = imag[pos] - ti
                    real[pos] += tr
                    imag[pos] += ti

                    val nextWr = wR * wStepR - wI * wStepI
                    val nextWi = wR * wStepI + wI * wStepR
                    wR = nextWr
                    wI = nextWi
                }
                i += len
            }
            len = len shl 1
        }
    }
}
