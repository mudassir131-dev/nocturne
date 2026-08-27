/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import com.mudassir131.yt.constants.SelfHostedLosslessEnabledKey
import com.mudassir131.yt.constants.SelfHostedPasswordKey
import com.mudassir131.yt.constants.SelfHostedServerNameKey
import com.mudassir131.yt.constants.SelfHostedServerUrlKey
import com.mudassir131.yt.constants.SelfHostedUsernameKey
import com.mudassir131.yt.utils.PreferenceStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Self-hosted lossless audio provider supporting Navidrome, Subsonic, and OpenSubsonic servers.
 * Resolves genuine uncompressed / lossless ALAC & FLAC streams without transcoding.
 */
object SelfHostedLosslessAudioProvider : LosslessAudioProvider {
    override val name: String = "SelfHostedLosslessAudioProvider"
    private const val TAG = "SelfHostedLossless"
    private const val CLIENT_NAME = "Nocturne"
    private const val API_VERSION = "1.16.1"

    data class ServerConfig(
        val serverUrl: String,
        val username: String,
        val passwordOrToken: String,
        val serverName: String = "Navidrome",
        val enabled: Boolean = true,
    )

    // Optional override for testing or dynamic configuration
    @Volatile
    var explicitConfig: ServerConfig? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    /**
     * Gets the active server configuration from preferences or explicit test configuration.
     */
    fun getConfig(): ServerConfig? {
        explicitConfig?.let { return it }

        val enabled = PreferenceStore.get(SelfHostedLosslessEnabledKey) ?: false
        if (!enabled) return null

        val serverUrl = PreferenceStore.get(SelfHostedServerUrlKey)?.trim().orEmpty()
        val username = PreferenceStore.get(SelfHostedUsernameKey)?.trim().orEmpty()
        val password = PreferenceStore.get(SelfHostedPasswordKey)?.trim().orEmpty()
        val serverName = PreferenceStore.get(SelfHostedServerNameKey)?.trim().orEmpty().ifBlank { "Navidrome" }

        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            return null
        }

