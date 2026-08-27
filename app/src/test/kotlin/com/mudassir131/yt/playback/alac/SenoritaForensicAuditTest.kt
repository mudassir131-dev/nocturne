/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import com.mudassir131.yt.db.entities.FormatEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SenoritaForensicAuditTest {

    private val targetVideoId = "VKC_hzJ3jzg"
    private val targetTitle = "Señorita"
    private val targetArtist = "Shawn Mendes, Camila Cabello"
    private val targetDuration = 191

    @Before
    fun setup() {
        LosslessStreamResolver.resetDefaultProviders()
        LosslessStreamResolver.enablePreflightValidation = false
        SelfHostedLosslessAudioProvider.explicitConfig = null
        LosslessStreamResolver.clearCache()
    }

    @Test
    fun `Senorita - Unconfigured environment traces gracefully to honest YouTube Opus fallback`() = runBlocking {
        // Zero-config default environment: no Navidrome server, no local file
        SelfHostedLosslessAudioProvider.explicitConfig = null

        val result = LosslessStreamResolver.resolve(
            videoId = targetVideoId,
            title = targetTitle,
            artist = targetArtist,
            durationSeconds = targetDuration,
        )

        // Must return null -> triggers graceful fallback
        assertNull("Lossless resolver must return null when no lossless source exists", result)

        // Runtime YouTube Opus format verification
        val ytOpusFormat = FormatEntity(
            id = targetVideoId,
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
        assertFalse("YouTube Opus must never be classified as lossless", info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("OPUS 158 kbps", info.qualityLabel)
        assertEquals("c2.android.opus.decoder", info.decoderName)
    }

    @Test
    fun `Senorita - Genuine FLAC stream resolves to Lossless`() = runBlocking {
        val flacStream = ResolvedLosslessStream(
            url = "https://example.com/stream/senorita_flac.flac",
            mimeType = "audio/flac",
            codec = "flac",
            bitDepth = 24,
            sampleRate = 48000,
            channels = 2,
            bitrate = 1600000,
            durationSeconds = targetDuration,
            sourceName = "lossless_stream",
        )

        LosslessStreamResolver.cacheExplicitStream(targetVideoId, flacStream)

        val result = LosslessStreamResolver.resolve(
            videoId = targetVideoId,
            title = targetTitle,
            artist = targetArtist,
            durationSeconds = targetDuration,
        )

        assertNotNull(result)
        assertTrue(result!!.format.mimeType.contains("flac"))
        assertEquals("lossless_stream", result.source)

        val media3Format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_FLAC)
            .setCodecs("flac")
            .setSampleRate(96000)
            .setChannelCount(2)
            .setPcmEncoding(androidx.media3.common.C.ENCODING_PCM_24BIT)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3Format,
            decoderName = "FlacDecoder",
            playbackUrl = result.streamUrl,
        )

        assertNotNull(info)
        assertEquals("FLAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
    }

    @Test
    fun `Senorita - AAC 320 stream is strictly rejected by codec gatekeeper`() = runBlocking {
        val aacStream = ResolvedLosslessStream(
            url = "https://example.com/stream/senorita_320.m4a",
            mimeType = "audio/mp4",
            codec = "mp4a.40.2",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 320000,
            durationSeconds = targetDuration,
            sourceName = "lossy_stream",
        )

        assertFalse("Lossy AAC 320 must be rejected by codec gatekeeper", LosslessStreamResolver.isGenuinelyLossless(aacStream))
    }
}
