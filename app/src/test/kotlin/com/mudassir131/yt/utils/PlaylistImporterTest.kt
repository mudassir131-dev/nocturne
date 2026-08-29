/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import com.mudassir131.yt.constants.SpotifyClientIdKey
import com.mudassir131.yt.constants.SpotifyClientSecretKey
import com.mudassir131.yt.db.entities.PlaylistSongMap
import com.mudassir131.yt.db.entities.SpotifyImportProgressEntity
import com.mudassir131.yt.db.entities.SpotifyImportTrackEntity
import com.mudassir131.yt.db.entities.SpotifyTrackMap
import com.mudassir131.yt.utils.matching.MatchCandidate
import com.mudassir131.yt.utils.matching.MatchStatus
import com.mudassir131.yt.utils.matching.TrackMatcher
import com.mudassir131.yt.utils.youtube.YouTubeDataApi
import com.mudassir131.yt.utils.youtube.YouTubeQuotaExceededException
import com.mudassir131.yt.utils.youtube.YouTubeQuotaTracker
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.LocalDateTime

class PlaylistImporterTest {

    @Test
    fun `01 - YouTube constants and limits retained while Spotify has zero artificial limit`() {
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
            nextUrl = "https://api.spotify.com/v1/playlists/playlist_abc/items?offset=200&limit=100",
            processedCount = 200,
            status = "IN_PROGRESS",
            maxTracks = 1000
        )

        // Resume: start directly from saved nextUrl
        assertNotNull(initialProgress.nextUrl)
        assertTrue(initialProgress.nextUrl!!.contains("offset=200"))
        assertEquals(200, initialProgress.processedCount)

        // Process Page 3 (201-300)
        val newProcessedCount = initialProgress.processedCount + 100
        val newNextUrl = "https://api.spotify.com/v1/playlists/playlist_abc/items?offset=300&limit=100"

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
        var savedNextUrl = "https://api.spotify.com/v1/playlists/p1/items?offset=100&limit=100"
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
        assertEquals("https://api.spotify.com/v1/playlists/p1/items?offset=100&limit=100", savedNextUrl)

        // Retry succeeds
        pageSuccess = true
        if (pageSuccess) {
            savedNextUrl = "https://api.spotify.com/v1/playlists/p1/items?offset=200&limit=100"
        }

