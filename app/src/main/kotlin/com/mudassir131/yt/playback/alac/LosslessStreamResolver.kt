/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import com.mudassir131.yt.innertube.models.response.PlayerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/**
 * Resolves lossless ALAC audio streams from registered providers and local files.
 * Provides seamless fallback when lossless audio is unavailable.
 */
object LosslessStreamResolver {
    private const val TAG = "LosslessStreamResolver"

    private val providers = CopyOnWriteArrayList<LosslessAudioProvider>()
    private val streamCache = ConcurrentHashMap<String, CachedStream>()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private data class CachedStream(
        val stream: ResolvedLosslessStream,
        val expiresAtMs: Long,
    )

    init {
        // Priority 2: Local FLAC / ALAC files
        registerProvider(LocalAlacFileProvider)
        // Priority 1: Self-Hosted Navidrome / Subsonic Lossless Server
        registerProvider(SelfHostedLosslessAudioProvider)
    }

    fun registerProvider(provider: LosslessAudioProvider) {
        if (!providers.any { it.name == provider.name }) {
            providers.add(0, provider)
            clearCache()
            Timber.tag(TAG).d("Registered LosslessAudioProvider: ${provider.name}")
        }
    }

    fun unregisterProvider(providerName: String) {
        providers.removeAll { it.name == providerName }
        clearCache()
    }

    fun resetDefaultProviders() {
        providers.clear()
        providers.add(SelfHostedLosslessAudioProvider)
        providers.add(LocalAlacFileProvider)
        clearCache()
    }

    var enablePreflightValidation: Boolean = true

    /**
     * Resolves a lossless ALAC audio stream for the given song.
     * Returns null if no lossless source is available or if network check fails.
     */
    suspend fun resolve(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int = -1,
        isMetered: Boolean = false,
    ): ResolvedLosslessResult? = withContext(Dispatchers.IO) {
        if (videoId.isBlank() && title.isBlank()) return@withContext null

        Timber.tag(TAG).i("----------------------------------------------------------------")
        Timber.tag(TAG).i("[LOSSLESS_PIPELINE] Starting Lossless Resolution for: '$title' by '$artist' (id=$videoId, duration=${durationSeconds}s)")
        Timber.tag(TAG).i("[LOSSLESS_PIPELINE] Registered Providers (${providers.size}): [${providers.joinToString { it.name }}]")

        // 1. Check in-memory cache
        val cacheKey = videoId.ifBlank { "$title:$artist" }
        streamCache[cacheKey]?.let { cached ->
            if (cached.expiresAtMs > System.currentTimeMillis()) {
                Timber.tag(TAG).i("[LOSSLESS_PIPELINE] Cache HIT for key '$cacheKey' -> using cached ${cached.stream.codec} stream (${cached.stream.url})")
                return@withContext cached.stream.toResult(videoId)
            } else {
                Timber.tag(TAG).d("[LOSSLESS_PIPELINE] Cache EXPIRED for key '$cacheKey', removing.")
                streamCache.remove(cacheKey)
            }
        }

        // 2. Query registered providers sequentially
        for ((idx, provider) in providers.withIndex()) {
            try {
                Timber.tag(TAG).i("[LOSSLESS_PIPELINE] [${idx + 1}/${providers.size}] Querying provider '${provider.name}'...")
                val candidate = provider.resolve(videoId, title, artist, durationSeconds)
                if (candidate == null) {
                    Timber.tag(TAG).d("[LOSSLESS_PIPELINE] Provider '${provider.name}' returned NO candidate (null).")
                    continue
                }

                // Log candidate details
                Timber.tag(TAG).i("[LOSSLESS_PIPELINE] Provider '${provider.name}' returned candidate:")
                Timber.tag(TAG).i("                    - URL: ${candidate.url}")
                Timber.tag(TAG).i("                    - MIME: ${candidate.mimeType}")
                Timber.tag(TAG).i("                    - CODEC: ${candidate.codec}")
                Timber.tag(TAG).i("                    - BITRATE: ${candidate.bitrate} bps")
                Timber.tag(TAG).i("                    - SAMPLE RATE: ${candidate.sampleRate} Hz")
                Timber.tag(TAG).i("                    - BIT DEPTH: ${candidate.bitDepth}-bit")
                Timber.tag(TAG).i("                    - SOURCE NAME: ${candidate.sourceName}")

                // Strict codec validation: only accept genuinely lossless audio (ALAC or FLAC)
                val isLossless = isGenuinelyLossless(candidate)
                Timber.tag(TAG).i("[CODEC_GATE] isGenuinelyLossless(candidate) evaluated to: $isLossless")

                if (!isLossless) {
                    val rejectionReason = if (candidate.codec.contains("mp4a", ignoreCase = true) || candidate.codec.contains("aac", ignoreCase = true)) {
                        "Lossy AAC (codec=${candidate.codec}, bitrate=${candidate.bitrate}bps). AAC 320 kbps in MP4 container is NOT lossless audio."
                    } else if (candidate.codec.contains("opus", ignoreCase = true)) {
                        "Lossy Opus stream. Cannot be treated as ALAC/lossless."
                    } else if (candidate.codec.contains("mp3", ignoreCase = true)) {
                        "Lossy MP3 stream. Cannot be treated as ALAC/lossless."
                    } else {
                        "Codec '${candidate.codec}' (mime='${candidate.mimeType}') is not verified ALAC or FLAC."
                    }
                    Timber.tag(TAG).w("[CODEC_GATE] REJECTED candidate from '${provider.name}': $rejectionReason")
                    continue
                }

                // Preflight validation for remote streams
                if (enablePreflightValidation && (candidate.url.startsWith("http://", ignoreCase = true) ||
                    candidate.url.startsWith("https://", ignoreCase = true))
                ) {
                    val isReachable = validateRemoteUrl(candidate.url)
                    Timber.tag(TAG).i("[LOSSLESS_PIPELINE] Preflight reachability check for '${candidate.url}' -> $isReachable")
                    if (!isReachable) {
                        Timber.tag(TAG).w("[LOSSLESS_PIPELINE] REJECTED candidate: Preflight HTTP HEAD failed for ${candidate.url}")
                        continue
                    }
                }

                // Cache verified result
                val expiresAtMs = System.currentTimeMillis() + (candidate.expiresInSeconds * 1000L)
                streamCache[cacheKey] = CachedStream(candidate, expiresAtMs)

                Timber.tag(TAG).i("[CODEC_GATE] -> ACCEPTED: Genuinely lossless ${candidate.codec.uppercase()} stream validated from '${provider.name}'")
                Timber.tag(TAG).i("----------------------------------------------------------------")
                return@withContext candidate.toResult(videoId)
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "[LOSSLESS_PIPELINE] Error querying provider '${provider.name}'")
            }
        }

