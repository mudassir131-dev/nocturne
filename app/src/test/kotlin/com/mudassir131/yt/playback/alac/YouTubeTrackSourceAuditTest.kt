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

class YouTubeTrackSourceAuditTest {

    @org.junit.Before
    fun setup() {
        LosslessStreamResolver.resetDefaultProviders()
        SelfHostedLosslessAudioProvider.explicitConfig = null
        LosslessStreamResolver.clearCache()
    }

    @Test
    fun `Audit Standard YouTube Track - Falls back honestly to Opus 251 with no fabrication`() = runBlocking {
        LosslessStreamResolver.clearCache()

        // 1. Attempt lossless resolution for a track with no lossless catalog match
        val losslessResult = LosslessStreamResolver.resolve(
            videoId = "unavailable_track_999",
            title = "Uncataloged Rare Demo 999",
            artist = "Unknown Artist",
            durationSeconds = 120,
        )

        // 2. Verified: No ALAC provider matches this track, so resolver returns null
        assertNull("Track without lossless provider match must return null from LosslessStreamResolver", losslessResult)

        // 3. Fallback path: YouTube returns itag 251 WebM Opus
        val youtubeOpusFormat = Format.Builder()
            .setSampleMimeType("audio/webm")
            .setCodecs("opus")
            .setSampleRate(48000)
            .setChannelCount(2)
            .setAverageBitrate(134000)
            .build()

        val youtubeFormatEntity = FormatEntity(
            id = "OsfAnsMY21M",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 134000,
            sampleRate = 48000,
            contentLength = 3300000,
            loudnessDb = 1.1,
            playbackUrl = "https://rr---sn-youtube.googlevideo.com/videoplayback?id=OsfAnsMY21M&itag=251",
        )

        // 4. Audit Format Resolution
        val formatInfo = AudioFormatInfo.resolve(
            media3Format = youtubeOpusFormat,
            formatEntity = youtubeFormatEntity,
            decoderName = "c2.android.opus.decoder",
            playbackUrl = youtubeFormatEntity.playbackUrl,
        )

        assertNotNull(formatInfo)
        val info = formatInfo!!

        // 5. Verify HONEST reporting (No fake ALAC, no fake Lossless badge)
        assertEquals("Opus", info.codec)
        assertEquals("audio/webm", info.mimeType)
        assertFalse("Must NOT be labeled lossless", info.isLossless)
        assertFalse("Must NOT be labeled hi-res", info.isHiRes)
        assertEquals(48000, info.sampleRate)
        assertEquals(16, info.bitDepth)
        assertEquals(134000, info.bitrate)
        assertEquals(251, info.itag)
        assertEquals("c2.android.opus.decoder", info.decoderName)
        assertNull("LosslessQuality must be null for Opus stream", info.losslessQuality())
    }

    @Test
    fun `Audit Genuine ALAC Source - Routes to AlacDecoder with genuine Lossless classification`() = runBlocking {
        // Register a genuine ALAC source provider (e.g. local lossless storage or custom stream)
        val genuineAlacProvider = object : LosslessAudioProvider {
            override val name: String = "AuditAlacProvider"
            override suspend fun resolve(
                videoId: String,
                title: String,
                artist: String,
                durationSeconds: Int,
            ): ResolvedLosslessStream {
                return ResolvedLosslessStream(
                    url = "https://lossless-cdn.example.org/audio/alac/track_24bit_96k.m4a",
                    mimeType = "audio/mp4",
                    codec = "alac",
                    bitDepth = 24,
                    sampleRate = 96000,
                    channels = 2,
                    bitrate = 2304000,
                    contentLength = 48000000L,
                    durationSeconds = 240,
                    sourceName = "verified_alac",
                )
            }
        }

        LosslessStreamResolver.enablePreflightValidation = false
        LosslessStreamResolver.registerProvider(genuineAlacProvider)

        val result = LosslessStreamResolver.resolve(
            videoId = "track_alac_001",
            title = "Hotel California (Hi-Res ALAC)",
            artist = "Eagles",
            durationSeconds = 240,
        )

        assertNotNull("Expected ALAC stream to be resolved", result)
        assertEquals("audio/mp4; codecs=\"alac\"", result!!.format.mimeType)
        assertEquals(999, result.format.itag)

        // Media3 demuxes the MP4 container and feeds ALAC frames to AlacAudioRenderer
        val media3AlacFormat = Format.Builder()
            .setSampleMimeType("audio/alac")
            .setCodecs("alac")
            .setSampleRate(96000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .setAverageBitrate(2304000)
            .build()

        val formatEntity = FormatEntity(
            id = "track_alac_001",
            itag = 999,
            mimeType = "audio/mp4",
            codecs = "alac",
            bitrate = 2304000,
            sampleRate = 96000,
            contentLength = 48000000L,
            loudnessDb = null,
            playbackUrl = result.streamUrl,
        )

        val formatInfo = AudioFormatInfo.resolve(
            media3Format = media3AlacFormat,
            formatEntity = formatEntity,
            decoderName = "AlacDecoder",
            playbackUrl = result.streamUrl,
        )

        assertNotNull(formatInfo)
        val info = formatInfo!!

        // Verify genuine ALAC attributes
        assertEquals("ALAC", info.codec)
        assertEquals("audio/alac", info.mimeType)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals(24, info.bitDepth)
        assertEquals(96000, info.sampleRate)
        assertEquals("AlacDecoder", info.decoderName)
        assertEquals("Hi-Res Lossless", info.losslessQuality()?.label)

        LosslessStreamResolver.unregisterProvider(genuineAlacProvider.name)
    }
}
