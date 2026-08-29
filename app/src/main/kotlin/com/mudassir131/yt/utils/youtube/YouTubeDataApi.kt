/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils.youtube

import com.mudassir131.yt.utils.matching.MatchCandidate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/** The API key is missing, malformed, or has been revoked. Not retryable. */
class YouTubeApiKeyException(message: String) : Exception(message)

/** The project's daily quota is spent. Not retryable — the caller must fall back. */
class YouTubeQuotaExceededException(message: String) : Exception(message)

/** Short-term throttling. Retryable after [retryAfterMs]. */
class YouTubeRateLimitException(
    message: String,
    val retryAfterMs: Long? = null,
) : Exception(message)

/**
 * Read-only YouTube Data API v3 client used to match Spotify tracks to video IDs.
 *
 * Only `search.list` and `videos.list` are called. Both are API-key-only endpoints, so no OAuth and
 * no Google sign-in is involved, and nothing here can touch the user's own YouTube account. This
 * pipeline deliberately implements no playlist-write endpoint at all — matches are stored in
 * Nocturne's own database only, and a unit test greps this file to keep it that way.
 *
 * Error handling mirrors the Spotify side of [com.mudassir131.yt.utils.PlaylistImporter]: typed
 * exceptions surfaced through [Result], Timber logging under a single tag, and exponential backoff
 * on transient failures.
 */
object YouTubeDataApi {

    private const val TAG = "YouTubeMatch"
    private const val BASE_URL = "https://www.googleapis.com/youtube/v3"

    // Quota costs, per https://developers.google.com/youtube/v3/determine_quota_cost
    // These are the documented defaults; a project with a raised quota simply never trips the cap.
    const val COST_SEARCH_LIST = 100
    const val COST_VIDEOS_LIST = 1
    const val DEFAULT_DAILY_QUOTA_UNITS = 10_000

    /**
     * `videos.list` accepts up to 50 comma-separated IDs for a flat 1 unit, which is what makes
     * duration verification affordable: 5 candidates for 99 tracks costs 10 units, not 495.
     */
    const val VIDEOS_LIST_MAX_IDS = 50

    /** Candidates pulled per track. The spec's "top N (e.g. 3-5)". */
    const val DEFAULT_MAX_RESULTS = 5

    private const val MAX_RETRIES = 4
    private const val INITIAL_BACKOFF_MS = 1_000L
    private const val MAX_BACKOFF_MS = 30_000L

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Parses an ISO-8601 duration as returned by `videos.list` `contentDetails.duration`
     * (`PT3M52S`, `PT1H2M3S`, `PT45S`, `P1DT2H`) into whole seconds.
     *
     * Returns null on anything unparseable rather than guessing, so an unreadable duration is
     * treated as unknown instead of as zero.
     */
    fun parseIso8601Duration(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val match = ISO8601_DURATION.matchEntire(raw.trim()) ?: return null
        val (days, hours, minutes, seconds) = match.destructured
        val total = (days.toLongOrNull() ?: 0L) * 86_400L +
            (hours.toLongOrNull() ?: 0L) * 3_600L +
            (minutes.toLongOrNull() ?: 0L) * 60L +
            (seconds.toLongOrNull() ?: 0L)
        if (total <= 0L) return null
        return total.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private val ISO8601_DURATION =
        Regex("""P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?)?""")

    /**
     * `search.list` for one track. Costs [COST_SEARCH_LIST] units — the caller is responsible for
     * reserving quota before calling.
     *
     * Durations are not populated here; `search.list` does not return them. Follow up with
     * [fetchDurations].
     */
    suspend fun search(
        query: String,
        apiKey: String,
        maxResults: Int = DEFAULT_MAX_RESULTS,
    ): Result<List<MatchCandidate>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(YouTubeApiKeyException("No YouTube Data API key configured"))
        }

        val url = BASE_URL.toHttpUrlBuilder("search")
            .addQueryParameter("part", "snippet")
            .addQueryParameter("q", query)
            .addQueryParameter("type", "video")
            .addQueryParameter("maxResults", maxResults.coerceIn(1, 50).toString())
            .addQueryParameter("key", apiKey)
            .build()
            .toString()

