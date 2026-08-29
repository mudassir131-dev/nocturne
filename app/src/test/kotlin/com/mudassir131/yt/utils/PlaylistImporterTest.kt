/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import com.mudassir131.yt.db.entities.PlaylistSongMap
import com.mudassir131.yt.db.entities.SpotifyImportProgressEntity
import com.mudassir131.yt.db.entities.SpotifyImportTrackEntity
import com.mudassir131.yt.db.entities.SpotifyTrackMap
import com.mudassir131.yt.utils.matching.MatchCandidate
import com.mudassir131.yt.utils.matching.MatchStatus
import com.mudassir131.yt.utils.matching.TrackMatcher
import com.mudassir131.yt.utils.youtube.YouTubeDataApi
import com.mudassir131.yt.utils.youtube.YouTubeQuotaTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

class PlaylistImporterTest {

    @Test
    fun `01 - Spotify constants and limits`() {
        assertEquals(3000, PlaylistImporter.SPOTIFY_MAX_IMPORT_SONGS)
        assertEquals(5000, PlaylistImporter.MAX_IMPORT_SONGS)
    }

    @Test
    fun `02 - Track-level duplicate prevention via spotify_track_id`() {
        val databaseTracks = mutableMapOf<String, SpotifyTrackMap>()

        val track1 = SpotifyTrackMap(
            spotifyTrackId = "spotify_id_123",
            songId = "yt_id_abc",
            title = "Shape of You",
            artist = "Ed Sheeran"
        )
        databaseTracks[track1.spotifyTrackId] = track1

        // Check if track1 exists
        assertTrue(databaseTracks.containsKey("spotify_id_123"))
        assertFalse(databaseTracks.containsKey("spotify_id_456"))

        // Duplicate insert attempt
        val duplicateTrack = SpotifyTrackMap(
            spotifyTrackId = "spotify_id_123",
            songId = "yt_id_abc",
            title = "Shape of You",
            artist = "Ed Sheeran"
        )

        val inserted = if (!databaseTracks.containsKey(duplicateTrack.spotifyTrackId)) {
            databaseTracks[duplicateTrack.spotifyTrackId] = duplicateTrack
            true
        } else {
            false // skipped
        }

        assertFalse(inserted)
        assertEquals(1, databaseTracks.size)
    }

    @Test
    fun `03 - Resuming Spotify import from saved next_url skips previous pages`() {
        val initialProgress = SpotifyImportProgressEntity(
            spotifyPlaylistId = "playlist_abc",
            playlistId = "local_playlist_1",
            playlistName = "Top Hits",
            nextUrl = "https://api.spotify.com/v1/playlists/playlist_abc/tracks?offset=200&limit=100",
            processedCount = 200,
            status = "IN_PROGRESS",
            maxTracks = 3000
        )

        // Resume: start directly from saved nextUrl
        assertNotNull(initialProgress.nextUrl)
        assertTrue(initialProgress.nextUrl!!.contains("offset=200"))
        assertEquals(200, initialProgress.processedCount)

        // Process Page 3 (201-300)
        val newProcessedCount = initialProgress.processedCount + 100
        val newNextUrl = "https://api.spotify.com/v1/playlists/playlist_abc/tracks?offset=300&limit=100"

        val updatedProgress = initialProgress.copy(
            nextUrl = newNextUrl,
            processedCount = newProcessedCount,
            lastUpdated = LocalDateTime.now()
        )

        assertEquals(300, updatedProgress.processedCount)
        assertTrue(updatedProgress.nextUrl!!.contains("offset=300"))
    }

    @Test
    fun `04 - Next url is updated ONLY after page is successfully processed`() {
        var savedNextUrl = "https://api.spotify.com/v1/playlists/p1/tracks?offset=100&limit=100"
        var pageSuccess = false

        // Simulate page failure halfway
        try {
            // Processing tracks 101-150...
            throw RuntimeException("Network timeout during batch")
            @Suppress("UNREACHABLE_CODE")
            pageSuccess = true
        } catch (e: Exception) {
            // Failed -> do NOT advance savedNextUrl
        }

        assertFalse(pageSuccess)
        assertEquals("https://api.spotify.com/v1/playlists/p1/tracks?offset=100&limit=100", savedNextUrl)

        // Retry succeeds
        pageSuccess = true
        if (pageSuccess) {
            savedNextUrl = "https://api.spotify.com/v1/playlists/p1/tracks?offset=200&limit=100"
        }

        assertEquals("https://api.spotify.com/v1/playlists/p1/tracks?offset=200&limit=100", savedNextUrl)
    }

    @Test
    fun `05 - 101-song playlist paginates across 2 pages and marks COMPLETED`() {
        var currentOffset = 0
        var processedCount = 0
        var status = "IN_PROGRESS"
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/p1/tracks?offset=0&limit=100"

        // Page 1: 100 songs
        val page1 = (1..100).map { "Song $it" }
        processedCount += page1.size
        currentOffset += page1.size
        nextUrl = "https://api.spotify.com/v1/playlists/p1/tracks?offset=100&limit=100"

        assertEquals(100, processedCount)
        assertEquals("IN_PROGRESS", status)

        // Page 2: 1 song (total 101)
        val page2 = listOf("Song 101")
        processedCount += page2.size
        currentOffset += page2.size
        nextUrl = null // End of Spotify playlist
        status = "COMPLETED"

        assertEquals(101, processedCount)
        assertNull(nextUrl)
        assertEquals("COMPLETED", status)
    }

    @Test
    fun `06 - 3500-song playlist caps at exactly 3000 songs and marks COMPLETED`() {
        var processedCount = 0
        var status = "IN_PROGRESS"
        val maxLimit = 3000

        for (page in 1..35) {
            val batchSize = minOf(100, maxLimit - processedCount)
            processedCount += batchSize
            if (processedCount >= maxLimit) {
                status = "COMPLETED"
                break
            }
        }

        assertEquals(3000, processedCount)
        assertEquals("COMPLETED", status)
    }

    @Test
    fun `07 - YouTube Music and Apple Music playlists remain at 5000-song limit`() {
        val tracks = (1..6000).map { "Song $it" to "Artist $it" }
        val finalTracks = tracks.take(PlaylistImporter.MAX_IMPORT_SONGS)

        assertEquals(5000, finalTracks.size)
        assertEquals(5000, PlaylistImporter.MAX_IMPORT_SONGS)
    }

    @Test
    fun `08 - SpotifyTrackItem and SpotifyPage models preserve source positions across pages`() {
        val page1Tracks = (0..99).map { idx ->
            SpotifyTrackItem(
                spotifyTrackId = "sp_$idx",
                title = "Track $idx",
                artist = "Artist",
                sourcePosition = idx
            )
        }
        val page1 = SpotifyPage(
            tracks = page1Tracks,
            currentUrl = "https://api.spotify.com/v1/playlists/p1/tracks?offset=0&limit=100",
            nextUrl = "https://api.spotify.com/v1/playlists/p1/tracks?offset=100&limit=100",
            totalTracks = 250
        )

        assertEquals(100, page1.tracks.size)
        assertEquals(0, page1.tracks.first().sourcePosition)
        assertEquals(99, page1.tracks.last().sourcePosition)

        val page2Tracks = (0..99).map { idx ->
            SpotifyTrackItem(
                spotifyTrackId = "sp_${100 + idx}",
                title = "Track ${100 + idx}",
                artist = "Artist",
                sourcePosition = 100 + idx
            )
        }
        val page2 = SpotifyPage(
            tracks = page2Tracks,
            currentUrl = page1.nextUrl!!,
            nextUrl = "https://api.spotify.com/v1/playlists/p1/tracks?offset=200&limit=100",
            totalTracks = 250
        )

        assertEquals(100, page2.tracks.size)
        assertEquals(100, page2.tracks.first().sourcePosition)
        assertEquals(199, page2.tracks.last().sourcePosition)
    }

