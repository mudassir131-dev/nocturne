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
import com.mudassir131.yt.constants.SpotifyClientIdKey
import com.mudassir131.yt.constants.SpotifyClientSecretKey
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

enum class SpotifyImportState {
    IDLE,
    STARTING,
    AUTHENTICATING,
    EXTRACTING,
    CSV_WRITING,
    CSV_VERIFYING,
    CSV_IMPORTING,
    MATCHING,
    DATABASE_INSERTING,
    COMPLETED,
    FAILED
}

sealed class SpotifyFetchResult {
    data class Success(val page: SpotifyPage) : SpotifyFetchResult()
    data class AuthError(val code: Int, val message: String) : SpotifyFetchResult()
    data class AccessDenied(val code: Int, val category: String, val message: String) : SpotifyFetchResult()
    data class RateLimited(val code: Int, val retryAfterMs: Long?, val message: String) : SpotifyFetchResult()
    data class QuotaExceeded(val code: Int, val message: String) : SpotifyFetchResult()
    data class ServerError(val code: Int, val message: String) : SpotifyFetchResult()
    data class NetworkError(val exception: Exception, val message: String) : SpotifyFetchResult()
}

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

// ==========================================
// CUSTOM SPOTIFY EXCEPTIONS
// ==========================================

open class SpotifyImportException(message: String, cause: Throwable? = null) : Exception(message, cause)
class SpotifyAuthException(message: String = "Spotify authentication expired. Please try again.") : SpotifyImportException(message)
class SpotifyAccessDeniedException(val category: String, message: String) : SpotifyImportException(message)
class SpotifyRateLimitException(val retryAfterMs: Long?, message: String = "Spotify rate limit reached. Please wait and try again.") : SpotifyImportException(message)
class SpotifyQuotaExceededException(message: String = "Spotify API quota has been exceeded. Please try again later.") : SpotifyImportException(message)
class SpotifyPaginationLoopException(val url: String, message: String = "Spotify pagination loop detected on $url") : SpotifyImportException(message)
class SpotifyImportInvariantException(val stage: String, val expected: Int, val actual: Int, message: String) : SpotifyImportException(message)
class SpotifyMaxTracksExceededException(message: String = "Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.") : SpotifyImportException(message)

/**
 * Internal CSV serializer and parser adhering to the RFC-4180 specification and Exportify Spotify schema.
 * Represents the physical dataset boundary between Spotify extraction and YouTube matching.
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

    private fun normalizeHeaderCell(cell: String): String =
        cell.trim()
            .trimStart('\uFEFF')
            .lowercase()
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "")

    fun parseFromCsv(csvContent: String): List<SpotifyTrackItem> {
        val records = parseRfc4180Records(csvContent)
        if (records.isEmpty()) return emptyList()

        val firstRow = records.firstOrNull() ?: emptyList()
        val normalizedHeader = firstRow.map(::normalizeHeaderCell)

        val hasHeader = normalizedHeader.any { cell ->
            cell in listOf("spotifytrackid", "trackname", "title", "songtitle", "tracktitle", "name", "artist", "artists", "artistname", "artistnames")
        }

        var trackIdIdx = -1
        var trackUriIdx = -1
        var titleIdx = -1
        var artistIdx = -1
        var albumIdx = -1
        var durationIdx = -1
        var sourcePosIdx = -1
        var isLocalIdx = -1

        if (hasHeader) {
            trackIdIdx = normalizedHeader.indexOfFirst { it == "spotifytrackid" || it == "trackid" || it == "id" }
            trackUriIdx = normalizedHeader.indexOfFirst { it == "spotifytrackuri" || it == "trackuri" || it == "uri" }
            titleIdx = normalizedHeader.indexOfFirst { it in listOf("trackname", "title", "songtitle", "tracktitle", "name") }
            artistIdx = normalizedHeader.indexOfFirst { it in listOf("artistname(s)", "artistnames", "artistname", "artist", "artists") || it.startsWith("artist") }
            albumIdx = normalizedHeader.indexOfFirst { it in listOf("albumname", "album") }
            durationIdx = normalizedHeader.indexOfFirst { it in listOf("duration(ms)", "durationms", "duration", "time") }
            sourcePosIdx = normalizedHeader.indexOfFirst { it in listOf("sourceposition", "position", "index", "tracknumber", "#") }
            isLocalIdx = normalizedHeader.indexOfFirst { it in listOf("islocal", "local") }
        }

        if (titleIdx == -1 && firstRow.getOrNull(0)?.trim()?.startsWith("Spotify Track ID") == true) {
            trackIdIdx = 0
            trackUriIdx = 1
            titleIdx = 2
            artistIdx = 3
            albumIdx = 4
            durationIdx = 5
            sourcePosIdx = 6
            isLocalIdx = 7
        }

        val dataRecords = if (hasHeader) records.drop(1) else records

        if (titleIdx == -1) {
            titleIdx = 0
            artistIdx = 1
        }

        val result = mutableListOf<SpotifyTrackItem>()
        for ((index, tokens) in dataRecords.withIndex()) {
            if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue
            val trackId = (if (trackIdIdx >= 0) tokens.getOrNull(trackIdIdx) else null)?.trim() ?: ""
            val trackUri = (if (trackUriIdx >= 0) tokens.getOrNull(trackUriIdx) else null)?.trim() ?: ""
            val title = (if (titleIdx >= 0) tokens.getOrNull(titleIdx) else tokens.getOrNull(0))?.trim()?.trimStart('\uFEFF') ?: ""
            val artist = (if (artistIdx >= 0) tokens.getOrNull(artistIdx) else tokens.getOrNull(1))?.trim() ?: ""
            val album = (if (albumIdx >= 0) tokens.getOrNull(albumIdx) else null)?.trim() ?: ""
            val durationMs = (if (durationIdx >= 0) tokens.getOrNull(durationIdx) else null)?.trim()?.toLongOrNull() ?: 0L
            val sourcePos = (if (sourcePosIdx >= 0) tokens.getOrNull(sourcePosIdx) else null)?.trim()?.toIntOrNull() ?: index
            val isLocal = (if (isLocalIdx >= 0) tokens.getOrNull(isLocalIdx) else null)?.trim()?.toBooleanStrictOrNull() ?: false

            if (title.isNotBlank()) {
                val finalId = if (trackId.isNotBlank()) trackId else "csv_${(title + artist + index).hashCode()}"
                result.add(
                    SpotifyTrackItem(
                        spotifyTrackId = finalId,
                        spotifyTrackUri = trackUri.ifBlank { "spotify:track:$finalId" },
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = durationMs,
                        sourcePosition = sourcePos,
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

    /**
     * Robust RFC-4180 compliant CSV record parser.
     * Accurately parses multiline fields with embedded newlines, commas, escaped quotes (""), CRLF/LF, and Unicode.
     */
    fun parseRfc4180Records(content: String): List<List<String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val len = content.length

        while (i < len) {
            val c = content[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && content[i + 1] == '"') {
                        currentField.append('"')
                        i++ // Skip escaped quote
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> {
                        inQuotes = true
                    }
                    ',' -> {
                        currentRecord.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < len && content[i + 1] == '\n') {
                            i++
                        }
                        currentRecord.add(currentField.toString())
                        currentField.setLength(0)
                        if (currentRecord.any { it.isNotBlank() }) {
                            records.add(currentRecord.toList())
                        }
                        currentRecord.clear()
                    }
                    '\n' -> {
                        currentRecord.add(currentField.toString())
                        currentField.setLength(0)
                        if (currentRecord.any { it.isNotBlank() }) {
                            records.add(currentRecord.toList())
                        }
                        currentRecord.clear()
                    }
                    else -> {
                        currentField.append(c)
                    }
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRecord.isNotEmpty()) {
            currentRecord.add(currentField.toString())
            if (currentRecord.any { it.isNotBlank() }) {
                records.add(currentRecord.toList())
            }
        }

        return records
    }
}

