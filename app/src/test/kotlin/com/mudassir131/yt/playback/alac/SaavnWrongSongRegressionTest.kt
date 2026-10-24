/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Forensic regression tests for the reported wrong-song bug:
 * playing "12 Saal" (Bilal Saeed) and "Brown Rang" (Yo Yo Honey Singh) fetched a
 * different track's audio. The Saavn catalog provider must only hand back a stream
 * when the row demonstrably *is* the searched recording (verified title + artist +
 * duration fingerprint), and must return null — routing the player to the correct
 * YouTube stream — for every lookalike.
 */
class SaavnWrongSongRegressionTest {

    private fun saavnRow(
        title: String,
        durationSec: Int,
        artistMap: List<String> = emptyList(),
        artistsCsv: String = "",
        singers: String = "",
    ): JSONObject {
        val moreInfo = JSONObject()
            .put("duration", durationSec.toString())
            .put("encrypted_media_url", "ENC-$title-$durationSec")
        if (artistMap.isNotEmpty()) {
            val primary = JSONArray()
            artistMap.forEach { primary.put(JSONObject().put("name", it)) }
            moreInfo.put("artistMap", JSONObject().put("primary_artists", primary))
        }
        if (artistsCsv.isNotBlank()) moreInfo.put("primary_artists", artistsCsv)
        if (singers.isNotBlank()) moreInfo.put("singers", singers)
        return JSONObject()
            .put("song", title)
            .put("title", title)
            .put("more_info", moreInfo)
    }

    private fun saavnResults(vararg rows: JSONObject): JSONArray =
        JSONArray().apply { rows.forEach { put(it) } }

    @Test
    fun `12 Saal by Bilal Saeed - same-name track by another artist, remix and preview cut all rejected, genuine track wins`() {
        val results = saavnResults(
            // Different performer wearing the same song name, duration accidentally close.
            saavnRow("12 Saal", 215, artistMap = listOf("Sarmad Qadeer"), artistsCsv = "Sarmad Qadeer"),
            // Right artist, but a different recording (remix edit).
            saavnRow("12 Saal (Remix)", 217, artistMap = listOf("Bilal Saeed"), artistsCsv = "Bilal Saeed"),
            // Right artist and name, but a 27s-off cut (preview / different edit) — duration fingerprints it.
            saavnRow("12 Saal", 190, artistMap = listOf("Bilal Saeed"), artistsCsv = "Bilal Saeed"),
            // The genuine studio master.
            saavnRow("12 Saal", 218, artistMap = listOf("Bilal Saeed"), artistsCsv = "Bilal Saeed"),
        )

        val winner = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = results,
            matchTitle = "12 Saal",
            matchArtist = "Bilal Saeed",
            expectedDuration = 217,
        )

