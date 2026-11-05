package com.mudassir131.yt.playback.alac.addon

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.mudassir131.yt.playback.alac.AudioFormatInfo
import com.mudassir131.yt.playback.alac.LosslessStreamResolver
import com.mudassir131.yt.ui.component.LosslessQuality
import com.mudassir131.yt.ui.component.losslessQuality

class AddonLosslessAudioProviderTest {

    @Before
    fun setup() {
        LosslessStreamResolver.enablePreflightValidation = false
        AddonLosslessAudioProvider.explicitConfig = null
    }

    @Test
    fun `1 - Manifest parsing validates lossless addon capabilities`() {
        val json = JSONObject(
            """
            {
                "id": "lossless-provider",
                "name": "HiFi Addon",
                "version": "1.2.0",
                "resources": ["search", "stream"]
            }
            """.trimIndent()
        )
        val manifest = AddonManifest.fromJson(json)
        assertNotNull(manifest)
        assertEquals("lossless-provider", manifest.id)
        assertEquals("HiFi Addon", manifest.name)
        assertEquals("1.2.0", manifest.version)
        assertTrue(manifest.hasResource("search"))
        assertTrue(manifest.hasResource("stream"))
        assertFalse(manifest.hasResource("lyrics"))
    }

    @Test
    fun `2 - Manifest parsing with string resources`() {
        val json = JSONObject(
            """
            {
                "id": "single-res",
                "name": "Simple",
                "version": "1.0",
                "resources": "search, stream"
            }
            """.trimIndent()
        )
        val manifest = AddonManifest.fromJson(json)
        assertNotNull(manifest)
        assertTrue(manifest.hasResource("search"))
        assertTrue(manifest.hasResource("stream"))
    }

    @Test
    fun `3 - Track response parsing extracts metadata`() {
        val json = JSONObject(
            """
            {
                "id": "trk_1",
                "title": "Dil Ibaadat",
                "artist": "KK",
                "duration": 329
            }
            """.trimIndent()
        )
        val track = AddonTrack.fromJson(json)
        assertNotNull(track)
        assertEquals("Dil Ibaadat", track?.title)
        assertEquals(329.0, track?.durationSeconds ?: 0.0, 0.01)
    }

    @Test
    fun `4 - Stream response parsing extracts flac streams`() {
        val stream = AddonStream(
            url = "https://cdn.lossless.org/audio/dil_ibaadat.flac",
            format = "flac",
            bitrate = 1411200
        )
        assertEquals("https://cdn.lossless.org/audio/dil_ibaadat.flac", stream.url)
        assertTrue(stream.isLossless)
    }

    @Test
    fun `5 - Search matching picks best track by name and artist`() {
        val items = listOf(
            AddonTrack("1", "Zara Sa", "KK", durationSeconds = 303.0),
            AddonTrack("2", "Dil Ibaadat (From Tum Mile)", "KK", durationSeconds = 329.0),
            AddonTrack("3", "Dil Ibaadat Remix", "DJ Suketu", durationSeconds = 280.0)
        )
        val best = AddonLosslessAudioProvider.pickBestCandidate(
            tracks = items,
            targetTitle = "Dil Ibaadat",
            targetArtist = "KK",
            targetDuration = 329
        )
        assertNotNull(best)
        assertEquals("2", best?.id)
    }

    @Test
    fun `6 - Search matching falls back gracefully when duration differs`() {
        val items = listOf(
            AddonTrack("1", "Tum Mile", "Neeraj Shridhar", durationSeconds = 340.0)
        )
        val best = AddonLosslessAudioProvider.pickBestCandidate(
            tracks = items,
            targetTitle = "Tum Mile",
            targetArtist = "Neeraj Shridhar",
            targetDuration = 338
        )
        assertNotNull(best)
        assertEquals("1", best?.id)
    }

    @Test
    fun `7 - File size tier categorization under 30MB is Bit-perfect`() {
        val info = AudioFormatInfo(
            codec = "flac",
            mimeType = "audio/flac",
            contentLength = 28L * 1024L * 1024L,
            isLossless = true
        )
        assertEquals(LosslessQuality.BIT_PERFECT, info.losslessQuality())
    }

    @Test
    fun `8 - File size tier categorization 30 to 50MB is Lossless`() {
        val info = AudioFormatInfo(
            codec = "flac",
            mimeType = "audio/flac",
            contentLength = 38L * 1024L * 1024L,
            isLossless = true
        )
        assertEquals(LosslessQuality.LOSSLESS, info.losslessQuality())
    }

    @Test
    fun `9 - File size tier categorization over 50MB is Hi-Res Lossless`() {
        val info = AudioFormatInfo(
            codec = "flac",
            mimeType = "audio/flac",
            contentLength = 65L * 1024L * 1024L,
            isLossless = true
        )
        assertEquals(LosslessQuality.HI_RES_LOSSLESS, info.losslessQuality())
    }

    @Test
    fun `10 - External Addon with token parameter split works correctly`() {
        val (base, query) = AddonClient.splitBaseAndQuery("https://api.external-addon.org/manifest.json?token=my_secret_key")
        assertEquals("https://api.external-addon.org", base)
        assertEquals("token=my_secret_key", query)
    }
}
