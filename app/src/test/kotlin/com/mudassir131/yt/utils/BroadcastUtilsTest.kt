/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BroadcastUtilsTest {

    @Test
    fun testNormalizeGitHubBlobUrl() {
        val blobUrl = "https://github.com/mudassir131-dev/nocturne/blob/main/app/src/main/res/drawable/developer_mudassir.png"
        val expected = "https://raw.githubusercontent.com/mudassir131-dev/nocturne/main/app/src/main/res/drawable/developer_mudassir.png"
        val normalized = BroadcastManager.normalizeMediaUrl(blobUrl)
        assertEquals(expected, normalized)
    }

    @Test
    fun testNormalizeRawGithubUrl() {
        val rawUrl = "https://raw.githubusercontent.com/mudassir131-dev/nocturne/main/app/src/main/res/drawable/developer_mudassir.png"
        val normalized = BroadcastManager.normalizeMediaUrl(rawUrl)
        assertEquals(rawUrl, normalized)
    }

    @Test
    fun testNormalizeLocalDeadFilePathForReceivingUsers() {
        val deadLocalPath = "file:/data/user/0/com.mudassir131.nocturne/files/broadcast_media_1787654543330.gif"
        val expected = "https://raw.githubusercontent.com/mudassir131-dev/nocturne/main/broadcast_assets/broadcast_media_1787654543330.gif"
        val normalized = BroadcastManager.normalizeMediaUrl(deadLocalPath)
        assertEquals(expected, normalized)
    }

    @Test
    fun testMarkdownImageExtraction() {
        val content = "Check out this update! ![Cool Image](https://raw.githubusercontent.com/mudassir131-dev/nocturne/main/broadcast_assets/demo.gif)"
        val mdImgRegex = Regex("""!\[.*?\]\((https?://[^\s\)]+)\)""")
        val match = mdImgRegex.find(content)
        assertNotNull(match)
        assertEquals("https://raw.githubusercontent.com/mudassir131-dev/nocturne/main/broadcast_assets/demo.gif", match?.groupValues?.get(1))
    }

    @Test
    fun testLinkAndDomainRegexExtraction() {
        val content = "Visit our website https://nocturne-music.vercel.app or check [GitHub repo](https://github.com/mudassir131-dev/nocturne) and nocturne-music.vercel.app for docs."
        val tokenRegex = Regex(
            """(\[([^\]]+)\]\((https?://[^\s\)]+)\))|(https?://[^\s]+)|((?:[a-zA-Z0-9-]+\.)+(?:com|org|net|app|io|dev|me|co|in|xyz|gl|be)(?:/[^\s]*)?)"""
        )
        val matches = tokenRegex.findAll(content).toList()
        assertTrue(matches.isNotEmpty())
        assertEquals(3, matches.size)

        // 1st match: https://nocturne-music.vercel.app
        assertEquals("https://nocturne-music.vercel.app", matches[0].value)

        // 2nd match: [GitHub repo](https://github.com/mudassir131-dev/nocturne)
        assertEquals("[GitHub repo](https://github.com/mudassir131-dev/nocturne)", matches[1].value)
        assertEquals("GitHub repo", matches[1].groups[2]?.value)
        assertEquals("https://github.com/mudassir131-dev/nocturne", matches[1].groups[3]?.value)

        // 3rd match: nocturne-music.vercel.app
        assertEquals("nocturne-music.vercel.app", matches[2].value)
    }
}
