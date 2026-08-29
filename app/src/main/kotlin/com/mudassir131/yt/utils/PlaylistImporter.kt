/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import com.mudassir131.yt.App
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.constants.SpotifyMatchConfidenceThresholdKey
import com.mudassir131.yt.constants.SpotifyUseDataApiMatchingKey
import com.mudassir131.yt.constants.YouTubeDataApiKeyKey
import com.mudassir131.yt.db.MusicDatabase
import com.mudassir131.yt.db.entities.PlaylistEntity
import com.mudassir131.yt.db.entities.PlaylistSongMap
import com.mudassir131.yt.db.entities.SpotifyImportProgressEntity
import com.mudassir131.yt.db.entities.SpotifyImportTrackEntity
import com.mudassir131.yt.db.entities.SpotifyTrackMap
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.models.toMediaMetadata
import com.mudassir131.yt.utils.matching.MatchCandidate
import com.mudassir131.yt.utils.matching.MatchStatus
import com.mudassir131.yt.utils.matching.TrackMatcher
import com.mudassir131.yt.utils.youtube.YouTubeApiKeyException
import com.mudassir131.yt.utils.youtube.YouTubeDataApi
import com.mudassir131.yt.utils.youtube.YouTubeQuotaExceededException
import com.mudassir131.yt.utils.youtube.YouTubeQuotaTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.File
import java.io.Reader
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.random.Random
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

data class SpotifyTrackItem(
    val spotifyTrackId: String,
    val title: String,
    val artist: String = "",
    val album: String = "",
    val sourcePosition: Int = 0,
    /** Milliseconds, as Spotify reports it. Zero when unknown (some no-API strategies). */
    val durationMs: Long = 0,
    val spotifyTrackUri: String = if (spotifyTrackId.isNotEmpty() && !spotifyTrackId.startsWith("sp_")) "spotify:track:$spotifyTrackId" else "",
    val isLocal: Boolean = false
)

data class SpotifyPage(
    val tracks: List<SpotifyTrackItem>,
    val currentUrl: String,
    val nextUrl: String?,
    val totalTracks: Int = 0,
    val skippedTracks: List<Pair<String, String>> = emptyList()
)

/**
 * How a Spotify import turned out. Reported through the optional `onSummary` callback of
 * [PlaylistImporter.importPlaylist] so the plain `Result<String>` contract stays as it was.
 */
data class ImportSummary(
    val playlistName: String,
    val matched: Int,
    val unmatched: Int,
    val quotaNotice: String? = null
)

/**
 * Internal CSV serializer and parser adhering to the Exportify Spotify schema.
 * Represents the intermediate dataset between Spotify extraction and YouTube matching.
 */
object SpotifyCsvSerializer {
    const val CSV_HEADER = "Spotify Track ID,Spotify Track URI,Track Name,Artist Name(s),Album Name,Duration (ms),Source Position,Is Local"

    fun exportToCsv(tracks: List<SpotifyTrackItem>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER).append("\n")
        for (track in tracks) {
            sb.append(escapeCsv(track.spotifyTrackId)).append(",")
            sb.append(escapeCsv(track.spotifyTrackUri)).append(",")
            sb.append(escapeCsv(track.title)).append(",")
            sb.append(escapeCsv(track.artist)).append(",")
            sb.append(escapeCsv(track.album)).append(",")
            sb.append(track.durationMs).append(",")
            sb.append(track.sourcePosition).append(",")
            sb.append(track.isLocal)
            sb.append("\n")
        }
        return sb.toString()
    }

    fun parseFromCsv(csvContent: String): List<SpotifyTrackItem> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val dataLines = if (lines.first().startsWith("Spotify Track ID")) lines.drop(1) else lines
        val result = mutableListOf<SpotifyTrackItem>()

        for ((index, line) in dataLines.withIndex()) {
            val tokens = parseCsvLine(line)
            if (tokens.isEmpty()) continue
            val trackId = tokens.getOrNull(0) ?: ""
            val trackUri = tokens.getOrNull(1) ?: ""
            val title = tokens.getOrNull(2) ?: ""
            val artist = tokens.getOrNull(3) ?: ""
            val album = tokens.getOrNull(4) ?: ""
            val durationMs = tokens.getOrNull(5)?.toLongOrNull() ?: 0L
            val sourcePos = tokens.getOrNull(6)?.toIntOrNull() ?: index
            val isLocal = tokens.getOrNull(7)?.toBooleanStrictOrNull() ?: false

            if (title.isNotBlank()) {
                val finalId = if (trackId.isNotBlank()) trackId else "sp_${(title + artist).hashCode()}"
                result.add(
                    SpotifyTrackItem(
                        spotifyTrackId = finalId,
                        title = title,
                        artist = artist,
                        album = album,
                        sourcePosition = sourcePos,
                        durationMs = durationMs,
                        spotifyTrackUri = trackUri.takeIf { it.isNotBlank() } ?: "spotify:track:$finalId",
                        isLocal = isLocal
                    )
                )
            }
        }
        return result
    }

    private fun escapeCsv(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '\"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                    sb.append('\"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
            i++
        }
        tokens.add(sb.toString().trim())
        return tokens
    }
}

private class SpotifyAuthException(message: String) : Exception(message)
private class SpotifyRateLimitException(message: String) : Exception(message)

/**
 * Imports playlists from YouTube, Spotify and Apple Music into Nocturne's own database.
 *
 * ## The Spotify endpoints here are unofficial and undocumented
 *
 * `https://open.spotify.com/get_access_token` is an internal endpoint of Spotify's web player. It is
 * not part of the documented Web API, carries no compatibility guarantee, and **may change or
 * disappear without notice** — as may the shape of the `__NEXT_DATA__` / `<script id="session">`
 * blobs and the embed page that [extractAccessToken] and the no-API strategies scrape. That is
 * precisely why the token acquisition has three independent methods and extraction has several
 * fallback strategies: any one of them breaking should degrade the import, not end it.
 *
 * Everything read from `api.spotify.com` here is public playlist metadata reached with an anonymous
 * token. Nothing is written back to Spotify, and no user login is involved.
 *
 * ## Nothing is written to the user's YouTube account
 *
 * Matched tracks are stored only in Nocturne's own tables. No OAuth, no Google sign-in, and no
 * YouTube playlist-write endpoint is called from anywhere in this file or in [YouTubeDataApi] —
 * matching uses `search.list` / `videos.list`, both read-only and API-key-only. A unit test greps
 * both files to keep it that way.
 */
object PlaylistImporter {

    const val MAX_IMPORT_SONGS = 5000
    const val SPOTIFY_MAX_IMPORT_SONGS = 3000

    /** Politeness window between Spotify pagination calls, jittered inside this range. */
    private const val SPOTIFY_PAGE_DELAY_MIN_MS = 300L
    private const val SPOTIFY_PAGE_DELAY_MAX_MS = 500L

    /**
     * Anonymous Spotify tokens last about an hour. Refreshing a little before that keeps a long
     * import (up to 30 pages) from stalling on a 401 mid-run; the reactive 401 refresh remains as
     * the backstop.
     */
    private const val SPOTIFY_TOKEN_MAX_AGE_MS = 55L * 60L * 1000L

    /** Backoff for a Spotify 429, replacing what used to be a flat 3s wait. */
    private const val SPOTIFY_RETRY_INITIAL_MS = 1_000L
    private const val SPOTIFY_RETRY_MAX_MS = 30_000L
    private const val SPOTIFY_MAX_RATE_LIMIT_RETRIES = 5

