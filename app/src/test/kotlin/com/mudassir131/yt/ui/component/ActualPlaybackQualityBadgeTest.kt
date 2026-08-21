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

    private fun format(
        codecs: String,
        playbackUrl: String? = null,
    ) = FormatEntity(
        id = "id",
        itag = 251,
        mimeType = "audio/webm",
        codecs = codecs,
        bitrate = 160_000,
        sampleRate = 48_000,
        contentLength = 1L,
        loudnessDb = null,
        playbackUrl = playbackUrl,
    )
}
