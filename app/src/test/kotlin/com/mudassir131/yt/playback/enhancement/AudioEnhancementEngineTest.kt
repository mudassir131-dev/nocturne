/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.enhancement

import com.mudassir131.yt.playback.enhancement.ai.LocalFallbackEnhancementProvider
import com.mudassir131.yt.playback.enhancement.analyzer.AudioAnalyzer
import com.mudassir131.yt.playback.enhancement.model.AudioCharacteristics
import com.mudassir131.yt.playback.enhancement.model.EnhancementMode
import com.mudassir131.yt.playback.enhancement.model.EnhancementProfile
import com.mudassir131.yt.playback.enhancement.model.ProfileSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class AudioEnhancementEngineTest {

    private lateinit var analyzer: AudioAnalyzer
    private lateinit var localFallbackProvider: LocalFallbackEnhancementProvider

    @Before
    fun setup() {
        analyzer = AudioAnalyzer(44100 * 4)
        localFallbackProvider = LocalFallbackEnhancementProvider()
    }

    @Test
    fun `01 - AudioAnalyzer accurately extracts spectral and dynamic features from synthetic sine`() {
        val sampleRate = 44100
        val channels = 2
        val durationSec = 1.0
        val sampleCount = (sampleRate * channels * durationSec).toInt()

        // 1 kHz pure sine wave at -6 dBFS (amplitude 0.5)
        val samples = FloatArray(sampleCount) { i ->
            val frame = i / channels
            (0.5 * sin(2.0 * PI * 1000.0 * frame / sampleRate)).toFloat()
        }

        val characteristics = analyzer.computeCharacteristicsFromBuffer(samples, sampleRate, channels)

        assertNotNull(characteristics)
        assertEquals(-9.0f, characteristics.rmsDb, 2.0f) // Sine RMS = Peak - 3dB = -6 - 3 = -9 dBFS
        assertEquals(-6.0f, characteristics.peakDb, 1.0f) // Peak = -6 dBFS
        assertEquals(3.0f, characteristics.crestFactorDb, 1.5f) // Sine crest factor = ~3.0 dB
        assertTrue("Spectral centroid around 1000 Hz", characteristics.spectralCentroidHz in 700.0f..1500.0f)
        assertEquals(0.0f, characteristics.estimatedClipping, 0.01f) // No clipping in 0.5 amplitude
    }

    @Test
    fun `02 - AudioAnalyzer detects digital clipping on clipped waveforms`() {
        val sampleRate = 44100
        val channels = 2
        val sampleCount = sampleRate * channels

        // Heavily clipped signal (square wave amplitude 1.0)
        val samples = FloatArray(sampleCount) { i ->
            val frame = i / channels
            if (sin(2.0 * PI * 200.0 * frame / sampleRate) >= 0.0) 1.0f else -1.0f
        }

        val characteristics = analyzer.computeCharacteristicsFromBuffer(samples, sampleRate, channels)

        assertTrue("Clipping ratio must be > 50% on square/clipped wave", characteristics.estimatedClipping > 0.50f)
    }

    @Test
    fun `03 - LocalFallbackProvider adapts DSP intelligently for bass-heavy source`() = runBlocking {
        val bassHeavyCharacteristics = AudioCharacteristics(
            subBassEnergy = 0.35f, // Heavy sub-bass
            bassEnergy = 0.40f,
            lowMidEnergy = 0.25f,
            highEnergy = 0.05f,
            crestFactorDb = 6.0f,  // Low crest factor
        )

        val result = localFallbackProvider.optimizeProfile(
            characteristics = bassHeavyCharacteristics,
            trackTitle = "Heavy Bass Track",
            trackArtist = "Artist",
            mode = EnhancementMode.HI_RES_FEEL,
            apiKey = "",
        )

        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()

        // Dynamic bass amount must be boosted to protect from sub-bass boom
        assertTrue("Dynamic bass factor must be active (>= 0.35)", profile.bassDynamicAmount >= 0.35f)
        // Static low-shelf boost must be restrained
        assertTrue("Static low shelf must not exceed 2.0 dB on bass-heavy track", profile.lowShelfDb <= 2.0f)
        assertEquals(ProfileSource.LOCAL_FALLBACK, profile.source)
    }

    @Test
    fun `04 - LocalFallbackProvider restores clarity and air on dull dark audio`() = runBlocking {
        val darkCharacteristics = AudioCharacteristics(
            spectralCentroidHz = 1500.0f, // Dull sound
            highEnergy = 0.03f,          // Low high-frequency energy
            presenceEnergy = 0.05f,
            subBassEnergy = 0.10f,
        )

        val result = localFallbackProvider.optimizeProfile(
            characteristics = darkCharacteristics,
            trackTitle = "Muffled Vintage Recording",
            trackArtist = "Artist",
            mode = EnhancementMode.CLEAR,
            apiKey = "",
        )

        assertTrue(result.isSuccess)
        val profile = result.getOrThrow()

        // Presence and air should be gently boosted to open up the mix
        assertTrue("Presence boost must be active (> 1.0 dB)", profile.presenceDb > 1.0f)
        assertTrue("Air boost must be active (> 1.0 dB)", profile.airDb > 1.0f)
        assertTrue("Preamp must compensate for boost to prevent clipping", profile.preampDb < 0.0f)
    }

    @Test
    fun `05 - EnhancementProfile sanitize prevents negative and extreme values`() {
        val dirty = EnhancementProfile(
            preampDb = 5.0f, // Positive preamp not allowed
            lowShelfDb = 25.0f,
            lowMidDb = -50.0f,
            presenceDb = 99.0f,
            airDb = -100.0f,
            bassDynamicAmount = -5.0f,
            harmonicAmount = 1.0f,
            stereoAmount = 5.0f,
            limiterCeilingDb = 10.0f,
            confidence = Float.NaN,
        )

        val clean = dirty.sanitize()

        assertTrue("Preamp must be <= 0.0", clean.preampDb <= 0.0f)
        assertTrue("Low shelf must be clamped in [-6, 6]", clean.lowShelfDb in -6.0f..6.0f)
        assertTrue("Low mid must be clamped in [-6, 3]", clean.lowMidDb in -6.0f..3.0f)
        assertTrue("Presence must be clamped in [-3, 5]", clean.presenceDb in -3.0f..5.0f)
        assertTrue("Air must be clamped in [-4, 5]", clean.airDb in -4.0f..5.0f)
        assertTrue("Harmonic must be clamped in [0, 0.3]", clean.harmonicAmount in 0.0f..0.30f)
        assertTrue("Stereo must be clamped in [0, 0.3]", clean.stereoAmount in 0.0f..0.30f)
        assertTrue("Limiter ceiling must be clamped in [-3, -0.1]", clean.limiterCeilingDb in -3.0f..-0.1f)
        assertFalse("Confidence must not be NaN", clean.confidence.isNaN())
    }

    @Test
    fun `06 - Quality modes produce appropriate default profiles`() {
        val natural = EnhancementProfile.forMode(EnhancementMode.NATURAL)
        val clear = EnhancementProfile.forMode(EnhancementMode.CLEAR)
        val hiRes = EnhancementProfile.forMode(EnhancementMode.HI_RES_FEEL)
        val studio = EnhancementProfile.forMode(EnhancementMode.STUDIO)

        // Clear mode should have stronger presence boost than Natural
        assertTrue(clear.presenceDb > natural.presenceDb)
        // Hi-Res Feel should have subtle harmonic enrichment
        assertTrue(hiRes.harmonicAmount > natural.harmonicAmount)
        // Studio should have robust dynamic bass control
        assertTrue(studio.bassDynamicAmount >= 0.30f)
    }
}
