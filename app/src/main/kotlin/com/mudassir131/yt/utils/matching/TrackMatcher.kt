/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils.matching

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * A single YouTube candidate under consideration for a Spotify track.
 *
 * [durationSec] is null until it has been resolved (Data API `videos.list` returns durations in a
 * separate call from `search.list`, and InnerTube does not always populate one).
 */
data class MatchCandidate(
    val videoId: String,
    val title: String,
    val channelTitle: String = "",
    val durationSec: Int? = null,
    val thumbnailUrl: String? = null,
    val albumName: String? = null,
    val artistNames: List<String> = emptyList(),
)

/** The component scores behind a candidate's [confidence], kept for logging and manual review. */
data class MatchScore(
    val titleScore: Double,
    val durationScore: Double,
    val channelScore: Double,
    val confidence: Double,
    val durationDeltaSec: Int?,
    val disqualified: Boolean = false,
) {
    /** True when the duration agreed within the ±5s window the import treats as high confidence. */
    val durationVerified: Boolean
        get() = durationDeltaSec != null && durationDeltaSec <= TrackMatcher.DURATION_TOLERANCE_SEC
}

enum class MatchStatus { MATCHED, UNMATCHED }

data class MatchResult(
    val candidate: MatchCandidate?,
    val score: MatchScore?,
    val status: MatchStatus,
) {
    val videoId: String? get() = candidate?.videoId.takeIf { status == MatchStatus.MATCHED }
    val confidence: Double get() = score?.confidence ?: 0.0
}

/**
 * Ranks YouTube candidates against a Spotify track on title similarity and duration agreement.
 *
 * Deliberately pure Kotlin with no Android or network dependencies so the whole ranking policy is
 * unit-testable, and no fuzzy-matching library is pulled in (the project ships none).
 */
object TrackMatcher {

    /** Below this, a track is stored as UNMATCHED for manual review rather than force-matched. */
    const val CONFIDENCE_THRESHOLD = 0.55

    /** Duration agreement within this many seconds counts as a full-score, high-confidence match. */
    const val DURATION_TOLERANCE_SEC = 5

    /** Duration score decays linearly from [DURATION_TOLERANCE_SEC] to zero at this delta. */
    const val DURATION_ZERO_SCORE_SEC = 30

    /**
     * A candidate whose duration is known and off by more than this is disqualified outright,
     * whatever its title says. This is what keeps hour-long mixes, full-album uploads and
     * "10 hours of X" videos from winning on a title that happens to match.
     */
    const val DURATION_HARD_REJECT_SEC = 60

    /**
     * The candidate title must agree with the *track name* at least this well, or the candidate is
     * disqualified whatever else it has going for it.
     *
     * Duration and channel are corroborating signals, not substitutes: two songs being the same
     * length is not rare, and without this floor a perfect duration plus an artist-named channel
     * contributes 0.40 of the 0.55 threshold on its own, leaving a completely different song needing
     * only 0.25 of title agreement — which two unrelated strings of similar length reach by accident.
     *
     * Measured against the track name alone rather than "track + artist", because a YouTube title
     * legitimately often omits the artist (it is in the channel name instead), and Spotify
     * legitimately appends suffixes like "- Remastered 2009" that the upload will not have.
     */
    const val MIN_TRACK_NAME_SCORE = 0.35

    private const val WEIGHT_TITLE = 0.60
    private const val WEIGHT_DURATION = 0.35
    private const val WEIGHT_CHANNEL = 0.05

    /**
     * Score used when a duration is unknown on either side: neutral, so a missing value neither
     * fakes a perfect match nor condemns an otherwise good candidate.
     */
    private const val DURATION_UNKNOWN_SCORE = 0.5

    /**
     * Containment can only ever reach this, never 1.0. A candidate title that merely *contains* the
     * query ("Song" inside "Song - Extended Remix") must not score as an exact match.
     */
    private const val CONTAINMENT_CEILING = 0.9

    // Upload noise that says nothing about which recording this is.
    private val NOISE_WORDS = setOf(
        "official", "officiel", "video", "videoclip", "vid", "audio", "lyric", "lyrics",
        "letra", "hd", "hq", "4k", "1080p", "720p", "visualizer", "visualiser", "mv",
        "remaster", "remastered", "remasterizado", "explicit", "clean", "promo", "teaser",
    )

    /**
     * Words marking a genuinely different recording. A parenthetical containing any of these is
     * kept, so a remix or a live cut never normalises into the studio original.
     */
    private val VARIANT_WORDS = setOf(
        "remix", "acoustic", "live", "instrumental", "cover", "karaoke", "edit", "extended",
        "sped", "slowed", "reverb", "version", "mix", "demo", "reprise", "unplugged",
        "orchestral", "piano", "radio", "club", "dub", "vip", "bootleg", "mashup", "medley",
        "intro", "outro", "interlude", "session", "rework", "flip", "refix",
    )

    /** Words carrying no identifying weight, tolerated inside an otherwise droppable bracket. */
    private val FILLER_WORDS = setOf("music", "the", "a", "an", "of", "from", "full", "and", "on", "in")

