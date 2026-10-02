/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import android.content.Context
import com.mudassir131.yt.App
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object MusicVideoCache {
    private const val PREFS_NAME = "nocturne_music_video_metadata_cache_v3"
    private const val SENTINEL_NONE = "__NONE__"
    private val NEGATIVE_CACHE_TTL_MS = TimeUnit.MINUTES.toMillis(3) // 3 minutes for negative results (in-memory only)
    private val POSITIVE_CACHE_TTL_MS = TimeUnit.DAYS.toMillis(14) // 14 days for resolved video IDs

    data class CachedMetadata(
        val videoId: String?,
        val title: String?,
        val artist: String?,
        val thumbnailUrl: String?,
        val durationMs: Long?,
        val timestamp: Long,
    ) {
        val isExpired: Boolean
            get() {
                val ttl = if (videoId == null) NEGATIVE_CACHE_TTL_MS else POSITIVE_CACHE_TTL_MS
                return (System.currentTimeMillis() - timestamp) > ttl
            }
    }

    data class CachedStream(
        val streamUrl: String,
        val qualityLabel: String?,
        val itag: Int?,
        val expiresAtMs: Long,
        val availableQualities: List<VideoQualityOption>,
    ) {
        val isValid: Boolean get() = System.currentTimeMillis() < (expiresAtMs - 30_000L) // 30s buffer
    }

    private val memoryMetadataCache = ConcurrentHashMap<String, CachedMetadata>()
    private val memoryStreamCache = ConcurrentHashMap<String, CachedStream>()

    private val prefs by lazy {
        runCatching {
            val app = App.instance
            val sp = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val toRemove = sp.all.filter { (_, v) -> (v as? String)?.startsWith(SENTINEL_NONE) == true }.keys
            if (toRemove.isNotEmpty()) {
                val edit = sp.edit()
                toRemove.forEach { edit.remove(it) }
                edit.apply()
            }
            sp
        }.getOrNull()
    }

    fun getMetadata(trackId: String): CachedMetadata? {
        // 1. Check memory
        memoryMetadataCache[trackId]?.let {
            if (!it.isExpired) return it
            memoryMetadataCache.remove(trackId)
        }

        // 2. Check persistent SharedPreferences (positive cache only)
        val sp = prefs ?: return null
        val raw = sp.getString(trackId, null) ?: return null
        val parts = raw.split("||")
        if (parts.size >= 6) {
            val videoId = parts[0].takeIf { it != SENTINEL_NONE }
            if (videoId == null) {
                sp.edit().remove(trackId).apply()
                return null
            }
            val title = parts[1].takeIf { it.isNotBlank() }
            val artist = parts[2].takeIf { it.isNotBlank() }
            val thumb = parts[3].takeIf { it.isNotBlank() }
            val durationMs = parts[4].toLongOrNull()
            val timestamp = parts[5].toLongOrNull() ?: System.currentTimeMillis()

            val entry = CachedMetadata(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumb,
                durationMs = durationMs,
                timestamp = timestamp,
            )

            if (!entry.isExpired) {
                memoryMetadataCache[trackId] = entry
                return entry
            } else {
                sp.edit().remove(trackId).apply()
            }
        }
        return null
    }

    fun putMetadata(
        trackId: String,
        videoId: String?,
        title: String? = null,
        artist: String? = null,
        thumbnailUrl: String? = null,
        durationMs: Long? = null,
    ) {
        val entry = CachedMetadata(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            durationMs = durationMs,
            timestamp = System.currentTimeMillis(),
        )
        // Store in memory (expires quickly if videoId == null)
        memoryMetadataCache[trackId] = entry

        // NEVER persist negative results to disk; only persist positive hits!
        if (videoId != null) {
            val serialized = listOf(
                videoId,
                title.orEmpty(),
                artist.orEmpty(),
                thumbnailUrl.orEmpty(),
                durationMs?.toString().orEmpty(),
                entry.timestamp.toString(),
            ).joinToString("||")

            prefs?.edit()?.putString(trackId, serialized)?.apply()
        }
    }

    fun getStream(videoId: String, qualityTag: String = "AUTO"): CachedStream? {
        val key = "$videoId:$qualityTag"
        val cached = memoryStreamCache[key] ?: return null
        return if (cached.isValid) cached else {
            memoryStreamCache.remove(key)
            null
        }
    }

    fun putStream(
        videoId: String,
        qualityTag: String = "AUTO",
        streamUrl: String,
        qualityLabel: String?,
        itag: Int?,
        expiresInSeconds: Int,
        availableQualities: List<VideoQualityOption>,
    ) {
        val key = "$videoId:$qualityTag"
        val expiresAtMs = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        memoryStreamCache[key] = CachedStream(
            streamUrl = streamUrl,
            qualityLabel = qualityLabel,
            itag = itag,
            expiresAtMs = expiresAtMs,
            availableQualities = availableQualities,
        )
    }

    fun clearAll() {
        memoryMetadataCache.clear()
        memoryStreamCache.clear()
        prefs?.edit()?.clear()?.apply()
    }
}
