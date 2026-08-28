/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import com.mudassir131.yt.constants.AudioDspPreset
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.playback.alac.AudioFormatInfo
import com.mudassir131.yt.playback.alac.toAudioFormatInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sin

class AudioDspProcessorTest {

    private lateinit var processor: AudioDspProcessor
    private val format16BitStereo = AudioProcessor.AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
    private val format24BitStereo = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_24BIT)
    private val formatFloatStereo = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT)

    @Before
    fun setup() {
        processor = AudioDspProcessor()
    }

    @Test
    fun `01 - Processor is inactive when disabled`() {
        processor.configure(format16BitStereo)
        processor.updateConfig(enabled = false)
        assertFalse(processor.isActive)
    }

    @Test
    fun `02 - Processor becomes active when enabled`() {
        processor.configure(format16BitStereo)
        processor.updateConfig(enabled = true)
        assertTrue(processor.isActive)
    }

    @Test
    fun `03 - 16-bit PCM - Full-scale input with EQ boost never clips or overflows`() {
        processor.configure(format16BitStereo)
        processor.updateConfig(
            enabled = true,
            preset = AudioDspPreset.LOUD_CLEAN,
        )

        val frameCount = 2048
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount) {
            val sample = (32767 * sin(2.0 * Math.PI * i / 64.0)).toInt().toShort()
            inputBuffer.putShort(sample) // L
            inputBuffer.putShort(sample) // R
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val outputBuffer = processor.output

        assertNotNull(outputBuffer)
        assertTrue(outputBuffer.hasRemaining())

        var maxSample = 0
        while (outputBuffer.remaining() >= 2) {
            val sample = outputBuffer.short
            val magnitude = abs(sample.toInt())
            if (magnitude > maxSample) {
                maxSample = magnitude
            }
        }

        assertTrue("Output sample magnitude ($maxSample) must never exceed 32767", maxSample <= 32767)
    }

    @Test
    fun `04 - 24-bit PCM - Full-scale input processing and peak protection`() {
        processor.configure(format24BitStereo)
        processor.updateConfig(
            enabled = true,
            preset = AudioDspPreset.BALANCED,
        )

        val frameCount = 1024
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 6).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount) {
            val sampleInt = (8388600 * sin(2.0 * Math.PI * i / 32.0)).toInt()
            // Write 24-bit little endian
            inputBuffer.put((sampleInt and 0xFF).toByte())
            inputBuffer.put(((sampleInt shr 8) and 0xFF).toByte())
            inputBuffer.put(((sampleInt shr 16) and 0xFF).toByte())
            inputBuffer.put((sampleInt and 0xFF).toByte())
            inputBuffer.put(((sampleInt shr 8) and 0xFF).toByte())
            inputBuffer.put(((sampleInt shr 16) and 0xFF).toByte())
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val outputBuffer = processor.output

        assertNotNull(outputBuffer)
        assertTrue(outputBuffer.hasRemaining())

        var maxSample = 0
        while (outputBuffer.remaining() >= 3) {
            val b0 = outputBuffer.get().toInt() and 0xFF
            val b1 = outputBuffer.get().toInt() and 0xFF
            val b2 = outputBuffer.get().toInt()
            val sample = (b2 shl 16) or (b1 shl 8) or b0
            val magnitude = abs(sample)
            if (magnitude > maxSample) {
                maxSample = magnitude
            }
        }

        assertTrue("24-bit output magnitude ($maxSample) must not exceed 8388607", maxSample <= 8388607)
    }

    @Test
    fun `05 - Float PCM - Full-scale input processing and limiter saturation`() {
        processor.configure(formatFloatStereo)
        processor.updateConfig(
            enabled = true,
            preset = AudioDspPreset.BASS_BOOST,
        )

        val frameCount = 512
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 8).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount) {
            inputBuffer.putFloat(0.99f)
            inputBuffer.putFloat(-0.99f)
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val outputBuffer = processor.output

        assertNotNull(outputBuffer)
        var maxFloat = 0.0f
        while (outputBuffer.remaining() >= 4) {
            val s = abs(outputBuffer.float)
            if (s > maxFloat) maxFloat = s
        }

        assertTrue("Float output ($maxFloat) must be strictly <= 1.0f", maxFloat <= 1.0f)
    }

    @Test
    fun `06 - Silence handling - All zeros in produce all zeros out without noise`() {
        processor.configure(format16BitStereo)
        processor.updateConfig(enabled = true, preset = AudioDspPreset.BALANCED)

        val frameCount = 1024
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount * 2) {
            inputBuffer.putShort(0)
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val outputBuffer = processor.output

        while (outputBuffer.remaining() >= 2) {
            assertEquals(0.toShort(), outputBuffer.short)
        }
    }

    @Test
    fun `07 - Low-level signals (-60 dBFS) process stably without numerical denormals`() {
        processor.configure(formatFloatStereo)
        processor.updateConfig(enabled = true, preset = AudioDspPreset.BALANCED)

        val frameCount = 512
        val lowLevelAmplitude = 0.001f // ~ -60 dBFS
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 8).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount) {
            inputBuffer.putFloat(lowLevelAmplitude)
            inputBuffer.putFloat(lowLevelAmplitude)
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val outputBuffer = processor.output

        assertTrue(outputBuffer.hasRemaining())
        while (outputBuffer.remaining() >= 4) {
            val sample = outputBuffer.float
            assertFalse("Sample must not be NaN", sample.isNaN())
            assertFalse("Sample must not be Infinite", sample.isInfinite())
            assertTrue("Low-level signal must remain stable", abs(sample) < 0.05f)
        }
    }

    @Test
    fun `08 - Stereo imaging is preserved by stereo-linked limiting`() {
        processor.configure(formatFloatStereo)
        processor.updateConfig(enabled = true, preset = AudioDspPreset.LOUD_CLEAN)

        // Hard-panned signal: Loud on Left (0.95), quiet on Right (0.1)
        val frameCount = 256
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 8).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount) {
            inputBuffer.putFloat(0.95f)
            inputBuffer.putFloat(0.1f)
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val outputBuffer = processor.output

        while (outputBuffer.remaining() >= 8) {
            val outL = outputBuffer.float
            val outR = outputBuffer.float

            // Ratio between Left and Right must be preserved proportionally (Left is consistently louder)
            assertTrue("Left channel must remain significantly louder than Right channel", outL > outR * 3.0f)
            assertTrue("Left channel must not clip", outL <= 1.0f)
        }
    }

    @Test
    fun `09 - Quality-first Presets configure exact specified parameters`() {
        processor.configure(format16BitStereo)

        // Balanced: +2 dB bass, +1 dB clarity, +0.5 dB treble
        processor.updateConfig(enabled = true, preset = AudioDspPreset.BALANCED)
        assertEquals(2.0f, processor.config.bassGainDb, 0.01f)
        assertEquals(1.0f, processor.config.clarityGainDb, 0.01f)
        assertEquals(0.5f, processor.config.trebleGainDb, 0.01f)
        assertFalse(processor.config.loudnessEnabled)

        // Loud & Clean: +2.5 dB bass, +1.0 dB clarity, +0.5 dB treble, loudness norm active
        processor.updateConfig(enabled = true, preset = AudioDspPreset.LOUD_CLEAN)
        assertEquals(2.5f, processor.config.bassGainDb, 0.01f)
        assertEquals(1.0f, processor.config.clarityGainDb, 0.01f)
        assertEquals(0.5f, processor.config.trebleGainDb, 0.01f)
        assertTrue(processor.config.loudnessEnabled)
    }

    @Test
    fun `10 - DSP NEVER modifies source codec classification - Opus remains OPUS`() {
        val ytOpusFormat = FormatEntity(
            id = "test_track",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 158000,
            sampleRate = 48000,
            contentLength = 3700000L,
            loudnessDb = null,
            playbackUrl = "https://rr---sn-youtube.googlevideo.com/videoplayback?itag=251",
        )

        val info = ytOpusFormat.toAudioFormatInfo(decoderName = "c2.android.opus.decoder")
        assertEquals("Opus", info.codec)
        assertFalse("DSP processing must never classify Opus as lossless", info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("OPUS 158 kbps", info.qualityLabel)
    }

    @Test
    fun `11 - DSP NEVER modifies source codec classification - AAC remains AAC`() {
        val ytAacFormat = FormatEntity(
            id = "test_aac",
            itag = 140,
            mimeType = "audio/mp4",
            codecs = "mp4a.40.2",
            bitrate = 128000,
            sampleRate = 44100,
            contentLength = 3100000L,
            loudnessDb = null,
            playbackUrl = "https://rr---sn-youtube.googlevideo.com/videoplayback?itag=140",
        )

        val info = ytAacFormat.toAudioFormatInfo(decoderName = "c2.android.aac.decoder")
        assertEquals("AAC", info.codec)
        assertFalse(info.isLossless)
        assertEquals("AAC 128 kbps", info.qualityLabel)
    }

    @Test
    fun `12 - DSP NEVER modifies source codec classification - FLAC remains FLAC`() {
        val media3Format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_FLAC)
            .setCodecs("flac")
            .setSampleRate(96000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3Format,
            decoderName = "FlacDecoder",
            playbackUrl = "file:///sdcard/Music/test.flac",
        )

        assertNotNull(info)
        assertEquals("FLAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
    }

    @Test
    fun `13 - DSP NEVER modifies source codec classification - ALAC remains ALAC`() {
        val media3Format = Format.Builder()
            .setSampleMimeType("audio/alac")
            .setCodecs("alac")
            .setSampleRate(44100)
            .setChannelCount(2)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3Format,
            decoderName = "AlacDecoder",
            playbackUrl = "file:///sdcard/Music/test.m4a",
        )

        assertNotNull(info)
        assertEquals("ALAC", info!!.codec)
        assertTrue(info.isLossless)
        assertEquals("Lossless", info.qualityLabel)
    }

    @Test
    fun `14 - Dynamic Bass Management protects limiter and prevents distortion during heavy sub-bass bursts`() {
        processor.configure(formatFloatStereo)
        val profile = com.mudassir131.yt.playback.enhancement.model.EnhancementProfile(
            lowShelfDb = 5.0f,
            bassDynamicAmount = 0.8f,
            limiterCeilingDb = -1.0f,
        )
        processor.applyEnhancementProfile(profile)

        // Inject heavy sub-bass burst (40 Hz sine wave)
        val frameCount = 2048
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 8).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount) {
            val sample = sin(2.0 * Math.PI * 40.0 * i / 48000.0).toFloat() * 0.95f
            inputBuffer.putFloat(sample)
            inputBuffer.putFloat(sample)
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val outputBuffer = processor.output
        assertNotNull(outputBuffer)

        var maxSample = 0.0f
        while (outputBuffer.remaining() >= 4) {
            val s = abs(outputBuffer.float)
            if (s > maxSample) maxSample = s
        }

        // Peak output must be strictly controlled and never exceed 1.0f
        assertTrue("Output with dynamic bass ($maxSample) must remain strictly <= 1.0f", maxSample <= 1.0f)
    }

    @Test
    fun `15 - Parameter sanitization prevents NaN and out-of-range floats from crashing DSP`() {
        processor.configure(format16BitStereo)
        val maliciousProfile = com.mudassir131.yt.playback.enhancement.model.EnhancementProfile(
            preampDb = Float.NaN,
            lowShelfDb = 100.0f, // Out of safe range
            lowMidDb = Float.NEGATIVE_INFINITY,
            presenceDb = 999.0f,
            airDb = Float.POSITIVE_INFINITY,
            bassDynamicAmount = 50.0f,
            limiterCeilingDb = 10.0f,
        )

        processor.applyEnhancementProfile(maliciousProfile)

        // Verify processor clamped parameters to safe ranges
        assertTrue("Bass gain must be clamped <= 6.0", processor.config.bassGainDb <= 6.0f)
        assertTrue("Presence gain must be clamped <= 5.0", processor.config.clarityGainDb <= 5.0f)
        assertFalse("Treble gain must not be NaN", processor.config.trebleGainDb.isNaN())
        assertFalse("Preamp must not be NaN", processor.config.lowMidDb.isNaN())

        // Ensure normal processing still executes safely
        val frameCount = 256
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount * 2) {
            inputBuffer.putShort(1000)
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        val output = processor.output
        assertTrue(output.hasRemaining())
    }

    @Test
    fun `16 - PCM Tap non-blockingly forwards samples to AudioAnalyzer`() {
        val analyzer = com.mudassir131.yt.playback.enhancement.analyzer.AudioAnalyzer(44100 * 2)
        processor.audioAnalyzer = analyzer
        processor.configure(format16BitStereo)
        processor.updateConfig(enabled = true)

        val frameCount = 1024
        val inputBuffer = ByteBuffer.allocateDirect(frameCount * 4).order(ByteOrder.nativeOrder())
        for (i in 0 until frameCount * 2) {
            inputBuffer.putShort((16000 * sin(i.toDouble())).toInt().toShort())
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        assertTrue("Analyzer must receive pushed samples", analyzer.hasSufficientSamples(10L))
    }
}
