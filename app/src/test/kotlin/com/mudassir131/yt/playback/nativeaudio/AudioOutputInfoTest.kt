/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.nativeaudio

import com.mudassir131.yt.playback.alac.AudioFormatInfo
import com.mudassir131.yt.ui.component.LosslessQuality
import com.mudassir131.yt.ui.component.losslessQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioOutputInfoTest {

    @Test
    fun test16Bit441kHzBitPerfect() {
        val outInfo = AudioOutputInfo(
            sourceSampleRate = 44100,
            sourceBitDepth = 16,
            sourceChannels = 2,
            outputSampleRate = 44100,
            outputBitDepth = 16,
            outputChannels = 2,
            backend = "AAudio",
            sharingMode = "Exclusive",
            isResampled = false,
            isProcessed = false,
            isBitPerfect = true,
            routeName = "USB DAC",
            codec = "FLAC",
        )

        val formatInfo = AudioFormatInfo(
            codec = "FLAC",
            mimeType = "audio/flac",
            isLossless = true,
            isHiRes = false,
            sampleRate = 44100,
            bitDepth = 16,
            channelCount = 2,
            bitrate = null,
            contentLength = null,
            itag = null,
            source = "flac",
            decoderName = "c2.android.flac.decoder",
            outputInfo = outInfo,
        )

        assertEquals("Bit-perfect", formatInfo.qualityLabel)
        assertEquals(LosslessQuality.BIT_PERFECT, formatInfo.losslessQuality())
        assertTrue(formatInfo.isBitPerfect)
        assertFalse(outInfo.isResampled)
    }

    @Test
    fun test24Bit192kHzBitPerfect() {
        val outInfo = AudioOutputInfo(
            sourceSampleRate = 192000,
            sourceBitDepth = 24,
            sourceChannels = 2,
            outputSampleRate = 192000,
            outputBitDepth = 24,
            outputChannels = 2,
            backend = "AAudio",
            sharingMode = "Exclusive",
            isResampled = false,
            isProcessed = false,
            isBitPerfect = true,
            routeName = "USB DAC (FiiO KA13)",
            codec = "FLAC",
        )

        val formatInfo = AudioFormatInfo(
            codec = "FLAC",
            mimeType = "audio/flac",
            isLossless = true,
            isHiRes = true,
            sampleRate = 192000,
            bitDepth = 24,
            channelCount = 2,
            bitrate = null,
            contentLength = null,
            itag = null,
            source = "flac",
            decoderName = "c2.android.flac.decoder",
            outputInfo = outInfo,
        )

        assertEquals("Bit-perfect", formatInfo.qualityLabel)
        assertEquals(LosslessQuality.BIT_PERFECT, formatInfo.losslessQuality())
        assertTrue(formatInfo.isBitPerfect)
        assertEquals("24-bit / 192 kHz FLAC", outInfo.displaySourceFormat)
        assertEquals("24-bit / 192 kHz", outInfo.displayOutputFormat)
    }

    @Test
    fun test24Bit192kHzResampledTo48kHz() {
        // When device Android mixer downsamples 192 kHz FLAC to 48 kHz
        val outInfo = AudioOutputInfo(
            sourceSampleRate = 192000,
            sourceBitDepth = 24,
            sourceChannels = 2,
            outputSampleRate = 48000,
            outputBitDepth = 24,
            outputChannels = 2,
            backend = "AAudio",
            sharingMode = "Shared",
            isResampled = true,
            isProcessed = false,
            isBitPerfect = false,
            routeName = "Internal Speaker",
            codec = "FLAC",
        )

        val formatInfo = AudioFormatInfo(
            codec = "FLAC",
            mimeType = "audio/flac",
            isLossless = true,
            isHiRes = true,
            sampleRate = 192000,
            bitDepth = 24,
            channelCount = 2,
            bitrate = null,
            contentLength = null,
            itag = null,
            source = "flac",
            decoderName = "c2.android.flac.decoder",
            outputInfo = outInfo,
        )

        // Must NOT claim bit-perfect!
        assertFalse(formatInfo.isBitPerfect)
        assertEquals("Resampled (48 kHz)", formatInfo.qualityLabel)
        assertEquals("24-bit / 192 kHz FLAC", outInfo.displaySourceFormat)
        assertEquals("24-bit / 48 kHz", outInfo.displayOutputFormat)
        assertEquals("Resampled (48 kHz)", outInfo.statusLabel)
    }

    @Test
    fun test24Bit96kHzWithDspActive() {
        // When DSP (EQ / Normalization) is active, cannot claim bit-perfect
        val outInfo = AudioOutputInfo(
            sourceSampleRate = 96000,
            sourceBitDepth = 24,
            sourceChannels = 2,
            outputSampleRate = 96000,
            outputBitDepth = 24,
            outputChannels = 2,
            backend = "AAudio",
            sharingMode = "Exclusive",
            isResampled = false,
            isProcessed = true,
            isBitPerfect = false,
            routeName = "USB DAC",
            codec = "FLAC",
        )

        val formatInfo = AudioFormatInfo(
            codec = "FLAC",
            mimeType = "audio/flac",
            isLossless = true,
            isHiRes = true,
            sampleRate = 96000,
            bitDepth = 24,
            channelCount = 2,
            bitrate = null,
            contentLength = null,
            itag = null,
            source = "flac",
            decoderName = "c2.android.flac.decoder",
            outputInfo = outInfo,
        )

        assertFalse(formatInfo.isBitPerfect)
        assertEquals("Hi-Res Lossless", formatInfo.qualityLabel)
        assertEquals("Processed (DSP)", outInfo.statusLabel)
    }
}