        Timber.tag(TAG).w("[LOSSLESS_FALLBACK] NO genuine ALAC or FLAC lossless source exists across all ${providers.size} providers for '$title' ($videoId)")
        Timber.tag(TAG).i("----------------------------------------------------------------")
        null
    }

    /**
     * Strictly verifies whether a candidate stream is genuinely lossless.
     * Rejects AAC (mp4a), Opus, MP3, Vorbis, etc.
     */
    fun isGenuinelyLossless(stream: ResolvedLosslessStream): Boolean {
        val codec = stream.codec.lowercase().trim()
        val mime = stream.mimeType.lowercase().trim()

        // Explicitly reject known lossy codecs
        if (codec.contains("mp4a") || codec.contains("aac") || codec.contains("opus") || codec.contains("mp3")) {
            return false
        }

        return codec == "alac" ||
                codec.startsWith("alac") ||
                codec == "flac" ||
                codec.startsWith("flac") ||
                mime == "audio/alac" ||
                mime == "audio/flac" ||
                mime == "audio/x-flac"
    }

    /**
     * Manually inject a verified lossless stream for testing or explicit local mapping.
     */
    fun cacheExplicitStream(videoId: String, stream: ResolvedLosslessStream) {
        val expiresAtMs = System.currentTimeMillis() + (stream.expiresInSeconds * 1000L)
        streamCache[videoId] = CachedStream(stream, expiresAtMs)
    }

    fun clearCache() {
        streamCache.clear()
    }

    private fun validateRemoteUrl(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .header("User-Agent", "Nocturne/1.0 (Android; ALAC)")
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful || response.code in 200..308
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun ResolvedLosslessStream.toResult(videoId: String): ResolvedLosslessResult {
        val approxDuration = durationSeconds?.takeIf { it > 0 }?.let { (it * 1000L).toString() }
        val format = PlayerResponse.StreamingData.Format(
            itag = 999,
            url = url,
            mimeType = "$mimeType; codecs=\"$codec\"",
            bitrate = bitrate ?: 0,
            width = null,
            height = null,
            contentLength = contentLength,
            quality = "hd1440",
            fps = null,
            qualityLabel = null,
            averageBitrate = bitrate,
            audioQuality = "AUDIO_QUALITY_HIGH",
            approxDurationMs = approxDuration,
            audioSampleRate = sampleRate,
            audioChannels = channels,
            loudnessDb = null,
            lastModified = System.currentTimeMillis(),
            signatureCipher = null,
            cipher = null,
        )
        return ResolvedLosslessResult(
            format = format,
            streamUrl = url,
            expiresInSeconds = expiresInSeconds,
            source = sourceName,
        )
    }
}

/**
 * Result payload containing the streaming format and URL.
 */
data class ResolvedLosslessResult(
    val format: PlayerResponse.StreamingData.Format,
    val streamUrl: String,
    val expiresInSeconds: Int,
    val source: String,
)

/**
 * Default local lossless file provider that checks for local .flac / .m4a ALAC files.
 */
object LocalAlacFileProvider : LosslessAudioProvider {
    override val name: String = "LocalLosslessFileProvider"
    private const val TAG = "LocalLossless"

    override suspend fun resolve(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int,
    ): ResolvedLosslessStream? {
        Timber.tag(TAG).d("[LOCAL] Checking local library for: '$title' by '$artist' (videoId=$videoId)")

        if (videoId.startsWith("file://") || videoId.startsWith("/") || videoId.startsWith("content://")) {
            val path = videoId.removePrefix("file://")
            val file = File(path)
            if (file.exists() && file.isFile) {
                val ext = file.extension.lowercase()
                if (ext == "flac") {
                    Timber.tag(TAG).i("[LOCAL] Found local FLAC file: ${file.absolutePath}")
                    return ResolvedLosslessStream(
                        url = file.toURI().toString(),
                        mimeType = "audio/flac",
                        codec = "flac",
                        bitDepth = 16,
                        sampleRate = 44100,
                        contentLength = file.length(),
                        durationSeconds = durationSeconds,
                        sourceName = "local_flac",
                    )
                } else if (ext == "m4a" || ext == "alac") {
                    Timber.tag(TAG).i("[LOCAL] Found local ALAC file: ${file.absolutePath}")
                    return ResolvedLosslessStream(
                        url = file.toURI().toString(),
                        mimeType = "audio/mp4",
                        codec = "alac",
                        bitDepth = 16,
                        sampleRate = 44100,
                        contentLength = file.length(),
                        durationSeconds = durationSeconds,
                        sourceName = "local_alac",
                    )
                }
            }
        }
        Timber.tag(TAG).d("[LOCAL] No local FLAC/ALAC file match for '$title' ($videoId)")
        return null
    }
}
