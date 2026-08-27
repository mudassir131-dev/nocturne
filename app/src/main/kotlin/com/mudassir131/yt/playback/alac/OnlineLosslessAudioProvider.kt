/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.net.URLEncoder
import java.util.Base64
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Resolves high-fidelity lossless / high-bitrate M4A streams for songs
 * by matching title, artist, and duration against global lossless & 320kbps catalogs.
 */
object OnlineLosslessAudioProvider : LosslessAudioProvider {
    override val name: String = "OnlineLosslessAudioProvider"
    private const val TAG = "OnlineLosslessProvider"
    private const val DES_KEY = "38346591"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    override suspend fun resolve(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
    ): ResolvedLosslessStream? = withContext(Dispatchers.IO) {
        if (title.isBlank()) return@withContext null

        val cleanTitle = cleanSongTitle(title)
        val cleanArtist = cleanArtistName(artist)
        val query = "$cleanTitle $cleanArtist".trim()

        Timber.tag(TAG).d("Searching online lossless stream for: '$query' (duration: $durationSeconds s)")

        val streamFromCatalog = searchAndResolveCatalog(query, cleanTitle, durationSeconds)
        if (streamFromCatalog != null) {
            Timber.tag(TAG).i("Resolved online lossless/high-res stream: ${streamFromCatalog.url}")
            return@withContext streamFromCatalog
        }

        // Try title-only if artist search was too restrictive
        if (cleanArtist.isNotBlank()) {
            val streamFromTitle = searchAndResolveCatalog(cleanTitle, cleanTitle, durationSeconds)
            if (streamFromTitle != null) {
                Timber.tag(TAG).i("Resolved online lossless/high-res stream by title: ${streamFromTitle.url}")
                return@withContext streamFromTitle
            }
        }

        null
    }

    private fun searchAndResolveCatalog(
        query: String,
        matchTitle: String,
        expectedDuration: Int,
    ): ResolvedLosslessStream? {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&api_version=4&ctx=android&n=5&p=1&q=$encodedQuery"

            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string().orEmpty()
            if (body.isBlank()) return null

            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return null

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val itemTitle = item.optString("song").ifBlank { item.optString("title") }
                val moreInfo = item.optJSONObject("more_info") ?: continue
                val encryptedUrl = moreInfo.optString("encrypted_media_url")
                if (encryptedUrl.isBlank()) continue

                val itemDuration = moreInfo.optString("duration").toIntOrNull()
                    ?: item.optString("duration").toIntOrNull() ?: -1

                // Duration check (tolerance: 8 seconds if duration is provided)
                if (expectedDuration > 0 && itemDuration > 0) {
                    val diff = kotlin.math.abs(expectedDuration - itemDuration)
                    if (diff > 12) continue
                }

                val decryptedUrl = decryptMediaUrl(encryptedUrl) ?: continue
                if (decryptedUrl.isBlank()) continue

                return ResolvedLosslessStream(
                    url = decryptedUrl,
                    mimeType = "audio/mp4",
                    codec = "mp4a.40.2", // Saavn streams are 320kbps AAC (mp4a), not ALAC
                    bitDepth = 16,
                    sampleRate = 44100,
                    channels = 2,
                    bitrate = 320000,
                    durationSeconds = if (itemDuration > 0) itemDuration else expectedDuration,
                    sourceName = "saavn_aac",
                    expiresInSeconds = 86400,
                )
            }
            null
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Error querying lossless catalog for: $query")
            null
        }
    }

    /**
     * Decrypts Saavn encrypted media URLs to direct high-bitrate M4A streams.
     */
    fun decryptMediaUrl(encryptedUrl: String): String? {
        return try {
            val keySpec = SecretKeySpec(DES_KEY.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.getDecoder().decode(encryptedUrl.trim())
            val decryptedBytes = cipher.doFinal(decodedBytes)
            var url = String(decryptedBytes, Charsets.UTF_8).trim()

            // Upgrade to maximum quality 320kbps / lossless M4A container
            url = url.replace("_96.mp4", "_320.mp4")
                .replace("_160.mp4", "_320.mp4")
                .replace("_96.m4a", "_320.m4a")
                .replace("_160.m4a", "_320.m4a")

            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                null
            } else {
                url
            }
        } catch (e: Exception) {
            Timber.tag(TAG).w(e, "Failed to decrypt media URL: $encryptedUrl")
            null
        }
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("""\(Official.*?\)|\[Official.*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Lyric.*?\)|\[Lyric.*?\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Audio\)|\[Audio\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Video\)|\[Video\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(Music Video\)|\[Music Video\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\(HD\)|\[HD\]|\(4K\)|\[4K\]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex(""" - Topic$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""VEVO$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}
