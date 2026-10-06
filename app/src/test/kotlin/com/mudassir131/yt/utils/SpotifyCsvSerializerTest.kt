package com.mudassir131.yt.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyCsvSerializerTest {

    @Test
    fun testStandardSpotifyCsv() {
        val csv = """
            Spotify Track ID,Spotify Track URI,Track Name,Artist Name(s),Album Name,Duration (ms),Source Position,Is Local
            track1,spotify:track:track1,Starboy,The Weeknd,Starboy,230000,0,false
            track2,spotify:track:track2,Blinding Lights,The Weeknd,After Hours,200000,1,false
        """.trimIndent()

        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
        assertEquals(2, parsed.size)
        assertEquals("track1", parsed[0].spotifyTrackId)
        assertEquals("Starboy", parsed[0].title)
        assertEquals("The Weeknd", parsed[0].artist)
        assertEquals(230000L, parsed[0].durationMs)
    }

    @Test
    fun testSemicolonDelimitedCsv() {
        val csv = """
            Title;Artist;Album;Duration
            Shape of You;Ed Sheeran;Divide;03:53
            Perfect;Ed Sheeran;Divide;04:23
        """.trimIndent()

        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
        assertEquals(2, parsed.size)
        assertEquals("Shape of You", parsed[0].title)
        assertEquals("Ed Sheeran", parsed[0].artist)
        assertEquals("Divide", parsed[0].album)
        assertEquals(233000L, parsed[0].durationMs)
    }

    @Test
    fun testAlternativeHeaderAliases() {
        val csv = """
            Song,Performer,Album
            Levitating,Dua Lipa,Future Nostalgia
            Don't Start Now,Dua Lipa,Future Nostalgia
        """.trimIndent()

        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
        assertEquals(2, parsed.size)
        assertEquals("Levitating", parsed[0].title)
        assertEquals("Dua Lipa", parsed[0].artist)
    }

    @Test
    fun testUtf8BomAndDuplicateTrackDisambiguation() {
        val csv = "\uFEFFTrack,Artist\nSong A,Artist 1\nSong A,Artist 1"
        val parsed = SpotifyCsvSerializer.parseFromCsv(csv)
        assertEquals(2, parsed.size)
        assertEquals("Song A", parsed[0].title)
        assertEquals("Song A", parsed[1].title)
        assertTrue(parsed[0].spotifyTrackId != parsed[1].spotifyTrackId)
    }
}