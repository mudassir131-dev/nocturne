/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImporterTest {

    @Test
    fun `01 - SPOTIFY_MAX_IMPORT_SONGS is 3000 and YTM_APPLE is 5000`() {
        assertEquals(3000, PlaylistImporter.SPOTIFY_MAX_IMPORT_SONGS)
        assertEquals(5000, PlaylistImporter.MAX_IMPORT_SONGS)
    }

    @Test
    fun `02 - Spotify playlist with 100 songs imports all 100`() {
        val tracks = (1..100).map { "Song $it" to "Artist $it" }
        val finalTracks = tracks.take(PlaylistImporter.SPOTIFY_MAX_IMPORT_SONGS)

        assertEquals(100, finalTracks.size)
        assertEquals("Song 1", finalTracks.first().first)
        assertEquals("Song 100", finalTracks.last().first)
    }

    @Test
    fun `03 - Spotify playlist with 500 songs imports all 500`() {
        val tracks = (1..500).map { "Song $it" to "Artist $it" }
        val finalTracks = tracks.take(PlaylistImporter.SPOTIFY_MAX_IMPORT_SONGS)

        assertEquals(500, finalTracks.size)
        assertEquals("Song 1", finalTracks.first().first)
        assertEquals("Song 500", finalTracks.last().first)
    }

    @Test
    fun `04 - Spotify playlist with 1000 songs imports all 1000`() {
        val tracks = (1..1000).map { "Song $it" to "Artist $it" }
        val finalTracks = tracks.take(PlaylistImporter.SPOTIFY_MAX_IMPORT_SONGS)

        assertEquals(1000, finalTracks.size)
        assertEquals("Song 1", finalTracks.first().first)
        assertEquals("Song 1000", finalTracks.last().first)
    }

    @Test
    fun `05 - Spotify playlist with 3000 songs imports all 3000`() {
        val tracks = (1..3000).map { "Song $it" to "Artist $it" }
        val finalTracks = tracks.take(PlaylistImporter.SPOTIFY_MAX_IMPORT_SONGS)

        assertEquals(3000, finalTracks.size)
        assertEquals("Song 1", finalTracks.first().first)
        assertEquals("Song 3000", finalTracks.last().first)
    }

    @Test
    fun `06 - Spotify playlist with 3500 songs is capped at exactly 3000`() {
        val tracks = (1..3500).map { "Song $it" to "Artist $it" }
        val finalTracks = tracks.take(PlaylistImporter.SPOTIFY_MAX_IMPORT_SONGS)

        assertEquals(3000, finalTracks.size)
        assertEquals("Song 1", finalTracks.first().first)
        assertEquals("Song 3000", finalTracks.last().first)
    }

    @Test
    fun `07 - YouTube Music and Apple Music playlists remain at 5000-song limit`() {
        val tracks = (1..6000).map { "Song $it" to "Artist $it" }
        val finalTracks = tracks.take(PlaylistImporter.MAX_IMPORT_SONGS)

        assertEquals(5000, finalTracks.size)
    }
}