    /** Candidates ranked per track, on either matching path. */
    private const val MATCH_CANDIDATE_COUNT = 5

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(object : CookieJar {
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val hostCookies = cookieStore.getOrPut(url.host) { mutableListOf() }
                    synchronized(hostCookies) {
                        for (c in cookies) {
                            hostCookies.removeAll { it.name == c.name }
                            hostCookies.add(c)
                        }
                    }
                }

                override fun loadForRequest(url: HttpUrl): List<Cookie> {
                    val hostCookies = cookieStore[url.host] ?: return emptyList()
                    return synchronized(hostCookies) { hostCookies.toList() }
                }
            })
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    internal suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://open.spotify.com/")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP Error: ${response.code}")
            return@withContext response.body?.string() ?: ""
        }
    }

    private suspend fun resolveRedirect(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .build()
            httpClient.newCall(request).execute().use { response ->
                return@withContext response.request.url.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext url
        }
    }

    private fun extractAccessToken(html: String): String? {
        try {
            val sessionMatch = Pattern.compile("<script id=\"session\"[^>]*>([\\s\\S]*?)</script>").matcher(html)
            if (sessionMatch.find()) {
                val sessionJson = sessionMatch.group(1)?.trim() ?: ""
                val token = JSONObject(sessionJson).optString("accessToken")
                if (token.isNotEmpty()) return token
            }
        } catch (_: Exception) {}

        try {
            val nextDataMatch = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>([\\s\\S]*?)</script>").matcher(html)
            if (nextDataMatch.find()) {
                val json = JSONObject(nextDataMatch.group(1)?.trim() ?: "")
                val props = json.optJSONObject("props")?.optJSONObject("pageProps")
                val token = props?.optString("accessToken")
                    ?: props?.optJSONObject("state")?.optJSONObject("data")?.optString("accessToken")
                    ?: props?.optJSONObject("session")?.optString("accessToken")
                if (!token.isNullOrEmpty()) return token
            }
        } catch (_: Exception) {}

        val patterns = listOf(
            Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\""),
            Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\""),
            Pattern.compile("accessToken\\s*=\\s*\"([^\"]+)\"")
        )
        for (pattern in patterns) {
            val matcher = pattern.matcher(html)
            if (matcher.find()) {
                val token = matcher.group(1)
                if (!token.isNullOrEmpty() && token.length > 20) {
                    return token
                }
            }
        }
        return null
    }

    internal suspend fun fetchSpotifyTokenViaWebView(playlistId: String): String? = withContext(Dispatchers.Main) {
        val context = try {
            App.instance
        } catch (_: Throwable) {
            null
        } ?: return@withContext null

        try {
            withTimeoutOrNull(12_000L) {
                suspendCancellableCoroutine<String?> { continuation ->
                    try {
                        val webView = WebView(context)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
                        webView.settings.javaScriptEnabled = true
                        webView.settings.domStorageEnabled = true
                        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

                        class TokenInterface {
                            @JavascriptInterface
                            fun onToken(token: String) {
                                if (token.isNotBlank() && token.length > 20 && continuation.isActive) {
                                    Timber.tag("SpotifyImport").d("Spotify access token received from WebView bridge")
                                    continuation.resume(token)
                                }
                            }
                        }

                        webView.addJavascriptInterface(TokenInterface(), "SpotifyTokenBridge")

                        val jsTokenFetch = """
                            (async function() {
                                try {
                                    let resp = await fetch('https://open.spotify.com/get_access_token?reason=transport&productType=web_player');
                                    let data = await resp.json();
                                    if (data && data.accessToken) {
                                        SpotifyTokenBridge.onToken(data.accessToken);
                                        return;
                                    }
                                } catch(e) {}
                                try {
                                    let resp = await fetch('https://open.spotify.com/get_access_token?reason=transport&productType=web-player');
                                    let data = await resp.json();
                                    if (data && data.accessToken) {
                                        SpotifyTokenBridge.onToken(data.accessToken);
                                        return;
                                    }
                                } catch(e) {}
                                try {
                                    let session = document.getElementById('session') || document.getElementById('config');
                                    if (session) {
                                        let json = JSON.parse(session.textContent || '{}');
                                        if (json.accessToken) {
                                            SpotifyTokenBridge.onToken(json.accessToken);
                                            return;
                                        }
                                    }
                                } catch(e) {}
                                try {
                                    if (window.__spotify && window.__spotify.accessToken) {
                                        SpotifyTokenBridge.onToken(window.__spotify.accessToken);
                                        return;
                                    }
                                } catch(e) {}
                            })();
                        """.trimIndent()

                        webView.webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                webView.evaluateJavascript(jsTokenFetch, null)
                            }
                        }

                        webView.loadUrl("https://open.spotify.com/embed/playlist/$playlistId")

                        continuation.invokeOnCancellation {
                            webView.destroy()
                        }
                    } catch (e: Exception) {
                        Timber.tag("SpotifyImport").w(e, "WebView token acquisition failed: ${e.message}")
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "fetchSpotifyTokenViaWebView error: ${e.message}")
            null
        }
    }

    internal suspend fun getSpotifyAccessToken(playlistId: String): String? = withContext(Dispatchers.IO) {
        // Step 1: Pre-warm session with open.spotify.com to establish cookies/session
        try {
            fetchHtml("https://open.spotify.com/playlist/$playlistId")
        } catch (_: Exception) {}

        // Method A: get_access_token endpoint (web_player)
        try {
            val tokenReq = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://open.spotify.com/playlist/$playlistId")
                .header("Origin", "https://open.spotify.com")
                .header("App-Platform", "WebPlayer")
                .header("Spotify-App-Version", "1.2.34.0")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .build()
            httpClient.newCall(tokenReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val tokenJson = JSONObject(body)
                    val token = tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                    if (token != null) {
                        Timber.tag("SpotifyImport").d("Obtained Spotify access token via web_player endpoint")
                        return@withContext token
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Method A token fetch failed: ${e.message}")
        }

        // Method A1: get_access_token endpoint (web-player hyphenated)
        try {
            val tokenReq = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web-player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://open.spotify.com/playlist/$playlistId")
                .header("Origin", "https://open.spotify.com")
                .header("App-Platform", "WebPlayer")
                .build()
            httpClient.newCall(tokenReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val tokenJson = JSONObject(body)
                    val token = tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                    if (token != null) {
                        Timber.tag("SpotifyImport").d("Obtained Spotify access token via hyphenated web-player endpoint")
                        return@withContext token
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Method A1 token fetch failed: ${e.message}")
        }

        // Method A2: get_access_token without productType
        try {
            val tokenReq = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://open.spotify.com/")
                .header("Origin", "https://open.spotify.com")
                .header("App-Platform", "WebPlayer")
                .build()
            httpClient.newCall(tokenReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val tokenJson = JSONObject(body)
                    val token = tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                    if (token != null) {
                        Timber.tag("SpotifyImport").d("Obtained Spotify access token via transport endpoint")
                        return@withContext token
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Method A2 token fetch failed: ${e.message}")
        }

        // Method B: Embed HTML token extraction
        try {
            val embedHtml = fetchHtml("https://open.spotify.com/embed/playlist/$playlistId")
            val token = extractAccessToken(embedHtml)
            if (token != null) {
                Timber.tag("SpotifyImport").d("Obtained Spotify access token via embed HTML")
                return@withContext token
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Method B token fetch failed: ${e.message}")
        }

        // Method C: Webpage HTML token extraction
        try {
            val pageHtml = fetchHtml("https://open.spotify.com/playlist/$playlistId")
            val token = extractAccessToken(pageHtml)
            if (token != null) {
                Timber.tag("SpotifyImport").d("Obtained Spotify access token via playlist page HTML")
                return@withContext token
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Method C token fetch failed: ${e.message}")
        }

        // Method E: Spotify Client Credentials Token
        val clientCredentials = listOf(
            "NDY4OTRhZGQ4YmE1NDhhZWE4OWU4YWQ1Y2MyNzk5ZjM6MmExM2RjNGE4NzU1NDI1N2ExYmZmOTY2ODgyOWM1OTU=", // Web
            "Mjc5ODgzZDcyOTZlNDFmYjk5ZTMzOTdhYmI0YmQwMGY6", // Android
            "ZDhhNWRlMzI3MWU2NGYxMDg3Zjg3Yjc0ZDMyOTQzZjI6"  // Desktop
        )
        for (basicAuth in clientCredentials) {
            try {
                val formBody = okhttp3.FormBody.Builder()
                    .add("grant_type", "client_credentials")
                    .build()
                val tokenReq = Request.Builder()
                    .url("https://accounts.spotify.com/api/token")
                    .header("Authorization", "Basic $basicAuth")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .post(formBody)
                    .build()
                httpClient.newCall(tokenReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val tokenJson = JSONObject(body)
                        val token = tokenJson.optString("access_token").takeIf { it.isNotEmpty() }
                            ?: tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                        if (token != null) {
                            Timber.tag("SpotifyImport").d("Obtained Spotify access token via client_credentials")
                            return@withContext token
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.tag("SpotifyImport").w(e, "Method E token fetch failed: ${e.message}")
            }
        }

        null
    }

    internal suspend fun fetchSpotifyPage(
        url: String,
        accessToken: String,
        currentOffset: Int,
        remainingLimit: Int
    ): Result<SpotifyPage> = withContext(Dispatchers.IO) {
        try {
            Timber.tag("SpotifyImport").d("Requesting Spotify page: URL=$url")
            val tracksReq = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Origin", "https://open.spotify.com")
                .header("Referer", "https://open.spotify.com/")
                .header("App-Platform", "WebPlayer")
                .header("Spotify-App-Version", "1.2.34.0")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .build()

            httpClient.newCall(tracksReq).execute().use { resp ->
                Timber.tag("SpotifyImport").d("Spotify page HTTP response: ${resp.code}")
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext Result.failure(SpotifyAuthException("Spotify token expired or unauthorized (${resp.code})"))
                }
                if (resp.code == 429) {
                    return@withContext Result.failure(SpotifyRateLimitException("Spotify rate limit (429)"))
                }
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${resp.code} fetching Spotify page"))
                }

                val body = resp.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val tracksJson = JSONObject(body)
                val items = tracksJson.optJSONArray("items") ?: tracksJson.optJSONArray("tracks")
                val total = tracksJson.optInt("total", 0)
                val nextRaw = tracksJson.optString("next").takeIf { !it.isNullOrEmpty() && it != "null" }

                val pageTracks = mutableListOf<SpotifyTrackItem>()
                val pageSkipped = mutableListOf<Pair<String, String>>()
                if (items != null) {
                    Timber.tag("SpotifyImport").d("Spotify page items count: ${items.length()} (total in playlist: $total)")
                    for (i in 0 until items.length()) {
                        if (pageTracks.size >= remainingLimit) break
                        val itemObj = items.optJSONObject(i)
                        if (itemObj == null) {
                            val skipId = "index_${currentOffset + i}"
                            val reason = "Null item object in Spotify items array"
                            pageSkipped.add(skipId to reason)
                            Timber.tag("SpotifyImport").w("Spotify skipped track: id=$skipId reason='$reason'")
                            continue
                        }
                        val isLocal = itemObj.optBoolean("is_local", false)
                        val trackObj = itemObj.optJSONObject("track") ?: itemObj.optJSONObject("item")
                        if (trackObj == null) {
                            val skipId = "index_${currentOffset + i}"
                            val reason = "Null track object (removed, unavailable or restricted on Spotify)"
                            pageSkipped.add(skipId to reason)
                            Timber.tag("SpotifyImport").w("Spotify skipped track: id=$skipId reason='$reason'")
                            continue
                        }
                        val id = trackObj.optString("id", "").trim()
                        val title = trackObj.optString("name", "").trim()
                        val albumObj = trackObj.optJSONObject("album")
                        val albumName = albumObj?.optString("name", "") ?: ""
                        val durationMs = trackObj.optLong("duration_ms", 0L).coerceAtLeast(0L)
                        val artistsArr = trackObj.optJSONArray("artists")
                        val artist = if (artistsArr != null && artistsArr.length() > 0) {
                            (0 until artistsArr.length()).mapNotNull { idx ->
                                artistsArr.optJSONObject(idx)?.optString("name")?.trim()?.takeIf { it.isNotEmpty() }
                            }.joinToString(", ")
                        } else {
                            ""
                        }
                        val uri = trackObj.optString("uri", if (id.isNotEmpty()) "spotify:track:$id" else "").trim()
                        if (title.isEmpty()) {
                            val skipId = if (id.isNotEmpty()) id else "index_${currentOffset + i}"
                            val reason = "Empty track title/name"
                            pageSkipped.add(skipId to reason)
                            Timber.tag("SpotifyImport").w("Spotify skipped track: id=$skipId reason='$reason'")
                            continue
                        }
                        val trackId = if (id.isNotEmpty()) id else if (isLocal) "sp_local_${(title + artist).hashCode()}" else "sp_${(title + artist).hashCode()}"
                        val position = currentOffset + pageTracks.size
                        pageTracks.add(
                            SpotifyTrackItem(
                                spotifyTrackId = trackId,
                                spotifyTrackUri = uri,
                                title = title,
                                artist = artist,
                                album = albumName,
                                sourcePosition = position,
                                durationMs = durationMs,
                                isLocal = isLocal
                            )
                        )
                        Timber.tag("SpotifyImport").d("SpotifyTrack: id=$trackId uri=$uri title='$title' artist='$artist' pos=$position durationMs=$durationMs")
                    }
                }

                Timber.tag("SpotifyImport").d("Spotify parsed valid tracks: ${pageTracks.size}, skipped: ${pageSkipped.size}, nextUrl: $nextRaw")
                Result.success(
                    SpotifyPage(
                        tracks = pageTracks,
                        currentUrl = url,
                        nextUrl = nextRaw,
                        totalTracks = total,
                        skippedTracks = pageSkipped
                    )
                )
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").e(e, "Exception fetching Spotify page: ${e.message}")
            Result.failure(e)
        }
    }

    // ==========================================
    // SPOTIFY → YOUTUBE MATCHING
    // ==========================================

    /**
     * Per-run matching settings, read once so 3000 tracks don't each hit DataStore.
     *
     * [apiKey] is the user's key from Settings, falling back to the build default. It is used only
     * for read-only `search.list` / `videos.list` calls.
     */
    internal data class SpotifyMatchConfig(
        val apiKey: String,
        val useDataApi: Boolean,
        val threshold: Double
    ) {
        val dataApiUsable: Boolean get() = useDataApi && apiKey.isNotBlank()

        companion object {
            val Disabled = SpotifyMatchConfig("", false, TrackMatcher.CONFIDENCE_THRESHOLD)

            suspend fun load(): SpotifyMatchConfig = try {
                val store = App.instance.dataStore
                val userKey = store.getAsync(YouTubeDataApiKeyKey, "").trim()
                SpotifyMatchConfig(
                    apiKey = userKey.ifEmpty { BuildConfig.YOUTUBE_DATA_API_KEY.trim() },
                    useDataApi = store.getAsync(SpotifyUseDataApiMatchingKey, true),
                    threshold = store
                        .getAsync(SpotifyMatchConfidenceThresholdKey, TrackMatcher.CONFIDENCE_THRESHOLD.toFloat())
                        .toDouble()
                        .coerceIn(0.0, 1.0)
                )
            } catch (e: Exception) {
                Timber.tag("SpotifyImport").w(e, "Could not read match settings; using InnerTube only")
                Disabled
            }
        }
    }

    private data class TrackMatchOutcome(
        val metadata: com.mudassir131.yt.models.MediaMetadata?,
        val songId: String?,
        val confidence: Double,
        val source: String,
        val matched: Boolean
    ) {
        companion object {
            fun unmatched(confidence: Double = 0.0) = TrackMatchOutcome(
                metadata = null,
                songId = null,
                confidence = confidence,
                source = SpotifyImportTrackEntity.SOURCE_NONE,
                matched = false
            )
        }
    }

    private fun SongItem.toCandidate() = MatchCandidate(
        videoId = id,
        title = title,
        channelTitle = artists.joinToString(", ") { it.name },
        durationSec = duration,
        thumbnailUrl = thumbnail,
        albumName = album?.name,
        artistNames = artists.map { it.name }
    )

    /**
     * Finds the best YouTube video for a Spotify track, or reports it unmatched.
     *
     * Order of preference:
     *  1. A previous match for this Spotify ID, reused from `spotify_track_map`.
     *  2. YouTube Data API v3 — five `search.list` candidates ranked on title plus a batched
     *     `videos.list` duration check — while a key is configured and quota remains.
     *  3. InnerTube search, ranked by the *same* [TrackMatcher] policy. Keyless and unmetered, so
     *     this is what carries a 3000-track import once the day's 10,000 units are gone.
     *
     * Nothing that fails to clear the confidence threshold is returned as a match: a track is stored
     * as UNMATCHED for review rather than pointed at a plausible-looking wrong video.
     *
     * [dataApiExhausted] latches for the whole run once the quota or the key is spent, so the
     * remaining tracks skip straight to InnerTube instead of re-failing a call each time.
     */
    private suspend fun matchTrack(
        track: SpotifyTrackItem,
        database: MusicDatabase,
        config: SpotifyMatchConfig,
        dataApiExhausted: AtomicBoolean
    ): TrackMatchOutcome = withContext(Dispatchers.IO) {
        val title = track.title
        val artist = track.artist
        val spotifyId = track.spotifyTrackId
        val query = if (artist.isBlank()) title else "$title - $artist"

        // 1. Reuse an earlier match for this exact Spotify track.
        if (spotifyId.isNotEmpty()) {
            val existing = database.getSpotifyTrack(spotifyId)
            if (existing != null) {
                Timber.tag("SpotifyImport").d("SpotifyResolution CACHE spotifyId=$spotifyId mediaId=${existing.songId}")
                return@withContext TrackMatchOutcome(
                    metadata = null,
                    songId = existing.songId,
                    confidence = existing.matchConfidence.toDouble(),
                    source = SpotifyImportTrackEntity.SOURCE_CACHE,
                    matched = true
                )
            }
        }

        var bestRejectedConfidence = 0.0

        // 2. Canonical YouTube Music search via InnerTube FILTER_SONG (pristine audio tracks).
        for (filter in listOf(YouTube.SearchFilter.FILTER_SONG, YouTube.SearchFilter.FILTER_VIDEO)) {
            val candidates = try {
                YouTube.search(query, filter).getOrNull()
                    ?.items
                    ?.filterIsInstance<SongItem>()
                    ?.take(MATCH_CANDIDATE_COUNT)
                    .orEmpty()
            } catch (e: Exception) {
                Timber.tag("SpotifyImport").w(e, "InnerTube search failed for '$query': ${e.message}")
                emptyList()
            }
            if (candidates.isEmpty()) continue

            val result = TrackMatcher.pickBest(
                trackName = title,
                artist = artist,
                spotifyDurationMs = track.durationMs,
                candidates = candidates.map { it.toCandidate() },
                threshold = config.threshold
            )
            bestRejectedConfidence = maxOf(bestRejectedConfidence, result.confidence)

            if (result.status == MatchStatus.MATCHED) {
                val chosenId = result.candidate?.videoId
                val songItem = candidates.firstOrNull { it.id == chosenId }
                if (songItem != null) {
                    Timber.tag("SpotifyImport").d(
                        "SpotifyResolution INNERTUBE spotifyId=%s mediaId=%s confidence=%.2f durationDelta=%s",
                        spotifyId, songItem.id, result.confidence, result.score?.durationDeltaSec?.toString() ?: "?"
                    )
                    return@withContext TrackMatchOutcome(
                        metadata = songItem.toMediaMetadata(),
                        songId = songItem.id,
                        confidence = result.confidence,
                        source = SpotifyImportTrackEntity.SOURCE_INNERTUBE,
                        matched = true
                    )
                }
            }
        }

        // 3. YouTube Data API v3 fallback, while a key is set and quota remains.
        if (config.dataApiUsable && !dataApiExhausted.get()) {
            val dataApiResult = matchViaDataApi(track, query, config, dataApiExhausted)
            if (dataApiResult != null) {
                if (dataApiResult.matched) return@withContext dataApiResult
                bestRejectedConfidence = maxOf(bestRejectedConfidence, dataApiResult.confidence)
            }
        }

        Timber.tag("SpotifyImport").w(
            "SpotifyResolution UNMATCHED spotifyId=%s title='%s' artist='%s' bestConfidence=%.2f",
            spotifyId, title, artist, bestRejectedConfidence
        )
        TrackMatchOutcome.unmatched(bestRejectedConfidence)
    }

    /**
     * One Data API attempt for a track. Returns null when the path is unavailable (no quota, key
     * rejected, transport error), an unmatched outcome when candidates were found but none was good
     * enough, or a matched outcome resolved to real InnerTube metadata.
     *
     * The chosen video ID is resolved back through [YouTube.queue] because a playable song row needs
     * the InnerTube metadata (artist IDs, album, thumbnails) that a `search.list` snippet lacks.
     */
    private suspend fun matchViaDataApi(
        track: SpotifyTrackItem,
        query: String,
        config: SpotifyMatchConfig,
        dataApiExhausted: AtomicBoolean
    ): TrackMatchOutcome? {
        val context = App.instance

        // Claim the units up front so concurrent chunks can't jointly overspend the day's budget.
        val reserved = try {
            YouTubeQuotaTracker.reserve(context, YouTubeQuotaTracker.UNITS_PER_TRACK)
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Quota ledger unavailable; using InnerTube only")
            false
        }
        if (!reserved) {
            if (dataApiExhausted.compareAndSet(false, true)) {
                Timber.tag("SpotifyImport").w("Data API quota spent for today — matching continues via InnerTube")
            }
            return null
        }

        val searchResult = YouTubeDataApi.search(query, config.apiKey, MATCH_CANDIDATE_COUNT)
        val searchError = searchResult.exceptionOrNull()
        if (searchError != null) {
            if (searchError is YouTubeQuotaExceededException || searchError is YouTubeApiKeyException) {
                if (dataApiExhausted.compareAndSet(false, true)) {
                    Timber.tag("SpotifyImport").w("Data API unavailable (%s) — matching continues via InnerTube", searchError.message)
                }
            } else {
                Timber.tag("SpotifyImport").w(searchError, "Data API search failed for '$query'")
            }
            return null
        }

        val candidates = searchResult.getOrDefault(emptyList())
        if (candidates.isEmpty()) return null

        // Duration verification: one batched videos.list call for all five candidates, 1 unit.
        val durations = YouTubeDataApi
            .fetchDurations(candidates.map { it.videoId }, config.apiKey)
            .getOrDefault(emptyMap())

        val result = TrackMatcher.pickBest(
            trackName = track.title,
            artist = track.artist,
            spotifyDurationMs = track.durationMs,
            candidates = candidates.map { it.copy(durationSec = durations[it.videoId]) },
            threshold = config.threshold
        )

        val videoId = result.candidate?.videoId
        if (result.status != MatchStatus.MATCHED || videoId == null) {
            return TrackMatchOutcome.unmatched(result.confidence)
        }

        val songItem = try {
            YouTube.queue(listOf(videoId)).getOrNull()?.firstOrNull()
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Could not resolve metadata for $videoId")
            null
        }
        if (songItem == null) {
            // Fall through to the InnerTube path, which produces its own metadata.
            Timber.tag("SpotifyImport").d("Data API picked $videoId but metadata lookup failed; retrying via InnerTube")
            return null
        }

        Timber.tag("SpotifyImport").d(
            "SpotifyResolution DATA_API spotifyId=%s mediaId=%s confidence=%.2f durationDelta=%s",
            track.spotifyTrackId, songItem.id, result.confidence,
            result.score?.durationDeltaSec?.toString() ?: "?"
        )
        return TrackMatchOutcome(
            metadata = songItem.toMediaMetadata(),
            songId = songItem.id,
            confidence = result.confidence,
            source = SpotifyImportTrackEntity.SOURCE_DATA_API,
            matched = true
        )
    }

    /** The per-track review record, written for matched and unmatched tracks alike. */
    private fun importTrackRow(
        playlistId: String,
        track: SpotifyTrackItem,
        outcome: TrackMatchOutcome
    ) = SpotifyImportTrackEntity(
        playlistId = playlistId,
        spotifyTrackId = track.spotifyTrackId,
        trackName = track.title,
        artist = track.artist,
        album = track.album,
        durationMs = track.durationMs,
        position = track.sourcePosition,
        youtubeVideoId = outcome.songId.takeIf { outcome.matched },
        matchConfidence = outcome.confidence.toFloat(),
        matchStatus = if (outcome.matched) {
            SpotifyImportTrackEntity.STATUS_MATCHED
        } else {
            SpotifyImportTrackEntity.STATUS_UNMATCHED
        },
        matchSource = outcome.source
    )

    // ==========================================
    // NO-API EXTRACTION STRATEGIES
    // ==========================================

    // Strategy A: Public Embed Page
    internal suspend fun fetchSpotifyNoApiEmbed(playlistId: String): Pair<String, List<SpotifyTrackItem>> = withContext(Dispatchers.IO) {
        var playlistName = "Imported Spotify Playlist"
        val tracks = mutableListOf<SpotifyTrackItem>()
        val knownIds = HashSet<String>()

        try {
            Timber.tag("SpotifyImport").d("SpotifyImport NO_API strategy=EMBED playlist=$playlistId")
            val html = fetchHtml("https://open.spotify.com/embed/playlist/$playlistId")
            val doc = Jsoup.parse(html)
            val nextDataScript = doc.getElementById("__NEXT_DATA__")
            val jsonText = nextDataScript?.data()?.trim() ?: ""

            if (jsonText.isNotEmpty()) {
                val root = JSONObject(jsonText)
                val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
                val pageState = pageProps?.optJSONObject("state")
                val entity = pageState?.optJSONObject("data")?.optJSONObject("entity")
                if (entity != null) {
                    playlistName = entity.optString("name", playlistName).takeIf { it.isNotEmpty() } ?: playlistName
                    val trackList = entity.optJSONArray("trackList")
                    if (trackList != null) {
                        for (i in 0 until trackList.length()) {
                            if (tracks.size >= SPOTIFY_MAX_IMPORT_SONGS) break
                            val trackObj = trackList.getJSONObject(i)
                            val id = trackObj.optString("id", "").trim()
                            val title = trackObj.optString("title", "").trim()
                            val subtitle = trackObj.optString("subtitle", "").trim()
                            val trackId = if (id.isNotEmpty()) id else "sp_${(title + subtitle).hashCode()}"
                            if (title.isNotEmpty() && knownIds.add(trackId)) {
                                tracks.add(
                                    SpotifyTrackItem(
                                        spotifyTrackId = trackId,
                                        title = title,
                                        artist = subtitle,
                                        sourcePosition = tracks.size
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Strategy A (Embed) failed: ${e.message}")
        }

        Pair(playlistName, tracks)
    }

    // Strategy B: Public Playlist Web Page (HTML, JSON-LD, schema.org)
    internal suspend fun fetchSpotifyNoApiPublicHtml(playlistId: String): Pair<String, List<SpotifyTrackItem>> = withContext(Dispatchers.IO) {
        var playlistName = "Imported Spotify Playlist"
        val tracks = mutableListOf<SpotifyTrackItem>()
        val knownIds = HashSet<String>()

        try {
            Timber.tag("SpotifyImport").d("SpotifyImport NO_API strategy=PUBLIC_HTML playlist=$playlistId")
            val html = fetchHtml("https://open.spotify.com/playlist/$playlistId")
            val doc = Jsoup.parse(html)
            val titleText = doc.title().replace(" | Spotify", "").replace(" - playlist by.*".toRegex(), "").trim()
            if (titleText.isNotEmpty() && !titleText.contains("Spotify")) {
                playlistName = titleText
            }

            // 1. Schema.org / ld+json
            val scripts = doc.select("script[type=application/ld+json]")
            for (script in scripts) {
                val jsonText = script.data().trim()
                if (jsonText.isEmpty()) continue
                try {
                    val root = JSONObject(jsonText)
                    val type = root.optString("@type")
                    if (type == "MusicPlaylist" || root.has("track")) {
                        playlistName = root.optString("name", playlistName)
                        val trackArray = root.optJSONArray("track")
                        if (trackArray != null) {
                            for (i in 0 until trackArray.length()) {
                                if (tracks.size >= SPOTIFY_MAX_IMPORT_SONGS) break
                                val trackObj = trackArray.getJSONObject(i)
                                val name = trackObj.optString("name", "").trim()
                                val byArtist = trackObj.optJSONObject("byArtist")?.optString("name", "")?.trim() ?: ""
                                val trackUrl = trackObj.optString("url", "").trim()
                                val id = trackUrl.substringAfterLast("/").substringBefore("?")
                                val trackId = if (id.isNotEmpty()) id else "sp_${(name + byArtist).hashCode()}"
                                if (name.isNotEmpty() && knownIds.add(trackId)) {
                                    tracks.add(
                                        SpotifyTrackItem(
                                            spotifyTrackId = trackId,
                                            title = name,
                                            artist = byArtist,
                                            sourcePosition = tracks.size
                                        )
                                    )
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // 2. Meta tags or HTML links fallback
            if (tracks.size < 50) {
                val trackAnchors = doc.select("a[href*=/track/]")
                for (a in trackAnchors) {
                    if (tracks.size >= SPOTIFY_MAX_IMPORT_SONGS) break
                    val href = a.attr("href")
                    val id = href.substringAfter("/track/").substringBefore("?").substringBefore("/")
                    val text = a.text().trim()
                    if (id.isNotEmpty() && text.isNotEmpty() && knownIds.add(id)) {
                        tracks.add(
                            SpotifyTrackItem(
                                spotifyTrackId = id,
                                title = text,
                                artist = "",
                                sourcePosition = tracks.size
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Strategy B (Public HTML) failed: ${e.message}")
        }

        Pair(playlistName, tracks)
    }

    // Strategy C: Android WebView Progressive DOM Extraction (with scrolling)
    internal suspend fun fetchSpotifyNoApiWebView(playlistId: String): Pair<String, List<SpotifyTrackItem>> = withContext(Dispatchers.Main) {
        val tracks = mutableListOf<SpotifyTrackItem>()
        var playlistName = "Imported Spotify Playlist"

        val context = try {
            App.instance
        } catch (_: Throwable) {
            null
        }

        if (context == null) {
            Timber.tag("SpotifyImport").w("WebView extraction skipped: Application context not available in current environment")
            return@withContext Pair(playlistName, tracks)
        }

        suspendCancellableCoroutine { continuation ->
            try {
                Timber.tag("SpotifyImport").d("SpotifyImport NO_API strategy=WEBVIEW playlist=$playlistId")
                val webView = WebView(context)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

                val knownIds = HashSet<String>()
                val handler = Handler(Looper.getMainLooper())
                var scrollCount = 0
                var stagnantScrolls = 0
                val maxScrolls = 120

                class SpotifyJsInterface {
                    @JavascriptInterface
                    fun onTracksExtracted(jsonStr: String, title: String) {
                        try {
                            if (title.isNotEmpty() && !title.contains("Spotify")) {
                                playlistName = title
                            }
                            val arr = JSONArray(jsonStr)
                            var newlyDiscovered = 0
                            for (i in 0 until arr.length()) {
                                if (tracks.size >= SPOTIFY_MAX_IMPORT_SONGS) break
                                val obj = arr.getJSONObject(i)
                                val id = obj.optString("id", "").trim()
                                val t = obj.optString("title", "").trim()
                                val a = obj.optString("artist", "").trim()
                                val trackId = if (id.isNotEmpty()) id else "sp_${(t + a).hashCode()}"
                                if (t.isNotEmpty() && knownIds.add(trackId)) {
                                    tracks.add(
                                        SpotifyTrackItem(
                                            spotifyTrackId = trackId,
                                            title = t,
                                            artist = a,
                                            sourcePosition = tracks.size
                                        )
                                    )
                                    newlyDiscovered++
                                }
                            }
                            if (newlyDiscovered > 0) {
                                stagnantScrolls = 0
                                Timber.tag("SpotifyImport").d("SpotifyImport NO_API discovered=${tracks.size}")
                            } else {
                                stagnantScrolls++
                            }
                        } catch (e: Exception) {
                            Timber.tag("SpotifyImport").e(e, "Error parsing JS extracted tracks: ${e.message}")
                        }
                    }
                }

                webView.addJavascriptInterface(SpotifyJsInterface(), "SpotifyExtractor")

                val jsExtract = """
                    (function() {
                        try {
                            document.querySelectorAll('#onetrust-banner-sdk, #onetrust-consent-sdk, div[data-testid="cookie-banner"], div[aria-label="Cookie Banner"]').forEach(function(e) { e.remove(); });
                        } catch(e) {}
                        var results = [];
                        var seen = {};
                        var rows = document.querySelectorAll('div[data-testid="tracklist-row"], div[role="row"]');
                        if (rows.length > 0) {
                            rows.forEach(function(row) {
                                var link = row.querySelector('a[href*="/track/"]');
                                var id = '';
                                if (link) {
                                    var href = link.getAttribute('href') || '';
                                    var match = href.match(/\/track\/([a-zA-Z0-9]+)/);
                                    if (match && match[1]) id = match[1];
                                }
                                var titleElem = row.querySelector('div[dir="auto"], a[href*="/track/"]');
                                var title = titleElem ? titleElem.textContent.trim() : '';
                                var artistElem = row.querySelector('a[href*="/artist/"]');
                                var artist = artistElem ? artistElem.textContent.trim() : '';
                                var trackKey = id || (title + artist);
                                if (title && !seen[trackKey]) {
                                    seen[trackKey] = true;
                                    results.push({id: id, title: title, artist: artist});
                                }
                            });
                        }
                        var trackLinks = document.querySelectorAll('a[href*="/track/"]');
                        trackLinks.forEach(function(a) {
                            var href = a.getAttribute('href') || '';
                            var match = href.match(/\/track\/([a-zA-Z0-9]+)/);
                            if (match && match[1]) {
                                var id = match[1];
                                if (!seen[id]) {
                                    seen[id] = true;
                                    var title = a.textContent.trim();
                                    var row = a.closest('div[data-testid="tracklist-row"]') || a.closest('div[role="row"]') || a.parentElement;
                                    var artist = '';
                                    if (row) {
                                        var artistLinks = row.querySelectorAll('a[href*="/artist/"]');
                                        if (artistLinks.length > 0) {
                                            artist = artistLinks[0].textContent.trim();
                                        }
                                    }
                                    if (title) {
                                        results.push({id: id, title: title, artist: artist});
                                    }
                                }
                            }
                        });
                        var title = document.title ? document.title.replace(' | Spotify', '').replace(/ - playlist by.*/, '').trim() : '';
                        SpotifyExtractor.onTracksExtracted(JSON.stringify(results), title);
                    })();
                """.trimIndent()

                val scrollRunnable = object : Runnable {
                    override fun run() {
                        scrollCount++
                        webView.evaluateJavascript(jsExtract, null)
                        webView.evaluateJavascript("window.scrollBy(0, 1800); document.scrollingElement.scrollTop += 1800;", null)

                        if (stagnantScrolls >= 10 || scrollCount >= maxScrolls || tracks.size >= SPOTIFY_MAX_IMPORT_SONGS) {
                            handler.postDelayed({
                                webView.destroy()
                                if (continuation.isActive) {
                                    continuation.resume(Pair(playlistName, tracks))
                                }
                            }, 500)
                        } else {
                            handler.postDelayed(this, 500)
                        }
                    }
                }

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        handler.postDelayed(scrollRunnable, 800)
                    }
                }

                webView.loadUrl("https://open.spotify.com/playlist/$playlistId")

                continuation.invokeOnCancellation {
                    handler.removeCallbacksAndMessages(null)
                    webView.destroy()
                }
            } catch (e: Exception) {
                Timber.tag("SpotifyImport").e(e, "WebView initialization failed: ${e.message}")
                if (continuation.isActive) {
                    continuation.resume(Pair(playlistName, tracks))
                }
            }
        }
    }

    // Comprehensive NO-API Extractor Combining Strategies A, B, and C
    internal suspend fun extractSpotifyTracksNoApi(playlistId: String): Pair<String, List<SpotifyTrackItem>> = withContext(Dispatchers.IO) {
        var playlistName = "Imported Spotify Playlist"
        val allTracks = mutableListOf<SpotifyTrackItem>()
        val knownIds = HashSet<String>()

        // 1. Try Strategy A: Embed
        val (embedName, embedTracks) = fetchSpotifyNoApiEmbed(playlistId)
        if (embedName != "Imported Spotify Playlist") playlistName = embedName
        for (t in embedTracks) {
            if (knownIds.add(t.spotifyTrackId)) {
                allTracks.add(t.copy(sourcePosition = allTracks.size))
            }
        }

        // 2. Try Strategy B: Public HTML (especially for schema.org)
        if (allTracks.size < SPOTIFY_MAX_IMPORT_SONGS) {
            val (htmlName, htmlTracks) = fetchSpotifyNoApiPublicHtml(playlistId)
            if (playlistName == "Imported Spotify Playlist" && htmlName != "Imported Spotify Playlist") {
                playlistName = htmlName
            }
            for (t in htmlTracks) {
                if (knownIds.add(t.spotifyTrackId)) {
                    allTracks.add(t.copy(sourcePosition = allTracks.size))
                }
            }
        }

        // 3. Try Strategy C: WebView progressive DOM if available
        if (allTracks.size < SPOTIFY_MAX_IMPORT_SONGS) {
            try {
                val (webName, webTracks) = withTimeoutOrNull(60_000L) {
                    fetchSpotifyNoApiWebView(playlistId)
                } ?: Pair("", emptyList())

                if (playlistName == "Imported Spotify Playlist" && webName.isNotEmpty() && webName != "Imported Spotify Playlist") {
                    playlistName = webName
                }
                for (t in webTracks) {
                    if (knownIds.add(t.spotifyTrackId)) {
                        allTracks.add(t.copy(sourcePosition = allTracks.size))
                    }
                }
            } catch (e: Exception) {
                Timber.tag("SpotifyImport").w(e, "WebView strategy failed/timed out: ${e.message}")
            }
        }

        Timber.tag("SpotifyImport").d("Spotify NO-API Total Extracted: ${allTracks.size} tracks for '$playlistName'")
        Pair(playlistName, allTracks.take(SPOTIFY_MAX_IMPORT_SONGS))
    }

    // ==========================================
    // CANONICAL SPOTIFY EXPORTER (Exportify Architecture)
    // ==========================================
    private suspend fun extractSpotifyPlaylistTracks(
        spotifyPlaylistId: String,
        onProgress: (phase: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Pair<String, List<SpotifyTrackItem>> = withContext(Dispatchers.IO) {
        var accessToken = getSpotifyAccessToken(spotifyPlaylistId)
        if (accessToken.isNullOrEmpty()) {
            throw Exception("Failed to authenticate with Spotify API. Unable to obtain access token.")
        }

        var playlistName = "Imported Spotify Playlist"
        var reportedTotal = 0

        // Step 1: Fetch playlist metadata (name + total tracks)
        try {
            val nameReq = Request.Builder()
                .url("https://api.spotify.com/v1/playlists/$spotifyPlaylistId?fields=name,tracks.total")
                .header("Authorization", "Bearer $accessToken")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .build()
            httpClient.newCall(nameReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val pObj = JSONObject(body)
                    val name = pObj.optString("name")
                    if (name.isNotEmpty()) playlistName = name
                    reportedTotal = pObj.optJSONObject("tracks")?.optInt("total", 0) ?: pObj.optInt("total", 0)
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w(e, "Playlist metadata fetch failed: ${e.message}")
        }

        // Step 2: Paginate through ALL tracks until next == null
        val allTracks = mutableListOf<SpotifyTrackItem>()
        var currentNextUrl: String? = "https://api.spotify.com/v1/playlists/$spotifyPlaylistId/tracks?offset=0&limit=100"
        val visitedUrls = mutableSetOf<String>()
        var pageCount = 0

        while (!currentNextUrl.isNullOrEmpty()) {
            val urlToFetch = currentNextUrl!!
            if (urlToFetch in visitedUrls) {
                val errorMsg = "Pagination loop detected on $urlToFetch. Halting export to prevent infinite loop."
                Timber.tag("SpotifyImport").e(errorMsg)
                throw Exception(errorMsg)
            }
            visitedUrls.add(urlToFetch)

            var pageResult: Result<SpotifyPage>? = null
            var retries = 0
            val maxRetries = 5

            while (retries < maxRetries) {
                pageResult = fetchSpotifyPage(
                    url = urlToFetch,
                    accessToken = accessToken!!,
                    currentOffset = allTracks.size,
                    remainingLimit = 100
                )

                if (pageResult.isSuccess) break

                val ex = pageResult.exceptionOrNull()
                retries++
                if (ex is SpotifyAuthException) {
                    Timber.tag("SpotifyImport").w("Spotify token expired on page $pageCount. Refreshing token (retry $retries/$maxRetries)...")
                    val renewed = getSpotifyAccessToken(spotifyPlaylistId)
                    if (!renewed.isNullOrEmpty()) {
                        accessToken = renewed
                    }
                    delay(500L)
                } else if (ex is SpotifyRateLimitException) {
                    val backoff = (1000L * (1 shl retries)).coerceAtMost(30000L)
                    Timber.tag("SpotifyImport").w("Spotify rate limited on page $pageCount. Waiting ${backoff}ms (retry $retries/$maxRetries)...")
                    delay(backoff)
                } else {
                    Timber.tag("SpotifyImport").w("Spotify page fetch failed: ${ex?.message}. Retrying ($retries/$maxRetries)...")
                    delay(1000L * retries)
                }
            }

            if (pageResult == null || pageResult.isFailure) {
                val failureReason = pageResult?.exceptionOrNull()?.message ?: "Unknown network error"
                throw Exception("Failed to retrieve Spotify playlist page at $urlToFetch after $maxRetries retries: $failureReason")
            }

            val spotifyPage = pageResult.getOrThrow()
            if (reportedTotal == 0 && spotifyPage.totalTracks > 0) {
                reportedTotal = spotifyPage.totalTracks
            }

            allTracks.addAll(spotifyPage.tracks)
            pageCount++

            val pageItemsCount = spotifyPage.tracks.size
            val currentOffset = allTracks.size - pageItemsCount
            val totalExpected = if (reportedTotal > 0) reportedTotal else allTracks.size

            onProgress("Fetching Spotify playlist", allTracks.size, totalExpected)
            Timber.tag("SPOTIFY_IMPORT").i("total=%d", totalExpected)
            Timber.tag("SPOTIFY_IMPORT").i("page=%d", pageCount)
            Timber.tag("SPOTIFY_IMPORT").i("pageOffset=%d", currentOffset)
            Timber.tag("SPOTIFY_IMPORT").i("pageItems=%d", pageItemsCount)
            Timber.tag("SPOTIFY_IMPORT").i("extracted=%d", allTracks.size)
            Timber.tag("SPOTIFY_IMPORT").i("next=%s", spotifyPage.nextUrl ?: "null")

            currentNextUrl = spotifyPage.nextUrl
            if (spotifyPage.tracks.isEmpty() || spotifyPage.nextUrl == null) {
                break
            }
            delay(Random.nextLong(150L, 300L))
        }

        // Step 3: Invariant verification against reported total
        if (reportedTotal > 0 && allTracks.size != reportedTotal) {
            val errorMsg = "Spotify extraction incomplete: expected $reportedTotal tracks but retrieved ${allTracks.size}"
            Timber.tag("SpotifyImport").e("[SPOTIFY_IMPORT_INVARIANT_FAILURE] stage=EXTRACTION expected=$reportedTotal actual=${allTracks.size}")
            throw Exception(errorMsg)
        }

        Pair(playlistName, allTracks)
    }

    /**
     * CANONICAL CSV IMPORTER
     *
     * Processes a real CSV file / Reader containing playlist track items.
     * Both manual CSV file upload and Spotify URL import call this EXACT same method.
     */
    suspend fun importFromCsv(
        database: MusicDatabase,
        csvReader: Reader,
        playlistName: String? = null,
        spotifyPlaylistId: String? = null,
        onProgress: (phase: String, current: Int, total: Int) -> Unit = { _, _, _ -> },
        onSummary: (ImportSummary) -> Unit = {}
    ): ImportSummary = withContext(Dispatchers.IO) {
        val csvText = csvReader.readText()
        val parsedTracks = SpotifyCsvSerializer.parseFromCsv(csvText)
        if (parsedTracks.isEmpty()) {
            throw Exception("No valid tracks found in CSV")
        }

        Timber.tag("SPOTIFY_CSV").i("rowsRead=%d", parsedTracks.size)
        Timber.tag("SPOTIFY_CSV").i("rowsParsed=%d", parsedTracks.size)
        Timber.tag("SPOTIFY_MATCH").i("submitted=%d", parsedTracks.size)

        val config = SpotifyMatchConfig.load()
        val dataApiExhausted = AtomicBoolean(!config.dataApiUsable)
        val quotaNotice = if (config.dataApiUsable) {
            buildQuotaNotice(parsedTracks.size.coerceAtMost(SPOTIFY_MAX_IMPORT_SONGS))
        } else {
            null
        }

        val finalPlaylistName = playlistName ?: "Imported Playlist"
        val internalPlaylistId = UUID.randomUUID().toString()

        database.withTransaction {
            insert(
                PlaylistEntity(
                    id = internalPlaylistId,
                    name = finalPlaylistName,
                    bookmarkedAt = LocalDateTime.now(),
                    isEditable = true
                )
            )

            upsertSpotifyImportProgress(
                SpotifyImportProgressEntity(
                    spotifyPlaylistId = spotifyPlaylistId ?: "csv_$internalPlaylistId",
                    playlistId = internalPlaylistId,
                    playlistName = finalPlaylistName,
                    nextUrl = null,
                    processedCount = 0,
                    status = "IN_PROGRESS",
                    maxTracks = SPOTIFY_MAX_IMPORT_SONGS,
                    lastUpdated = LocalDateTime.now()
                )
            )
        }

        val totalTracks = parsedTracks.size
        val fetchedSpotifyTrackIds = LinkedHashSet<String>()
        val skippedSpotifyTracks = mutableListOf<Pair<String, String>>()
        val matchingInputTrackIds = LinkedHashSet<String>()
        val matchedTrackIds = LinkedHashSet<String>()
        val unmatchedTrackIds = LinkedHashSet<String>()
        var totalPlaylistInserted = 0

        for (t in parsedTracks) {
            fetchedSpotifyTrackIds.add(t.spotifyTrackId)
            matchingInputTrackIds.add(t.spotifyTrackId)
        }

        val batches = parsedTracks.chunked(100)
        var processedCount = 0

        for (batch in batches) {
            // Concurrent resolution in chunks of 6 with structured isolation
            val chunks = batch.chunked(6)
            val batchResults = mutableListOf<Pair<SpotifyTrackItem, TrackMatchOutcome>>()

            for (chunk in chunks) {
                coroutineScope {
                    val deferreds = chunk.map { track ->
                        async {
                            try {
                                track to matchTrack(track, database, config, dataApiExhausted)
                            } catch (e: Exception) {
                                Timber.tag("SpotifyImport").w(e, "Matching exception for ${track.title}: ${e.message}")
                                track to TrackMatchOutcome.unmatched()
                            }
                        }
                    }
                    val resolved = deferreds.awaitAll()
                    batchResults.addAll(resolved)
                }
                onProgress("Matching songs", matchedTrackIds.size + unmatchedTrackIds.size + batchResults.size, totalTracks)
            }

            val newMetadataToInsert = mutableListOf<com.mudassir131.yt.models.MediaMetadata>()
            val newSpotifyMapsToInsert = mutableListOf<SpotifyTrackMap>()
            val playlistMapsToInsert = mutableListOf<PlaylistSongMap>()
            val importTrackRows = mutableListOf<SpotifyImportTrackEntity>()

            for ((track, outcome) in batchResults) {
                importTrackRows.add(importTrackRow(internalPlaylistId, track, outcome))

                val songId = outcome.songId
                if (!outcome.matched || songId == null) {
                    unmatchedTrackIds.add(track.spotifyTrackId)
                    Timber.tag("SpotifyImport").w("Track '${track.title}' unmatched (confidence=${"%.2f".format(outcome.confidence)})")
                    continue
                }

                matchedTrackIds.add(track.spotifyTrackId)

                val metadata = outcome.metadata
                if (metadata != null) {
                    newMetadataToInsert.add(metadata)
                    if (track.spotifyTrackId.isNotEmpty()) {
                        newSpotifyMapsToInsert.add(
                            SpotifyTrackMap(
                                spotifyTrackId = track.spotifyTrackId,
                                songId = songId,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                durationMs = track.durationMs,
                                matchConfidence = outcome.confidence.toFloat(),
                                matchSource = outcome.source
                            )
                        )
                    }
                }

                playlistMapsToInsert.add(
                    PlaylistSongMap(
                        playlistId = internalPlaylistId,
                        songId = songId,
                        position = track.sourcePosition,
                        setVideoId = metadata?.setVideoId
                    )
                )
            }

            totalPlaylistInserted += playlistMapsToInsert.size
            onProgress("Importing matched songs", totalPlaylistInserted, totalTracks)
            val newProcessedCount = processedCount + batch.size
            val isComplete = newProcessedCount >= totalTracks
            val newStatus = if (isComplete) "COMPLETED" else "IN_PROGRESS"

            database.withTransaction {
                newMetadataToInsert.forEach { meta ->
                    try { insert(meta) } catch (e: Exception) { Timber.tag("SpotifyImport").e(e, "Error inserting metadata ${meta.id}") }
                }
                newSpotifyMapsToInsert.forEach { sMap ->
                    try { insert(sMap) } catch (e: Exception) { Timber.tag("SpotifyImport").e(e, "Error inserting SpotifyTrackMap ${sMap.spotifyTrackId}") }
                }
                playlistMapsToInsert.forEach { pMap ->
                    try { insert(pMap) } catch (e: Exception) { Timber.tag("SpotifyImport").e(e, "Error inserting PlaylistSongMap pos ${pMap.position}") }
                }
                importTrackRows.forEach { row ->
                    try { upsertSpotifyImportTrack(row) } catch (e: Exception) { Timber.tag("SpotifyImport").e(e, "Error inserting import record ${row.spotifyTrackId}") }
                }

                upsertSpotifyImportProgress(
                    SpotifyImportProgressEntity(
                        spotifyPlaylistId = spotifyPlaylistId ?: "csv_$internalPlaylistId",
                        playlistId = internalPlaylistId,
                        playlistName = finalPlaylistName,
                        nextUrl = null,
                        processedCount = newProcessedCount,
                        status = newStatus,
                        maxTracks = SPOTIFY_MAX_IMPORT_SONGS,
                        lastUpdated = LocalDateTime.now()
                    )
                )
            }

            processedCount = newProcessedCount
        }

        val summary = summarize(database, internalPlaylistId, finalPlaylistName, quotaNotice)
        val completedCount = matchedTrackIds.size + unmatchedTrackIds.size

        Timber.tag("SPOTIFY_MATCH").i("completed=%d", completedCount)
        Timber.tag("SPOTIFY_MATCH").i("matched=%d", summary.matched)
        Timber.tag("SPOTIFY_MATCH").i("unmatched=%d", summary.unmatched)
        Timber.tag("SPOTIFY_MATCH").i("skipped=0")
        Timber.tag("SPOTIFY_DB").i("inserted=%d", totalPlaylistInserted)

        Timber.tag("SPOTIFY_IMPORT_COMPLETE").i(
            "total=%d\nextracted=%d\ncsvRows=%d\nparsed=%d\nsubmitted=%d\ncompleted=%d\nmatched=%d\nunmatched=%d\nskipped=0\ninserted=%d",
            totalTracks,
            totalTracks,
            totalTracks,
            totalTracks,
            totalTracks,
            completedCount,
            summary.matched,
            summary.unmatched,
            totalPlaylistInserted
        )

        if (completedCount != totalTracks) {
            val invariantErr = "SPOTIFY IMPORT INVARIANT FAILURE: total=$totalTracks parsed=$totalTracks submitted=$totalTracks completed=$completedCount matched=${summary.matched} inserted=$totalPlaylistInserted"
            Timber.tag("SpotifyImport").e(invariantErr)
            throw Exception(invariantErr)
        }

        onSummary(summary)
        summary
    }

    /** Reads the final matched / unmatched tally straight off the review table. */
    private fun summarize(
        database: MusicDatabase,
        internalPlaylistId: String,
        playlistName: String,
        quotaNotice: String? = null
    ): ImportSummary = try {
        ImportSummary(
            playlistName = playlistName,
            matched = database.matchedSpotifyImportTrackCount(internalPlaylistId),
            unmatched = database.unmatchedSpotifyImportTrackCount(internalPlaylistId),
            quotaNotice = quotaNotice
        )
    } catch (e: Exception) {
        Timber.tag("SpotifyImport").w(e, "Could not read import tally")
        ImportSummary(playlistName, matched = 0, unmatched = 0, quotaNotice = quotaNotice)
    }

    // ==========================================
    // MAIN SPOTIFY IMPORT COORDINATOR
    // ==========================================
    private suspend fun importSpotifyPlaylist(
        database: MusicDatabase,
        spotifyPlaylistId: String,
        onProgress: (phase: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): ImportSummary = withContext(Dispatchers.IO) {
        Timber.tag("SpotifyImport").i("playlist URL = https://open.spotify.com/playlist/%s", spotifyPlaylistId)

        // Step 1: Exportify-style Spotify playlist extraction
        val (playlistName, extractedTracks) = extractSpotifyPlaylistTracks(spotifyPlaylistId, onProgress)

        if (extractedTracks.isEmpty()) {
            throw Exception("No tracks could be extracted from Spotify playlist: $spotifyPlaylistId")
        }

        // Step 2: Write REAL PHYSICAL CSV FILE to disk using BufferedWriter
        val cacheDir = App.instance.cacheDir
        val csvFile = File(cacheDir, "spotify_playlist_${spotifyPlaylistId}.csv")
        
        csvFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(SpotifyCsvSerializer.exportToCsv(extractedTracks))
            writer.flush()
        }

        // Step 3: Close and reopen from disk, independently verifying physical row count
        val reopenedFile = File(cacheDir, "spotify_playlist_${spotifyPlaylistId}.csv")
        val physicalCsvLines = reopenedFile.readLines().filter { it.isNotBlank() }
        val physicalCsvRows = (physicalCsvLines.size - 1).coerceAtLeast(0)

        if (physicalCsvRows != extractedTracks.size) {
            val msg = "Physical CSV rows ($physicalCsvRows) does not match extracted tracks (${extractedTracks.size})"
            Timber.tag("SpotifyImport").e("[SPOTIFY_IMPORT_INVARIANT_FAILURE] stage=CSV_DISK_WRITE expected=${extractedTracks.size} actual=$physicalCsvRows")
            throw Exception(msg)
        }

        Timber.tag("SPOTIFY_CSV").i("rowsWritten=%d", extractedTracks.size)
        Timber.tag("SPOTIFY_CSV").i("rowsOnDisk=%d", physicalCsvRows)

        // Step 4: Route reopened file reader directly into the CANONICAL CSV IMPORTER
        try {
            reopenedFile.reader(Charsets.UTF_8).use { reader ->
                importFromCsv(
                    database = database,
                    csvReader = reader,
                    playlistName = playlistName,
                    spotifyPlaylistId = spotifyPlaylistId,
                    onProgress = onProgress
                )
            }
        } finally {
            try {
                // Keep only the 5 most recent CSV files in cache, cleaning up older stale ones
                val oldFiles = cacheDir.listFiles { _, name -> name.startsWith("spotify_playlist_") && name.endsWith(".csv") }
                if (oldFiles != null && oldFiles.size > 5) {
                    oldFiles.sortedBy { it.lastModified() }.take(oldFiles.size - 5).forEach { it.delete() }
                }
            } catch (_: Exception) {}
        }
    }

    private fun logDetailedReport(
        total: Int,
        pages: Int,
        itemsPerPage: List<Int>,
        extractedTracksCount: Int,
        csvRowsGenerated: Int,
        csvRowsParsed: Int,
        fetchedIds: Set<String>,
        skippedTracks: List<Pair<String, String>>,
        matchingInputIds: Set<String>,
        matchedIds: Set<String>,
        unmatchedIds: Set<String>,
        finalPlaylistInsertCount: Int
    ) {
        Timber.tag("SpotifyImport").i("=== Spotify Import Detailed Report ===")
        Timber.tag("SpotifyImport").i("Spotify total: %d", total)
        Timber.tag("SpotifyImport").i("Spotify pages: %d", pages)
        Timber.tag("SpotifyImport").i("Spotify items per page: %s", itemsPerPage.joinToString())
        Timber.tag("SpotifyImport").i("Spotify tracks extracted: %d", extractedTracksCount)
        Timber.tag("SpotifyImport").i("CSV rows generated: %d", csvRowsGenerated)
        Timber.tag("SpotifyImport").i("CSV rows parsed: %d", csvRowsParsed)
        Timber.tag("SpotifyImport").i("Spotify unique track IDs fetched: %d", fetchedIds.size)
        Timber.tag("SpotifyImport").i("Spotify skipped track IDs + reason: %s", if (skippedTracks.isEmpty()) "none" else skippedTracks.joinToString { "${it.first} (${it.second})" })
        Timber.tag("SpotifyImport").i("Matching input unique track IDs: %d", matchingInputIds.size)
        Timber.tag("SpotifyImport").i("Matched unique track IDs: %d", matchedIds.size)
        Timber.tag("SpotifyImport").i("Unmatched unique track IDs: %d", unmatchedIds.size)
        Timber.tag("SpotifyImport").i("Final playlist insert count: %d", finalPlaylistInsertCount)
        Timber.tag("SpotifyImport").i(
            "Stage Invariant Check: extracted (%d) == CSV generated (%d) == CSV parsed (%d) == matchingInput (%d) == matched (%d) + unmatched (%d) -> %b",
            extractedTracksCount, csvRowsGenerated, csvRowsParsed, matchingInputIds.size, matchedIds.size, unmatchedIds.size,
            extractedTracksCount == csvRowsGenerated && csvRowsGenerated == csvRowsParsed && csvRowsParsed == matchingInputIds.size && matchingInputIds.size == (matchedIds.size + unmatchedIds.size)
        )
    }

    /**
     * Pre-flight quota report for a Data API run.
     *
     * At 100 units per `search.list` against a 10,000 unit/day default, only about 99 tracks per day
     * can be matched through the Data API. Anything past that is matched through InnerTube instead,
     * so a large import still completes — this just tells the user which way it went.
     */
    private suspend fun buildQuotaNotice(trackCount: Int): String? = try {
        val remaining = YouTubeQuotaTracker.remainingUnits(App.instance)
        val estimate = YouTubeQuotaTracker.estimate(trackCount, remaining)
        if (!estimate.willExceed) {
            null
        } else {
            val viaInnerTube = (trackCount - estimate.tracksCoverable).coerceAtLeast(0)
            Timber.tag("SpotifyImport").w(
                "Data API quota covers ${estimate.tracksCoverable}/$trackCount tracks " +
                    "(${estimate.estimatedUnits} units needed, $remaining left); rest via InnerTube"
            )
            "YouTube API quota covers ${estimate.tracksCoverable} of $trackCount tracks today; " +
                "$viaInnerTube matched via YouTube Music search instead."
        }
    } catch (e: Exception) {
        Timber.tag("SpotifyImport").w(e, "Could not build quota notice")
        null
    }

    internal suspend fun fetchAppleMusicTracks(resolvedUrl: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val html = try {
            fetchHtml(resolvedUrl)
        } catch (e: Exception) {
            throw Exception("Apple Music block: ${e.localizedMessage}. Please try Spotify/YouTube.")
        }
        val doc = Jsoup.parse(html)
        var playlistName = doc.title().replace(" on Apple Music", "").trim()
        val tracks = mutableListOf<Pair<String, String>>()

        val scripts = doc.select("script[type=application/ld+json]")
        for (script in scripts) {
            val jsonText = script.data().trim()
            try {
                val root = JSONObject(jsonText)
                val type = root.optString("@type")
                if (type == "MusicPlaylist" || root.has("track") || root.has("itemListElement")) {
                    playlistName = root.optString("name", playlistName)
                    val trackArray = root.optJSONArray("track") ?: root.optJSONArray("itemListElement")
                    if (trackArray != null) {
                        for (i in 0 until trackArray.length()) {
                            if (tracks.size >= MAX_IMPORT_SONGS) break
                            val trackObj = trackArray.getJSONObject(i)
                            val item = if (trackObj.optString("@type") == "ListItem") {
                                trackObj.optJSONObject("item")
                            } else {
                                trackObj
                            }
                            if (item != null) {
                                val name = item.optString("name")
                                val artistObj = item.optJSONObject("byArtist")
                                val artistName = artistObj?.optString("name") ?: ""
                                if (name.isNotEmpty()) {
                                    tracks.add(name to artistName)
                                }
                            }
                        }
                    }
                    if (tracks.isNotEmpty()) break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (tracks.isEmpty()) {
            val songElements = doc.select("div.songs-list-row, li.songs-list-row, div[data-testid='track-cell'], div[role='row']")
            for (el in songElements) {
                if (tracks.size >= MAX_IMPORT_SONGS) break
                val title = el.select("div.songs-list-row__song-name, .songs-list-row__song-name, div[data-testid='track-title']").text().trim()
                val artist = el.select("a.songs-list-row__link, .songs-list-row__by-line, div[data-testid='track-subtitle']").text().trim()
                if (title.isNotEmpty()) {
                    tracks.add(title to artist)
                }
            }
        }

        Pair(playlistName, tracks.take(MAX_IMPORT_SONGS))
    }

    /**
     * Imports the playlist at [url] and returns its name.
     *
     * [onSummary] reports the per-track match tally, and is invoked by the Spotify path only — the
     * YouTube and Apple Music paths import whole songs already resolved by their own source, so
     * there is nothing to score and their behaviour is deliberately left exactly as it was.
     */
    suspend fun importPlaylist(
        database: MusicDatabase,
        url: String,
        onProgress: (phase: String, current: Int, total: Int) -> Unit = { _, _, _ -> },
        onSummary: (ImportSummary) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedUrl = url.trim()
            val resolvedUrl = if (trimmedUrl.contains("spotify.link")) {
                resolveRedirect(trimmedUrl)
            } else {
                trimmedUrl
            }

            val youtubePlaylistId = when {
                resolvedUrl.contains("youtube.com") || resolvedUrl.contains("youtu.be") -> {
                    val uri = Uri.parse(resolvedUrl)
                    uri.getQueryParameter("list")
                }
                resolvedUrl.startsWith("PL") || resolvedUrl.startsWith("OLAK") -> {
                    resolvedUrl
                }
                else -> null
            }

            if (youtubePlaylistId != null) {
                val initialPage = YouTube.playlist(youtubePlaylistId).getOrThrow()
                val playlistName = initialPage.playlist.title ?: "Imported YouTube Playlist"

                val allSongs = initialPage.songs.toMutableList()
                var continuation = initialPage.songsContinuation ?: initialPage.continuation
                val seenContinuations = mutableSetOf<String>()

                while (continuation != null && allSongs.size < MAX_IMPORT_SONGS) {
                    if (continuation in seenContinuations) break
                    seenContinuations.add(continuation)

                    val continuationPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
                    if (continuationPage.songs.isEmpty()) break

                    allSongs.addAll(continuationPage.songs)
                    continuation = continuationPage.continuation
                }

                val finalSongs = allSongs.take(MAX_IMPORT_SONGS)

                val newPlaylistId = UUID.randomUUID().toString()
                database.withTransaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )

                    finalSongs.forEachIndexed { index, songItem ->
                        val metadata = songItem.toMediaMetadata()
                        insert(metadata)
                        insert(
                            PlaylistSongMap(
                                playlistId = newPlaylistId,
                                songId = songItem.id,
                                position = index,
                                setVideoId = songItem.setVideoId
                            )
                        )
                    }
                }
                return@runCatching playlistName
            } else if (resolvedUrl.contains("spotify.com/playlist/")) {
                val spotifyPlaylistId = resolvedUrl.substringAfter("playlist/").substringBefore("?").substringBefore("/")
                val summary = importSpotifyPlaylist(database, spotifyPlaylistId, onProgress)
                onSummary(summary)
                return@runCatching summary.playlistName
            } else if (resolvedUrl.contains("music.apple.com/")) {
                val (playlistName, tracks) = fetchAppleMusicTracks(resolvedUrl)

                if (tracks.isEmpty()) {
                    return@runCatching Result.failure<String>(Exception("No tracks found in Apple Music playlist")).getOrThrow()
                }

                val finalTracks = tracks.take(MAX_IMPORT_SONGS)

                val newPlaylistId = UUID.randomUUID().toString()
                database.withTransaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )
                }

                val results = mutableListOf<Triple<Int, com.mudassir131.yt.models.MediaMetadata, String?>>()
                val chunks = finalTracks.mapIndexed { index, pair -> index to pair }.chunked(5)
                for (chunk in chunks) {
                    val deferreds = chunk.map { (index, pair) ->
                        async {
                            runCatching {
                                val (songName, artistName) = pair
                                val query = "$songName $artistName".trim()
                                val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                val songItem = searchResult?.items?.filterIsInstance<SongItem>()?.firstOrNull()
                                    ?: (searchResult?.items?.firstOrNull() as? SongItem)
                                if (songItem != null) {
                                    Triple(index, songItem.toMediaMetadata(), songItem.setVideoId)
                                } else {
                                    null
                                }
                            }.getOrNull()
                        }
                    }
                    results.addAll(deferreds.awaitAll().filterNotNull())
                }

                database.withTransaction {
                    results.forEach { (index, metadata, setVideoId) ->
                        insert(metadata)
                        insert(
                            PlaylistSongMap(
                                playlistId = newPlaylistId,
                                songId = metadata.id,
                                position = index,
                                setVideoId = setVideoId
                            )
                        )
                    }
                }
                return@runCatching playlistName
            } else {
                return@runCatching Result.failure<String>(IllegalArgumentException("Unsupported Playlist URL")).getOrThrow()
            }
        }
    }
}