/**
 * Centralized, thread-safe Spotify token provider.
 * Manages token lifecycle, single-flight refresh with Mutex to prevent stampedes, safe invalidation, and caching.
 */
object SpotifyTokenProvider {
    private val mutex = Mutex()
    @Volatile
    internal var cachedToken: String? = null
    @Volatile
    private var tokenExpiryEpochMs: Long = 0L

    fun invalidateToken() {
        cachedToken = null
        tokenExpiryEpochMs = 0L
        Timber.tag("SpotifyImport").d("Spotify token invalidated")
    }

    suspend fun getAccessToken(playlistId: String = ""): String? = mutex.withLock {
        val now = System.currentTimeMillis()
        if (cachedToken != null && now < tokenExpiryEpochMs) {
            return@withLock cachedToken
        }

        val freshToken = fetchFreshToken(playlistId)
        if (freshToken != null) {
            cachedToken = freshToken
            // Standard Spotify anonymous tokens last ~60 minutes; keep a 55-minute safety window.
            tokenExpiryEpochMs = System.currentTimeMillis() + (55L * 60L * 1000L)
            Timber.tag("SpotifyImport").d("Obtained and cached fresh Spotify access token")
        }
        return@withLock freshToken
    }

    private suspend fun fetchFreshToken(playlistId: String): String? = withContext(Dispatchers.IO) {
        // Method 0: User-configured or build-configured Spotify Developer Credentials
        try {
            val userClientId = runCatching { App.instance.dataStore.getAsync(SpotifyClientIdKey, "") }.getOrDefault("").trim()
            val userClientSecret = runCatching { App.instance.dataStore.getAsync(SpotifyClientSecretKey, "") }.getOrDefault("").trim()
            val clientId = userClientId.ifEmpty { BuildConfig.SPOTIFY_CLIENT_ID.trim() }
            val clientSecret = userClientSecret.ifEmpty { BuildConfig.SPOTIFY_CLIENT_SECRET.trim() }
            if (clientId.isNotEmpty() && clientSecret.isNotEmpty()) {
                val basicAuth = String(
                    java.util.Base64.getEncoder().encode("$clientId:$clientSecret".toByteArray(Charsets.UTF_8)),
                    Charsets.UTF_8
                )
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
                PlaylistImporter.httpClient.newCall(tokenReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val tokenJson = JSONObject(body)
                        val token = tokenJson.optString("access_token").takeIf { it.isNotEmpty() }
                            ?: tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                        if (token != null) {
                            Timber.tag("SpotifyImport").d("Successfully authenticated with Spotify Developer Credentials")
                            return@withContext token
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w("Method 0 Developer credentials token fetch failed: ${e.message}")
        }

        // Method 1: Web Player transport endpoint
        try {
            val req = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://open.spotify.com/")
                .header("Origin", "https://open.spotify.com")
                .header("App-Platform", "WebPlayer")
                .header("Spotify-App-Version", "1.2.34.0")
                .build()
            PlaylistImporter.httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val tokenJson = JSONObject(body)
                    val token = tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                    if (token != null) return@withContext token
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w("Method 1 token fetch failed: ${e.message}")
        }

        // Method 2: Web-player hyphenated
        try {
            val req = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web-player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://open.spotify.com/")
                .header("Origin", "https://open.spotify.com")
                .header("App-Platform", "WebPlayer")
                .build()
            PlaylistImporter.httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val tokenJson = JSONObject(body)
                    val token = tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                    if (token != null) return@withContext token
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w("Method 2 token fetch failed: ${e.message}")
        }

        // Method 3: Transport without productType
        try {
            val req = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json, text/plain, */*")
                .header("Referer", "https://open.spotify.com/")
                .header("Origin", "https://open.spotify.com")
                .header("App-Platform", "WebPlayer")
                .build()
            PlaylistImporter.httpClient.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val tokenJson = JSONObject(body)
                    val token = tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                    if (token != null) return@withContext token
                }
            }
        } catch (e: Exception) {
            Timber.tag("SpotifyImport").w("Method 3 token fetch failed: ${e.message}")
        }

        // Method 5: Embed HTML token extraction if playlistId is provided
        if (playlistId.isNotEmpty()) {
            try {
                val embedHtml = PlaylistImporter.fetchHtml("https://open.spotify.com/embed/playlist/$playlistId")
                val token = PlaylistImporter.extractAccessToken(embedHtml)
                if (token != null) return@withContext token
            } catch (e: Exception) {
                Timber.tag("SpotifyImport").w("Method 5 embed token fetch failed: ${e.message}")
            }
        }

        null
    }
}

/**
 * Imports playlists from YouTube, Spotify and Apple Music into Nocturne's own database.
 *
 * ## The Spotify endpoints here are unofficial and undocumented
 *
 * `https://open.spotify.com/get_access_token` is an internal endpoint of Spotify's web player. It is
 * not part of the documented Web API, carries no compatibility guarantee, and **may change or
 * disappear without notice** — as may the shape of the `__NEXT_DATA__` / `<script id="session">`
 * blobs and the embed page that [extractAccessToken] and the no-API strategies scrape.
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

    @Volatile
    var currentSessionState: SpotifyImportState = SpotifyImportState.IDLE
        internal set

    const val MAX_IMPORT_SONGS = 5000

    /** Candidates ranked per track, on either matching path. */
    private const val MATCH_CANDIDATE_COUNT = 5

    private val cookieStore = ConcurrentHashMap<String, MutableList<Cookie>>()

    internal val httpClient: OkHttpClient by lazy {
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

    internal fun extractAccessToken(html: String): String? {
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

    internal suspend fun getSpotifyAccessToken(playlistId: String = ""): String? =
        SpotifyTokenProvider.getAccessToken(playlistId)

    internal suspend fun fetchSpotifyPage(
        url: String,
        accessToken: String,
        currentOffset: Int
    ): SpotifyFetchResult = withContext(Dispatchers.IO) {
        try {
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
                val code = resp.code
                val body = resp.body?.string() ?: ""

                if (code == 401) {
                    Timber.tag("SPOTIFY_HTTP_ERROR").w(
                        "status=401 url=%s offset=%d reason=UNAUTHORIZED",
                        url, currentOffset
                    )
                    return@withContext SpotifyFetchResult.AuthError(401, "Spotify authentication expired (401)")
                }

                if (code == 403) {
                    val lowerBody = body.lowercase()
                    val category = when {
                        lowerBody.contains("playlist") || lowerBody.contains("not found") -> "SPOTIFY_PLAYLIST_ACCESS_DENIED"
                        lowerBody.contains("auth") || lowerBody.contains("scope") || lowerBody.contains("permission") -> "SPOTIFY_AUTHORIZATION_DENIED"
                        lowerBody.contains("policy") || lowerBody.contains("restriction") || lowerBody.contains("country") || lowerBody.contains("geo") -> "SPOTIFY_POLICY_RESTRICTION"
                        else -> "SPOTIFY_UNKNOWN_403"
                    }
                    Timber.tag("SPOTIFY_HTTP_ERROR").w(
                        "status=403 url=%s offset=%d reason=%s",
                        url, currentOffset, category
                    )
                    return@withContext SpotifyFetchResult.AccessDenied(403, category, "Spotify access denied ($category)")
                }

                if (code == 429) {
                    val retryAfterSec = resp.header("Retry-After")?.trim()?.toLongOrNull()
                    val retryAfterMs = retryAfterSec?.let { it * 1000L }
                    // Only classify as explicit quota exceeded if Spotify returned a structured JSON error body with "QUOTA_EXCEEDED"
                    val isExplicitQuota = body.contains("\"QUOTA_EXCEEDED\"", ignoreCase = true) ||
                            (body.contains("\"message\"") && body.contains("quota exceeded", ignoreCase = true))

                    Timber.tag("SPOTIFY_HTTP").w(
                        "stage=EXTRACTING endpoint=%s status=429 offset=%d retryAfter=%s reason=%s",
                        url, currentOffset, retryAfterMs?.toString() ?: "none", if (isExplicitQuota) "SPOTIFY_QUOTA_EXCEEDED" else "RATE_LIMITED"
                    )

                    return@withContext if (isExplicitQuota) {
                        SpotifyFetchResult.QuotaExceeded(429, "Spotify API quota has been exceeded.")
                    } else {
                        SpotifyFetchResult.RateLimited(429, retryAfterMs, "Spotify rate limit reached.")
                    }
                }

                if (code in 500..599) {
                    Timber.tag("SPOTIFY_HTTP_ERROR").w(
                        "status=%d url=%s offset=%d reason=SERVER_ERROR",
                        code, url, currentOffset
                    )
                    return@withContext SpotifyFetchResult.ServerError(code, "Spotify server error ($code)")
                }

                if (!resp.isSuccessful) {
                    Timber.tag("SPOTIFY_HTTP_ERROR").w(
                        "status=%d url=%s offset=%d reason=HTTP_ERROR",
                        code, url, currentOffset
                    )
                    return@withContext SpotifyFetchResult.ServerError(code, "HTTP $code fetching Spotify page")
                }

                val tracksJson = JSONObject(body)
                val items = tracksJson.optJSONArray("items") ?: tracksJson.optJSONArray("tracks")
                val total = tracksJson.optInt("total", 0)
                val nextRaw = tracksJson.optString("next").takeIf { !it.isNullOrEmpty() && it != "null" }

                val pageTracks = mutableListOf<SpotifyTrackItem>()
                val pageSkipped = mutableListOf<Pair<String, String>>()
                if (items != null) {
                    for (i in 0 until items.length()) {
                        val itemObj = items.optJSONObject(i)
                        if (itemObj == null) {
                            val skipId = "index_${currentOffset + i}"
                            val reason = "Null item object in Spotify items array"
                            pageSkipped.add(skipId to reason)
                            continue
                        }
                        val isLocal = itemObj.optBoolean("is_local", false)
                        val trackObj = itemObj.optJSONObject("track") ?: itemObj.optJSONObject("item")
                        if (trackObj == null) {
                            val skipId = "index_${currentOffset + i}"
                            val reason = "Null track object (removed, unavailable or restricted on Spotify)"
                            pageSkipped.add(skipId to reason)
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
                    }
                }

                Timber.tag("SPOTIFY_HTTP").d(
                    "status=200 offset=%d items=%d total=%d",
                    currentOffset, pageTracks.size, total
                )

                SpotifyFetchResult.Success(
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
            Timber.tag("SPOTIFY_HTTP_ERROR").e(e, "Exception fetching Spotify page: ${e.message}")
            SpotifyFetchResult.NetworkError(e, e.message ?: "Network error")
        }
    }

    // ==========================================
    // SPOTIFY → YOUTUBE MATCHING
    // ==========================================

    /**
     * Per-run matching settings, read once so tracks don't each hit DataStore.
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

    private suspend fun matchViaDataApi(
        track: SpotifyTrackItem,
        query: String,
        config: SpotifyMatchConfig,
        dataApiExhausted: AtomicBoolean
    ): TrackMatchOutcome? {
        val context = App.instance

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
    // SPOTIFY WEB / EMBED EXTRACTION FALLBACK
    // ==========================================
    internal suspend fun extractTracksFromSpotifyWeb(spotifyPlaylistId: String): Pair<String, List<SpotifyTrackItem>> = withContext(Dispatchers.IO) {
        var playlistName = "Imported Spotify Playlist"
        val tracks = mutableListOf<SpotifyTrackItem>()
        val seenTrackIds = mutableSetOf<String>()

        val urlsToTry = listOf(
            "https://open.spotify.com/embed/playlist/$spotifyPlaylistId",
            "https://open.spotify.com/playlist/$spotifyPlaylistId"
        )

        for (url in urlsToTry) {
            try {
                val html = fetchHtml(url)
                if (html.isEmpty()) continue

                // 1. Try __NEXT_DATA__
                val nextDataMatch = Pattern.compile("<script id=\"__NEXT_DATA__\"[^>]*>([\\s\\S]*?)</script>").matcher(html)
                if (nextDataMatch.find()) {
                    val jsonStr = nextDataMatch.group(1)?.trim() ?: ""
                    val json = JSONObject(jsonStr)
                    val pageProps = json.optJSONObject("props")?.optJSONObject("pageProps")
                    val entity = pageProps?.optJSONObject("state")?.optJSONObject("data")?.optJSONObject("entity")
                        ?: pageProps?.optJSONObject("entity")
                    
                    val name = entity?.optString("name", "") ?: ""
                    if (name.isNotEmpty()) playlistName = name

                    val trackList = entity?.optJSONArray("trackList")
                        ?: entity?.optJSONObject("tracks")?.optJSONArray("items")
                        ?: pageProps?.optJSONObject("state")?.optJSONObject("data")?.optJSONArray("trackList")

                    if (trackList != null && trackList.length() > 0) {
                        for (i in 0 until trackList.length()) {
                            val tObj = trackList.optJSONObject(i) ?: continue
                            val innerTrack = tObj.optJSONObject("track") ?: tObj
                            val uri = innerTrack.optString("uri", "")
                            val trackId = if (uri.startsWith("spotify:track:")) {
                                uri.substringAfter("spotify:track:")
                            } else {
                                innerTrack.optString("id", innerTrack.optString("uid", "sp_$i"))
                            }
                            val title = innerTrack.optString("title", innerTrack.optString("name", "")).trim()
                            val subtitle = innerTrack.optString("subtitle", "").trim()
                            val artistsArr = innerTrack.optJSONArray("artists")
                            val artist = if (subtitle.isNotEmpty()) {
                                subtitle
                            } else if (artistsArr != null && artistsArr.length() > 0) {
                                (0 until artistsArr.length()).mapNotNull { idx ->
                                    artistsArr.optJSONObject(idx)?.optString("name")?.trim()?.takeIf { it.isNotEmpty() }
                                }.joinToString(", ")
                            } else {
                                ""
                            }
                            val durationMs = innerTrack.optLong("duration", innerTrack.optLong("duration_ms", 0L))
                            val isLocal = innerTrack.optBoolean("is_local", false)
                            val albumName = innerTrack.optJSONObject("album")?.optString("name", "") ?: ""

                            if (title.isNotEmpty() && seenTrackIds.add(trackId)) {
                                tracks.add(
                                    SpotifyTrackItem(
                                        spotifyTrackId = trackId,
                                        spotifyTrackUri = if (uri.isNotEmpty()) uri else "spotify:track:$trackId",
                                        title = title,
                                        artist = artist,
                                        album = albumName,
                                        sourcePosition = tracks.size,
                                        durationMs = durationMs,
                                        isLocal = isLocal
                                    )
                                )
                            }
                        }
                    }
                }

                // 2. Try initialState (used on mobile open.spotify.com/playlist/...)
                val initialStateMatch = Pattern.compile("<script id=\"initialState\"[^>]*>([\\s\\S]*?)</script>").matcher(html)
                if (initialStateMatch.find()) {
                    var raw = initialStateMatch.group(1)?.trim() ?: ""
                    if (raw.startsWith("<!--") && raw.endsWith("-->")) {
                        raw = raw.removeSurrounding("<!--", "-->")
                    }
                    val decodedJson = try {
                        val decodedBytes = android.util.Base64.decode(raw, android.util.Base64.DEFAULT)
                        String(decodedBytes, Charsets.UTF_8)
                    } catch (e: Exception) {
                        try {
                            String(java.util.Base64.getDecoder().decode(raw), Charsets.UTF_8)
                        } catch (e2: Exception) {
                            raw
                        }
                    }
                    try {
                        val json = JSONObject(decodedJson)
                        val itemsObj = json.optJSONObject("entities")?.optJSONObject("items")
                        val playlistObj = itemsObj?.optJSONObject("spotify:playlist:$spotifyPlaylistId")
                            ?: itemsObj?.let { obj ->
                                val k = obj.keys().asSequence().firstOrNull { it.contains(spotifyPlaylistId) }
                                if (k != null) obj.optJSONObject(k) else null
                            }
                        val name = playlistObj?.optString("name", "") ?: ""
                        if (name.isNotEmpty()) playlistName = name

                        val content = playlistObj?.optJSONObject("content")
                        val contentItems = content?.optJSONArray("items")
                        if (contentItems != null && contentItems.length() > 0) {
                            for (i in 0 until contentItems.length()) {
                                val itemWrapper = contentItems.optJSONObject(i) ?: continue
                                val itemData = itemWrapper.optJSONObject("itemV2")?.optJSONObject("data")
                                    ?: itemWrapper.optJSONObject("data")
                                    ?: itemWrapper
                                val uri = itemData.optString("uri", "")
                                val trackId = if (uri.startsWith("spotify:track:")) {
                                    uri.substringAfter("spotify:track:")
                                } else {
                                    itemData.optString("id", "sp_init_$i")
                                }
                                val title = itemData.optString("name", itemData.optString("title", "")).trim()
                                val artistsArr = itemData.optJSONObject("artists")?.optJSONArray("items")
                                val artist = if (artistsArr != null && artistsArr.length() > 0) {
                                    (0 until artistsArr.length()).mapNotNull { idx ->
                                        val aObj = artistsArr.optJSONObject(idx)
                                        aObj?.optJSONObject("profile")?.optString("name")
                                            ?: aObj?.optString("name")
                                    }.joinToString(", ")
                                } else {
                                    ""
                                }
                                val durationMs = itemData.optJSONObject("duration")?.optLong("totalMilliseconds", 0L)
                                    ?: itemData.optLong("duration_ms", 0L)
                                val albumName = itemData.optJSONObject("albumOfTrack")?.optString("name", "")
                                    ?: itemData.optJSONObject("album")?.optString("name", "") ?: ""

                                if (title.isNotEmpty() && seenTrackIds.add(trackId)) {
                                    tracks.add(
                                        SpotifyTrackItem(
                                            spotifyTrackId = trackId,
                                            spotifyTrackUri = if (uri.isNotEmpty()) uri else "spotify:track:$trackId",
                                            title = title,
                                            artist = artist,
                                            album = albumName,
                                            sourcePosition = tracks.size,
                                            durationMs = durationMs,
                                            isLocal = false
                                        )
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("SpotifyImport").w("initialState JSON parsing failed: ${e.message}")
                    }
                }

                if (tracks.isNotEmpty()) {
                    Timber.tag("SPOTIFY_DEBUG").i("webExtractionComplete total=%d", tracks.size)
                    break
                }
            } catch (e: Exception) {
                Timber.tag("SpotifyImport").w(e, "Web extraction attempt on $url failed: ${e.message}")
            }
        }

        Pair(playlistName, tracks)
    }

    // ==========================================
    // CANONICAL SPOTIFY EXPORTER
    // ==========================================
    private suspend fun extractSpotifyPlaylistTracks(
        spotifyPlaylistId: String,
        onProgress: (phase: String, current: Int, total: Int) -> Unit = { _, _, _ -> }
    ): Pair<String, List<SpotifyTrackItem>> = withContext(Dispatchers.IO) {
        var accessToken = SpotifyTokenProvider.getAccessToken(spotifyPlaylistId)
        if (accessToken.isNullOrEmpty()) {
            val (webName, webTracks) = extractTracksFromSpotifyWeb(spotifyPlaylistId)
            if (webTracks.isNotEmpty()) {
                return@withContext Pair(webName, webTracks)
            }
            throw SpotifyAuthException("Failed to authenticate with Spotify API. Unable to obtain access token.")
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
                    if (reportedTotal > 100) {
                        Timber.tag("SpotifyImport").w("Spotify playlist has $reportedTotal tracks (> 100 limit). Rejecting with CSV recommendation.")
                        throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
                    }
                }
            }
        } catch (e: Exception) {
            if (e is SpotifyMaxTracksExceededException) throw e
            Timber.tag("SpotifyImport").w(e, "Playlist metadata fetch failed: ${e.message}")
        }

        // Step 2: Paginate through tracks sequentially until next == null (up to 100 max for Spotify URL import)
        val allTracks = mutableListOf<SpotifyTrackItem>()
        var currentNextUrl: String? = "https://api.spotify.com/v1/playlists/$spotifyPlaylistId/items?offset=0&limit=100"
        val visitedUrls = mutableSetOf<String>()
        var pageCount = 0
        var fallbackToTracksEndpoint = false

        while (!currentNextUrl.isNullOrEmpty()) {
            val urlToFetch = currentNextUrl!!
            if (urlToFetch in visitedUrls) {
                val errorMsg = "Pagination loop detected on $urlToFetch"
                Timber.tag("SpotifyImport").e(errorMsg)
                throw SpotifyPaginationLoopException(urlToFetch, errorMsg)
            }
            visitedUrls.add(urlToFetch)

            var pageSuccess: SpotifyPage? = null
            var authRetries = 0
            var generalRetries = 0
            val maxGeneralRetries = 5

            while (pageSuccess == null && authRetries < 2 && generalRetries < maxGeneralRetries) {
                val result = fetchSpotifyPage(
                    url = urlToFetch,
                    accessToken = accessToken!!,
                    currentOffset = allTracks.size
                )

                when (result) {
                    is SpotifyFetchResult.Success -> {
                        pageSuccess = result.page
                    }
                    is SpotifyFetchResult.AuthError -> {
                        authRetries++
                        if (authRetries >= 2) {
                            throw SpotifyAuthException("Spotify authentication expired. Please try again.")
                        }
                        Timber.tag("SpotifyImport").w("Spotify token expired on page $pageCount. Invaliding and refreshing...")
                        SpotifyTokenProvider.invalidateToken()
                        accessToken = SpotifyTokenProvider.getAccessToken(spotifyPlaylistId)
                        if (accessToken.isNullOrEmpty()) {
                            throw SpotifyAuthException("Failed to renew Spotify access token.")
                        }
                        delay(500L)
                    }
                    is SpotifyFetchResult.AccessDenied -> {
                        throw SpotifyAccessDeniedException(
                            result.category,
                            "Spotify denied access to this playlist: ${result.message}"
                        )
                    }
                    is SpotifyFetchResult.RateLimited -> {
                        generalRetries++
                        if (generalRetries >= maxGeneralRetries) {
                            throw SpotifyRateLimitException(result.retryAfterMs, "Spotify rate limit reached and retries exhausted.")
                        }
                        val waitMs = result.retryAfterMs ?: (1500L * generalRetries + Random.nextLong(200, 800)).coerceAtMost(30000L)
                        Timber.tag("SPOTIFY_HTTP").w(
                            "stage=EXTRACTING status=429 page=%d retry=%d/%d waiting=%dms reason=RATE_LIMITED",
                            pageCount, generalRetries, maxGeneralRetries, waitMs
                        )
                        delay(waitMs)
                    }
                    is SpotifyFetchResult.QuotaExceeded -> {
                        Timber.tag("SPOTIFY_HTTP").w("Spotify API quota reached. Attempting web extraction fallback...")
                        val (webName, webTracks) = extractTracksFromSpotifyWeb(spotifyPlaylistId)
                        if (webTracks.isNotEmpty()) {
                            if (webTracks.size > 100) {
                                throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
                            }
                            return@withContext Pair(webName.ifEmpty { playlistName }, webTracks)
                        }
                        throw SpotifyQuotaExceededException(result.message)
                    }
                    is SpotifyFetchResult.ServerError, is SpotifyFetchResult.NetworkError -> {
                        // If items endpoint returned a 404 on the very first page, try fallback to tracks endpoint
                        if (pageCount == 0 && allTracks.isEmpty() && !fallbackToTracksEndpoint && urlToFetch.contains("/items")) {
                            fallbackToTracksEndpoint = true
                            currentNextUrl = "https://api.spotify.com/v1/playlists/$spotifyPlaylistId/tracks?offset=0&limit=100"
                            break
                        }
                        generalRetries++
                        if (generalRetries >= maxGeneralRetries) {
                            val (webName, webTracks) = extractTracksFromSpotifyWeb(spotifyPlaylistId)
                            if (webTracks.isNotEmpty()) {
                                if (webTracks.size > 100) {
                                    throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
                                }
                                return@withContext Pair(webName.ifEmpty { playlistName }, webTracks)
                            }
                            throw SpotifyImportException("Failed to retrieve Spotify playlist page at $urlToFetch after $maxGeneralRetries attempts")
                        }
                        val backoff = (1000L * generalRetries + Random.nextLong(100, 500)).coerceAtMost(10000L)
                        Timber.tag("SpotifyImport").w("Spotify page fetch failed. Retrying in ${backoff}ms ($generalRetries/$maxGeneralRetries)...")
                        delay(backoff)
                    }
                }
            }

            if (fallbackToTracksEndpoint && pageSuccess == null) {
                continue
            }

            if (pageSuccess == null) {
                val (webName, webTracks) = extractTracksFromSpotifyWeb(spotifyPlaylistId)
                if (webTracks.isNotEmpty()) {
                    if (webTracks.size > 100) {
                        throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
                    }
                    return@withContext Pair(webName.ifEmpty { playlistName }, webTracks)
                }
                throw SpotifyImportException("Failed to retrieve Spotify playlist page at $urlToFetch")
            }

            val spotifyPage = pageSuccess
            if (reportedTotal == 0 && spotifyPage.totalTracks > 0) {
                reportedTotal = spotifyPage.totalTracks
            }
            if (reportedTotal > 100) {
                throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
            }

            allTracks.addAll(spotifyPage.tracks)
            if (allTracks.size > 100) {
                throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
            }
            pageCount++

            val pageItemsCount = spotifyPage.tracks.size
            val currentOffset = allTracks.size - pageItemsCount
            val totalExpected = if (reportedTotal > 0) reportedTotal else allTracks.size

            onProgress("Fetching Spotify playlist", allTracks.size, totalExpected)
            Timber.tag("SPOTIFY_DEBUG").i("page=%d offset=%d items=%d", pageCount, currentOffset, pageItemsCount)
            Timber.tag("SPOTIFY_IMPORT").i(
                "total=%d page=%d offset=%d pageItems=%d extracted=%d nextPresent=%b",
                totalExpected, pageCount, currentOffset, pageItemsCount, allTracks.size, spotifyPage.nextUrl != null
            )

            currentNextUrl = spotifyPage.nextUrl
            if (spotifyPage.tracks.isEmpty() || spotifyPage.nextUrl == null) {
                break
            }
            delay(Random.nextLong(150L, 300L))
        }

        if (allTracks.size > 100) {
            throw SpotifyMaxTracksExceededException("Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV.")
        }

        Timber.tag("SPOTIFY_DEBUG").i("extractionComplete total=%d extracted=%d", reportedTotal, allTracks.size)

        // Step 3: Invariant verification against reported total
        if (reportedTotal > 0 && allTracks.size != reportedTotal) {
            val errorMsg = "Spotify playlist could not be fully retrieved. Expected $reportedTotal tracks but retrieved ${allTracks.size}. No songs were imported."
            Timber.tag("SpotifyImport").e("[SPOTIFY_IMPORT_INVARIANT_FAILURE] stage=EXTRACTION expected=$reportedTotal actual=${allTracks.size}")
            throw SpotifyImportInvariantException("EXTRACTION", reportedTotal, allTracks.size, errorMsg)
        }

        Pair(playlistName, allTracks)
    }

    /**
     * CANONICAL CSV IMPORTER
     *
     * Processes a physical CSV file / Reader containing playlist track items until EOF.
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
            throw SpotifyImportException("No valid tracks found in CSV")
        }

        val totalTracks = parsedTracks.size
        Timber.tag("SPOTIFY_CSV").i("rowsRead=%d", totalTracks)
        Timber.tag("SPOTIFY_CSV").i("rowsParsed=%d", totalTracks)
        Timber.tag("SPOTIFY_MATCH").i("submitted=%d", totalTracks)

        val config = SpotifyMatchConfig.load()
        val dataApiExhausted = AtomicBoolean(!config.dataApiUsable)
        val quotaNotice = if (config.dataApiUsable) {
            buildQuotaNotice(totalTracks)
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
                    maxTracks = totalTracks,
                    lastUpdated = LocalDateTime.now()
                )
            )
        }

        val matchedTrackIds = mutableListOf<String>()
        val unmatchedTrackIds = mutableListOf<String>()
        var totalPlaylistInserted = 0

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

                // Preserve original position and duplicates
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
                        maxTracks = totalTracks,
                        lastUpdated = LocalDateTime.now()
                    )
                )
            }

            processedCount = newProcessedCount
        }

        val summary = summarize(database, internalPlaylistId, finalPlaylistName, quotaNotice)
        val completedCount = matchedTrackIds.size + unmatchedTrackIds.size

        Timber.tag("SPOTIFY_MATCH_DEBUG").i("submitted=%d completed=%d matched=%d unmatched=%d", totalTracks, completedCount, summary.matched, summary.unmatched)
        Timber.tag("SPOTIFY_DB_DEBUG").i("inserted=%d", totalPlaylistInserted)
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
            throw SpotifyImportInvariantException("MATCHING_COMPLETION", totalTracks, completedCount, invariantErr)
        }

        onSummary(summary)
        summary
    }

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
        currentSessionState = SpotifyImportState.EXTRACTING
        Timber.tag("SPOTIFY_DEBUG").i("extractionStarted")
        val (playlistName, extractedTracks) = extractSpotifyPlaylistTracks(spotifyPlaylistId, onProgress)

        if (extractedTracks.isEmpty()) {
            throw SpotifyImportException("No tracks could be extracted from Spotify playlist: $spotifyPlaylistId")
        }

        // Step 2: Write REAL PHYSICAL CSV FILE to disk using BufferedWriter
        currentSessionState = SpotifyImportState.CSV_WRITING
        val cacheDir = App.instance.cacheDir
        val csvFile = File(cacheDir, "spotify_playlist_${spotifyPlaylistId}.csv")
        
        csvFile.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.write(SpotifyCsvSerializer.exportToCsv(extractedTracks))
            writer.flush()
        }

        // Step 3: Close and reopen from disk, independently verifying physical row count
        currentSessionState = SpotifyImportState.CSV_VERIFYING
        val reopenedFile = File(cacheDir, "spotify_playlist_${spotifyPlaylistId}.csv")
        val parsedDiskTracks = reopenedFile.reader(Charsets.UTF_8).use { reader ->
            SpotifyCsvSerializer.parseFromCsv(reader.readText())
        }

        val rowsWritten = extractedTracks.size
        val rowsOnDisk = parsedDiskTracks.size
        Timber.tag("SPOTIFY_CSV_DEBUG").i("file=%s rowsWritten=%d rowsOnDisk=%d rowsParsed=%d", csvFile.absolutePath, rowsWritten, rowsOnDisk, rowsOnDisk)
        Timber.tag("SPOTIFY_CSV").i("rowsWritten=%d", rowsWritten)
        Timber.tag("SPOTIFY_CSV").i("rowsOnDisk=%d", rowsOnDisk)
        Timber.tag("SPOTIFY_CSV").i("rowsParsed=%d", rowsOnDisk)

        if (rowsOnDisk != rowsWritten) {
            val msg = "Physical CSV rows ($rowsOnDisk) does not match extracted tracks ($rowsWritten)"
            Timber.tag("SpotifyImport").e("[SPOTIFY_IMPORT_INVARIANT_FAILURE] stage=CSV_DISK_WRITE expected=$rowsWritten actual=$rowsOnDisk")
            throw SpotifyImportInvariantException("CSV_DISK_WRITE", rowsWritten, rowsOnDisk, msg)
        }

        // Step 4: Route reopened file reader directly into the CANONICAL CSV IMPORTER
        currentSessionState = SpotifyImportState.CSV_IMPORTING
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
                Timber.tag("SPOTIFY_DEBUG").i("importStarted playlistId=%s", spotifyPlaylistId)
                currentSessionState = SpotifyImportState.STARTING
                try {
                    val summary = importSpotifyPlaylist(database, spotifyPlaylistId, onProgress)
                    currentSessionState = SpotifyImportState.COMPLETED
                    Timber.tag("SPOTIFY_DEBUG").i("importComplete playlistName=%s", summary.playlistName)
                    onSummary(summary)
                    return@runCatching summary.playlistName
                } catch (e: Throwable) {
                    currentSessionState = SpotifyImportState.FAILED
                    Timber.tag("SPOTIFY_DEBUG").e(e, "importFailed error=%s", e.message)
                    throw e
                } finally {
                    currentSessionState = SpotifyImportState.IDLE
                }
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
