package com.mudassir131.yt.utils

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslatorTest {
    @Test
    fun testLyricsTranslationPreservesTimestamps() = runBlocking {
        val input = """
            [00:10.50] Hello world
            [00:15.20] Good morning
            [00:20.00] How are you today?
        """.trimIndent()

        val translated = LyricsTranslator.translateLyrics(input, "SPANISH")
        println("TRANSLATED LYRICS:\n$translated")

        val lines = translated.split("\n")
        assertEquals(3, lines.size)
        assertTrue(lines[0].startsWith("[00:10.50]"))
        assertTrue(lines[1].startsWith("[00:15.20]"))
        assertTrue(lines[2].startsWith("[00:20.00]"))
    }

    @Test
    fun testResolveLanguageCode() {
        assertEquals("es", LyricsTranslator.resolveLanguageCode("SPANISH"))
        assertEquals("hi", LyricsTranslator.resolveLanguageCode("HINDI"))
        assertEquals("ur", LyricsTranslator.resolveLanguageCode("URDU"))
        assertEquals("en", LyricsTranslator.resolveLanguageCode("ENGLISH"))
        assertEquals("ja", LyricsTranslator.resolveLanguageCode("JAPANESE"))
        assertEquals("fr", LyricsTranslator.resolveLanguageCode("French"))
        assertEquals("de", LyricsTranslator.resolveLanguageCode("German"))
        assertEquals("zh-CN", LyricsTranslator.resolveLanguageCode("CHINESE_SIMPLIFIED"))
    }
}