        assertNotNull("Genuine '12 Saal' by Bilal Saeed must be selected", winner)
        assertEquals("12 Saal", winner!!.title)
        assertTrue(
            "Winner must be verified as Bilal Saeed's release, got [${winner.artists.joinToString(", ")}]",
            winner.artists.any { it.contains("Bilal Saeed", ignoreCase = true) },
        )
        assertEquals(218, winner.durationSeconds)
    }

    @Test
    fun `12 Saal - with only lookalikes in results the provider must return null so playback falls back to YouTube`() {
        val results = saavnResults(
            saavnRow("12 Saal", 215, artistMap = listOf("Sarmad Qadeer"), artistsCsv = "Sarmad Qadeer"),
            saavnRow("12 Saal (Remix)", 217, artistMap = listOf("Bilal Saeed"), artistsCsv = "Bilal Saeed"),
            saavnRow("12 Saal", 190, artistMap = listOf("Bilal Saeed"), artistsCsv = "Bilal Saeed"),
        )

        val winner = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = results,
            matchTitle = "12 Saal",
            matchArtist = "Bilal Saeed",
            expectedDuration = 217,
        )

        assertNull(
            "No verified match exists, so the provider must return null instead of guessing a lookalike",
            winner,
        )
    }

    @Test
    fun `Brown Rang by Yo Yo Honey Singh - cover and slowed-reverb variants rejected, genuine track wins`() {
        val results = saavnResults(
            // A cover version with the identical name and near-identical length.
            saavnRow("Brown Rang", 246, artistMap = listOf("Rahul Vaidya"), artistsCsv = "Rahul Vaidya"),
            // Right artist, but a slowed + reverb edit — a different recording.
            saavnRow("Brown Rang (Slowed + Reverb)", 245, artistMap = listOf("Yo Yo Honey Singh"), artistsCsv = "Yo Yo Honey Singh"),
            // The genuine master (Saavn sometimes lists the artist without the "Yo Yo" prefix).
            saavnRow("Brown Rang", 246, artistMap = listOf("Honey Singh"), artistsCsv = "Honey Singh"),
        )

        val winner = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = results,
            matchTitle = "Brown Rang",
            matchArtist = "Yo Yo Honey Singh",
            expectedDuration = 245,
        )

        assertNotNull("Genuine 'Brown Rang' by Honey Singh must be selected", winner)
        assertEquals("Brown Rang", winner!!.title)
        assertTrue(
            "Winner must be verified as Honey Singh's release, got [${winner.artists.joinToString(", ")}]",
            winner.artists.any { it.contains("Honey Singh", ignoreCase = true) },
        )
        assertEquals(246, winner.durationSeconds)
        assertTrue(
            "Cover and slowed variants must never win the search",
            !winner.artists.any { it.contains("Rahul Vaidya", ignoreCase = true) },
        )
    }

    @Test
    fun `Brown Rang - with only the cover and the slowed variant present the provider must return null`() {
        val results = saavnResults(
            saavnRow("Brown Rang", 246, artistMap = listOf("Rahul Vaidya"), artistsCsv = "Rahul Vaidya"),
            saavnRow("Brown Rang (Slowed + Reverb)", 245, artistMap = listOf("Yo Yo Honey Singh"), artistsCsv = "Yo Yo Honey Singh"),
        )

        val winner = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = results,
            matchTitle = "Brown Rang",
            matchArtist = "Yo Yo Honey Singh",
            expectedDuration = 245,
        )

        assertNull("Cover / slowed edit must not be served as the searched master", winner)
    }

    @Test
    fun `Result row without any performer fields must not win unless the duration fingerprints the exact master`() {
        // Artist cannot be verified at all and the duration is 8s off: reject.
        val onlyTitleOffDuration = saavnResults(saavnRow("12 Saal", 225))
        val rejected = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = onlyTitleOffDuration,
            matchTitle = "12 Saal",
            matchArtist = "Bilal Saeed",
            expectedDuration = 217,
        )
        assertNull("Unverifiable artist + 8s duration delta must be rejected", rejected)

        // Artist cannot be verified at all but the duration is an exact master fingerprint: accept.
        val onlyTitleExactDuration = saavnResults(saavnRow("12 Saal", 218))
        val accepted = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = onlyTitleExactDuration,
            matchTitle = "12 Saal",
            matchArtist = "Bilal Saeed",
            expectedDuration = 217,
        )
        assertNotNull("Exact-duration fingerprint with exact title is an acceptable unverified-artist match", accepted)
        assertEquals("12 Saal", accepted!!.title)
        assertTrue(accepted.artists.isEmpty())
    }

    @Test
    fun `Title-only retry path (no artist supplied) requires the exact duration fingerprint`() {
        // This mirrors the internal title-only rerun: matchArtist is null.
        val offDuration = saavnResults(saavnRow("12 Saal", 225))
        val rejected = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = offDuration,
            matchTitle = "12 Saal",
            matchArtist = null,
            expectedDuration = 217,
        )
        assertNull("Title-only retry with 8s duration delta must be rejected", rejected)

        val exactDuration = saavnResults(saavnRow("12 Saal", 218))
        val accepted = OnlineLosslessAudioProvider.pickVerifiedSaavnCandidate(
            results = exactDuration,
            matchTitle = "12 Saal",
            matchArtist = null,
            expectedDuration = 217,
        )
        assertNotNull("Title-only retry with exact master duration is acceptable", accepted)
        assertEquals(218, accepted!!.durationSeconds)
    }
}