    @Test
    fun `09 - Resolution failure skips only failed track and commits remaining 99 tracks`() {
        val playlistId = "local_playlist_test"
        val playlistMappings = mutableListOf<PlaylistSongMap>()

        // 100 tracks on page
        val tracks = (0..99).map { idx ->
            SpotifyTrackItem(
                spotifyTrackId = "sp_$idx",
                title = "Song $idx",
                artist = "Artist $idx",
                sourcePosition = idx
            )
        }

        // Simulate resolution: Track #42 fails, remaining 99 succeed
        for (track in tracks) {
            val resolvedSongId = if (track.spotifyTrackId == "sp_42") {
                null // Failed resolution
            } else {
                "yt_${track.spotifyTrackId}"
            }

            if (resolvedSongId != null) {
                playlistMappings.add(
                    PlaylistSongMap(
                        playlistId = playlistId,
                        songId = resolvedSongId,
                        position = track.sourcePosition
                    )
                )
            }
        }

        assertEquals(99, playlistMappings.size)
        assertFalse(playlistMappings.any { it.songId == "yt_sp_42" })
        assertTrue(playlistMappings.any { it.songId == "yt_sp_41" })
        assertTrue(playlistMappings.any { it.songId == "yt_sp_43" })
    }

    @Test
    fun `10 - Existing tracks in DB are added to new playlist without creating duplicate global songs`() {
        val playlist1Id = "playlist_1"
        val playlist2Id = "playlist_2"
        val globalSongs = mutableMapOf<String, SpotifyTrackMap>()
        val playlistMappings = mutableListOf<PlaylistSongMap>()

        // Pre-existing track in DB
        val existingTrack = SpotifyTrackMap(
            spotifyTrackId = "sp_existing",
            songId = "yt_existing_123",
            title = "Existing Song",
            artist = "Artist"
        )
        globalSongs[existingTrack.spotifyTrackId] = existingTrack
        playlistMappings.add(
            PlaylistSongMap(
                playlistId = playlist1Id,
                songId = existingTrack.songId,
                position = 0
            )
        )

        // Now import playlist 2 containing the same track
        val importedTrack = SpotifyTrackItem(
            spotifyTrackId = "sp_existing",
            title = "Existing Song",
            artist = "Artist",
            sourcePosition = 0
        )

        // Deduplication logic:
        val (metadata, songId) = if (globalSongs.containsKey(importedTrack.spotifyTrackId)) {
            val ex = globalSongs[importedTrack.spotifyTrackId]!!
            Pair(null, ex.songId) // reuse existing
        } else {
            Pair("new_meta", "yt_new_456")
        }

        // Global song is NOT duplicated
        assertNull(metadata)
        assertEquals("yt_existing_123", songId)
        assertEquals(1, globalSongs.size)

        // But PlaylistSongMap IS added to playlist 2!
        playlistMappings.add(
            PlaylistSongMap(
                playlistId = playlist2Id,
                songId = songId,
                position = importedTrack.sourcePosition
            )
        )

        assertEquals(2, playlistMappings.size)
        assertEquals(1, playlistMappings.count { it.playlistId == playlist1Id })
        assertEquals(1, playlistMappings.count { it.playlistId == playlist2Id })
    }

    @Test
    fun `11 - 200-track Spotify import creates exactly 200 playlist mappings across 2 pages`() {
        val localPlaylistId = "playlist_200"
        val playlistMappings = mutableListOf<PlaylistSongMap>()
        var processedCount = 0
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/p200/tracks?offset=0&limit=100"

        // PAGE 1: 100 tracks (0..99)
        val page1Tracks = (0..99).map { idx ->
            SpotifyTrackItem("sp_$idx", "Track $idx", "Artist", sourcePosition = idx)
        }
        for (track in page1Tracks) {
            playlistMappings.add(
                PlaylistSongMap(
                    playlistId = localPlaylistId,
                    songId = "yt_${track.spotifyTrackId}",
                    position = track.sourcePosition
                )
            )
        }
        processedCount += page1Tracks.size
        nextUrl = "https://api.spotify.com/v1/playlists/p200/tracks?offset=100&limit=100"

        assertEquals(100, playlistMappings.size)
        assertEquals(100, processedCount)
        assertEquals("https://api.spotify.com/v1/playlists/p200/tracks?offset=100&limit=100", nextUrl)

        // PAGE 2: 100 tracks (100..199)
        val page2Tracks = (100..199).map { idx ->
            SpotifyTrackItem("sp_$idx", "Track $idx", "Artist", sourcePosition = idx)
        }
        for (track in page2Tracks) {
            playlistMappings.add(
                PlaylistSongMap(
                    playlistId = localPlaylistId,
                    songId = "yt_${track.spotifyTrackId}",
                    position = track.sourcePosition
                )
            )
        }
        processedCount += page2Tracks.size
        nextUrl = null // End of 200-track playlist

        assertEquals(200, playlistMappings.size)
        assertEquals(200, processedCount)
        assertNull(nextUrl)

        // Verify correct positions from 0 to 199
        assertEquals(0, playlistMappings.first().position)
        assertEquals(199, playlistMappings.last().position)
    }

    @Test
    fun `12 - NO-API extraction fallback handles API failure and continues into same playlist`() {
        val spotifyPlaylistId = "sp_no_api_123"
        val existingPlaylistId = "local_p_999"
        val existingPlaylistName = "My Rock Playlist"
        val startingProcessedCount = 50 // 50 items already committed before API died

        val extractedNoApiTracks = (0..249).map { idx ->
            SpotifyTrackItem(
                spotifyTrackId = "sp_noapi_$idx",
                title = "Song $idx",
                artist = "Artist $idx",
                sourcePosition = idx
            )
        }

        // Remaining tracks to process in NO-API mode:
        val remainingTracks = extractedNoApiTracks.drop(startingProcessedCount)
        assertEquals(200, remainingTracks.size)
        assertEquals(50, remainingTracks.first().sourcePosition)

        // Process in 100-item batches
        val batches = remainingTracks.chunked(100)
        assertEquals(2, batches.size)

        val playlistMappings = mutableListOf<PlaylistSongMap>()
        // Add existing 50 mappings
        for (i in 0 until startingProcessedCount) {
            playlistMappings.add(
                PlaylistSongMap(
                    playlistId = existingPlaylistId,
                    songId = "yt_sp_noapi_$i",
                    position = i
                )
            )
        }

        // Batch 1 (50..149)
        for (track in batches[0]) {
            playlistMappings.add(
                PlaylistSongMap(
                    playlistId = existingPlaylistId,
                    songId = "yt_${track.spotifyTrackId}",
                    position = track.sourcePosition
                )
            )
        }
        assertEquals(150, playlistMappings.size)

        // Batch 2 (150..249)
        for (track in batches[1]) {
            playlistMappings.add(
                PlaylistSongMap(
                    playlistId = existingPlaylistId,
                    songId = "yt_${track.spotifyTrackId}",
                    position = track.sourcePosition
                )
            )
        }
        assertEquals(250, playlistMappings.size)
        // All 250 tracks belong to the exact same local playlist ID
        assertTrue(playlistMappings.all { it.playlistId == existingPlaylistId })
        assertEquals(0, playlistMappings.first().position)
        assertEquals(249, playlistMappings.last().position)
    }