    private val FEAT_MARKER = Regex("""\b(?:feat|ft|featuring|w/|with)\b\.?""", RegexOption.IGNORE_CASE)
    private val BRACKETED = Regex("""[(\[{]([^)\]}]*)[)\]}]""")
    private val NON_ALNUM = Regex("""[^\p{L}\p{N}\s]""")
    private val WHITESPACE = Regex("""\s+""")
    private val YEAR = Regex("""^(19|20)\d{2}$""")
    private val TOPIC_CHANNEL = Regex("""\s*-\s*topic$""", RegexOption.IGNORE_CASE)

    /**
     * Folds a title down to comparable words: drops upload-noise brackets, the feat./ft. marker
     * word (keeping the artist names it introduces), punctuation and case.
     */
    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        var s = raw

        s = BRACKETED.replace(s) { match ->
            val inner = match.groupValues[1]
            if (isDroppableBracket(inner)) " " else " ${inner} "
        }

        s = TOPIC_CHANNEL.replace(s, " ")
        s = FEAT_MARKER.replace(s, " ")
        s = NON_ALNUM.replace(s, " ")
        s = s.lowercase()

        return WHITESPACE.replace(s, " ").trim()
    }

    /**
     * A bracket is upload noise only if it names noise *and* names no variant. "(Official Video)"
     * goes; "(Official Live Video)" and "(Live at Wembley)" stay.
     */
    private fun isDroppableBracket(inner: String): Boolean {
        val words = NON_ALNUM.replace(inner, " ")
            .lowercase()
            .split(WHITESPACE)
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return true
        if (words.any { it in VARIANT_WORDS }) return false
        return words.any { it in NOISE_WORDS } &&
            words.all { it in NOISE_WORDS || it in FILLER_WORDS || YEAR.matches(it) }
    }

    private fun tokens(normalized: String): List<String> =
        if (normalized.isEmpty()) emptyList() else normalized.split(" ").filter { it.isNotBlank() }

    /** Levenshtein distance, two-row DP so memory is O(min(m, n)) rather than O(m*n). */
    fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        // Iterate over the longer string so the row buffers stay as small as possible.
        val (short, long) = if (a.length <= b.length) a to b else b to a
        var previous = IntArray(short.length + 1) { it }
        var current = IntArray(short.length + 1)

        for (i in 1..long.length) {
            current[0] = i
            val longChar = long[i - 1]
            for (j in 1..short.length) {
                val substitution = previous[j - 1] + if (longChar == short[j - 1]) 0 else 1
                current[j] = min(min(current[j - 1] + 1, previous[j] + 1), substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[short.length]
    }

    /** Edit distance expressed as 0.0..1.0 similarity. */
    fun levenshteinRatio(a: String, b: String): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val longest = maxOf(a.length, b.length)
        return 1.0 - levenshtein(a, b).toDouble() / longest
    }

    /**
     * Compares token multisets after sorting, so "Artist - Title" and "Title by Artist" match.
     *
     * Sort rather than set semantics on purpose: a set-subset ratio would score "Song" against
     * "Song Extended Remix" as a perfect match.
     */
    fun tokenSortRatio(a: String, b: String): Double {
        val sortedA = tokens(a).sorted().joinToString(" ")
        val sortedB = tokens(b).sorted().joinToString(" ")
        return levenshteinRatio(sortedA, sortedB)
    }

    /** Fraction of query tokens present in the candidate, capped so it can never imply equality. */
    fun containmentRatio(query: String, candidate: String): Double {
        val queryTokens = tokens(query)
        if (queryTokens.isEmpty()) return 0.0
        val candidateTokens = tokens(candidate).toHashSet()
        val present = queryTokens.count { it in candidateTokens }
        return (present.toDouble() / queryTokens.size) * CONTAINMENT_CEILING
    }

    /** Best of the three title measures; each covers a failure mode of the others. */
    fun titleScore(query: String, candidateTitle: String): Double {
        val normalizedQuery = normalize(query)
        val normalizedCandidate = normalize(candidateTitle)
        if (normalizedQuery.isEmpty() || normalizedCandidate.isEmpty()) return 0.0
        return maxOf(
            levenshteinRatio(normalizedQuery, normalizedCandidate),
            tokenSortRatio(normalizedQuery, normalizedCandidate),
            containmentRatio(normalizedQuery, normalizedCandidate),
        ).coerceIn(0.0, 1.0)
    }

    /** 1.0 within ±[DURATION_TOLERANCE_SEC], decaying to 0.0 at [DURATION_ZERO_SCORE_SEC]. */
    fun durationScore(spotifyDurationMs: Long, candidateDurationSec: Int?): Double {
        val delta = durationDeltaSec(spotifyDurationMs, candidateDurationSec) ?: return DURATION_UNKNOWN_SCORE
        return when {
            delta <= DURATION_TOLERANCE_SEC -> 1.0
            delta >= DURATION_ZERO_SCORE_SEC -> 0.0
            else -> {
                val span = (DURATION_ZERO_SCORE_SEC - DURATION_TOLERANCE_SEC).toDouble()
                1.0 - (delta - DURATION_TOLERANCE_SEC) / span
            }
        }
    }

    /** Absolute seconds between the two durations, or null when either side is unknown. */
    fun durationDeltaSec(spotifyDurationMs: Long, candidateDurationSec: Int?): Int? {
        if (spotifyDurationMs <= 0L) return null
        if (candidateDurationSec == null || candidateDurationSec <= 0) return null
        val spotifySec = (spotifyDurationMs / 1000.0).roundToInt()
        return abs(spotifySec - candidateDurationSec)
    }

    /**
     * Credits a channel that names the artist, or an auto-generated "<Artist> - Topic" channel —
     * YouTube only creates those for distributed official audio, so it is a strong signal.
     */
    fun channelScore(artist: String, channelTitle: String): Double {
        if (channelTitle.isBlank()) return 0.0
        if (channelTitle.trimEnd().endsWith("- Topic", ignoreCase = true)) return 1.0
        if (artist.isBlank()) return 0.0
        val normalizedArtist = normalize(artist)
        val normalizedChannel = normalize(channelTitle)
        if (normalizedArtist.isEmpty() || normalizedChannel.isEmpty()) return 0.0
        return if (normalizedChannel.contains(normalizedArtist) || normalizedArtist.contains(normalizedChannel)) 1.0 else 0.0
    }

    private val SUSPICIOUS_VARIANTS = setOf(
        "remix", "cover", "karaoke", "instrumental", "slowed", "reverb", "sped", "nightcore",
        "live", "acoustic", "unplugged", "mashup", "medley", "8d", "bass", "status", "shorts",
        "lyrics", "lyric", "dj", "8k", "female", "male", "unofficial"
    )

    fun hasUnwantedVariant(trackName: String, candidateTitle: String): Boolean {
        val lowerTrack = trackName.lowercase()
        val lowerCandidate = candidateTitle.lowercase()

        for (variant in SUSPICIOUS_VARIANTS) {
            val pattern = Regex("""\b$variant\b""", RegexOption.IGNORE_CASE)
            val candidateHasVariant = pattern.containsMatchIn(lowerCandidate)
            val trackHasVariant = pattern.containsMatchIn(lowerTrack)
            if (candidateHasVariant && !trackHasVariant) {
                return true
            }
        }
        return false
    }

    /** Scores one candidate. A disqualified candidate carries confidence 0.0 and can never win. */
    fun score(
        trackName: String,
        artist: String,
        spotifyDurationMs: Long,
        candidate: MatchCandidate,
    ): MatchScore {
        val query = "$trackName $artist".trim()
        val delta = durationDeltaSec(spotifyDurationMs, candidate.durationSec)

        // Two hard rejections, both for the same reason: no secondary signal, however strong, may
        // carry a candidate that fails a primary one.
        val durationRejected = delta != null && delta > DURATION_HARD_REJECT_SEC
        val titleRejected = trackName.isNotBlank() &&
            titleScore(trackName, candidate.title) < MIN_TRACK_NAME_SCORE

        if (durationRejected || titleRejected) {
            return MatchScore(
                titleScore = 0.0,
                durationScore = 0.0,
                channelScore = 0.0,
                confidence = 0.0,
                durationDeltaSec = delta,
                disqualified = true,
            )
        }

        val titleQueryScore = titleScore(query, candidate.title)
        val titleTrackScore = if (trackName.isNotBlank()) titleScore(trackName, candidate.title) else 0.0
        var rawTitle = maxOf(titleQueryScore, titleTrackScore)
        if (hasUnwantedVariant(trackName, candidate.title)) {
            rawTitle *= 0.3
        }

        val title = rawTitle
        val duration = durationScore(spotifyDurationMs, candidate.durationSec)
        val channel = channelScore(artist, candidate.channelTitle)

        val confidence = (WEIGHT_TITLE * title) +
            (WEIGHT_DURATION * duration) +
            (WEIGHT_CHANNEL * channel)

        return MatchScore(
            titleScore = title,
            durationScore = duration,
            channelScore = channel,
            confidence = confidence.coerceIn(0.0, 1.0),
            durationDeltaSec = delta,
        )
    }

    /**
     * Picks the best-scoring candidate, or reports UNMATCHED when nothing clears [threshold].
     *
     * Returning UNMATCHED rather than the least-bad candidate is the point: a wrong video in the
     * playlist is worse than a gap flagged for review.
     */
    fun pickBest(
        trackName: String,
        artist: String,
        spotifyDurationMs: Long,
        candidates: List<MatchCandidate>,
        threshold: Double = CONFIDENCE_THRESHOLD,
    ): MatchResult {
        if (candidates.isEmpty()) return MatchResult(null, null, MatchStatus.UNMATCHED)

        val scored = candidates
            .map { it to score(trackName, artist, spotifyDurationMs, it) }
            .filterNot { (_, score) -> score.disqualified }

        val best = scored.maxByOrNull { (_, score) -> score.confidence }
            ?: return MatchResult(null, null, MatchStatus.UNMATCHED)

        val (candidate, score) = best
        val status = if (score.confidence >= threshold) MatchStatus.MATCHED else MatchStatus.UNMATCHED
        return MatchResult(candidate, score, status)
    }
}
