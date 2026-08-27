package com.mudassir131.yt.ui.component

import com.mudassir131.yt.db.entities.FormatEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ActualPlaybackQualityBadgeTest {
    @Test
    fun `resolved Opus codec is labeled Opus`() {
        assertEquals("OPUS 160 kbps", format(codecs = "opus").actualPlaybackQualityLabel())
    }

    @Test
    fun `YouTube AAC fallback is not mislabeled Saavn`() {
        assertEquals("AAC 160 kbps", format(codecs = "mp4a.40.2").actualPlaybackQualityLabel())
    }

    @Test
    fun `actual Saavn playback URL is labeled Saavn`() {
        assertEquals(
            "SAAVN 160 kbps",
            format(codecs = "mp4a.40.2", playbackUrl = "https://stream.saavn.example/audio").actualPlaybackQualityLabel(),
        )
    }

    @Test
    fun `ALAC 16-bit 44100Hz is labeled Lossless`() {
        assertEquals(
            "Lossless",
            format(
                codecs = "alac",
                mimeType = "audio/mp4",
                sampleRate = 44_100,
                bitrate = 800_000,
            ).actualPlaybackQualityLabel(),
        )
    }

    @Test
    fun `ALAC 24-bit 48000Hz is labeled Hi-Res Lossless`() {
        assertEquals(
            "Hi-Res Lossless",
            format(
                codecs = "alac.24",
                mimeType = "audio/mp4",
                sampleRate = 48_000,
                bitrate = 1_600_000,
            ).actualPlaybackQualityLabel(),
        )
    }

    @Test
    fun `ALAC 24-bit 96000Hz is labeled Hi-Res Lossless`() {
        assertEquals(
            "Hi-Res Lossless",
            format(
                codecs = "alac",
                mimeType = "audio/mp4",
                sampleRate = 96_000,
                bitrate = 2_800_000,
            ).actualPlaybackQualityLabel(),
        )
    }

    private fun format(
        codecs: String,
        mimeType: String = "audio/webm",
        sampleRate: Int = 48_000,
        bitrate: Int = 160_000,
        playbackUrl: String? = null,
    ) = FormatEntity(
        id = "id",
        itag = 251,
        mimeType = mimeType,
        codecs = codecs,
        bitrate = bitrate,
        sampleRate = sampleRate,
        contentLength = 1L,
        loudnessDb = null,
        playbackUrl = playbackUrl,
    )
}
