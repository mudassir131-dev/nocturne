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
