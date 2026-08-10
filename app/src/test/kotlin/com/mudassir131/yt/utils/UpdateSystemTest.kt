package com.mudassir131.yt.utils

import com.mudassir131.yt.BuildConfig
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSystemTest {

    @Test
    fun testVersionComparison() {
        val mainActivityKtClass = Class.forName("com.mudassir131.yt.MainActivityKt")
        val compareMethod = mainActivityKtClass.getDeclaredMethod("compareVersion", String::class.java, String::class.java)
        compareMethod.isAccessible = true

        val compare = { v1: String, v2: String ->
            compareMethod.invoke(null, v1, v2) as Int
        }

        // Verify Scenario 1: v1.1.2 vs v2.2.3.4.01
        val result1 = compare("v2.2.3.4.01", "v1.1.2")
        println("Diagnostic log - compareVersion('v2.2.3.4.01', 'v1.1.2') = $result1")
        assertTrue("v2.2.3.4.01 should be newer than v1.1.2", result1 > 0)

        // Verify Scenario 2: v2.2.3.4.0 vs v2.2.3.4.01
        val result2 = compare("v2.2.3.4.01", "v2.2.3.4.0")
        println("Diagnostic log - compareVersion('v2.2.3.4.01', 'v2.2.3.4.0') = $result2")
        assertTrue("v2.2.3.4.01 should be newer than v2.2.3.4.0", result2 > 0)

        // Verify Scenario 3: Equal versions
        val result3 = compare("v2.2.3.4.01", "v2.2.3.4.01")
        println("Diagnostic log - compareVersion('v2.2.3.4.01', 'v2.2.3.4.01') = $result3")
        assertEquals("Equal versions should return 0", 0, result3)

        // Verify Rollback Protection: Older version vs newer version
        val result4 = compare("v2.2.3.4.0", "v2.2.3.4.01")
        println("Diagnostic log - compareVersion('v2.2.3.4.0', 'v2.2.3.4.01') = $result4")
        assertTrue("v2.2.3.4.0 is older, should return negative or 0", result4 < 0)

        // Test varied segment counts and leading zeros
        assertEquals("2.2.3.4.01 should equal 2.2.3.4.1", 0, compare("2.2.3.4.01", "2.2.3.4.1"))
        assertTrue("2.2.3.4.01 should be newer than 2.2.3.4", compare("2.2.3.4.01", "2.2.3.4") > 0)
        assertTrue("Uppercase V labels must compare normally", compare("V2.22.03.21", "v2.22.3.19") > 0)
        assertTrue("2.22.21 must update 2.22.03.21", compare("v2.22.21", "v2.22.03.21") > 0)
        assertTrue("New release label must remain monotonic", compare("Nocturne V2.22.03.21", "2.2.3.4.09") > 0)
    }

    @Test
    fun testParseReleaseJson() {
        val updaterClass = Updater::class.java
        val parseMethod = updaterClass.getDeclaredMethod("parseSingleReleaseJson", JSONObject::class.java)
        parseMethod.isAccessible = true

        val mockJson = JSONObject("""{
            "tag_name": "v2.2.3.4.01",
            "name": "Nocturne v2.2.3.4.01",
            "draft": false,
            "prerelease": false,
            "body": "Dynamic release notes from GitHub",
            "published_at": "2026-06-22T15:23:51Z",
            "html_url": "https://github.com/mudassir131-dev/nocturne/releases/tag/v2.2.3.4.01",
            "assets": [
                {
                    "name": "app-universal-release.apk",
                    "browser_download_url": "https://github.com/mudassir131-dev/nocturne/releases/download/v2.2.3.4.01/app-universal-release.apk"
                },
                {
                    "name": "source-code.zip",
                    "browser_download_url": "https://github.com/mudassir131-dev/nocturne/archive/refs/tags/v2.2.3.4.01.zip"
                }
            ]
        }""")

        val info = parseMethod.invoke(Updater, mockJson) as ReleaseInfo
        println("Diagnostic log - Parsed tagName: ${info.tagName}")
        println("Diagnostic log - Parsed downloadUrl: ${info.browserDownloadUrl}")
        assertEquals("v2.2.3.4.01", info.tagName)
        assertEquals("Dynamic release notes from GitHub", info.body)
        assertEquals("https://github.com/mudassir131-dev/nocturne/releases/download/v2.2.3.4.01/app-universal-release.apk", info.browserDownloadUrl)
    }

    @Test
    fun arm64ReleaseIsPreferredRegardlessOfAssetOrder() {
        val assets = listOf(
            ReleaseAsset("Nocturne-V2.22.03.21-universal-release.apk", "https://example.com/universal.apk", "application/vnd.android.package-archive", 10),
            ReleaseAsset("Nocturne-V2.22.03.21-arm64-v8a-release.apk", "https://example.com/arm64.apk", "application/vnd.android.package-archive", 8),
        )

        val selected = Updater.selectCompatibleApk(assets, listOf("arm64-v8a"), "universal")

        assertEquals("https://example.com/arm64.apk", selected?.browserDownloadUrl)
    }

    @Test
    fun universalIsUsedOnlyAsCompatibleFallback() {
        val assets = listOf(
            ReleaseAsset("Nocturne-V2.22.03.21-x86_64-release.apk", "https://example.com/x86.apk", "application/vnd.android.package-archive", 8),
            ReleaseAsset("Nocturne-V2.22.03.21-universal-release.apk", "https://example.com/universal.apk", "application/vnd.android.package-archive", 10),
        )

        val selected = Updater.selectCompatibleApk(assets, listOf("arm64-v8a"), "universal")

        assertEquals("https://example.com/universal.apk", selected?.browserDownloadUrl)
    }

    @Test
    fun debugUnsignedAndNonApkAssetsAreNeverSelected() {
        val assets = listOf(
            ReleaseAsset("Nocturne-arm64-debug.apk", "https://example.com/debug.apk", "application/vnd.android.package-archive", 8),
            ReleaseAsset("Nocturne-arm64-unsigned.apk", "https://example.com/unsigned.apk", "application/vnd.android.package-archive", 8),
            ReleaseAsset("SHA256SUMS.txt", "https://example.com/checksums", "text/plain", 1),
            ReleaseAsset("mapping.txt", "https://example.com/mapping", "text/plain", 1),
        )

        assertNull(Updater.selectCompatibleApk(assets, listOf("arm64-v8a"), "arm64"))
    }

    @Test
    fun notificationSummaryComesFromCurrentReleaseBody() {
        val body = """
            ## What's New

            ### Immersive UI Design
            Experience a refined interface.
        """.trimIndent()

        assertEquals("Immersive UI Design", UpdateNotificationManager.releaseNotesSummary(body))
        assertEquals(Updater.GenericReleaseNotes, UpdateNotificationManager.releaseNotesSummary(null))
    }

    @Test
    fun testLiveNetworkFetch() = runBlocking {
        println("Diagnostic log - Current BuildConfig.VERSION_NAME = ${BuildConfig.VERSION_NAME}")
        println("Diagnostic log - Executing getLatestReleaseInfo() against live GitHub API...")
        val result = Updater.getLatestReleaseInfo()
        if (result.isSuccess) {
            val info = result.getOrThrow()
            println("Diagnostic log - Fetch SUCCESS!")
            println("Diagnostic log - Live Tag Name: ${info.tagName}")
            println("Diagnostic log - Live Download URL: ${info.browserDownloadUrl}")
        } else {
            val exception = result.exceptionOrNull()
            println("Diagnostic log - Fetch FAILED!")
            println("Diagnostic log - Exception: ${exception?.message}")
        }
    }
}
