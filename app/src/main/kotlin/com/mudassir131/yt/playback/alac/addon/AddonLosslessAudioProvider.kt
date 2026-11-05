/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac.addon

import com.mudassir131.yt.constants.AddonLosslessEnabledKey
import com.mudassir131.yt.constants.AddonLosslessNameKey
import com.mudassir131.yt.constants.AddonLosslessUrlKey
import com.mudassir131.yt.constants.SelfHostedLosslessEnabledKey
import com.mudassir131.yt.constants.SelfHostedServerNameKey
import com.mudassir131.yt.constants.SelfHostedServerUrlKey
import com.mudassir131.yt.playback.alac.LosslessAudioProvider
import com.mudassir131.yt.playback.alac.ResolvedLosslessStream
import com.mudassir131.yt.utils.PreferenceStore
import com.mudassir131.yt.utils.dataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import timber.log.Timber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/**
 * Pluggable external Addon lossless provider engine.
 * Connects to user-configured lossless addon links (providing FLAC, ALAC, or Hi-Res streams)
 * and resolves song queries to genuine lossless audio.
 */
object AddonLosslessAudioProvider : LosslessAudioProvider {
    override val name: String = "AddonLosslessAudioProvider"
    private const val TAG = "AddonLossless"

    data class AddonConfig(
        val url: String,
        val name: String = "Lossless Addon",
        val enabled: Boolean = true,
    )

    @Volatile
    var explicitConfig: AddonConfig? = null

    fun getConfig(): AddonConfig? {
        explicitConfig?.let { return it }

        // 1. Instant Synchronous Read: SharedPreferences cache
        val context = runCatching { com.mudassir131.yt.App.instance }.getOrNull()
        if (context != null) {
            val sp = context.getSharedPreferences("addon_sources_cache", android.content.Context.MODE_PRIVATE)
            val spUrl = sp.getString("addon_url", null)?.trim().orEmpty()
            val spEnabled = sp.getBoolean("addon_enabled", true)
            val spName = sp.getString("addon_name", null)?.trim().orEmpty().ifBlank { "Lossless Addon" }
            if (spUrl.isNotBlank()) {
                return AddonConfig(
                    url = AddonClient.normalizeBase(spUrl),
                    name = spName,
                    enabled = spEnabled,
                )
            }
        }

        // 2. Primary: Addon configuration from PreferenceStore
        val addonUrl = PreferenceStore.get(AddonLosslessUrlKey)?.trim().orEmpty()
        val addonEnabled = PreferenceStore.get(AddonLosslessEnabledKey) ?: true
        val addonName = PreferenceStore.get(AddonLosslessNameKey)?.trim().orEmpty().ifBlank { "Lossless Addon" }

        if (addonUrl.isNotBlank()) {
            return AddonConfig(
                url = AddonClient.normalizeBase(addonUrl),
                name = addonName,
                enabled = addonEnabled,
            )
        }

        // 3. Direct DataStore read fallback if PreferenceStore has not emitted yet
        if (context != null) {
            val dsUrl = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                runCatching {
                    context.dataStore.data.first()[AddonLosslessUrlKey]
                }.getOrNull()
            }?.trim().orEmpty()
            if (dsUrl.isNotBlank()) {
                return AddonConfig(
                    url = AddonClient.normalizeBase(dsUrl),
                    name = addonName,
                    enabled = addonEnabled,
                )
            }
        }

        // 4. Migration fallback: if legacy self-hosted URL was set
        val legacyEnabled = PreferenceStore.get(SelfHostedLosslessEnabledKey) ?: false
        val legacyUrl = PreferenceStore.get(SelfHostedServerUrlKey)?.trim().orEmpty()
        val legacyName = PreferenceStore.get(SelfHostedServerNameKey)?.trim().orEmpty().ifBlank { "Lossless Provider" }

        if (legacyEnabled && legacyUrl.isNotBlank()) {
            return AddonConfig(
                url = AddonClient.normalizeBase(legacyUrl),
                name = legacyName,
                enabled = true,
            )
        }