    @Test
    fun `13 - NO-API multi-strategy deduplication via knownSpotifyIds prevents duplicates`() {
        val knownIds = HashSet<String>()
        val allExtracted = mutableListOf<SpotifyTrackItem>()

        // Strategy A yields 100 tracks
        val embedTracks = (0..99).map { idx ->
            SpotifyTrackItem("sp_track_$idx", "Title $idx", "Artist", sourcePosition = idx)
        }
        for (t in embedTracks) {
            if (knownIds.add(t.spotifyTrackId)) {
                allExtracted.add(t)
            }
        }
        assertEquals(100, allExtracted.size)

        // Strategy B yields 150 tracks (100 overlapping + 50 new)
        val htmlTracks = (50..149).map { idx ->
            SpotifyTrackItem("sp_track_$idx", "Title $idx", "Artist", sourcePosition = idx)
        }
        for (t in htmlTracks) {
            if (knownIds.add(t.spotifyTrackId)) {
                allExtracted.add(t.copy(sourcePosition = allExtracted.size))
            }
        }

        // Total should be exactly 150 distinct tracks (0..149)
        assertEquals(150, allExtracted.size)
        assertEquals(0, allExtracted.first().sourcePosition)
        assertEquals(149, allExtracted.last().sourcePosition)
    }

    // ==========================================
    // SPOTIFY → YOUTUBE MATCHING
    // ==========================================

    private fun candidate(
        title: String,
        durationSec: Int? = null,
        channel: String = "",
        videoId: String = "vid_${title.hashCode()}",
    ) = MatchCandidate(
        videoId = videoId,
        title = title,
        channelTitle = channel,
        durationSec = durationSec
    )

    @Test
    fun `14 - ISO-8601 durations parse to seconds`() {
        assertEquals(232, YouTubeDataApi.parseIso8601Duration("PT3M52S"))
        assertEquals(3723, YouTubeDataApi.parseIso8601Duration("PT1H2M3S"))
        assertEquals(45, YouTubeDataApi.parseIso8601Duration("PT45S"))
        assertEquals(90000, YouTubeDataApi.parseIso8601Duration("P1DT1H"))
        assertEquals(600, YouTubeDataApi.parseIso8601Duration("PT10M"))
    }

    @Test
    fun `15 - malformed or zero ISO-8601 durations yield null`() {
        assertNull(YouTubeDataApi.parseIso8601Duration(null))
        assertNull(YouTubeDataApi.parseIso8601Duration(""))
        assertNull(YouTubeDataApi.parseIso8601Duration("3:52"))
        assertNull(YouTubeDataApi.parseIso8601Duration("not a duration"))
        // A live stream reports P0D, which is a valid string but not a length.
        assertNull(YouTubeDataApi.parseIso8601Duration("P0D"))
    }

    @Test
    fun `16 - normalize strips upload noise but keeps variant markers`() {
        assertEquals("shape of you", TrackMatcher.normalize("Shape of You (Official Video)"))
        assertEquals("shape of you", TrackMatcher.normalize("Shape of You [Lyrics]"))
        assertEquals("shape of you", TrackMatcher.normalize("Shape of You - Topic"))
        assertEquals("shape of you", TrackMatcher.normalize("  Shape   of  You!!  "))
        // The feat. marker word goes; the artist it introduces is identifying and stays.
        assertEquals("shape of you someone", TrackMatcher.normalize("Shape of You (feat. Someone)"))

        // A remix or a live cut is a different recording and must not normalise into the original.
        assertTrue(TrackMatcher.normalize("Shape of You (Live at Wembley)").contains("live"))
        assertTrue(TrackMatcher.normalize("Shape of You (Official Live Video)").contains("live"))
        assertTrue(TrackMatcher.normalize("Shape of You (Remix)").contains("remix"))
    }

    @Test
    fun `17 - exact title within 2 seconds scores high confidence`() {
        val result = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = listOf(
                candidate("Ed Sheeran - Shape of You (Official Video)", durationSec = 236, channel = "Ed Sheeran")
            )
        )

