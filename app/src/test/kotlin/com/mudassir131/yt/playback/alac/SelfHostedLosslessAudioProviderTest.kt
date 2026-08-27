/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.ui.component.losslessQuality
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SelfHostedLosslessAudioProviderTest {

    @Before
    fun setup() {
        LosslessStreamResolver.enablePreflightValidation = false
        SelfHostedLosslessAudioProvider.explicitConfig = null
    }

    @Test
    fun `1 - Navidrome FLAC 16-bit 44_1kHz resolves to Lossless`() = runBlocking {
        val flac16Stream = ResolvedLosslessStream(
            url = "https://navidrome.example.com/rest/stream?id=flac16&format=raw",
            mimeType = "audio/flac",
            codec = "flac",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 850000,
            contentLength = 22000000L,
            durationSeconds = 203,
            sourceName = "navidrome_lossless",
        )

        assertTrue(LosslessStreamResolver.isGenuinelyLossless(flac16Stream))

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
            playbackUrl = flac16Stream.url,
        )

        assertNotNull(info)
        assertEquals("FLAC", info!!.codec)
        assertTrue(info.isLossless)
        assertFalse(info.isHiRes)
        assertEquals(16, info.bitDepth)
        assertEquals(44100, info.sampleRate)
        assertEquals("Lossless", info.qualityLabel)
    }

    @Test
    fun `2 - Navidrome FLAC 24-bit 96kHz resolves to Hi-Res Lossless`() = runBlocking {
        val flac24Stream = ResolvedLosslessStream(
            url = "https://navidrome.example.com/rest/stream?id=flac24&format=raw",
            mimeType = "audio/flac",
            codec = "flac",
            bitDepth = 24,
            sampleRate = 96000,
            channels = 2,
            bitrate = 3200000,
            contentLength = 75000000L,
            durationSeconds = 203,
            sourceName = "navidrome_lossless",
        )

        assertTrue(LosslessStreamResolver.isGenuinelyLossless(flac24Stream))

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
            playbackUrl = flac24Stream.url,
        )

        assertNotNull(info)
        assertEquals("FLAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals(24, info.bitDepth)
        assertEquals(96000, info.sampleRate)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
    }

    @Test
    fun `3 - Navidrome ALAC 24-bit 96kHz resolves to Hi-Res Lossless`() = runBlocking {
        val alacStream = ResolvedLosslessStream(
            url = "https://navidrome.example.com/rest/stream?id=alac24&format=raw",
            mimeType = "audio/mp4",
            codec = "alac",
            bitDepth = 24,
            sampleRate = 96000,
            channels = 2,
            bitrate = 2304000,
            contentLength = 60000000L,
            durationSeconds = 203,
            sourceName = "navidrome_lossless",
        )

        assertTrue(LosslessStreamResolver.isGenuinelyLossless(alacStream))

        val media3Format = Format.Builder()
            .setSampleMimeType(MimeTypes.AUDIO_ALAC)
            .setCodecs("alac")
            .setSampleRate(96000)
            .setChannelCount(2)
            .setPcmEncoding(C.ENCODING_PCM_24BIT)
            .build()

        val info = AudioFormatInfo.resolve(
            media3Format = media3Format,
            decoderName = "AlacDecoder",
            playbackUrl = alacStream.url,
        )

        assertNotNull(info)
        assertEquals("ALAC", info!!.codec)
        assertTrue(info.isLossless)
        assertTrue(info.isHiRes)
        assertEquals("AlacDecoder", info.decoderName)
        assertEquals("Hi-Res Lossless", info.qualityLabel)
    }

    @Test
    fun `4 - Navidrome AAC 320 is rejected from lossless path`() {
        val aacStream = ResolvedLosslessStream(
            url = "https://navidrome.example.com/rest/stream?id=aac320",
            mimeType = "audio/mp4",
            codec = "mp4a.40.2",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 320000,
            durationSeconds = 203,
            sourceName = "navidrome_aac",
        )

        assertFalse("AAC 320 kbps must be rejected by isGenuinelyLossless", LosslessStreamResolver.isGenuinelyLossless(aacStream))
    }

    @Test
    fun `5 - Navidrome MP3 is rejected from lossless path`() {
        val mp3Stream = ResolvedLosslessStream(
            url = "https://navidrome.example.com/rest/stream?id=mp3",
            mimeType = "audio/mpeg",
            codec = "mp3",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 320000,
            durationSeconds = 203,
            sourceName = "navidrome_mp3",
        )

        assertFalse("MP3 must be rejected by isGenuinelyLossless", LosslessStreamResolver.isGenuinelyLossless(mp3Stream))
    }

    @Test
    fun `6 - Navidrome Opus is rejected from lossless path`() {
        val opusStream = ResolvedLosslessStream(
            url = "https://navidrome.example.com/rest/stream?id=opus",
            mimeType = "audio/webm",
            codec = "opus",
            bitDepth = 16,
            sampleRate = 48000,
            channels = 2,
            bitrate = 160000,
            durationSeconds = 203,
            sourceName = "navidrome_opus",
        )

        assertFalse("Opus must be rejected by isGenuinelyLossless", LosslessStreamResolver.isGenuinelyLossless(opusStream))
    }

    @Test
    fun `7 - m4a file containing AAC is rejected from lossless path`() {
        val m4aAacStream = ResolvedLosslessStream(
            url = "file:///music/song.m4a",
            mimeType = "audio/mp4",
            codec = "aac",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 256000,
            durationSeconds = 203,
            sourceName = "local_m4a",
        )

        assertFalse("m4a with AAC must be rejected", LosslessStreamResolver.isGenuinelyLossless(m4aAacStream))
    }

    @Test
    fun `8 - m4a file containing ALAC is accepted as lossless`() {
        val m4aAlacStream = ResolvedLosslessStream(
            url = "file:///music/song_alac.m4a",
            mimeType = "audio/mp4",
            codec = "alac",
            bitDepth = 16,
            sampleRate = 44100,
            channels = 2,
            bitrate = 850000,
            durationSeconds = 203,
            sourceName = "local_alac",
        )

        assertTrue("m4a with ALAC must be accepted", LosslessStreamResolver.isGenuinelyLossless(m4aAlacStream))
    }

    @Test
    fun `9 - No self-hosted match gracefully falls back to YouTube Opus`() = runBlocking {
        // Mock self-hosted provider that returns null (no match)
        SelfHostedLosslessAudioProvider.explicitConfig = SelfHostedLosslessAudioProvider.ServerConfig(
            serverUrl = "https://navidrome.example.com",
            username = "user",
            passwordOrToken = "pass",
            enabled = false, // Disabled / No match
        )

        val result = LosslessStreamResolver.resolve(
            videoId = "OsfAnsMY21M",
            title = "Levitating",
            artist = "Dua Lipa",
            durationSeconds = 203,
        )

        assertNull("Expected null when no self-hosted match exists", result)

        // Verifying fallback format is honest Opus
        val youtubeOpusFormat = FormatEntity(
            id = "OsfAnsMY21M",
            itag = 251,
            mimeType = "audio/webm",
            codecs = "opus",
            bitrate = 134000,
            sampleRate = 48000,
            contentLength = 3300000L,
            loudnessDb = null,
            playbackUrl = "https://rr---sn-youtube.googlevideo.com/videoplayback?itag=251",
        )

        val info = youtubeOpusFormat.toAudioFormatInfo(decoderName = "c2.android.opus.decoder")
        assertEquals("Opus", info.codec)
        assertFalse(info.isLossless)
        assertEquals("OPUS 134 kbps", info.qualityLabel)
        assertEquals("c2.android.opus.decoder", info.decoderName)
    }

    @Test
    fun `10 - Self-hosted server unavailable falls back without crash`() = runBlocking {
        // Invalid/unreachable server config
        SelfHostedLosslessAudioProvider.explicitConfig = SelfHostedLosslessAudioProvider.ServerConfig(
            serverUrl = "https://invalid-nonexistent-server-999.example.com",
            username = "test",
            passwordOrToken = "test",
            enabled = true,
        )

        val result = LosslessStreamResolver.resolve(
            videoId = "OsfAnsMY21M",
            title = "Levitating",
            artist = "Dua Lipa",
            durationSeconds = 203,
        )

        assertNull("Expected fallback when server is unreachable", result)
    }

    @Test
    fun `11 - Incorrect metadata match rejects candidate`() {
        val score = SelfHostedLosslessAudioProvider.calculateMatchScore(
            targetTitle = "Levitating",
            targetArtist = "Dua Lipa",
            targetDuration = 203,
            candidateTitle = "Completely Different Song",
            candidateArtist = "Another Artist",
            candidateDuration = 360,
        )

        assertTrue("Mismatched song should have score below 60 (got $score)", score < 60)
    }

    @Test
    fun `12 - Authentication credentials and tokens are redacted from logs`() {
        val sensitiveUrl = "https://navidrome.example.com/rest/stream?id=123&u=myuser&p=supersecret&t=a1b2c3d4&s=salt123"
        val redacted = SelfHostedLosslessAudioProvider.redactSensitiveUrl(sensitiveUrl)

        assertFalse("Password must be redacted", redacted.contains("supersecret"))
        assertFalse("Token must be redacted", redacted.contains("a1b2c3d4"))
        assertFalse("Salt must be redacted", redacted.contains("salt123"))
        assertFalse("Username must be redacted", redacted.contains("myuser"))
        assertTrue(redacted.contains("u=REDACTED"))
        assertTrue(redacted.contains("t=REDACTED"))
    }
}
