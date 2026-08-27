/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.C
import androidx.media3.common.Format
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.ui.component.losslessQuality
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class AlacPlaybackIntegrationTest {

    @Test
    fun `LosslessStreamResolver resolves ALAC stream when provider is available`() = runBlocking {
        LosslessStreamResolver.enablePreflightValidation = false
        // Register mock lossless provider
        val mockProvider = object : LosslessAudioProvider {
            override val name: String = "TestAlacProvider"
            override suspend fun resolve(
                videoId: String,
                title: String,
                artist: String,
                durationSeconds: Int,
            ): ResolvedLosslessStream? {
                if (videoId == "OsfAnsMY21M" || title.contains("Levitating", ignoreCase = true)) {
                    return ResolvedLosslessStream(
                        url = "https://lossless.example.com/audio/alac/levitating.m4a",
                        mimeType = "audio/mp4",
                        codec = "alac",
                        bitDepth = 24,
                        sampleRate = 48000,
                        channels = 2,
                        bitrate = 1536000,
                        durationSeconds = 203,
                        sourceName = "test_alac",
                    )
                }
                return null
            }
        }

        LosslessStreamResolver.registerProvider(mockProvider)

        val result = LosslessStreamResolver.resolve(
            videoId = "OsfAnsMY21M",
            title = "Levitating",
            artist = "Dua Lipa",
            durationSeconds = 203,
        )

        assertNotNull("Expected lossless ALAC stream to be resolved", result)
        assertEquals("audio/mp4; codecs=\"alac\"", result!!.format.mimeType)
        assertEquals("https://lossless.example.com/audio/alac/levitating.m4a", result.streamUrl)

        // Verify that Media3 format and AudioFormatInfo correctly identify it as ALAC Hi-Res Lossless
        val media3Format = Format.Builder()
            .setSampleMimeType("audio/alac")
            .setCodecs("alac")
            .setSampleRate(48000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .build()

        val formatInfo = AudioFormatInfo.resolve(
            media3Format = media3Format,
            formatEntity = null,
            decoderName = "AlacDecoder",
            playbackUrl = result.streamUrl,
        )

        assertNotNull(formatInfo)
        val info = formatInfo!!
        assertEquals("ALAC", info.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals(24, info.bitDepth)
        assertEquals(48000, info.sampleRate)
        assertEquals("AlacDecoder", info.decoderName)
        assertEquals("Hi-Res Lossless", info.losslessQuality()?.label)

        LosslessStreamResolver.unregisterProvider(mockProvider.name)
    }

    @Test
    fun `Opus itag 251 is correctly resolved as Opus lossy format`() {
        val media3Format = Format.Builder()
            .setSampleMimeType("audio/webm")
            .setCodecs("opus")
            .setSampleRate(48000)
            .setChannelCount(2)
            .setAverageBitrate(134000)
            .build()

        val formatEntity = FormatEntity(
            id = "OsfAnsMY21M",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 134000,
            sampleRate = 48000,
            contentLength = 3300000,
            loudnessDb = 1.1,
            playbackUrl = "https://rr---sn-youtube.googlevideo.com/videoplayback",
        )

        val formatInfo = AudioFormatInfo.resolve(
            media3Format = media3Format,
            formatEntity = formatEntity,
            decoderName = "c2.android.opus.decoder",
            playbackUrl = formatEntity.playbackUrl,
        )

        assertNotNull(formatInfo)
        val info = formatInfo!!
        assertEquals("Opus", info.codec)
        assertFalse(info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals(16, info.bitDepth)
        assertEquals(48000, info.sampleRate)
        assertEquals("c2.android.opus.decoder", info.decoderName)
        assertEquals(251, info.itag)
        assertNull(info.losslessQuality())
    }

    @Test
    fun `AlacDecoder decodes PCM frames correctly`() {
        val config = AlacConfig(
            frameLength = 16,
            compatibleVersion = 0,
            bitDepth = 16,
            pb = 40,
            mb = 10,
            kb = 14,
            numChannels = 2,
            maxRun = 255,
            maxFrameBytes = 0,
            avgBitRate = 0,
            sampleRate = 44100,
        )
        val decoder = AlacDecoder(config)

        val totalBits = 23 + (16 * 2 * 16) + 7
        val totalBytes = (totalBits + 7) / 8
        val frameBytes = ByteArray(totalBytes)
        frameBytes[2] = 0x02.toByte() // isEscaped uncompressed frame

        val inputBuffer = decoder.createInputBuffer()
        inputBuffer.data = ByteBuffer.wrap(frameBytes)
        val outputBuffer = decoder.createOutputBuffer()

        val error = decoder.decode(inputBuffer, outputBuffer, false)
        assertNull(error)
        assertNotNull(outputBuffer.data)
        // 16 samples * 2 channels * 2 bytes = 64 bytes
        assertEquals(64, outputBuffer.data!!.remaining())
    }
}
