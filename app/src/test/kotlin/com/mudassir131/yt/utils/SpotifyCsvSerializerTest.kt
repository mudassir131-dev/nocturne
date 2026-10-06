package com.mudassir131.yt.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyCsvSerializerTest {

    @Test
    fun testStandardSpotifyCsv() {
        val csv = """
            Spotify Track ID,Spotify Track URI,Track Name,Artist Name(s),Album Name,Duration (ms),Source Position,Is Local