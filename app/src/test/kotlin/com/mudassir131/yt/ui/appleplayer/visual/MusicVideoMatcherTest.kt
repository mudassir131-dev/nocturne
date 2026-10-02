/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicVideoMatcherTest {

    @Test
    fun officialVideoBeatsThirdPartyWithExactDuration() {
        val candidates = listOf(
            // Third-party uploader with exact song audio duration
            MusicVideoMatcher.Candidate(
                videoId = "fake_pop_stars",
                title = "The Weeknd \"Blinding Lights\" (Music Video)",
                artist = "Pop Stars Inc.",
                durationSec = 200,
                thumbnailUrl = "https://thumb.url/fake",
            ),
            // Unwanted drift edit video
            MusicVideoMatcher.Candidate(
                videoId = "fake_drift",
                title = "The Weeknd - Blinding Lights ( AMG Night Drift Music Video )",
                artist = "Alex Djay",
                durationSec = 228,
                thumbnailUrl = "https://thumb.url/drift",
            ),
            // Real official video on artist's channel with story scenes (+63s)
            MusicVideoMatcher.Candidate(
                videoId = "real_official_4NRXx6U8ABQ",
                title = "Blinding Lights (Official Video)",
                artist = "The Weeknd",
                durationSec = 263,
                thumbnailUrl = "https://thumb.url/real",
            ),
        )

        val best = MusicVideoMatcher.findBestMatch(
            trackTitle = "Blinding Lights",
            trackArtist = "The Weeknd",
            trackAlbum = "After Hours",
            trackDurationSec = 200,
            candidates = candidates,
        )

        assertNotNull(best)
        assertEquals("real_official_4NRXx6U8ABQ", best?.videoId)
    }

    @Test
    fun officialMusicVideoMatchesWithHighScore() {
        val candidates = listOf(
            MusicVideoMatcher.Candidate(
                videoId = "vid1",
                title = "The Weeknd - Blinding Lights (Official Music Video)",
                artist = "The Weeknd",
                durationSec = 260,
                thumbnailUrl = "https://thumb.url/1",
                musicVideoType = "MUSIC_VIDEO_TYPE_OMV",
            ),
            MusicVideoMatcher.Candidate(
                videoId = "vid2",
                title = "The Weeknd - Blinding Lights (Lyrics)",
                artist = "Lyrics Hub",
                durationSec = 200,
                thumbnailUrl = "https://thumb.url/2",
            ),
        )

        val best = MusicVideoMatcher.findBestMatch(
            trackTitle = "Blinding Lights",
            trackArtist = "The Weeknd",
            trackAlbum = "After Hours",
            trackDurationSec = 200,
            candidates = candidates,
        )

        assertNotNull(best)
        assertEquals("vid1", best?.videoId)
    }

    @Test
    fun lyricVideosAreDisqualified() {
        val candidate = MusicVideoMatcher.Candidate(
            videoId = "vid_lyrics",
            title = "Dua Lipa - Levitating (Lyric Video)",
            artist = "Dua Lipa",
            durationSec = 203,
            thumbnailUrl = null,
        )

        val score = MusicVideoMatcher.scoreCandidate(
            trackTitle = "Levitating",
            trackArtist = "Dua Lipa",
            trackAlbum = "Future Nostalgia",
            trackDurationSec = 203,
            candidate = candidate,
        )

        assertTrue("Lyric video score must be negative", score < 0)
    }

    @Test
    fun livePerformancesAreDisqualified() {
        val candidate = MusicVideoMatcher.Candidate(
            videoId = "vid_live",
            title = "Coldplay - Yellow (Live in São Paulo)",
            artist = "Coldplay",
            durationSec = 290,
            thumbnailUrl = null,
        )

        val score = MusicVideoMatcher.scoreCandidate(
            trackTitle = "Yellow",
            trackArtist = "Coldplay",
            trackAlbum = "Parachutes",
            trackDurationSec = 269,
            candidate = candidate,
        )

        assertTrue("Live performance score must be negative", score < 0)
    }

    @Test
    fun coversAreDisqualified() {
        val candidate = MusicVideoMatcher.Candidate(
            videoId = "vid_cover",
            title = "Adele - Easy On Me (Cover by Jane Doe)",
            artist = "Jane Doe",
            durationSec = 224,
            thumbnailUrl = null,
        )

        val score = MusicVideoMatcher.scoreCandidate(
            trackTitle = "Easy On Me",
            trackArtist = "Adele",
            trackAlbum = "30",
            trackDurationSec = 224,
            candidate = candidate,
        )

        assertTrue("Cover video score must be negative", score < 0)
    }

    @Test
    fun excessiveDurationDifferenceIsPenalized() {
        val candidate = MusicVideoMatcher.Candidate(
            videoId = "vid_10hr",
            title = "Taylor Swift - Anti-Hero (Official Video)",
            artist = "Taylor Swift",
            durationSec = 3600, // 1 hour loop
            thumbnailUrl = null,
        )

        val score = MusicVideoMatcher.scoreCandidate(
            trackTitle = "Anti-Hero",
            trackArtist = "Taylor Swift",
            trackAlbum = "Midnights",
            trackDurationSec = 200,
            candidate = candidate,
        )

        val match = MusicVideoMatcher.findBestMatch(
            trackTitle = "Anti-Hero",
            trackArtist = "Taylor Swift",
            trackAlbum = "Midnights",
            trackDurationSec = 200,
            candidates = listOf(candidate),
        )

        // Excessive duration should prevent it from passing the threshold
        assertNull(match)
    }

    @Test
    fun visualResolutionResultHelperPropertiesWork() {
        val emptyResult = VisualResolutionResult()
        assertFalse(emptyResult.hasVideo)
        assertFalse(emptyResult.hasCanvas)
        assertFalse(emptyResult.hasBothVideoAndCanvas)

        val videoResult = VisualResolutionResult(
            musicVideo = VisualContent.MusicVideo(
                videoId = "abc",
                title = "Title",
                artist = "Artist",
                thumbnailUrl = null,
                durationMs = 120_000L,
                streamUrl = "https://stream.url/video.mp4",
            ),
            activeMode = VisualMode.MUSIC_VIDEO,
        )
        assertTrue(videoResult.hasVideo)
        assertFalse(videoResult.hasCanvas)
        assertFalse(videoResult.hasBothVideoAndCanvas)

        val bothResult = videoResult.copy(
            canvas = VisualContent.Canvas(
                artwork = com.mudassir131.yt.ui.appleplayer.liveart.CanvasArtwork(),
                primaryUrl = "https://canvas.url/hls.m3u8",
                fallbackUrl = null,
            ),
        )
        assertTrue(bothResult.hasVideo)
        assertTrue(bothResult.hasCanvas)
        assertTrue(bothResult.hasBothVideoAndCanvas)
    }

    @Test
    fun musicVideoResolverReturnsHighQualityStream() {
        kotlinx.coroutines.runBlocking {
            val result = MusicVideoResolver.resolveVideoStream(
                videoId = "4NRXx6U8ABQ", // Blinding Lights
                title = "Blinding Lights",
                artist = "The Weeknd",
                thumbnailUrl = null,
                durationMs = 260_000L,
                isMetered = false,
                dataSaver = false,
            )

            assertNotNull("MusicVideo resolution must succeed", result)
            assertNotNull("Stream URL must be non-null", result?.streamUrl)
            assertTrue("Stream URL must be non-empty", result?.streamUrl?.isNotEmpty() == true)
            assertTrue("Available qualities must not be empty", result?.availableQualities?.isNotEmpty() == true)

            // Verify resolution is high quality (1080p for Blinding Lights, or at least >= 720p)
            val topQuality = result?.availableQualities?.firstOrNull()
            assertNotNull("Top quality option must exist", topQuality)
            assertTrue(
                "Top quality should be at least 720p or 1080p (was ${topQuality?.height}p)",
                (topQuality?.height ?: 0) >= 720,
            )
        }
    }
}
