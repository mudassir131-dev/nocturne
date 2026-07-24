/*
 * Echo Music lyrics provider architecture adapted for Nocturne (GPL-3.0).
 * Timing parsing is intentionally strict: no estimated word timestamps are created.
 */
package com.mudassir131.yt.ui.appleplayer.lyrics

import android.content.Context
import android.util.LruCache
import com.mudassir131.yt.lyrics.LyricsProviderRegistry
import com.mudassir131.yt.lyrics.YouTubeLyricsProvider
import com.mudassir131.yt.lyrics.YouTubeSubtitleLyricsProvider
import com.mudassir131.yt.models.MediaMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

/** Uses the same persisted provider registry and precedence as the standard player. */
object AppleLyricsPipeline {
    val providerNames = LyricsProviderRegistry.configurableProviders.map { it.name }
    private val cache = LruCache<String, AppleLyricsResult>(8)

    suspend fun resolve(context: Context, metadata: MediaMetadata): AppleLyricsResult? {
        cache.get(metadata.id)?.let { return it }
        val providers = LyricsProviderRegistry.orderedProviders(context) +
            listOf(YouTubeSubtitleLyricsProvider, YouTubeLyricsProvider)
        for (provider in providers) {
            try {
                if (!provider.isEnabled(context)) continue
                val raw = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                    provider.getLyrics(
                        id = metadata.id,
                        title = metadata.title,
                        artist = metadata.artists.joinToString { it.name },
                        album = metadata.album?.title,
                        duration = metadata.duration,
                    ).getOrNull()?.takeIf { it.isNotBlank() }
                } ?: continue
                val result = AppleLyricsResult(
                    provider = provider.name,
                    raw = raw,
                    lines = AppleLyricsTimingParser.parse(raw),
                )
                if (result.lines.isNotEmpty()) {
                    cache.put(metadata.id, result)
                    return result
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Deterministically advance to the next configured provider.
            }
        }
        return null
    }

    private const val PROVIDER_TIMEOUT_MS = 12_000L
}
