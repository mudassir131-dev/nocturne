/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac.addon

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import timber.log.Timber
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * HTTP client for communicating with external Addon sources.
 * Fully aligned with Addon protocol specification:
 * - Manifest at `{base}/manifest.json`
 * - Search at `{base}/search?q={query}&quality=LOSSLESS&tier=lossless&atmos=auto`
 * - Stream at `{base}/stream/{id}?quality=LOSSLESS&tier=lossless&atmos=auto`
 * - Resilient timeouts (15s connect, 60s read, 45s call) to handle serverless cold-starts
 */
class AddonClient(rawBaseUrl: String) {
    val baseUrl: String
    val baseQuery: String?

    init {
        val (normBase, query) = splitBaseAndQuery(rawBaseUrl)
        this.baseUrl = normBase
        this.baseQuery = query
    }

    private fun appendBaseQuery(url: String): String {
        if (baseQuery.isNullOrBlank()) return url
        val delimiter = if (url.contains("?")) "&" else "?"
        return "$url$delimiter$baseQuery"
    }

    companion object {
        private const val TAG = "AddonClient"
        private const val MANIFEST_SUFFIX = "/manifest.json"
        // Standard user-agent for community addon endpoints
        internal const val USER_AGENT = "Nocturne/v1.0.0 (Lossless Addon Engine)"

        fun splitBaseAndQuery(raw: String): Pair<String, String?> {
            val trimmed = raw.trim()
            val queryIdx = trimmed.indexOf('?')
            val (pathPart, queryPart) = if (queryIdx != -1) {
                trimmed.substring(0, queryIdx) to trimmed.substring(queryIdx + 1).takeIf { it.isNotBlank() }
            } else {
                trimmed to null
            }

            var cleanPath = pathPart.trimEnd('/')
            if (cleanPath.endsWith(MANIFEST_SUFFIX, ignoreCase = true)) {
                cleanPath = cleanPath.dropLast(MANIFEST_SUFFIX.length).trimEnd('/')
            }
            if (cleanPath.endsWith("/search", ignoreCase = true)) {
                cleanPath = cleanPath.dropLast("/search".length).trimEnd('/')
            }
            if (cleanPath.endsWith("/stream", ignoreCase = true)) {
                cleanPath = cleanPath.dropLast("/stream".length).trimEnd('/')
            }
            // Strip any trailing .json file name (e.g. /addon.json)
            val lastSegment = cleanPath.substringAfterLast('/', "")
            if (lastSegment.endsWith(".json", ignoreCase = true) && cleanPath.contains("://")) {
                cleanPath = cleanPath.dropLast(lastSegment.length).trimEnd('/')
            }
            return cleanPath to queryPart
        }

        fun normalizeBase(raw: String): String = splitBaseAndQuery(raw).first

        // Optimized OkHttpClient timeouts (60s read, 45s call)
        internal val httpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
        }
    }

    /**
     * Checks if the addon server answers with a valid manifest.
     */
    suspend fun manifest(): Result<AddonManifest> = withContext(Dispatchers.IO) {
        runCatching {
            val url = appendBaseQuery("$baseUrl$MANIFEST_SUFFIX")
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Addon manifest returned HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty().trim().removePrefix("\uFEFF")
                val json = JSONObject(body)
                val manifest = AddonManifest.fromJson(json)
                if (manifest.id.isBlank() && manifest.name.isBlank()) {
                    throw IllegalStateException("Addon manifest does not contain id or name")
                }
                manifest
            }
        }
    }

    /**
     * Health check probe to test whether the search endpoint responds.
     */
    suspend fun probeSearch(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val res1 = search("Starboy", tier = "LOSSLESS").getOrNull()
            if (!res1.isNullOrEmpty()) return@runCatching res1.size
            val res2 = search("music", tier = "LOSSLESS").getOrNull()
            if (!res2.isNullOrEmpty()) return@runCatching res2.size
            val res3 = search("test").getOrThrow()
            res3.size
        }
    }

    /**
     * Searches the addon library for tracks matching [query].
     */
    suspend fun search(query: String, tier: String = "LOSSLESS"): Result<List<AddonTrack>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8").replace("+", "%20")

                // 1. Primary search with uppercase quality=LOSSLESS & tier=lossless
                val urlWithQuality = appendBaseQuery("$baseUrl/search?q=$encodedQuery&quality=LOSSLESS&tier=lossless&atmos=auto")
                Timber.tag(TAG).d("Searching addon (LOSSLESS): $urlWithQuality")
                var tracks = executeSearchRequest(urlWithQuality)
                if (!tracks.isNullOrEmpty()) return@runCatching tracks

                // 2. Lowercase quality=lossless
                val urlLossless = appendBaseQuery("$baseUrl/search?q=$encodedQuery&quality=lossless&tier=lossless&atmos=auto")
                Timber.tag(TAG).d("Searching addon (lossless): $urlLossless")
                tracks = executeSearchRequest(urlLossless)
                if (!tracks.isNullOrEmpty()) return@runCatching tracks

                // 3. Fallback search without quality/tier
                val urlWithoutTier = appendBaseQuery("$baseUrl/search?q=$encodedQuery")
                Timber.tag(TAG).d("Searching addon (fallback without tier): $urlWithoutTier")
                tracks = executeSearchRequest(urlWithoutTier)
                if (!tracks.isNullOrEmpty()) return@runCatching tracks

                // 4. Fallback search with query= parameter
                val urlQueryParam = appendBaseQuery("$baseUrl/search?query=$encodedQuery&quality=LOSSLESS")
                Timber.tag(TAG).d("Searching addon (fallback with query param): $urlQueryParam")
                tracks = executeSearchRequest(urlQueryParam)
                tracks ?: emptyList()
            }
        }

    private fun executeSearchRequest(url: String): List<AddonTrack>? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty().trim().removePrefix("\uFEFF")
                parseTracks(body)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d("Search request to $url failed: ${e.message}")
            null
        }
    }

    /**
     * Resolves the stream details for [trackId].
     */
    suspend fun stream(trackId: String, tier: String = "LOSSLESS"): Result<AddonStream> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cleanId = trackId.trim()
                if (cleanId.startsWith("http://", ignoreCase = true) || cleanId.startsWith("https://", ignoreCase = true)) {
                    return@runCatching AddonStream(url = cleanId)
                }

                val encodedId = URLEncoder.encode(cleanId, "UTF-8")

                // 1. Primary stream endpoint: /stream/{id}?quality=LOSSLESS&tier=lossless&atmos=auto
                val url1 = appendBaseQuery("$baseUrl/stream/$encodedId?quality=LOSSLESS&tier=lossless&atmos=auto")
                var stream = executeStreamRequest(url1)
                if (stream != null) return@runCatching stream

                // 2. Lowercase quality stream endpoint: /stream/{id}?quality=lossless
                val url2 = appendBaseQuery("$baseUrl/stream/$encodedId?quality=lossless&tier=lossless&atmos=auto")
                stream = executeStreamRequest(url2)
                if (stream != null) return@runCatching stream

                // 3. Path endpoint without quality/tier: /stream/{id}
                val url3 = appendBaseQuery("$baseUrl/stream/$encodedId")
                stream = executeStreamRequest(url3)
                if (stream != null) return@runCatching stream

                // 4. Query param endpoint: /stream?id={id}&quality=LOSSLESS
                val url4 = appendBaseQuery("$baseUrl/stream?id=$encodedId&quality=LOSSLESS&tier=lossless")
                stream = executeStreamRequest(url4)
                if (stream != null) return@runCatching stream

                // 5. Query param endpoint without quality/tier: /stream?id={id}
                val url5 = appendBaseQuery("$baseUrl/stream?id=$encodedId")
                stream = executeStreamRequest(url5)
                if (stream != null) return@runCatching stream

                throw IllegalStateException("Addon could not resolve stream for track $trackId")
            }
        }

    private fun executeStreamRequest(url: String): AddonStream? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", USER_AGENT)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                if (contentType.startsWith("audio/") || contentType.contains("flac") || contentType.contains("mp4")) {
                    return AddonStream(
                        url = response.request.url.toString(),
                        mimeType = contentType.substringBefore(";"),
                    )
                }

                val body = response.body?.string().orEmpty().trim().removePrefix("\uFEFF")
                if (body.startsWith("{")) {
                    val json = JSONObject(body)
                    // If stream object is nested under "stream" or "data"
                    val streamJson = json.optJSONObject("stream")
                        ?: json.optJSONObject("data")
                        ?: json
                    AddonStream.fromJson(streamJson)
                } else null
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d("Stream request to $url failed: ${e.message}")
            null
        }
    }

    private fun parseTracks(rawJson: String): List<AddonTrack> {
        val tracks = mutableListOf<AddonTrack>()
        if (rawJson.isBlank()) return tracks

        try {
            val tokener = JSONTokener(rawJson)
            val nextVal = tokener.nextValue()

            if (nextVal is JSONObject) {
                val array = nextVal.optJSONArray("tracks")
                    ?: nextVal.optJSONArray("results")
                    ?: nextVal.optJSONArray("data")
                    ?: nextVal.optJSONArray("items")

                if (array != null) {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        AddonTrack.fromJson(obj)?.let { tracks.add(it) }
                    }
                } else {
                    // Check if the root object is a single track
                    AddonTrack.fromJson(nextVal)?.let { tracks.add(it) }
                }
            } else if (nextVal is JSONArray) {
                for (i in 0 until nextVal.length()) {
                    val obj = nextVal.optJSONObject(i) ?: continue
                    AddonTrack.fromJson(obj)?.let { tracks.add(it) }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error parsing addon track JSON: $rawJson")
        }

        return tracks
    }
}





// Refinement 73: AddonClient network specifications

// Refinement 87: OkHttp keep-alive optimization

// Refinement 96: HTTP error response message formatting