        return null
    }

    override suspend fun resolve(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
    ): ResolvedLosslessStream? = withContext(Dispatchers.IO) {
        val config = getConfig()
        if (config == null || !config.enabled || config.url.isBlank()) {
            Timber.tag(TAG).d("Addon lossless provider is not configured or disabled.")
            return@withContext null
        }

        if (title.isBlank()) return@withContext null

        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)
        val client = AddonClient(config.url)

        Timber.tag(TAG).i("Querying lossless addon (${config.name}) for: '$cleanTitle' by '$cleanArtist' (duration=${durationSeconds}s)")

        // 1. Search addon source with multi-step fallback
        var searchResult: List<AddonTrack>? = null

        // Attempt A: Clean title + artist
        if (cleanArtist.isNotBlank()) {
            searchResult = client.search("$cleanTitle $cleanArtist", tier = "lossless").getOrNull()
        }

        // Attempt B: Clean title only
        if (searchResult.isNullOrEmpty() && cleanTitle.isNotBlank()) {
            searchResult = client.search(cleanTitle, tier = "lossless").getOrNull()
        }

        // Attempt C: Raw title (in case special punctuation was critical)
        if (searchResult.isNullOrEmpty() && title.trim().isNotBlank() && title.trim() != cleanTitle) {
            val rawTrimmed = title.trim().substringBefore(" - ").substringBefore(" (")
            searchResult = client.search(rawTrimmed, tier = "lossless").getOrNull()
        }

        if (searchResult.isNullOrEmpty()) {
            Timber.tag(TAG).d("Addon returned no track results for '$title'")
            return@withContext null
        }

        // 2. Find best matching track candidate
        val bestCandidate = pickBestCandidate(searchResult, title, artist, durationSeconds)
        if (bestCandidate == null) {
            Timber.tag(TAG).d("None of the ${searchResult.size} addon candidates matched '$title' by '$artist'")
            return@withContext null
        }

        Timber.tag(TAG).i("Addon matched track '${bestCandidate.title}' by '${bestCandidate.artist}' (id=${bestCandidate.id})")

        // 3. Resolve stream details
        val stream = resolveStreamForTrack(client, bestCandidate)
        if (stream == null) {
            Timber.tag(TAG).w("Failed to resolve audio stream from addon for track '${bestCandidate.id}'")
            return@withContext null
        }

        // 4. Codec & Lossless Verification Gate
        if (!stream.isLossless) {
            Timber.tag(TAG).w("Candidate from addon is explicitly lossy (codec=${stream.codec}, format=${stream.format}). Rejected.")
            return@withContext null
        }

        val rawCodec = (stream.codec ?: stream.format ?: "flac").lowercase(Locale.ROOT)
        val isAlac = rawCodec.contains("alac")
        val sampleRate = stream.sampleRate ?: 48000
        val isHiRes = stream.isHiRes || sampleRate > 48000 || (stream.bitDepth ?: 0) > 16 || rawCodec.contains("24")
        val bitDepth = stream.bitDepth ?: (if (isHiRes) 24 else 16)

        val finalCodec = if (isAlac) {
            if (isHiRes) "alac.24" else "alac"
        } else {
            if (isHiRes) "flac.24" else "flac"
        }
        val finalMime = if (isAlac) "audio/alac" else "audio/flac"

        val duration = bestCandidate.durationSeconds?.toInt()?.takeIf { it > 0 } ?: durationSeconds
        val bitrate = stream.bitrate?.takeIf { it > 0 } ?: (sampleRate * bitDepth * 2 * 6 / 10)
        val contentLength = stream.contentLength ?: probeContentLength(stream.url)
        val mbFormatted = contentLength?.let { String.format(Locale.ROOT, "%.2f MB", it / (1024.0 * 1024.0)) } ?: "unknown size"

        Timber.tag(TAG).i("[ADDON_SUCCESS] Resolved genuine lossless stream from addon: ${stream.url}")
        Timber.tag(TAG).i("               Codec=$finalCodec, SampleRate=$sampleRate Hz, BitDepth=$bitDepth-bit, isHiRes=$isHiRes, Size=$mbFormatted ($contentLength bytes)")

        ResolvedLosslessStream(
            url = stream.url,
            mimeType = finalMime,
            codec = finalCodec,
            bitDepth = bitDepth,
            sampleRate = sampleRate,
            channels = 2,
            bitrate = bitrate,
            contentLength = contentLength,
            durationSeconds = duration,
            sourceName = "addon_lossless",
            expiresInSeconds = 86400,
        )
    }

    suspend fun probeContentLength(url: String): Long? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null

        // 1. Quick HTTP HEAD request
        runCatching {
            val headRequest = Request.Builder()
                .url(url)
                .header("User-Agent", AddonClient.USER_AGENT)
                .head()
                .build()
            AddonClient.httpClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) {
                    val cl = response.header("Content-Length")?.toLongOrNull()
                    if (cl != null && cl > 0L) return@withContext cl
                }
            }
        }

        // 2. HTTP GET with Range: bytes=0-0 to extract total length from Content-Range
        runCatching {
            val rangeRequest = Request.Builder()
                .url(url)
                .header("User-Agent", AddonClient.USER_AGENT)
                .header("Range", "bytes=0-0")
                .get()
                .build()
            AddonClient.httpClient.newCall(rangeRequest).execute().use { response ->
                val cr = response.header("Content-Range")
                if (cr != null && cr.contains("/")) {
                    val total = cr.substringAfterLast("/").trim().toLongOrNull()
                    if (total != null && total > 0L) return@withContext total
                }
                val cl = response.header("Content-Length")?.toLongOrNull()
                if (cl != null && cl > 1L) return@withContext cl
            }
        }

        null
    }

    private suspend fun resolveStreamForTrack(
        client: AddonClient,
        candidate: AddonTrack,
    ): AddonStream? {
        // Direct streamUrl on candidate
        if (!candidate.streamUrl.isNullOrBlank()) {
            val candidateStream = AddonStream(
                url = candidate.streamUrl,
                format = candidate.format,
                quality = candidate.quality,
                sampleRate = candidate.sampleRate,
                bitDepth = candidate.bitDepth,
                bitrate = candidate.bitrate,
            )
            if (candidateStream.isLossless) {
                return candidateStream
            }
        }

        // Candidate ID is already a playable HTTP link
        if (candidate.id.startsWith("http://", ignoreCase = true) || candidate.id.startsWith("https://", ignoreCase = true)) {
            return AddonStream(
                url = candidate.id,
                format = candidate.format,
                quality = candidate.quality,
                sampleRate = candidate.sampleRate,
                bitDepth = candidate.bitDepth,
                bitrate = candidate.bitrate,
            )
        }

        // Query /stream/{id}
        if (candidate.id.isNotBlank()) {
            val streamResult = client.stream(candidate.id, tier = "lossless").getOrNull()
            if (streamResult != null && streamResult.url.isNotBlank()) {
                return streamResult
            }
        }

        // Fallback to track streamUrl if present
        if (!candidate.streamUrl.isNullOrBlank()) {
            return AddonStream(
                url = candidate.streamUrl,
                format = candidate.format,
                quality = candidate.quality,
                sampleRate = candidate.sampleRate,
                bitDepth = candidate.bitDepth,
                bitrate = candidate.bitrate,
            )
        }

        return null
    }

    internal fun pickBestCandidate(
        tracks: List<AddonTrack>,
        targetTitle: String,
        targetArtist: String,
        targetDuration: Int,
    ): AddonTrack? {
        var bestTrack: AddonTrack? = null
        var bestScore = -1.0

        for (track in tracks) {
            val score = calculateMatchScore(track, targetTitle, targetArtist, targetDuration)
            if (score >= 0.40 && score > bestScore) {
                bestScore = score
                bestTrack = track
            }
        }

        // Single candidate fallback: if addon returned exactly 1 track for our specific query
        if (bestTrack == null && tracks.size == 1) {
            val single = tracks.first()
            val score = calculateMatchScore(single, targetTitle, targetArtist, targetDuration)
            if (score >= 0.28) {
                Timber.tag(TAG).d("Accepting single candidate '${single.title}' (score=$score)")
                return single
            }
        }

        return bestTrack
    }

    internal fun calculateMatchScore(
        track: AddonTrack,
        targetTitle: String,
        targetArtist: String,
        targetDuration: Int,
    ): Double {
        val candTitle = cleanSearchTerm(track.title)
        val candArtist = cleanSearchTerm(track.artist)
        val cleanTargetTitle = cleanSearchTerm(targetTitle)
        val cleanTargetArtist = cleanSearchTerm(targetArtist)

        // Title score
        var titleSim = calculateStringSimilarity(candTitle, cleanTargetTitle)

        // Check if YouTube title had artist embedded
        if (cleanTargetArtist.isNotBlank() && cleanTargetTitle.contains(cleanTargetArtist)) {
            val titleWithoutArtist = cleanTargetTitle.replace(cleanTargetArtist, "").trim()
            val altSim = calculateStringSimilarity(candTitle, titleWithoutArtist)
            if (altSim > titleSim) {
                titleSim = altSim
            }
        }

        // Substring containment gives strong title agreement
        if (candTitle.isNotBlank() && cleanTargetTitle.isNotBlank()) {
            if (candTitle.contains(cleanTargetTitle) || cleanTargetTitle.contains(candTitle)) {
                titleSim = max(titleSim, 0.85)
            }
        }

        // Minimum title similarity floor
        if (titleSim < 0.28) {
            return 0.0
        }

        // Artist score
        var artistScore = 0.5
        if (cleanTargetArtist.isNotBlank() && candArtist.isNotBlank()) {
            artistScore = calculateStringSimilarity(candArtist, cleanTargetArtist)
            if (candArtist.contains(cleanTargetArtist) || cleanTargetArtist.contains(candArtist)) {
                artistScore = max(artistScore, 0.90)
            }
        } else if (cleanTargetArtist.isNotBlank() && cleanTargetTitle.contains(cleanTargetArtist)) {
            artistScore = 0.80
        }

        // Duration score
        var durationScore = 0.70
        val candDuration = track.durationSeconds?.toInt() ?: -1
        if (targetDuration > 0 && candDuration > 0) {
            val delta = abs(targetDuration - candDuration)
            if (delta > 120 && artistScore < 0.65) {
                return 0.0 // Reject only on huge delta with weak artist match
            }
            durationScore = when {
                delta <= 8 -> 1.0
                delta <= 25 -> 0.90
                delta <= 50 -> 0.75
                delta <= 90 -> 0.60
                else -> 0.40
            }
        }

        return (titleSim * 0.55) + (artistScore * 0.25) + (durationScore * 0.20)
    }

    internal fun cleanSearchTerm(term: String): String {
        return term
            .replace(Regex("""(?i)\(?(official\s*(video|audio|music\s*video|lyric\s*video|hd|4k|remastered|remaster)?)\)?"""), "")
            .replace(Regex("""(?i)\[?(official\s*(video|audio|music\s*video|lyric\s*video|hd|4k|remastered|remaster)?)\]?"""), "")
            .replace(Regex("""(?i)\(from\s+[^)]+\)"""), "")
            .replace(Regex("""(?i)\[from\s+[^\]]+\]"""), "")
            .replace(Regex("""(?i)\b(feat\.|ft\.|featuring)\b.*"""), "")
            .replace(Regex("""[^\p{L}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .lowercase(Locale.ROOT)
    }

    internal fun calculateStringSimilarity(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        if (s1.isBlank() || s2.isBlank()) return 0.0
        if (s1.contains(s2) || s2.contains(s1)) {
            val shorter = minOf(s1.length, s2.length).toDouble()
            val longer = maxOf(s1.length, s2.length).toDouble()
            return 0.85 * (shorter / longer).coerceAtLeast(0.7)
        }

        // Token overlap (Jaccard similarity)
        val tokens1 = s1.split(" ").filter { it.isNotBlank() }.toSet()
        val tokens2 = s2.split(" ").filter { it.isNotBlank() }.toSet()
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val intersection = tokens1.intersect(tokens2).size
        val union = tokens1.union(tokens2).size
        return intersection.toDouble() / union.toDouble()
    }

    internal fun redactSensitiveUrl(url: String): String {
        return try {
            val uri = java.net.URI(url)
            "${uri.scheme}://${uri.host}/***"
        } catch (_: Exception) {
            "***"
        }
    }
}