        executeWithRetry(url, "search.list").mapCatching { body ->
            val items = JSONObject(body).optJSONArray("items")
            val candidates = mutableListOf<MatchCandidate>()
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val videoId = item.optJSONObject("id")?.optString("videoId", "")?.trim().orEmpty()
                    if (videoId.isEmpty()) continue
                    val snippet = item.optJSONObject("snippet")
                    candidates.add(
                        MatchCandidate(
                            videoId = videoId,
                            title = snippet?.optString("title", "")?.decodeHtmlEntities().orEmpty(),
                            channelTitle = snippet?.optString("channelTitle", "")?.decodeHtmlEntities().orEmpty(),
                            durationSec = null,
                            thumbnailUrl = snippet?.optJSONObject("thumbnails")
                                ?.let { it.optJSONObject("high") ?: it.optJSONObject("medium") ?: it.optJSONObject("default") }
                                ?.optString("url")
                                ?.takeIf { it.isNotEmpty() },
                        ),
                    )
                }
            }
            Timber.tag(TAG).d("search.list query='%s' candidates=%d", query, candidates.size)
            candidates
        }
    }

    /**
     * Resolves durations for up to [VIDEOS_LIST_MAX_IDS] IDs per request.
     *
     * Returns whatever it could resolve; a video that 404s or is region-blocked is simply absent
     * from the map and its candidate is then scored as duration-unknown.
     */
    suspend fun fetchDurations(
        videoIds: List<String>,
        apiKey: String,
    ): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(YouTubeApiKeyException("No YouTube Data API key configured"))
        }
        val distinct = videoIds.filter { it.isNotBlank() }.distinct()
        if (distinct.isEmpty()) return@withContext Result.success(emptyMap())

        val durations = mutableMapOf<String, Int>()
        for (batch in distinct.chunked(VIDEOS_LIST_MAX_IDS)) {
            val url = BASE_URL.toHttpUrlBuilder("videos")
                .addQueryParameter("part", "contentDetails")
                .addQueryParameter("id", batch.joinToString(","))
                .addQueryParameter("key", apiKey)
                .build()
                .toString()

            val result = executeWithRetry(url, "videos.list")
            if (result.isFailure) return@withContext Result.failure(result.exceptionOrNull()!!)

            val items = JSONObject(result.getOrThrow()).optJSONArray("items")
            if (items != null) {
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val id = item.optString("id", "").trim()
                    val iso = item.optJSONObject("contentDetails")?.optString("duration")
                    val seconds = parseIso8601Duration(iso)
                    if (id.isNotEmpty() && seconds != null) durations[id] = seconds
                }
            }
        }
        Timber.tag(TAG).d(
            "videos.list resolved %d/%d durations in %d call(s)",
            durations.size,
            distinct.size,
            videosListCallCount(distinct.size),
        )
        Result.success(durations)
    }

    /** Number of `videos.list` calls (and therefore units) [idCount] IDs will cost. */
    fun videosListCallCount(idCount: Int): Int =
        if (idCount <= 0) 0 else (idCount + VIDEOS_LIST_MAX_IDS - 1) / VIDEOS_LIST_MAX_IDS

    /**
     * Issues the request, retrying transient failures with exponential backoff.
     *
     * Quota exhaustion and key errors are returned immediately — retrying either just burns time,
     * and quota exhaustion is the signal the caller needs to switch to the InnerTube fallback.
     */
    private suspend fun executeWithRetry(url: String, label: String): Result<String> {
        var backoff = INITIAL_BACKOFF_MS
        var lastError: Throwable? = null

        repeat(MAX_RETRIES + 1) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("Accept", "application/json")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()

                    if (response.isSuccessful) return Result.success(body)

                    val reason = extractErrorReason(body)
                    val message = "$label HTTP ${response.code}${reason?.let { " ($it)" }.orEmpty()}"

                    when {
                        reason == "quotaExceeded" || reason == "dailyLimitExceeded" -> {
                            Timber.tag(TAG).w("Daily quota exhausted on %s", label)
                            return Result.failure(YouTubeQuotaExceededException(message))
                        }

                        reason == "keyInvalid" || reason == "keyExpired" ||
                            reason == "ipRefererBlocked" || reason == "accessNotConfigured" -> {
                            Timber.tag(TAG).w("API key rejected on %s: %s", label, reason)
                            return Result.failure(YouTubeApiKeyException(message))
                        }

                        response.code == 429 || reason == "rateLimitExceeded" || reason == "userRateLimitExceeded" -> {
                            val retryAfter = response.header("Retry-After")?.toLongOrNull()?.times(1000)
                            lastError = YouTubeRateLimitException(message, retryAfter)
                        }

                        response.code in 500..599 -> lastError = Exception(message)

                        // Any other 4xx is a request-shape problem; retrying will not fix it.
                        else -> return Result.failure(Exception(message))
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }

            if (attempt < MAX_RETRIES) {
                val wait = (lastError as? YouTubeRateLimitException)?.retryAfterMs ?: backoff
                Timber.tag(TAG).w(
                    "%s failed (attempt %d/%d), backing off %dms: %s",
                    label, attempt + 1, MAX_RETRIES + 1, wait, lastError?.message,
                )
                delay(wait.coerceAtMost(MAX_BACKOFF_MS))
                backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
            }
        }

        return Result.failure(lastError ?: Exception("$label failed after ${MAX_RETRIES + 1} attempts"))
    }

    /** Pulls `error.errors[0].reason` out of a Google API error payload. */
    private fun extractErrorReason(body: String): String? = try {
        JSONObject(body)
            .optJSONObject("error")
            ?.optJSONArray("errors")
            ?.optJSONObject(0)
            ?.optString("reason")
            ?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    private fun String.toHttpUrlBuilder(path: String) =
        okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("youtube/v3/$path")

    /** `search.list` snippets arrive HTML-escaped (`&amp;`, `&#39;`). */
    private fun String.decodeHtmlEntities(): String =
        replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
}