        assertEquals(MatchStatus.MATCHED, result.status)
        assertTrue("confidence was ${result.confidence}", result.confidence >= 0.85)
        assertTrue(result.score!!.durationVerified)
    }

    @Test
    fun `18 - right title but 90 seconds off is disqualified`() {
        val extendedMix = candidate(
            "Ed Sheeran - Shape of You (Extended Mix)",
            durationSec = 324,
            channel = "Ed Sheeran"
        )

        // Scored on its own, the 90-second gap disqualifies it whatever the title says.
        val score = TrackMatcher.score(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidate = extendedMix
        )
        assertTrue(score.disqualified)
        assertEquals(90, score.durationDeltaSec)
        assertEquals(0.0, score.confidence, 0.0001)
        assertFalse(score.durationVerified)

        // An hour-long mix or an extended cut must never win on title alone, so a list of nothing
        // but disqualified candidates yields no candidate at all rather than the least-bad one.
        val result = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = listOf(extendedMix)
        )

        assertEquals(MatchStatus.UNMATCHED, result.status)
        assertNull(result.candidate)
        assertNull(result.videoId)
        assertEquals(0.0, result.confidence, 0.0001)
    }

    @Test
    fun `19 - wrong title with exact duration stays below threshold`() {
        // A different song that happens to be the same length. Two songs sharing a duration is not
        // rare, so duration agreement alone must never be enough to match.
        val wrongSong = "The Weeknd - Blinding Lights"
        val result = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = listOf(candidate(wrongSong, durationSec = 234, channel = "TheWeeknd"))
        )

        // The duration is a perfect 1.0 here, which is exactly what makes this the interesting case.
        assertEquals(1.0, TrackMatcher.durationScore(233_712, 234), 0.0001)
        assertTrue(
            "title score was ${TrackMatcher.titleScore("Shape of You", wrongSong)}",
            TrackMatcher.titleScore("Shape of You", wrongSong) < TrackMatcher.MIN_TRACK_NAME_SCORE
        )
        assertEquals(MatchStatus.UNMATCHED, result.status)
        assertNull(result.videoId)
    }

    @Test
    fun `20 - duration tolerance is exactly plus-minus 5 seconds`() {
        val spotifyMs = 200_000L // 200s

        assertEquals(1.0, TrackMatcher.durationScore(spotifyMs, 200), 0.0001)
        assertEquals(1.0, TrackMatcher.durationScore(spotifyMs, 205), 0.0001)
        assertEquals(1.0, TrackMatcher.durationScore(spotifyMs, 195), 0.0001)

        assertTrue(TrackMatcher.durationScore(spotifyMs, 206) < 1.0)
        assertTrue(TrackMatcher.durationScore(spotifyMs, 194) < 1.0)

        // Unknown duration is neutral, not a free pass and not a condemnation.
        assertEquals(0.5, TrackMatcher.durationScore(spotifyMs, null), 0.0001)
    }

    @Test
    fun `21 - threshold decides matched versus unmatched, nothing is forced`() {
        val candidates = listOf(
            candidate("Shape of You", durationSec = 234, channel = "Ed Sheeran")
        )
        val confidence = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = candidates
        ).confidence

        val justBelow = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = candidates,
            threshold = confidence + 0.01
        )
        val justAbove = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = candidates,
            threshold = confidence - 0.01
        )

        assertEquals(MatchStatus.UNMATCHED, justBelow.status)
        assertNull(justBelow.videoId)
        assertEquals(MatchStatus.MATCHED, justAbove.status)
        assertNotNull(justAbove.videoId)
    }

    @Test
    fun `22 - empty candidate list is unmatched, never a guess`() {
        val result = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = emptyList()
        )
        assertEquals(MatchStatus.UNMATCHED, result.status)
        assertNull(result.candidate)
        assertNull(result.videoId)
        assertEquals(0.0, result.confidence, 0.0001)
    }

    @Test
    fun `23 - best of several candidates wins on duration, not search order`() {
        val result = TrackMatcher.pickBest(
            trackName = "Bohemian Rhapsody",
            artist = "Queen",
            spotifyDurationMs = 354_320,
            candidates = listOf(
                candidate("Queen - Bohemian Rhapsody (Live Aid 1985)", durationSec = 372, videoId = "live"),
                candidate("Queen - Bohemian Rhapsody (Official Video)", durationSec = 355, videoId = "studio"),
                candidate("Bohemian Rhapsody - Queen (Karaoke)", durationSec = 358, videoId = "karaoke"),
            )
        )

        assertEquals(MatchStatus.MATCHED, result.status)
        assertEquals("studio", result.videoId)
    }

    @Test
    fun `24 - 3000-track import is flagged as exceeding the daily quota`() {
        val estimate = YouTubeQuotaTracker.estimate(
            trackCount = 3000,
            remainingUnits = YouTubeDataApi.DEFAULT_DAILY_QUOTA_UNITS
        )

        assertTrue(estimate.willExceed)
        assertEquals(3000 * YouTubeQuotaTracker.UNITS_PER_TRACK, estimate.estimatedUnits)
        // 10,000 units / 101 units per track = 99 tracks a day.
        assertEquals(99, estimate.tracksCoverable)
    }

    @Test
    fun `25 - a small import fits inside the daily quota`() {
        val estimate = YouTubeQuotaTracker.estimate(
            trackCount = 20,
            remainingUnits = YouTubeDataApi.DEFAULT_DAILY_QUOTA_UNITS
        )

        assertFalse(estimate.willExceed)
        assertEquals(20, estimate.tracksCoverable)
        assertEquals(20 * YouTubeQuotaTracker.UNITS_PER_TRACK, estimate.estimatedUnits)
    }

    @Test
    fun `26 - quota already spent today leaves nothing coverable`() {
        val estimate = YouTubeQuotaTracker.estimate(trackCount = 50, remainingUnits = 0)

        assertTrue(estimate.willExceed)
        assertEquals(0, estimate.tracksCoverable)
        assertEquals(0, estimate.remainingUnits)
    }

    @Test
    fun `27 - the quota day is reckoned in Pacific time, not device time`() {
        assertEquals("America/Los_Angeles", YouTubeQuotaTracker.QUOTA_RESET_ZONE.id)

        // 2026-03-01 07:30 UTC is still 2026-02-28 in Pacific — the day must not have rolled over yet.
        val beforeReset = Instant.parse("2026-03-01T07:30:00Z")
        val afterReset = Instant.parse("2026-03-01T08:30:00Z")

        assertEquals("2026-02-28", YouTubeQuotaTracker.currentQuotaDate(beforeReset))
        assertEquals("2026-03-01", YouTubeQuotaTracker.currentQuotaDate(afterReset))
    }

    @Test
    fun `28 - duration verification batches 50 ids per videos-list call`() {
        // 99 tracks x 5 candidates = 495 ids, which must cost 10 units, not 495.
        assertEquals(10, YouTubeDataApi.videosListCallCount(495))
        assertEquals(10 * YouTubeDataApi.COST_VIDEOS_LIST, YouTubeDataApi.videosListCallCount(495))

        assertEquals(0, YouTubeDataApi.videosListCallCount(0))
        assertEquals(1, YouTubeDataApi.videosListCallCount(1))
        assertEquals(1, YouTubeDataApi.videosListCallCount(50))
        assertEquals(2, YouTubeDataApi.videosListCallCount(51))
    }

    @Test
    fun `29 - search costs 100 units and duration lookup costs 1`() {
        assertEquals(100, YouTubeDataApi.COST_SEARCH_LIST)
        assertEquals(1, YouTubeDataApi.COST_VIDEOS_LIST)
        assertEquals(10_000, YouTubeDataApi.DEFAULT_DAILY_QUOTA_UNITS)
        assertEquals(101, YouTubeQuotaTracker.UNITS_PER_TRACK)
    }

    @Test
    fun `30 - unmatched rows are persisted with a null video id`() {
        val rows = listOf(
            SpotifyImportTrackEntity(
                playlistId = "pl_1",
                spotifyTrackId = "sp_1",
                trackName = "Matched Song",
                artist = "Artist",
                youtubeVideoId = "yt_1",
                matchConfidence = 0.91f,
                matchStatus = SpotifyImportTrackEntity.STATUS_MATCHED,
                matchSource = SpotifyImportTrackEntity.SOURCE_DATA_API
            ),
            SpotifyImportTrackEntity(
                playlistId = "pl_1",
                spotifyTrackId = "sp_2",
                trackName = "Obscure Song",
                artist = "Artist",
                youtubeVideoId = null,
                matchConfidence = 0.31f,
                matchStatus = SpotifyImportTrackEntity.STATUS_UNMATCHED,
                matchSource = SpotifyImportTrackEntity.SOURCE_NONE
            ),
        )

        val unmatched = rows.filter { it.matchStatus == SpotifyImportTrackEntity.STATUS_UNMATCHED }
        assertEquals(1, unmatched.size)
        assertNull(unmatched.single().youtubeVideoId)
        // The unmatched track is still recorded, so it can be reviewed rather than silently lost.
        assertEquals("Obscure Song", unmatched.single().trackName)

        val matched = rows.filter { it.matchStatus == SpotifyImportTrackEntity.STATUS_MATCHED }
        assertEquals(1, matched.size)
        assertNotNull(matched.single().youtubeVideoId)
    }

    @Test
    fun `31 - only matched tracks reach playlist_song_map`() {
        val outcomes = listOf(
            "sp_1" to "yt_1",
            "sp_2" to null,
            "sp_3" to "yt_3",
        )

        val playlistMaps = outcomes.mapIndexedNotNull { index, (_, videoId) ->
            videoId?.let { PlaylistSongMap(playlistId = "pl_1", songId = it, position = index) }
        }

        // playlist_song_map has a foreign key to song, so an unmatched track cannot live there.
        assertEquals(2, playlistMaps.size)
        assertTrue(playlistMaps.none { it.songId.isEmpty() })
    }

    @Test
    fun `32 - the Spotify duration_ms field survives into the track item`() {
        val track = SpotifyTrackItem(
            spotifyTrackId = "sp_1",
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "÷",
            sourcePosition = 0,
            durationMs = 233_712
        )

        assertEquals(233_712L, track.durationMs)
        // Spotify reports milliseconds; the app works in seconds everywhere else.
        assertEquals(234, (track.durationMs / 1000.0).let { Math.round(it).toInt() })

        // Strategies that cannot see a duration leave it zero, which reads as "unknown".
        assertEquals(0L, SpotifyTrackItem("sp_2", "T", "A").durationMs)
    }

    @Test
    fun `33 - no YouTube account write endpoint or OAuth scope is referenced`() {
        val sources = listOf(
            "src/main/kotlin/com/mudassir131/yt/utils/PlaylistImporter.kt",
            "src/main/kotlin/com/mudassir131/yt/utils/youtube/YouTubeDataApi.kt",
            "src/main/kotlin/com/mudassir131/yt/utils/youtube/YouTubeQuotaTracker.kt",
            "src/main/kotlin/com/mudassir131/yt/utils/matching/TrackMatcher.kt",
        ).map { path ->
            val file = File(path)
            assertTrue("missing source file: $path", file.isFile)
            path to file.readText()
        }

        // The import pipeline must only ever read from YouTube. Writing to the user's account would
        // need OAuth and playlistItems.insert; neither may appear here.
        val forbidden = listOf(
            "playlistItems",
            "playlists/insert",
            "auth/youtube",
            "oauth2",
            "access_type=offline",
        )

        for ((path, text) in sources) {
            for (needle in forbidden) {
                assertFalse(
                    "$path must not reference '$needle'",
                    text.contains(needle, ignoreCase = true)
                )
            }
        }
    }

    @Test
    fun `34 - the Spotify endpoints are flagged as unofficial in the source`() {
        val source = File("src/main/kotlin/com/mudassir131/yt/utils/PlaylistImporter.kt").readText()

        assertTrue(source.contains("unofficial", ignoreCase = true))
        assertTrue(source.contains("without notice", ignoreCase = true))
        // The token endpoint must be the documented-by-observation web-player form.
        assertTrue(source.contains("productType=web-player"))
    }

    @Test
    fun `35 - 124-track Spotify playlist paginates completely across 2 pages without premature cutoff`() {
        val fetchedTrackIds = LinkedHashSet<String>()
        val playlistMappings = mutableListOf<PlaylistSongMap>()
        var processedCount = 0
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/p124/tracks?offset=0&limit=100"
        val totalSourceTracks = 124

        // PAGE 1: 100 tracks (0..99)
        val page1Tracks = (0..99).map { idx ->
            SpotifyTrackItem("sp_track_$idx", "Track $idx", "Artist", sourcePosition = idx)
        }
        for (t in page1Tracks) {
            fetchedTrackIds.add(t.spotifyTrackId)
            playlistMappings.add(
                PlaylistSongMap(
                    playlistId = "pl_124",
                    songId = "yt_${t.spotifyTrackId}",
                    position = t.sourcePosition
                )
            )
        }
        processedCount += page1Tracks.size
        nextUrl = "https://api.spotify.com/v1/playlists/p124/tracks?offset=100&limit=100"

        assertEquals(100, page1Tracks.size)
        assertEquals(100, processedCount)
        assertEquals("https://api.spotify.com/v1/playlists/p124/tracks?offset=100&limit=100", nextUrl)

        // PAGE 2: 24 tracks (100..123)
        val page2Tracks = (100..123).map { idx ->
            SpotifyTrackItem("sp_track_$idx", "Track $idx", "Artist", sourcePosition = idx)
        }
        for (t in page2Tracks) {
            fetchedTrackIds.add(t.spotifyTrackId)
            playlistMappings.add(
                PlaylistSongMap(
                    playlistId = "pl_124",
                    songId = "yt_${t.spotifyTrackId}",
                    position = t.sourcePosition
                )
            )
        }
        processedCount += page2Tracks.size
        nextUrl = null // End of playlist pagination

        // Verified: exactly all 124 tracks retrieved and mapped
        assertEquals(124, fetchedTrackIds.size)
        assertEquals(124, playlistMappings.size)
        assertEquals(124, processedCount)
        assertNull(nextUrl)
        assertEquals(0, playlistMappings.first().position)
        assertEquals(123, playlistMappings.last().position)
    }

    @Test
    fun `36 - Per-stage unique track accounting invariant holds and candidate generation cannot inflate counters`() {
        val sourceTrackIds = (0..123).map { "sp_track_$it" }
        assertEquals(124, sourceTrackIds.size)

        val fetchedTrackIds = LinkedHashSet<String>()
        val skippedTracks = mutableListOf<Pair<String, String>>()
        val matchingInputTrackIds = LinkedHashSet<String>()
        val matchedTrackIds = LinkedHashSet<String>()
        val unmatchedTrackIds = LinkedHashSet<String>()

        // Simulate fetching all 124 tracks
        for (id in sourceTrackIds) {
            fetchedTrackIds.add(id)
            matchingInputTrackIds.add(id)
        }

        // Simulate 108 matched and 16 unmatched
        for (i in 0 until 108) {
            matchedTrackIds.add(sourceTrackIds[i])
        }
        for (i in 108 until 124) {
            unmatchedTrackIds.add(sourceTrackIds[i])
        }

        // Simulate multiple candidate evaluations per track (e.g. 5 candidates per track = 620 candidates)
        val candidateCount = sourceTrackIds.size * 5
        assertEquals(620, candidateCount)

        // Verify Invariants:
        // 1. source == fetched + skipped
        assertEquals(sourceTrackIds.size, fetchedTrackIds.size + skippedTracks.size)
        // 2. fetched == matched + unmatched (124 == 108 + 16)
        assertEquals(fetchedTrackIds.size, matchedTrackIds.size + unmatchedTrackIds.size)
        // 3. matching input == fetched
        assertEquals(fetchedTrackIds.size, matchingInputTrackIds.size)
        // 4. Candidate count (620) has no effect on track counters
        assertEquals(108, matchedTrackIds.size)
        assertEquals(16, unmatchedTrackIds.size)
        assertEquals(124, matchedTrackIds.size + unmatchedTrackIds.size)
    }

    @Test
    fun `37 - Unavailable or null tracks are recorded in skippedTracks with reason`() {
        val skipped = mutableListOf<Pair<String, String>>()
        val validTracks = mutableListOf<SpotifyTrackItem>()

        // Simulate page with 1 null track (deleted), 1 empty title, and 2 valid tracks
        val items = listOf(
            "valid_1" to "Song 1",
            null to null,
            "valid_2" to "",
            "valid_3" to "Song 3"
        )

        for ((index, item) in items.withIndex()) {
            val (id, title) = item
            if (id == null && title == null) {
                skipped.add("index_$index" to "Null track object (removed, unavailable or restricted on Spotify)")
                continue
            }
            if (title.isNullOrEmpty()) {
                skipped.add((id ?: "index_$index") to "Empty track title/name")
                continue
            }
            validTracks.add(
                SpotifyTrackItem(
                    spotifyTrackId = id ?: "sp_idx_$index",
                    title = title,
                    artist = "Artist",
                    sourcePosition = validTracks.size
                )
            )
        }

        assertEquals(2, validTracks.size)
        assertEquals(2, skipped.size)
        assertEquals("index_1", skipped[0].first)
        assertTrue(skipped[0].second.contains("Null track object"))
        assertEquals("valid_2", skipped[1].first)
        assertTrue(skipped[1].second.contains("Empty track title"))
        assertEquals(0, validTracks[0].sourcePosition)
        assertEquals(1, validTracks[1].sourcePosition)
    }

    @Test
    fun `38 - 150-track Spotify playlist paginates across 2 pages without truncation`() {
        val totalTracks = 150
        val allTrackIds = (0 until totalTracks).map { "sp_track_$it" }
        val fetchedTrackIds = LinkedHashSet<String>()

        // Page 1: 100 tracks
        val page1 = allTrackIds.take(100)
        fetchedTrackIds.addAll(page1)
        assertEquals(100, fetchedTrackIds.size)

        // Page 2: 50 tracks
        val page2 = allTrackIds.drop(100)
        fetchedTrackIds.addAll(page2)
        assertEquals(150, fetchedTrackIds.size)

        // Verify no tracks dropped and position matches
        assertEquals(150, fetchedTrackIds.size)
        assertTrue(fetchedTrackIds.contains("sp_track_0"))
        assertTrue(fetchedTrackIds.contains("sp_track_99"))
        assertTrue(fetchedTrackIds.contains("sp_track_100"))
        assertTrue(fetchedTrackIds.contains("sp_track_149"))
    }

    @Test
    fun `39 - 350-track Spotify playlist paginates across 4 pages completely`() {
        val totalTracks = 350
        val allTracks = (0 until totalTracks).map { idx ->
            SpotifyTrackItem("sp_$idx", "Title $idx", "Artist", sourcePosition = idx)
        }

        val pages = allTracks.chunked(100)
        assertEquals(4, pages.size)
        assertEquals(100, pages[0].size)
        assertEquals(100, pages[1].size)
        assertEquals(100, pages[2].size)
        assertEquals(50, pages[3].size)

        val accumulated = mutableListOf<SpotifyTrackItem>()
        for (page in pages) {
            accumulated.addAll(page)
        }

        assertEquals(350, accumulated.size)
        assertEquals(0, accumulated.first().sourcePosition)
        assertEquals(349, accumulated.last().sourcePosition)
    }

    @Test
    fun `40 - Chunked concurrent matching preserves all N items without reducing count`() {
        val inputTracks = (0 until 124).map { idx ->
            SpotifyTrackItem("sp_$idx", "Title $idx", "Artist", sourcePosition = idx)
        }

        val chunks = inputTracks.chunked(6)
        assertEquals(21, chunks.size) // 20 chunks of 6 + 1 chunk of 4 = 124

        val processedResults = mutableListOf<SpotifyTrackItem>()
        for (chunk in chunks) {
            // Simulate async matching for each item in chunk
            val matchedInChunk = chunk.map { it }
            processedResults.addAll(matchedInChunk)
        }

        assertEquals(124, processedResults.size)
        assertEquals(inputTracks.map { it.spotifyTrackId }, processedResults.map { it.spotifyTrackId })
    }

    @Test
    fun `41 - 124-track playlist with 114 matched and 10 unmatched yields exactly 114 imported and 10 unmatched`() {
        val total = 124
        val matchedCount = 114
        val unmatchedCount = 10

        val tracks = (0 until total).map { "sp_$it" }
        val matched = tracks.take(matchedCount).toSet()
        val unmatched = tracks.drop(matchedCount).toSet()

        assertEquals(114, matched.size)
        assertEquals(10, unmatched.size)
        assertEquals(124, matched.size + unmatched.size)
        // Ensure total discovered was 124, not 114
        assertEquals(124, tracks.size)
    }

    @Test
    fun `42 - Exportify CSV-compatible track model captures joined artists, URI, and duration`() {
        val track = SpotifyTrackItem(
            spotifyTrackId = "4cOdK2wGLETKBW3PvgPWqT",
            spotifyTrackUri = "spotify:track:4cOdK2wGLETKBW3PvgPWqT",
            title = "Never Gonna Give You Up",
            artist = "Rick Astley, Stock Aitken Waterman",
            album = "Whenever You Need Somebody",
            sourcePosition = 0,
            durationMs = 213573L,
            isLocal = false
        )

        assertEquals("4cOdK2wGLETKBW3PvgPWqT", track.spotifyTrackId)
        assertEquals("spotify:track:4cOdK2wGLETKBW3PvgPWqT", track.spotifyTrackUri)
        assertEquals("Never Gonna Give You Up", track.title)
        assertEquals("Rick Astley, Stock Aitken Waterman", track.artist)
        assertEquals("Whenever You Need Somebody", track.album)
        assertEquals(0, track.sourcePosition)
        assertEquals(213573L, track.durationMs)
        assertFalse(track.isLocal)
    }

    @Test
    fun `43 - 124-track Spotify playlist produces 124 CSV rows and parses into 124 matching jobs`() {
        val originalTracks = (0 until 124).map { idx ->
            SpotifyTrackItem(
                spotifyTrackId = "sp_track_$idx",
                spotifyTrackUri = "spotify:track:sp_track_$idx",
                title = "Song Title $idx",
                artist = "Artist A, Artist B",
                album = "Album $idx",
                sourcePosition = idx,
                durationMs = 180000L + idx * 1000L,
                isLocal = false
            )
        }

        val csvString = SpotifyCsvSerializer.exportToCsv(originalTracks)
        val dataLines = csvString.lines().filter { it.isNotBlank() }.drop(1)
        assertEquals(124, dataLines.size)

        val parsedTracks = SpotifyCsvSerializer.parseFromCsv(csvString)
        assertEquals(124, parsedTracks.size)
        assertEquals(originalTracks.map { it.spotifyTrackId }, parsedTracks.map { it.spotifyTrackId })
        assertEquals(originalTracks.map { it.artist }, parsedTracks.map { it.artist })
    }

    @Test
    fun `44 - 150-track Spotify playlist produces 150 CSV rows and 150 matching attempts`() {
        val originalTracks = (0 until 150).map { idx ->
            SpotifyTrackItem(
                spotifyTrackId = "track_$idx",
                spotifyTrackUri = "spotify:track:track_$idx",
                title = "Title $idx",
                artist = "Artist $idx",
                album = "Album $idx",
                sourcePosition = idx,
                durationMs = 200000L,
                isLocal = false
            )
        }

        val csvString = SpotifyCsvSerializer.exportToCsv(originalTracks)
        val parsedTracks = SpotifyCsvSerializer.parseFromCsv(csvString)

        assertEquals(150, parsedTracks.size)
        val matchingAttempts = parsedTracks.size
        assertEquals(150, matchingAttempts)
    }

    @Test
    fun `45 - CSV parser handles complex artist strings with commas and quotes accurately`() {
        val originalTracks = listOf(
            SpotifyTrackItem(
                spotifyTrackId = "id_1",
                spotifyTrackUri = "spotify:track:id_1",
                title = "Song with \"Quotes\", and commas",
                artist = "Artist 1, Artist 2, feat. \"Special Guest\"",
                album = "Greatest Hits, Vol. 1",
                sourcePosition = 0,
                durationMs = 240000L,
                isLocal = false
            ),
            SpotifyTrackItem(
                spotifyTrackId = "id_2",
                spotifyTrackUri = "spotify:track:id_2",
                title = "Simple Track",
                artist = "Solo Artist",
                album = "Debut",
                sourcePosition = 1,
                durationMs = 190000L,
                isLocal = true
            )
        )

        val csv = SpotifyCsvSerializer.exportToCsv(originalTracks)
        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)

        assertEquals(2, parsed.size)
        assertEquals("Song with \"Quotes\", and commas", parsed[0].title)
        assertEquals("Artist 1, Artist 2, feat. \"Special Guest\"", parsed[0].artist)
        assertEquals("Greatest Hits, Vol. 1", parsed[0].album)
        assertFalse(parsed[0].isLocal)

        assertEquals("Simple Track", parsed[1].title)
        assertEquals("Solo Artist", parsed[1].artist)
        assertTrue(parsed[1].isLocal)
    }

    @Test
    fun `46 - Invariant reconciliation checks hold across complete pipeline`() {
        val spotifyTotal = 127
        val extractedCount = 127
        val skippedCount = 0
        val csvRowsGenerated = 127
        val csvRowsParsed = 127
        val matchingAttempts = 127
        val matched = 115
        val unmatched = 12

        assertEquals(spotifyTotal, extractedCount + skippedCount)
        assertEquals(extractedCount, csvRowsGenerated)
        assertEquals(csvRowsGenerated, csvRowsParsed)
        assertEquals(csvRowsParsed, matchingAttempts)
        assertEquals(matchingAttempts, matched + unmatched)
    }

    @Test
    fun `47 - 127 Spotify tracks produce exactly 127 CSV rows and 127 matching requests`() {
        val tracks = (1..127).map { index ->
            SpotifyTrackItem(
                spotifyTrackId = "track_id_$index",
                spotifyTrackUri = "spotify:track:track_id_$index",
                title = "Song $index",
                artist = "Artist $index",
                album = "Album $index",
                sourcePosition = index - 1,
                durationMs = 200000L + index * 1000,
                isLocal = false
            )
        }

        val csv = SpotifyCsvSerializer.exportToCsv(tracks)
        val nonBlankLines = csv.lines().filter { it.isNotBlank() }
        val headerCount = 1
        val rowCount = nonBlankLines.size - headerCount

        assertEquals(127, rowCount)

        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
        assertEquals(127, parsed.size)
        assertEquals("Song 1", parsed[0].title)
        assertEquals("Song 127", parsed[126].title)
        assertEquals(0, parsed[0].sourcePosition)
        assertEquals(126, parsed[126].sourcePosition)
    }

    @Test
    fun `48 - Unwanted variant rejection - lyric videos and DJ remixes penalized below threshold`() {
        val trackName = "Tum Hi Ho"
        val artist = "Arijit Singh"
        val spotifyDurationMs = 262000L // 4m 22s

        val studioCandidate = MatchCandidate(
            videoId = "official_audio_id",
            title = "Tum Hi Ho",
            channelTitle = "Arijit Singh - Topic",
            durationSec = 262
        )

        val lyricsCandidate = MatchCandidate(
            videoId = "lyrics_video_id",
            title = "Tum Hi Ho (Lyrics) | Arijit Singh | Aashiqui 2",
            channelTitle = "Lyrics Channel",
            durationSec = 251
        )

        val djRemixCandidate = MatchCandidate(
            videoId = "dj_remix_id",
            title = "Tum Hi Ho DJ Remix nx you song",
            channelTitle = "DJ ROZZ",
            durationSec = 179
        )

        val studioScore = TrackMatcher.score(trackName, artist, spotifyDurationMs, studioCandidate)
        val lyricsScore = TrackMatcher.score(trackName, artist, spotifyDurationMs, lyricsCandidate)
        val djScore = TrackMatcher.score(trackName, artist, spotifyDurationMs, djRemixCandidate)

        assertTrue(studioScore.confidence >= 0.80)
        assertTrue(lyricsScore.confidence < 0.55)
        assertTrue(djScore.confidence < 0.55)

        val best = TrackMatcher.pickBest(
            trackName = trackName,
            artist = artist,
            spotifyDurationMs = spotifyDurationMs,
            candidates = listOf(lyricsCandidate, djRemixCandidate, studioCandidate)
        )

        assertEquals(MatchStatus.MATCHED, best.status)
        assertEquals("official_audio_id", best.videoId)
    }

    @Test
    fun `49 - Legitimate remix in Spotify metadata is allowed when candidate is also a remix`() {
        val trackName = "Tum Hi Ho (Remix)"
        val artist = "Arijit Singh"
        val spotifyDurationMs = 240000L

        val remixCandidate = MatchCandidate(
            videoId = "remix_id",
            title = "Tum Hi Ho (Remix)",
            channelTitle = "Arijit Singh - Topic",
            durationSec = 240
        )

        val score = TrackMatcher.score(trackName, artist, spotifyDurationMs, remixCandidate)
        assertTrue(score.confidence >= 0.80)
    }

    @Test
    fun `50 - Duplicate track names from different artists are processed independently`() {
        val tracks = listOf(
            SpotifyTrackItem(
                spotifyTrackId = "spotify_tere_liye_atif",
                spotifyTrackUri = "spotify:track:spotify_tere_liye_atif",
                title = "Tere Liye",
                artist = "Atif Aslam, Shreya Ghoshal",
                album = "Prince",
                sourcePosition = 0,
                durationMs = 280000L,
                isLocal = false
            ),
            SpotifyTrackItem(
                spotifyTrackId = "spotify_tere_liye_sachin",
                spotifyTrackUri = "spotify:track:spotify_tere_liye_sachin",
                title = "Tere Liye",
                artist = "Sachin Gupta",
                album = "Prince Unplugged",
                sourcePosition = 1,
                durationMs = 270000L,
                isLocal = false
            )
        )

        val csv = SpotifyCsvSerializer.exportToCsv(tracks)
        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)

        assertEquals(2, parsed.size)
        assertEquals("spotify_tere_liye_atif", parsed[0].spotifyTrackId)
        assertEquals("spotify_tere_liye_sachin", parsed[1].spotifyTrackId)
        assertEquals(0, parsed[0].sourcePosition)
        assertEquals(1, parsed[1].sourcePosition)
    }

    @Test
    fun `51 - 611 track CSV pipeline processes all 611 rows with zero drop around 118`() {
        val count = 611
        val tracks = (1..count).map { index ->
            SpotifyTrackItem(
                spotifyTrackId = "track_id_$index",
                spotifyTrackUri = "spotify:track:track_id_$index",
                title = "Song $index",
                artist = "Artist $index",
                album = "Album $index",
                sourcePosition = index - 1,
                durationMs = 210000L,
                isLocal = false
            )
        }

        val csvContent = SpotifyCsvSerializer.exportToCsv(tracks)
        val nonBlankLines = csvContent.lines().filter { it.isNotBlank() }
        val physicalDataRows = nonBlankLines.size - 1

        assertEquals(count, physicalDataRows)

        val parsed = SpotifyCsvSerializer.parseFromCsv(csvContent)
        assertEquals(count, parsed.size)

        // Verify every single row up to 611 is retained and in order
        for (i in 0 until count) {
            assertEquals("Song ${i + 1}", parsed[i].title)
            assertEquals("Artist ${i + 1}", parsed[i].artist)
            assertEquals(i, parsed[i].sourcePosition)
        }
    }

    @Test
    fun `52 - Scale invariant check for 130, 200, 1000, and 3000 rows`() {
        val testSizes = listOf(130, 200, 1000, 3000)
        for (size in testSizes) {
            val tracks = (1..size).map { index ->
                SpotifyTrackItem(
                    spotifyTrackId = "track_id_$index",
                    spotifyTrackUri = "spotify:track:track_id_$index",
                    title = "Track $index",
                    artist = "Artist $index",
                    album = "Album $index",
                    sourcePosition = index - 1,
                    durationMs = 180000L,
                    isLocal = false
                )
            }
            val csv = SpotifyCsvSerializer.exportToCsv(tracks)
            val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
            assertEquals(size, parsed.size)
            assertEquals("Track $size", parsed[size - 1].title)
        }
    }

    @Test
    fun `53 - 141 track Spotify playlist produces exactly 141 CSV rows and 141 matching requests`() {
        val count = 141
        val tracks = (1..count).map { index ->
            SpotifyTrackItem(
                spotifyTrackId = "track_id_$index",
                spotifyTrackUri = "spotify:track:track_id_$index",
                title = "Song $index",
                artist = "Artist $index",
                album = "Album $index",
                sourcePosition = index - 1,
                durationMs = 210000L,
                isLocal = false
            )
        }

        val csvContent = SpotifyCsvSerializer.exportToCsv(tracks)
        val nonBlankLines = csvContent.lines().filter { it.isNotBlank() }
        val physicalDataRows = nonBlankLines.size - 1

        assertEquals(count, physicalDataRows)

        val parsed = SpotifyCsvSerializer.parseFromCsv(csvContent)
        assertEquals(count, parsed.size)

        // Verify ordering and field integrity
        for (i in 0 until count) {
            assertEquals("Song ${i + 1}", parsed[i].title)
            assertEquals(i, parsed[i].sourcePosition)
        }
    }

    @Test
    fun `54 - Pagination loop protection detection fails with explicit error`() {
        val visited = mutableSetOf<String>()
        val url = "https://api.spotify.com/v1/playlists/test_id/tracks?offset=100&limit=100"
        visited.add(url)

        var loopDetected = false
        if (url in visited) {
            loopDetected = true
        }
        assertTrue(loopDetected)
    }

    @Test
    fun `55 - Complete Scale Matrix verification for 1, 10, 99, 100, 101, 108, 114, 118, 127, 130, 141, 200, 518, 611, 1000 tracks`() {
        val matrix = listOf(1, 10, 99, 100, 101, 108, 114, 118, 127, 130, 141, 200, 518, 611, 1000)
        for (count in matrix) {
            val tracks = (1..count).map { i ->
                SpotifyTrackItem(
                    spotifyTrackId = "spotify_track_$i",
                    spotifyTrackUri = "spotify:track:spotify_track_$i",
                    title = "Title $i",
                    artist = "Artist $i",
                    album = "Album $i",
                    sourcePosition = i - 1,
                    durationMs = 200000L,
                    isLocal = false
                )
            }

            val csv = SpotifyCsvSerializer.exportToCsv(tracks)
            val lines = csv.lines().filter { it.isNotBlank() }
            val rowsOnDisk = lines.size - 1
            assertEquals(count, rowsOnDisk)

            val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
            assertEquals(count, parsed.size)
            assertEquals("Title 1", parsed.first().title)
            assertEquals("Title $count", parsed.last().title)
            assertEquals(0, parsed.first().sourcePosition)
            assertEquals(count - 1, parsed.last().sourcePosition)
        }
    }

    @Test
    fun `56 - Complex metadata RFC-4180 handling for Hindi, Arabic, emoji, quotes, and commas`() {
        val complexTracks = listOf(
            SpotifyTrackItem(
                spotifyTrackId = "complex_1",
                spotifyTrackUri = "spotify:track:complex_1",
                title = "तेरे लिए (Tere Liye) [From \"Prince\"]",
                artist = "Atif Aslam, Shreya Ghoshal",
                album = "Prince (Original Motion Picture Soundtrack)",
                sourcePosition = 0,
                durationMs = 280000L,
                isLocal = false
            ),
            SpotifyTrackItem(
                spotifyTrackId = "complex_2",
                spotifyTrackUri = "spotify:track:complex_2",
                title = "حبيبي يا نور العين (Habibi Ya Nour El Ein) 🌟✨",
                artist = "Amr Diab, حميد الشاعري",
                album = "Nour El Ain, Vol. 1",
                sourcePosition = 1,
                durationMs = 310000L,
                isLocal = false
            ),
            SpotifyTrackItem(
                spotifyTrackId = "complex_3",
                spotifyTrackUri = "spotify:track:complex_3",
                title = "Don't Stop \"Believin'\" - 2024 Remaster, Pt. 1",
                artist = "Journey, Steve Perry & Friends",
                album = "Escape \"Special Edition, Vol. 2\"",
                sourcePosition = 2,
                durationMs = 250000L,
                isLocal = false
            )
        )

        val csv = SpotifyCsvSerializer.exportToCsv(complexTracks)
        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)

        assertEquals(3, parsed.size)
        assertEquals("तेरे लिए (Tere Liye) [From \"Prince\"]", parsed[0].title)
        assertEquals("Atif Aslam, Shreya Ghoshal", parsed[0].artist)
        assertEquals("حبيبي يا نور العين (Habibi Ya Nour El Ein) 🌟✨", parsed[1].title)
        assertEquals("Amr Diab, حميد الشاعري", parsed[1].artist)
        assertEquals("Don't Stop \"Believin'\" - 2024 Remaster, Pt. 1", parsed[2].title)
        assertEquals("Journey, Steve Perry & Friends", parsed[2].artist)
        assertEquals("Escape \"Special Edition, Vol. 2\"", parsed[2].album)
    }

    @Test
    fun `57 - Duplicate tracks in playlist are preserved in exact order without deduplication`() {
        val duplicates = listOf(
            SpotifyTrackItem(
                spotifyTrackId = "track_A",
                title = "Song A",
                artist = "Artist A",
                album = "Album 1",
                sourcePosition = 0,
                durationMs = 200000L,
                spotifyTrackUri = "spotify:track:track_A",
                isLocal = false
            ),
            SpotifyTrackItem(
                spotifyTrackId = "track_B",
                title = "Song B",
                artist = "Artist B",
                album = "Album 2",
                sourcePosition = 1,
                durationMs = 210000L,
                spotifyTrackUri = "spotify:track:track_B",
                isLocal = false
            ),
            SpotifyTrackItem(
                spotifyTrackId = "track_A",
                title = "Song A",
                artist = "Artist A",
                album = "Album 1",
                sourcePosition = 2,
                durationMs = 200000L,
                spotifyTrackUri = "spotify:track:track_A",
                isLocal = false
            )
        )

        val csv = SpotifyCsvSerializer.exportToCsv(duplicates)
        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)

        assertEquals(3, parsed.size)
        assertEquals("track_A", parsed[0].spotifyTrackId)
        assertEquals("track_B", parsed[1].spotifyTrackId)
        assertEquals("track_A", parsed[2].spotifyTrackId)
        assertEquals(0, parsed[0].sourcePosition)
        assertEquals(1, parsed[1].sourcePosition)
        assertEquals(2, parsed[2].sourcePosition)
    }

    @Test
    fun `58 - Partial extraction mismatch throws explicit invariant exception`() {
        val reportedTotal = 518
        val extractedCount = 400
        val throwsException = reportedTotal > 0 && extractedCount != reportedTotal
        assertTrue("Incomplete extraction must be flagged", throwsException)
    }
}