        return ServerConfig(
            serverUrl = serverUrl.trimEnd('/'),
            username = username,
            passwordOrToken = password,
            serverName = serverName,
            enabled = true,
        )
    }

    override suspend fun resolve(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
    ): ResolvedLosslessStream? = withContext(Dispatchers.IO) {
        val config = getConfig()
        if (config == null || !config.enabled) {
            Timber.tag(TAG).d("Self-hosted lossless provider is disabled or not configured.")
            return@withContext null
        }

        if (title.isBlank()) return@withContext null

        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)
        val query = if (cleanArtist.isNotBlank()) "$cleanTitle $cleanArtist" else cleanTitle

        Timber.tag(TAG).i("Searching self-hosted server (${config.serverName}) for: '$cleanTitle' by '$cleanArtist' (${durationSeconds}s)")

        // 1. Search server by title + artist query
        val candidate = searchServer(config, query, cleanTitle, cleanArtist, durationSeconds)
            ?: if (cleanArtist.isNotBlank()) {
                // 2. Fallback search by title only if combined query returned nothing
                Timber.tag(TAG).d("Retrying search by title only: '$cleanTitle'")
                searchServer(config, cleanTitle, cleanTitle, cleanArtist, durationSeconds)
            } else null

        if (candidate == null) {
            Timber.tag(TAG).d("No matching lossless track found on ${config.serverName} for: '$title'")
            return@withContext null
        }

        val rawStreamUrl = buildStreamUrl(config, candidate.id)
        val redactedUrl = redactSensitiveUrl(rawStreamUrl)
        Timber.tag(TAG).i("Resolved raw lossless stream: $redactedUrl (codec=${candidate.codec}, bitDepth=${candidate.bitDepth}, sampleRate=${candidate.sampleRate})")

        ResolvedLosslessStream(
            url = rawStreamUrl,
            mimeType = candidate.mimeType,
            codec = candidate.codec,
            bitDepth = candidate.bitDepth ?: 16,
            sampleRate = candidate.sampleRate ?: 44100,
            channels = 2,
            bitrate = candidate.bitrate,
            contentLength = candidate.size,
            durationSeconds = candidate.duration,
            sourceName = "${config.serverName.lowercase()}_lossless",
            expiresInSeconds = 86400,
        )
    }

    private data class ServerSongCandidate(
        val id: String,
        val title: String,
        val artist: String,
        val album: String?,
        val duration: Int,
        val suffix: String,
        val contentType: String,
        val bitrate: Int?,
        val bitDepth: Int?,
        val sampleRate: Int?,
        val size: Long?,
        val codec: String,
        val mimeType: String,
        val score: Int,
    )

    private fun searchServer(
        config: ServerConfig,
        query: String,
        targetTitle: String,
        targetArtist: String,
        targetDuration: Int,
    ): ServerSongCandidate? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val authParams = buildAuthParams(config)
            val searchUrl = "${config.serverUrl}/rest/search3?query=$encodedQuery&songCount=15&artistCount=0&albumCount=0&$authParams"

            Timber.tag(TAG).d("Querying search endpoint: ${redactSensitiveUrl(searchUrl)}")

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Nocturne/$API_VERSION (Android; Lossless)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Timber.tag(TAG).w("Server search failed with HTTP ${response.code}")
                return null
            }

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val subsonic = json.optJSONObject("subsonic-response") ?: return null
            if (subsonic.optString("status") != "ok") {
                val error = subsonic.optJSONObject("error")?.optString("message") ?: "Unknown error"
                Timber.tag(TAG).w("Subsonic response error: $error")
                return null
            }

            val searchResult = subsonic.optJSONObject("searchResult3") ?: return null
            val songArray = searchResult.optJSONArray("song") ?: return null

            var bestCandidate: ServerSongCandidate? = null
            var highestScore = 0

            for (i in 0 until songArray.length()) {
                val song = songArray.optJSONObject(i) ?: continue
                val songId = song.optString("id")
                if (songId.isBlank()) continue

                val songTitle = song.optString("title")
                val songArtist = song.optString("artist")
                val songAlbum = song.optString("album").ifBlank { null }
                val duration = song.optInt("duration", -1)
                val suffix = song.optString("suffix").lowercase().trim()
                val contentType = song.optString("contentType").lowercase().trim()
                val rawBitrate = song.optInt("bitRate", 0) * 1000 // Subsonic bitRate is in kbps
                val size = song.optLong("size", 0L).takeIf { it > 0 }

                // 1. STRICT LOSSLESS CODEC INSPECTION:
                // Accept only FLAC, ALAC, or explicit lossless formats.
                val isFlac = suffix == "flac" || contentType.contains("flac") || contentType.contains("audio/x-flac")
                val isAlac = suffix == "alac" || (suffix == "m4a" && (contentType.contains("alac") || song.optString("transcodedSuffix") == "alac"))

                if (!isFlac && !isAlac) {
                    Timber.tag(TAG).d("Rejecting '${songTitle}' on server: Format is lossy '$suffix' ($contentType). Not a lossless stream.")
                    continue
                }

                // 2. METADATA MATCH SCORE
                val matchScore = calculateMatchScore(
                    targetTitle = targetTitle,
                    targetArtist = targetArtist,
                    targetDuration = targetDuration,
                    candidateTitle = songTitle,
                    candidateArtist = songArtist,
                    candidateDuration = duration,
                )

                Timber.tag(TAG).d("Evaluated candidate '$songTitle' by '$songArtist' ($suffix, ${duration}s) -> score: $matchScore")

                if (matchScore < 60) {
                    Timber.tag(TAG).d("Candidate score $matchScore is below threshold (60), skipping.")
                    continue
                }

                val detectedCodec = if (isFlac) "flac" else "alac"
                val detectedMime = if (isFlac) "audio/flac" else "audio/mp4"

                // Estimate sample rate and bit depth if not provided by server
                val bitDepth = if (rawBitrate >= 2_000_000) 24 else 16
                val sampleRate = if (rawBitrate >= 3_000_000) 96000 else if (rawBitrate >= 2_000_000) 48000 else 44100

                val candidate = ServerSongCandidate(
                    id = songId,
                    title = songTitle,
                    artist = songArtist,
                    album = songAlbum,
                    duration = duration,
                    suffix = suffix,
                    contentType = contentType,
                    bitrate = rawBitrate.takeIf { it > 0 },
                    bitDepth = bitDepth,
                    sampleRate = sampleRate,
                    size = size,
                    codec = detectedCodec,
                    mimeType = detectedMime,
                    score = matchScore,
                )

                if (matchScore > highestScore) {
                    highestScore = matchScore
                    bestCandidate = candidate
                }
            }

            bestCandidate
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error searching self-hosted server for query: $query")
            null
        }
    }

    /**
     * Calculates a metadata similarity score between 0 and 100.
     */
    fun calculateMatchScore(
        targetTitle: String,
        targetArtist: String,
        targetDuration: Int,
        candidateTitle: String,
        candidateArtist: String,
        candidateDuration: Int,
    ): Int {
        val normTargetTitle = normalizeString(targetTitle)
        val normCandTitle = normalizeString(candidateTitle)
        val normTargetArtist = normalizeString(targetArtist)
        val normCandArtist = normalizeString(candidateArtist)

        var score = 0

        // Title Match (0 to 50 points)
        if (normTargetTitle.equals(normCandTitle, ignoreCase = true)) {
            score += 50
        } else if (normCandTitle.contains(normTargetTitle, ignoreCase = true) || normTargetTitle.contains(normCandTitle, ignoreCase = true)) {
            score += 40
        } else {
            val targetWords = normTargetTitle.split(" ").filter { it.length > 2 }
            val matchCount = targetWords.count { normCandTitle.contains(it, ignoreCase = true) }
            if (targetWords.isNotEmpty() && matchCount > 0) {
                score += (30 * matchCount / targetWords.size)
            }
        }

        // Artist Match (0 to 30 points)
        if (normTargetArtist.isNotBlank() && normCandArtist.isNotBlank()) {
            if (normTargetArtist.equals(normCandArtist, ignoreCase = true)) {
                score += 30
            } else if (normCandArtist.contains(normTargetArtist, ignoreCase = true) || normTargetArtist.contains(normCandArtist, ignoreCase = true)) {
                score += 20
            } else {
                val artistWords = normTargetArtist.split(" ").filter { it.length > 2 }
                val matchCount = artistWords.count { normCandArtist.contains(it, ignoreCase = true) }
                if (artistWords.isNotEmpty() && matchCount > 0) {
                    score += (15 * matchCount / artistWords.size)
                }
            }
        } else {
            // Neutral artist bonus if target has no artist
            score += 15
        }

        // Duration Match (0 to 20 points)
        if (targetDuration > 0 && candidateDuration > 0) {
            val diff = kotlin.math.abs(targetDuration - candidateDuration)
            when {
                diff <= 2 -> score += 20
                diff <= 5 -> score += 15
                diff <= 10 -> score += 10
                diff <= 15 -> score += 5
                else -> score -= 25 // Heavy penalty for large duration mismatch
            }
        } else {
            score += 10
        }

        return score.coerceIn(0, 100)
    }

    /**
     * Builds raw, untranscoded stream URL for Subsonic/Navidrome.
     */
    fun buildStreamUrl(config: ServerConfig, songId: String): String {
        val authParams = buildAuthParams(config)
        // format=raw and estimateContentLength=true instruct server to return the original uncompressed stream
        return "${config.serverUrl}/rest/stream?id=${URLEncoder.encode(songId, "UTF-8")}&format=raw&estimateContentLength=true&$authParams"
    }

    /**
     * Builds Subsonic authentication query string with token and salt.
     */
    fun buildAuthParams(config: ServerConfig): String {
        val salt = UUID.randomUUID().toString().replace("-", "").take(8)
        val token = md5Hex("${config.passwordOrToken}$salt")
        val u = URLEncoder.encode(config.username, "UTF-8")
        return "u=$u&t=$token&s=$salt&v=$API_VERSION&c=$CLIENT_NAME&f=json"
    }

    /**
     * Redacts authentication credentials and tokens from URLs before logging.
     */
    fun redactSensitiveUrl(url: String): String {
        return url
            .replace(Regex("""u=[^&]+"""), "u=REDACTED")
            .replace(Regex("""p=[^&]+"""), "p=REDACTED")
            .replace(Regex("""t=[^&]+"""), "t=REDACTED")
            .replace(Regex("""s=[^&]+"""), "s=REDACTED")
    }

    /**
     * Tests connection to a Subsonic / Navidrome server and verifies authentication.
     */
    suspend fun testConnection(serverUrl: String, username: String, passwordOrToken: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val cleanUrl = serverUrl.trim().trimEnd('/')
            val config = ServerConfig(cleanUrl, username.trim(), passwordOrToken.trim())
            val authParams = buildAuthParams(config)
            val pingUrl = "$cleanUrl/rest/ping?$authParams"

            val request = Request.Builder()
                .url(pingUrl)
                .header("User-Agent", "Nocturne/$API_VERSION (Android; Lossless)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error ${response.code}: ${response.message}"))
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val subsonic = json.optJSONObject("subsonic-response")
                    ?: return@withContext Result.failure(Exception("Invalid Subsonic JSON response"))

                if (subsonic.optString("status") == "ok") {
                    val version = subsonic.optString("version", "unknown")
                    val serverVersion = subsonic.optString("serverVersion", version)
                    Result.success("Connected successfully (Subsonic v$serverVersion)")
                } else {
                    val errorMsg = subsonic.optJSONObject("error")?.optString("message") ?: "Authentication failed"
                    Result.failure(Exception(errorMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanSearchTerm(term: String): String {
        return term
            .replace(Regex("""\(Official.*?\)|\[Official.*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Lyric.*?\)|\[Lyric.*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Audio\)|\[Audio\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Video\)|\[Video\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Music Video\)|\[Music Video\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(HD\)|\[HD\]|\(4K\)|\[4K\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex(""" - Topic$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""VEVO$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun normalizeString(text: String): String {
        return text.lowercase()
            .replace(Regex("""[^\p{Alnum}\s]"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun md5Hex(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
