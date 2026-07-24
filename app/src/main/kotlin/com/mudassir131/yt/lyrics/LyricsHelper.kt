/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.lyrics

import android.content.Context
import android.util.Log
import android.util.LruCache
import com.mudassir131.yt.utils.GlobalLog
import com.mudassir131.yt.db.entities.LyricsEntity.Companion.LYRICS_NOT_FOUND
import com.mudassir131.yt.models.MediaMetadata
import com.mudassir131.yt.utils.reportException
import com.mudassir131.yt.utils.NetworkConnectivityObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.async
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class LyricsHelper
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val networkConnectivity: NetworkConnectivityObserver,
) {
    private val systemFallbackProviders = listOf(
        YouTubeSubtitleLyricsProvider,
        YouTubeLyricsProvider,
    )

    private val cache = LruCache<String, List<LyricsResult>>(MAX_CACHE_SIZE)
    private var currentLyricsJob: Job? = null

    suspend fun getLyrics(mediaMetadata: MediaMetadata, preferredProviderOnly: Boolean = false): String {
        val artists = mediaMetadata.artists.joinToString { it.name }
        val cacheKey = cacheKey(
            mediaMetadata.id,
            mediaMetadata.title,
            artists,
            mediaMetadata.album?.title,
            mediaMetadata.duration,
        )
        val cached = cache.get(cacheKey)?.firstOrNull()
        if (cached != null) {
            GlobalLog.append(Log.DEBUG, "LyricsHelper", "Found lyrics in cache for ${mediaMetadata.title}")
            return cached.lyrics
        }
        
        GlobalLog.append(Log.DEBUG, "LyricsHelper", "Fetching lyrics for ${mediaMetadata.title} (Artist: ${mediaMetadata.artists.joinToString { it.name }}, Album: ${mediaMetadata.album?.title})")

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            GlobalLog.append(Log.WARN, "LyricsHelper", "Network unavailable, aborting lyrics fetch")
            return LYRICS_NOT_FOUND
        }

        val enabledProviders = orderedProviders().filter { it.isEnabled(context) }
        val providers = if (preferredProviderOnly) enabledProviders.take(1) else enabledProviders
        return withContext(Dispatchers.IO) {
            for (provider in providers) {
                try {
                    val result = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                        provider.getLyrics(
                            mediaMetadata.id,
                            mediaMetadata.title,
                            artists,
                            mediaMetadata.album?.title,
                            mediaMetadata.duration,
                        )
                    } ?: Result.failure(IllegalStateException("${provider.name} timed out"))

                    val lyrics = result.getOrNull()
                    if (lyrics != null && isMeaningfulLyrics(lyrics)) {
                        cache.put(cacheKey, listOf(LyricsResult(provider.name, lyrics)))
                        return@withContext lyrics
                    }
                    result.exceptionOrNull()?.let(::reportException)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    reportException(error)
                }
            }
            LYRICS_NOT_FOUND
        }
    }

    suspend fun getAllLyrics(
        mediaId: String,
        songTitle: String,
        songArtists: String,
        songAlbum: String?,
        duration: Int,
        callback: (LyricsResult) -> Unit,
    ) {
        currentLyricsJob?.cancel()

        val cacheKey = cacheKey(mediaId, songTitle, songArtists, songAlbum, duration)
        cache.get(cacheKey)?.let { results ->
            results.forEach {
                callback(it)
            }
            return
        }

        val isNetworkAvailable = try {
            networkConnectivity.isCurrentlyConnected()
        } catch (e: Exception) {
            true
        }
        
        if (!isNetworkAvailable) {
            return
        }

        val allResult = mutableListOf<LyricsResult>()
        val providers = orderedProviders()
        currentLyricsJob = CoroutineScope(SupervisorJob() + Dispatchers.IO).async {
            providers.forEach { provider ->
                if (provider.isEnabled(context)) {
                    try {
                        withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                            provider.getAllLyrics(mediaId, songTitle, songArtists, songAlbum, duration) lyricsCallback@{ lyrics ->
                                if (!isMeaningfulLyrics(lyrics)) return@lyricsCallback
                                val result = LyricsResult(provider.name, lyrics)
                                if (allResult.none { it.providerName == result.providerName && it.lyrics == result.lyrics }) {
                                    allResult += result
                                    callback(result)
                                }
                            }
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Exception) {
                        reportException(error)
                    }
                }
            }
            cache.put(cacheKey, allResult)
        }

        currentLyricsJob?.join()
    }

    private suspend fun orderedProviders(): List<LyricsProvider> {
        return LyricsProviderRegistry.orderedProviders(context) + systemFallbackProviders
    }

    private fun cacheKey(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): String =
        id.takeIf { it.isNotBlank() }
            ?: listOf(artist, title, album.orEmpty(), duration.toString())
                .joinToString("|") { it.lowercase().trim() }

    private fun isMeaningfulLyrics(lyrics: String): Boolean {
        val normalized =
            lyrics
                .replace("\uFEFF", "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        if (normalized.isEmpty()) return false
        if (normalized == LYRICS_NOT_FOUND) return false

        val remaining =
            TIMESTAMP_REGEX
                .replace(normalized, "")
                .replace(INVISIBLE_CHARS_REGEX, "")
                .trim { it.isWhitespace() || it == '\u00A0' }

        return remaining.any { !it.isWhitespace() && it != '\u00A0' }
    }

    fun cancelCurrentLyricsJob() {
        currentLyricsJob?.cancel()
        currentLyricsJob = null
    }

    companion object {
        private const val MAX_CACHE_SIZE = 3
        private const val PROVIDER_TIMEOUT_MS = 12_000L
        private val TIMESTAMP_REGEX = Regex("""\[[0-9]{1,2}:[0-9]{2}(?:\.[0-9]{1,3})?]""")
        private val INVISIBLE_CHARS_REGEX = Regex("""[\u200B\u200C\u200D\u2060\u00AD]""")
    }
}

data class LyricsResult(
    val providerName: String,
    val lyrics: String,
)
