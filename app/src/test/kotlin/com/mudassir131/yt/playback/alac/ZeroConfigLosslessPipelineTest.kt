/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import com.mudassir131.yt.db.entities.FormatEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ZeroConfigLosslessPipelineTest {

    @Before
    fun setup() {
        LosslessStreamResolver.resetDefaultProviders()
        LosslessStreamResolver.enablePreflightValidation = false
        SelfHostedLosslessAudioProvider.explicitConfig = null
        LosslessStreamResolver.clearCache()
    }

    @After
    fun tearDown() {
        LosslessStreamResolver.resetDefaultProviders()
        SelfHostedLosslessAudioProvider.explicitConfig = null
        LosslessStreamResolver.clearCache()
    }

    @Test
    fun `01 - Genuine FLAC stream is accepted by codec gatekeeper`() {
        val flacStream = ResolvedLosslessStream(
            url = "https://example.com/audio.flac",
            mimeType = "audio/flac",
            codec = "flac",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 1411200,
            durationSeconds = 210,
            sourceName = "lossless_flac",
        )

        assertTrue(LosslessStreamResolver.isGenuinelyLossless(flacStream))
    }

    @Test
    fun `02 - Genuine ALAC stream is accepted by codec gatekeeper`() {
        val alacStream = ResolvedLosslessStream(
            url = "https://example.com/audio.m4a",
            mimeType = "audio/mp4",
            codec = "alac",
            bitDepth = 24,
            sampleRate = 96000,
            channels = 2,
            bitrate = 2304000,
            durationSeconds = 210,
            sourceName = "lossless_alac",
        )

        assertTrue(LosslessStreamResolver.isGenuinelyLossless(alacStream))
    }

    @Test
    fun `03 - AAC 320 kbps stream is strictly rejected`() {
        val aacStream = ResolvedLosslessStream(
            url = "https://example.com/audio_320.m4a",
            mimeType = "audio/mp4",
            codec = "mp4a.40.2",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 320000,
            durationSeconds = 210,
            sourceName = "lossy_aac",
        )

        assertFalse("AAC 320 in MP4 must be rejected", LosslessStreamResolver.isGenuinelyLossless(aacStream))
    }

    @Test
    fun `04 - Opus stream is strictly rejected`() {
        val opusStream = ResolvedLosslessStream(
            url = "https://example.com/audio.opus",
            mimeType = "audio/webm",
            codec = "opus",
            bitDepth = 16,
            sampleRate = 48000,
            channels = 2,
            bitrate = 160000,
            durationSeconds = 210,
            sourceName = "lossy_opus",
        )

        assertFalse("Opus must be rejected", LosslessStreamResolver.isGenuinelyLossless(opusStream))
    }

    @Test
    fun `05 - MP3 stream is strictly rejected`() {
        val mp3Stream = ResolvedLosslessStream(
            url = "https://example.com/audio.mp3",
            mimeType = "audio/mpeg",
            codec = "mp3",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 320000,
            durationSeconds = 210,
            sourceName = "lossy_mp3",
        )

        assertFalse("MP3 must be rejected", LosslessStreamResolver.isGenuinelyLossless(mp3Stream))
    }

    @Test
    fun `06 - M4A containing AAC is strictly rejected`() {
        val m4aAacStream = ResolvedLosslessStream(
            url = "file:///storage/emulated/0/Music/song.m4a",
            mimeType = "audio/mp4",
            codec = "aac",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 256000,
            durationSeconds = 180,
            sourceName = "local_m4a_aac",
        )

        assertFalse("M4A containing AAC must be rejected", LosslessStreamResolver.isGenuinelyLossless(m4aAacStream))
    }

    @Test
    fun `07 - M4A containing ALAC is accepted`() {
        val m4aAlacStream = ResolvedLosslessStream(
            url = "file:///storage/emulated/0/Music/song.m4a",
            mimeType = "audio/mp4",
            codec = "alac",
            bitDepth = 24,
            sampleRate = 48000,
            channels = 2,
            bitrate = 1500000,
            durationSeconds = 180,
            sourceName = "local_m4a_alac",
        )

        assertTrue("M4A containing ALAC must be accepted", LosslessStreamResolver.isGenuinelyLossless(m4aAlacStream))
    }

    @Test
    fun `08 - Unknown MP4 codec is strictly rejected`() {
        val unknownMp4Stream = ResolvedLosslessStream(
            url = "https://example.com/stream.mp4",
            mimeType = "audio/mp4",
            codec = "unknown",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 320000,
            durationSeconds = 200,
            sourceName = "unknown_mp4",
        )

        assertFalse("Unknown MP4 codec must be rejected", LosslessStreamResolver.isGenuinelyLossless(unknownMp4Stream))
    }

    @Test
    fun `09 - No lossless source available falls back to YouTube Opus 251`() = runBlocking {
        SelfHostedLosslessAudioProvider.explicitConfig = null

        val result = LosslessStreamResolver.resolve(
            videoId = "VKC_hzJ3jzg",
            title = "Señorita",
            artist = "Shawn Mendes, Camila Cabello",
            durationSeconds = 191,
        )

        assertNull("Zero-config without lossless matches must return null to trigger YouTube fallback", result)

        val ytOpusFormat = FormatEntity(
            id = "VKC_hzJ3jzg",
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
        assertFalse(info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("OPUS 158 kbps", info.qualityLabel)
        assertEquals("c2.android.opus.decoder", info.decoderName)
    }

    @Test
    fun `10 - Network failure returns null and falls back to YouTube without crashing`() = runBlocking {
        val failingProvider = object : LosslessAudioProvider {
            override val name: String = "FailingNetworkProvider"
            override suspend fun resolve(videoId: String, title: String, artist: String, durationSeconds: Int): ResolvedLosslessStream? {
                throw java.io.IOException("Network connection reset")
            }
        }

        LosslessStreamResolver.registerProvider(failingProvider)

        val result = LosslessStreamResolver.resolve(
            videoId = "test_video_fail",
            title = "Failing Track",
            artist = "Artist",
            durationSeconds = 180,
        )

        assertNull("Network failure in provider must be caught and return null", result)
    }

    @Test
    fun `11 - Standard Lossless 16-bit 44_1kHz format classification`() {
        val media3Format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_FLAC)
            .setCodecs("flac")
            .setSampleRate(44100)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_16BIT)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3Format,
            decoderName = "FlacDecoder",
            playbackUrl = "https://example.com/track.flac",
        )

        assertNotNull(info)
        assertEquals("FLAC", info!!.codec)
        assertTrue(info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals("Lossless", info.qualityLabel)
        assertEquals(44100, info.sampleRate)
        assertEquals(16, info.bitDepth)
    }

    @Test
    fun `12 - Hi-Res Lossless 24-bit 96kHz format classification`() {
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
            playbackUrl = "https://example.com/track.flac",
        )

        assertNotNull(info)
        assertEquals("FLAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
        assertEquals(96000, info.sampleRate)
        assertEquals(24, info.bitDepth)
    }

    @Test
    fun `13 - Existing YouTube Opus regression is preserved`() {
        val ytFormat = FormatEntity(
            id = "test_yt_opus",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 134000,
            sampleRate = 48000,
            contentLength = 3400000L,
            loudnessDb = null,
            playbackUrl = "https://rr---sn-youtube.googlevideo.com/videoplayback?itag=251",
        )

        val info = ytFormat.toAudioFormatInfo(decoderName = "c2.android.opus.decoder")
        assertEquals("Opus", info.codec)
        assertFalse(info.isLossless)
        assertEquals("OPUS 134 kbps", info.qualityLabel)
    }

    @Test
    fun `14 - Weak metadata match is rejected`() {
        val score = SelfHostedLosslessAudioProvider.calculateMatchScore(
            targetTitle = "Levitating",
            targetArtist = "Dua Lipa",
            targetDuration = 203,
            candidateTitle = "Completely Different Song",
            candidateArtist = "Different Artist",
            candidateDuration = 300,
        )

        assertTrue("Mismatch score must be below acceptance threshold", score < 60)
    }

    @Test
    fun `15 - Unknown codec is rejected by gatekeeper`() {
        val stream = ResolvedLosslessStream(
            url = "https://example.com/raw.bin",
            mimeType = "application/octet-stream",
            codec = "unknown",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            durationSeconds = 180,
            sourceName = "unknown",
        )

        assertFalse(LosslessStreamResolver.isGenuinelyLossless(stream))
    }
}
