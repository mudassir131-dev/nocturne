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
