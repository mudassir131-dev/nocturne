package com.mudassir131.yt.ui.appleplayer.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppleLyricsTimingParserTest {
    @Test
    fun lineSyncedLrcDoesNotInventWordTiming() {
        val lines = AppleLyricsTimingParser.parse("[00:01.000]One two\n[00:03.000]Three")
        assertEquals(2, lines.size)
        assertTrue(lines.all { it.words.isEmpty() })
    }

    @Test
    fun richLrcPreservesProviderWordStarts() {
        val lines = AppleLyricsTimingParser.parse(
            "[00:01.000]<00:01.000>One <00:01.500>two\n[00:03.000]Three",
        )
        assertEquals(listOf(1000L, 1500L), lines.first().words.map { it.startMs })
        assertEquals(3000L, lines.first().words.last().endMs)
    }

    @Test
    fun ttmlOnlyUsesExplicitSpanTiming() {
        val lines = AppleLyricsTimingParser.parse(
            """<tt xmlns="http://www.w3.org/ns/ttml"><body><div>
                <p begin="1s" end="3s"><span begin="1s" end="2s">Real</span> line</p>
                <p begin="3s" end="5s">Line only</p>
            </div></body></tt>""",
        )
        assertTrue(lines.first().words.isNotEmpty())
        assertFalse(lines.last().words.isNotEmpty())
    }

    @Test
    fun paxsenixWordPayloadKeepsExplicitStartAndEnd() {
        val lines = AppleLyricsTimingParser.parse(
            "[00:01.00]One two\n<One:1.0:1.4|two:1.4:2.0>",
        )
        assertEquals(1000L, lines.single().words.first().startMs)
        assertEquals(2000L, lines.single().words.last().endMs)
    }
}