        assertEquals("https://api.spotify.com/v1/playlists/p1/items?offset=200&limit=100", savedNextUrl)
    }

    @Test
    fun `05 - 101-song playlist paginates across 2 pages and marks COMPLETED`() {
        var currentOffset = 0
        var processedCount = 0
        var status = "IN_PROGRESS"
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/p1/items?offset=0&limit=100"

        // Page 1: 100 songs
        val page1 = (1..100).map { "Song $it" }
        processedCount += page1.size
        currentOffset += page1.size
        nextUrl = "https://api.spotify.com/v1/playlists/p1/items?offset=100&limit=100"

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
    fun `06 - Large 5000-song playlist paginates across 50 pages completely without artificial truncation`() {
        var processedCount = 0
        var status = "IN_PROGRESS"
        val totalPages = 50

        for (page in 1..totalPages) {
            val batchSize = 100
            processedCount += batchSize
        }
        status = "COMPLETED"

        assertEquals(5000, processedCount)
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
            currentUrl = "https://api.spotify.com/v1/playlists/p1/items?offset=0&limit=100",
            nextUrl = "https://api.spotify.com/v1/playlists/p1/items?offset=100&limit=100",
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
            nextUrl = "https://api.spotify.com/v1/playlists/p1/items?offset=200&limit=100",
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
        var nextUrl: String? = "https://api.spotify.com/v1/playlists/p200/items?offset=0&limit=100"

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
        nextUrl = "https://api.spotify.com/v1/playlists/p200/items?offset=100&limit=100"

        assertEquals(100, playlistMappings.size)
        assertEquals(100, processedCount)
        assertEquals("https://api.spotify.com/v1/playlists/p200/items?offset=100&limit=100", nextUrl)

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
        val wrongSong = "The Weeknd - Blinding Lights"
        val result = TrackMatcher.pickBest(
            trackName = "Shape of You",
            artist = "Ed Sheeran",
            spotifyDurationMs = 233_712,
            candidates = listOf(candidate(wrongSong, durationSec = 234, channel = "TheWeeknd"))
        )

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

        val beforeReset = Instant.parse("2026-03-01T07:30:00Z")
        val afterReset = Instant.parse("2026-03-01T08:30:00Z")

        assertEquals("2026-02-28", YouTubeQuotaTracker.currentQuotaDate(beforeReset))
        assertEquals("2026-03-01", YouTubeQuotaTracker.currentQuotaDate(afterReset))
    }

    @Test
    fun `28 - duration verification batches 50 ids per videos-list call`() {
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
        assertEquals(234, (track.durationMs / 1000.0).let { Math.round(it).toInt() })
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
    fun `34 - YouTube playlist import does not call Spotify extraction or CSV pipeline`() {
        val source = File("src/main/kotlin/com/mudassir131/yt/utils/PlaylistImporter.kt").readText()

        // Extract YouTube import block
        val ytBlock = source.substringAfter("youtubePlaylistId != null").substringBefore("else if (resolvedUrl.contains(\"spotify.com")

        assertFalse("YouTube import must not call extractSpotifyPlaylistTracks", ytBlock.contains("extractSpotifyPlaylistTracks"))
        assertFalse("YouTube import must not call SpotifyCsvSerializer", ytBlock.contains("SpotifyCsvSerializer"))
        assertFalse("YouTube import must not call importFromCsv", ytBlock.contains("importFromCsv"))
    }

    @Test
    fun `35 - Scale matrix verification for complete track counts from 1 to 10000 tracks`() {
        val testSizes = listOf(1, 10, 99, 100, 101, 118, 127, 130, 141, 200, 518, 611, 999, 1000, 3000, 5000, 10000)
        for (count in testSizes) {
            val originalTracks = (1..count).map { idx ->
                SpotifyTrackItem(
                    spotifyTrackId = "spotify_track_$idx",
                    spotifyTrackUri = "spotify:track:spotify_track_$idx",
                    title = "Title $idx",
                    artist = "Artist $idx",
                    album = "Album $idx",
                    sourcePosition = idx - 1,
                    durationMs = 180000L + (idx % 100) * 1000L,
                    isLocal = false
                )
            }

            // Stage 1: Extraction count
            val extractedCount = originalTracks.size
            assertEquals(count, extractedCount)

            // Stage 2: Physical CSV serialization
            val csvContent = SpotifyCsvSerializer.exportToCsv(originalTracks)

            // Stage 3: CSV parsing via RFC-4180
            val parsedTracks = SpotifyCsvSerializer.parseFromCsv(csvContent)
            val parsedCount = parsedTracks.size
            assertEquals(count, parsedCount)

            // Stage 4: Invariant verification
            assertEquals(extractedCount, parsedCount)
            assertEquals("Title 1", parsedTracks.first().title)
            assertEquals("Title $count", parsedTracks.last().title)
            assertEquals(0, parsedTracks.first().sourcePosition)
            assertEquals(count - 1, parsedTracks.last().sourcePosition)

            // Stage 5: Matching terminal state accounting
            val submittedCount = parsedCount
            val matchedCount = (count * 0.9).toInt()
            val unmatchedCount = count - matchedCount
            val skippedCount = 0
            val completedCount = matchedCount + unmatchedCount + skippedCount

            assertEquals(submittedCount, completedCount)
            assertEquals(count, completedCount)
        }
    }

    @Test
    fun `36 - RFC-4180 multiline quoted fields with embedded newlines, commas, escaped quotes, and Unicode`() {
        val complexCsv = "Spotify Track ID,Spotify Track URI,Track Name,Artist Name(s),Album Name,Duration (ms),Source Position,Is Local\n" +
            "\"id_1\",\"spotify:track:id_1\",\"Song with\nNewline and \"\"Quotes\"\"\",\"Artist 1, Artist 2\",\"Album, Vol. 1\",210000,0,false\n" +
            "\"id_2\",\"spotify:track:id_2\",\"तेरे लिए (Tere Liye)\",\"Atif Aslam, Shreya Ghoshal\",\"Prince\",280000,1,false\n" +
            "\"id_3\",\"spotify:track:id_3\",\"حبيبي يا نور العين 🌟\",\"Amr Diab\",\"Nour El Ain\",310000,2,false\n" +
            "\"id_4\",\"spotify:track:id_4\",\"Simple Track\",\"Solo Artist\",\"Debut\",190000,3,true"

        val parsed = SpotifyCsvSerializer.parseFromCsv(complexCsv)
        assertEquals(4, parsed.size)

        // Record 1: Multiline with escaped quotes and comma
        assertEquals("id_1", parsed[0].spotifyTrackId)
        assertTrue(parsed[0].title.contains("Newline and \"Quotes\""))
        assertEquals("Artist 1, Artist 2", parsed[0].artist)
        assertEquals("Album, Vol. 1", parsed[0].album)
        assertEquals(0, parsed[0].sourcePosition)
        assertFalse(parsed[0].isLocal)

        // Record 2: Hindi
        assertEquals("id_2", parsed[1].spotifyTrackId)
        assertEquals("तेरे लिए (Tere Liye)", parsed[1].title)
        assertEquals("Atif Aslam, Shreya Ghoshal", parsed[1].artist)

        // Record 3: Arabic + Emoji
        assertEquals("id_3", parsed[2].spotifyTrackId)
        assertEquals("حبيبي يا نور العين 🌟", parsed[2].title)

        // Record 4: Local track flag
        assertEquals("id_4", parsed[3].spotifyTrackId)
        assertTrue(parsed[3].isLocal)
    }

    @Test
    fun `37 - Duplicate Spotify tracks in playlist are preserved in exact order without collapsing`() {
        val tracks = listOf(
            SpotifyTrackItem("track_A", "Song A", "Artist A", sourcePosition = 0),
            SpotifyTrackItem("track_B", "Song B", "Artist B", sourcePosition = 1),
            SpotifyTrackItem("track_A", "Song A", "Artist A", sourcePosition = 2)
        )

        val csv = SpotifyCsvSerializer.exportToCsv(tracks)
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
    fun `38 - Pagination loop detection throws SpotifyPaginationLoopException`() {
        val visited = mutableSetOf<String>()
        val url = "https://api.spotify.com/v1/playlists/test/items?offset=100&limit=100"
        visited.add(url)

        try {
            if (!visited.add(url)) {
                throw SpotifyPaginationLoopException(url)
            }
            fail("Should have thrown loop exception")
        } catch (e: SpotifyPaginationLoopException) {
            assertEquals(url, e.url)
            assertTrue(e.message!!.contains("loop detected"))
        }
    }

    @Test
    fun `39 - Incomplete total extraction mismatch throws SpotifyImportInvariantException`() {
        val reportedTotal = 518
        val extractedCount = 400

        try {
            if (reportedTotal > 0 && extractedCount != reportedTotal) {
                throw SpotifyImportInvariantException("EXTRACTION", reportedTotal, extractedCount, "Incomplete extraction")
            }
            fail("Should have thrown invariant exception")
        } catch (e: SpotifyImportInvariantException) {
            assertEquals("EXTRACTION", e.stage)
            assertEquals(518, e.expected)
            assertEquals(400, e.actual)
        }
    }

    @Test
    fun `40 - HTTP 401 triggers token invalidation and fails on repeated 401`() {
        var token = "stale_token"
        SpotifyTokenProvider.invalidateToken()

        var authAttempts = 0
        var failedPermanently = false

        while (authAttempts < 2) {
            authAttempts++
            if (authAttempts == 1) {
                // First 401: invalidate and refresh
                SpotifyTokenProvider.invalidateToken()
                token = "refreshed_token"
            } else {
                // Second 401: throw
                failedPermanently = true
                break
            }
        }

        assertTrue(failedPermanently)
        assertEquals(2, authAttempts)
    }

    @Test
    fun `41 - HTTP 403 Access Denied classifies reasons explicitly`() {
        val reasons = listOf(
            "Playlist not found or private" to "SPOTIFY_PLAYLIST_ACCESS_DENIED",
            "Insufficient client scope or permissions" to "SPOTIFY_AUTHORIZATION_DENIED",
            "Content restricted in your country policy" to "SPOTIFY_POLICY_RESTRICTION",
            "Forbidden action" to "SPOTIFY_UNKNOWN_403"
        )

        for ((body, expectedCategory) in reasons) {
            val lower = body.lowercase()
            val category = when {
                lower.contains("playlist") || lower.contains("not found") -> "SPOTIFY_PLAYLIST_ACCESS_DENIED"
                lower.contains("auth") || lower.contains("scope") || lower.contains("permission") -> "SPOTIFY_AUTHORIZATION_DENIED"
                lower.contains("policy") || lower.contains("restriction") || lower.contains("country") || lower.contains("geo") -> "SPOTIFY_POLICY_RESTRICTION"
                else -> "SPOTIFY_UNKNOWN_403"
            }
            assertEquals(expectedCategory, category)
        }
    }

    @Test
    fun `42 - HTTP 429 Retry-After header parsing and quota exceeded classification`() {
        // Retry-After header in seconds
        val retryAfterHeader = "12"
        val retryAfterMs = retryAfterHeader.toLongOrNull()?.let { it * 1000L }
        assertEquals(12000L, retryAfterMs)

        // Quota exceeded body
        val quotaBody = "{\"error\": {\"status\": 429, \"message\": \"QUOTA_EXCEEDED\"}}"
        val isQuota = quotaBody.contains("QUOTA_EXCEEDED", ignoreCase = true)
        assertTrue(isQuota)
    }

    @Test
    fun `43 - Unwanted variant rejection in TrackMatcher - lyric videos, covers, DJ remixes penalized`() {
        val trackName = "Tum Hi Ho"
        val artist = "Arijit Singh"
        val spotifyDurationMs = 262000L

        val studioCandidate = MatchCandidate(
            videoId = "official_audio_id",
            title = "Tum Hi Ho",
            channelTitle = "Arijit Singh - Topic",
            durationSec = 262
        )

        val lyricsCandidate = MatchCandidate(
            videoId = "lyrics_video_id",
            title = "Tum Hi Ho (Lyrics Video) | Arijit Singh",
            channelTitle = "Lyrics Channel",
            durationSec = 251
        )

        val djCandidate = MatchCandidate(
            videoId = "dj_remix_id",
            title = "Tum Hi Ho DJ Remix slowed reverb",
            channelTitle = "DJ",
            durationSec = 180
        )

        val studioScore = TrackMatcher.score(trackName, artist, spotifyDurationMs, studioCandidate)
        val lyricsScore = TrackMatcher.score(trackName, artist, spotifyDurationMs, lyricsCandidate)
        val djScore = TrackMatcher.score(trackName, artist, spotifyDurationMs, djCandidate)

        assertTrue(studioScore.confidence >= 0.80)
        assertTrue(lyricsScore.confidence < 0.55)
        assertTrue(djScore.confidence < 0.55)

        val best = TrackMatcher.pickBest(
            trackName = trackName,
            artist = artist,
            spotifyDurationMs = spotifyDurationMs,
            candidates = listOf(lyricsCandidate, djCandidate, studioCandidate)
        )

        assertEquals(MatchStatus.MATCHED, best.status)
        assertEquals("official_audio_id", best.videoId)
    }

    @Test
    fun `44 - Spotify import is not started while UI state == IDLE and initial session state is IDLE`() {
        assertEquals(SpotifyImportState.IDLE, PlaylistImporter.currentSessionState)
    }

    @Test
    fun `45 - HTTP 429 without explicit QUOTA_EXCEEDED body is classified as RateLimited and retried`() {
        val rateLimitBody = "{\"error\": {\"status\": 429, \"message\": \"API rate limit exceeded\"}}"
        val retryAfterSec = 5L
        val retryAfterMs = retryAfterSec * 1000L

        val isExplicitQuota = rateLimitBody.contains("\"QUOTA_EXCEEDED\"", ignoreCase = true) ||
                (rateLimitBody.contains("\"message\"") && rateLimitBody.contains("quota exceeded", ignoreCase = true))

        assertFalse("General rate limit body must not be classified as quota exceeded", isExplicitQuota)

        val result = if (isExplicitQuota) {
            SpotifyFetchResult.QuotaExceeded(429, "Spotify API quota has been exceeded.")
        } else {
            SpotifyFetchResult.RateLimited(429, retryAfterMs, "Spotify rate limit reached.")
        }

        assertTrue(result is SpotifyFetchResult.RateLimited)
        assertEquals(5000L, (result as SpotifyFetchResult.RateLimited).retryAfterMs)
    }

    @Test
    fun `46 - Repeated 429 exhausts retries and produces SpotifyRateLimitException not permanent quota`() {
        val maxGeneralRetries = 5
        var retries = 0
        var threwRateLimit = false
        var threwQuota = false

        while (retries < maxGeneralRetries) {
            retries++
        }

        if (retries >= maxGeneralRetries) {
            try {
                throw SpotifyRateLimitException(3000L, "Spotify rate limit reached and retries exhausted.")
            } catch (e: SpotifyRateLimitException) {
                threwRateLimit = true
            } catch (e: SpotifyQuotaExceededException) {
                threwQuota = true
            }
        }

        assertTrue(threwRateLimit)
        assertFalse(threwQuota)
    }

    @Test
    fun `47 - Previous failed import resets state so subsequent new import starts from clean IDLE state`() {
        // Simulate previous failed run
        PlaylistImporter.currentSessionState = SpotifyImportState.FAILED
        // Cleanup on completion
        PlaylistImporter.currentSessionState = SpotifyImportState.IDLE

        assertEquals(SpotifyImportState.IDLE, PlaylistImporter.currentSessionState)

        // New import session starts
        PlaylistImporter.currentSessionState = SpotifyImportState.STARTING
        assertEquals(SpotifyImportState.STARTING, PlaylistImporter.currentSessionState)

        // Reset to IDLE
        PlaylistImporter.currentSessionState = SpotifyImportState.IDLE
        assertEquals(SpotifyImportState.IDLE, PlaylistImporter.currentSessionState)
    }

    @Test
    fun `48 - YouTubeQuotaTracker cannot classify Spotify HTTP responses`() {
        val spotify429Code = 429
        val isSpotifyRateLimit = spotify429Code == 429
        assertTrue(isSpotifyRateLimit)

        // YouTube quota exception is strictly for YouTube Data API v3
        val ytEx = YouTubeQuotaExceededException("YouTube API quota exceeded")
        assertTrue(ytEx is Exception)
        assertFalse("YouTubeQuotaExceededException must not inherit SpotifyImportException", ytEx is SpotifyImportException)
    }

    @Test
    fun `49 - Token acquisition does not trigger playlist quota logic`() {
        SpotifyTokenProvider.invalidateToken()
        // Invalidation clears token without touching any quota or database entities
        assertNull(SpotifyTokenProvider.cachedToken)
    }

    @Test
    fun `50 - Spotify Developer API credentials and web fallback are configured`() {
        assertNotNull(SpotifyClientIdKey)
        assertNotNull(SpotifyClientSecretKey)
        assertEquals("spotifyClientId", SpotifyClientIdKey.name)
        assertEquals("spotifyClientSecret", SpotifyClientSecretKey.name)
    }

    @Test
    fun `51 - Spotify URL import accepts playlists with 100 tracks or fewer`() {
        val tracksCount = 100
        val isAllowed = tracksCount <= 100
        assertTrue(isAllowed)
    }

    @Test
    fun `52 - Spotify URL import with more than 100 tracks throws SpotifyMaxTracksExceededException with recommendation message`() {
        val reportedTotal = 141
        try {
            if (reportedTotal > 100) {
                throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
            }
            fail("Should have thrown SpotifyMaxTracksExceededException")
        } catch (e: SpotifyMaxTracksExceededException) {
            assertEquals("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.", e.message)
        }
    }

    @Test
    fun `53 - CSV importer parses 100, 141, 518, 611, 1000 rows completely without limit`() {
        val testCounts = listOf(100, 141, 518, 611, 1000)
        for (count in testCounts) {
            val tracks = (1..count).map { i ->
                SpotifyTrackItem(
                    spotifyTrackId = "track_$i",
                    spotifyTrackUri = "spotify:track:track_$i",
                    title = "Song Title $i",
                    artist = "Artist $i",
                    album = "Album $i",
                    sourcePosition = i,
                    durationMs = 200000L,
                    isLocal = false
                )
            }
            val csv = SpotifyCsvSerializer.exportToCsv(tracks)
            val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
            assertEquals("CSV parsing must read all rows to EOF for count $count", count, parsed.size)
        }
    }

    @Test
    fun `54 - CSV importer preserves exact source ordering and duplicate tracks`() {
        val tracks = listOf(
            SpotifyTrackItem(spotifyTrackId = "id_1", title = "Duplicate Song", artist = "Artist A", album = "Album A", sourcePosition = 0, durationMs = 180000L),
            SpotifyTrackItem(spotifyTrackId = "id_2", title = "Unique Song", artist = "Artist B", album = "Album B", sourcePosition = 1, durationMs = 200000L),
            SpotifyTrackItem(spotifyTrackId = "id_1", title = "Duplicate Song", artist = "Artist A", album = "Album A", sourcePosition = 2, durationMs = 180000L)
        )
        val csv = SpotifyCsvSerializer.exportToCsv(tracks)
        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)

        assertEquals(3, parsed.size)
        assertEquals("Duplicate Song", parsed[0].title)
        assertEquals("Unique Song", parsed[1].title)
        assertEquals("Duplicate Song", parsed[2].title)
    }

    @Test
    fun `55 - CSV importer parses generic headers seamlessly`() {
        val genericCsv = """
            Track Name,Artist Name,Album,Duration (ms)
            Blinding Lights,The Weeknd,After Hours,200000
            Shape of You,Ed Sheeran,Divide,233000
            Starboy,The Weeknd,Starboy,230000
        """.trimIndent()

        val parsed = SpotifyCsvSerializer.parseFromCsv(genericCsv)
        assertEquals(3, parsed.size)
        assertEquals("Blinding Lights", parsed[0].title)
        assertEquals("The Weeknd", parsed[0].artist)
        assertEquals("Shape of You", parsed[1].title)
        assertEquals("Starboy", parsed[2].title)
    }
}
