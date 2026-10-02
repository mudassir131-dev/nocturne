/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import java.util.Locale
import kotlin.math.abs

object MusicVideoMatcher {
    private const val MIN_CONFIDENCE_SCORE = 50

    private val UNWANTED_KEYWORDS = listOf(
        "lyric",
        "lyrics",
        "live",
        "concert",
        "tour",
        "live at",
        "acoustic",
        "unplugged",
        "cover",
        "reaction",
        "reacts",
        "karaoke",
        "instrumental",
        "parody",
        "teaser",
        "snippet",
        "trailer",
        "behind the scenes",
        "making of",
        "shorts",
        "#shorts",
        "extended",
        "1 hour",
        "10 hours",
        "loop",
        "slowed",
        "reverb",
        "sped up",
        "speed up",
        "nightcore",
        "bass boosted",
        "8d audio",
        "sub español",
        "subtitulad",
        "amv",
        "edit",
        "drift",
        "bmw",
        "dance compilation",
        "compilation",
        "fan made",
        "unofficial",
        "tribute",
    )

    private val TITLE_CLEANUP_REGEX = Regex(
        "(?i)\\s*[\\[(]?(official\\s*(music\\s*)?video|official\\s*audio|lyric\\s*video|lyrics|hd|4k|remastered|explicit|audio|visualizer)[\\])]?",
    )

    data class Candidate(
        val videoId: String,
        val title: String,
        val artist: String,
        val durationSec: Int?,
        val thumbnailUrl: String?,
        val musicVideoType: String? = null,
    )

    fun findBestMatch(
        trackTitle: String,
        trackArtist: String,
        trackAlbum: String?,
        trackDurationSec: Int?,
        candidates: List<Candidate>,
    ): Candidate? {
        if (candidates.isEmpty()) return null

        val scored = candidates.map { candidate ->
            val score = scoreCandidate(
                trackTitle = trackTitle,
                trackArtist = trackArtist,
                trackAlbum = trackAlbum,
                trackDurationSec = trackDurationSec,
                candidate = candidate,
            )
            candidate to score
        }

        val best = scored
            .filter { it.second >= MIN_CONFIDENCE_SCORE }
            .maxByOrNull { it.second }

        return best?.first
    }

    fun scoreCandidate(
        trackTitle: String,
        trackArtist: String,
        trackAlbum: String?,
        trackDurationSec: Int?,
        candidate: Candidate,
    ): Int {
        val normTrackTitle = cleanTitle(trackTitle)
        val normTrackArtist = trackArtist.lowercase(Locale.ROOT).trim()
        val candTitleLower = candidate.title.lowercase(Locale.ROOT)
        val candArtistLower = candidate.artist.lowercase(Locale.ROOT).trim()

        // 1. Disqualification check for unwanted terms unless track title itself requested them
        for (kw in UNWANTED_KEYWORDS) {
            if (candTitleLower.contains(kw) && !normTrackTitle.contains(kw)) {
                return -1000
            }
        }

        var score = 0

        // 2. Official music video indicator in metadata or title
        val isExplicitOmv = candidate.musicVideoType == "MUSIC_VIDEO_TYPE_OMV"
        val hasOfficialVideoInTitle = candTitleLower.contains("official music video") ||
            candTitleLower.contains("official video")
        val hasMusicVideoInTitle = candTitleLower.contains("music video")

        if (isExplicitOmv) {
            score += 60
        }
        if (hasOfficialVideoInTitle) {
            score += 55
        } else if (hasMusicVideoInTitle) {
            score += 25
        }

        // 3. Artist match - HEAVILY prioritize matching the channel/artist name
        if (normTrackArtist.isNotBlank()) {
            val primaryArtist = normTrackArtist.split(",", "&", "feat.", "ft.", " x ").firstOrNull()?.trim() ?: normTrackArtist
            val channelMatches = candArtistLower == normTrackArtist ||
                candArtistLower.contains(normTrackArtist) ||
                normTrackArtist.contains(candArtistLower) ||
                candArtistLower.contains(primaryArtist)

            if (channelMatches) {
                score += 70
                if (candArtistLower.contains("vevo") || candTitleLower.contains("vevo")) {
                    score += 20
                }
            } else {
                // Channel does NOT match artist!
                if (candTitleLower.contains(normTrackArtist) || candTitleLower.contains(primaryArtist)) {
                    // Artist name is only in the title by third-party uploader
                    score -= 40
                } else {
                    // Neither channel nor title mentions artist
                    score -= 80
                }
            }
        }

        // 4. Title match
        val trackTokens = normTrackTitle.split("\\s+".toRegex()).filter { it.length > 1 }
        if (trackTokens.isNotEmpty()) {
            val matchedTokens = trackTokens.count { candTitleLower.contains(it) }
            val ratio = matchedTokens.toFloat() / trackTokens.size.toFloat()
            if (ratio >= 0.9f) {
                score += 45
            } else if (ratio >= 0.6f) {
                score += 25
            } else {
                score -= 30
            }
        }

        // 5. Duration match - Official videos often have intros/outros/story scenes (+20-90s)
        if (trackDurationSec != null && trackDurationSec > 0 && candidate.durationSec != null && candidate.durationSec > 0) {
            val diff = abs(candidate.durationSec - trackDurationSec)
            val isOfficialCandidate = isExplicitOmv || hasOfficialVideoInTitle

            if (isOfficialCandidate) {
                when {
                    diff <= 15 -> score += 30
                    diff <= 45 -> score += 25
                    diff <= 90 -> score += 20 // Story intro/outro
                    diff <= 120 -> score += 10
                    diff in 121..240 -> score -= 40
                    else -> score -= 150
                }
            } else {
                when {
                    diff <= 10 -> score += 25
                    diff <= 30 -> score += 15
                    diff in 31..60 -> score -= 10
                    diff in 61..120 -> score -= 50
                    else -> score -= 150
                }
            }
        }

        return score
    }

    private fun cleanTitle(raw: String): String {
        return raw
            .replace(TITLE_CLEANUP_REGEX, "")
            .replace("[^\\p{L}\\p{N}\\s]".toRegex(), " ")
            .lowercase(Locale.ROOT)
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}
