/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.utils

import android.net.ConnectivityManager
import androidx.media3.common.PlaybackException
import com.mudassir131.yt.constants.AudioQuality
import com.mudassir131.yt.constants.PlayerStreamClient
import com.mudassir131.yt.innertube.pages.NewPipeUtils
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.YouTubeClient
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.IOS
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.mudassir131.yt.innertube.models.response.PlayerResponse
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.ANDROID_MUSIC
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.ANDROID_TESTSUITE
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.ANDROID_UNPLUGGED
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.IPADOS
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.IOS_MUSIC
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.MOBILE
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.TVHTML5
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.VISIONOS
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.WEB
import com.mudassir131.yt.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import com.mudassir131.yt.playback.alac.LosslessStreamResolver
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.AudioStream

object YTPlayerUtils {
    private const val logTag = "YTPlayerUtils"
    private const val FAILED_CLIENT_BACKOFF_MS = 60 * 1000L // 1 minute scoped backoff

    @Volatile
    private var cachedSignatureTimestamp: Int? = null

    @Volatile private var streamClientPair: Pair<java.net.Proxy?, OkHttpClient>? = null

    private val inFlightResolutions = ConcurrentHashMap<String, kotlinx.coroutines.Deferred<Result<PlaybackData>>>()

    private fun currentStreamClient(): OkHttpClient {
        val current = YouTube.streamProxy
        streamClientPair?.let { (proxy, client) ->
            if (proxy == current) return client
        }
        val client = OkHttpClient.Builder()
            .proxy(current)
            .build()
        streamClientPair = current to client
        return client
    }
    /**
     * The main client is used for metadata and initial streams.
     * Do not use other clients for this because it can result in inconsistent metadata.
     * For example other clients can have different normalization targets (loudnessDb).
     *
     * [com.mudassir131.yt.innertube.models.YouTubeClient.WEB_REMIX] should be preferred here because currently it is the only client which provides:
     * - the correct metadata (like loudnessDb)
     * - premium formats
     */
    private val MAIN_CLIENT: YouTubeClient = WEB_REMIX
    /**
     * Clients used for fallback streams in case the streams of the main client do not work.
     */
    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        MOBILE,
        ANDROID_MUSIC,
        ANDROID_VR_NO_AUTH,
        IOS,
        IOS_MUSIC,
        TVHTML5_SIMPLY_EMBEDDED_PLAYER,
        ANDROID_VR_1_61_48,
        ANDROID_VR_1_43_32,
        ANDROID_CREATOR,
        ANDROID_TESTSUITE,
        ANDROID_UNPLUGGED,
        IPADOS,
        VISIONOS,
        TVHTML5,
        WEB,
        WEB_CREATOR,
        WEB_REMIX
    )
    private data class CachedStreamUrl(
        val url: String,
        val expiresAtMs: Long,
    )

    private val streamUrlCache = ConcurrentHashMap<String, CachedStreamUrl>()
    private val failedStreamClientsUntil = ConcurrentHashMap<String, Long>()

    fun invalidateCachedStreamUrls(videoId: String) {
        val prefix = "$videoId:"
        streamUrlCache.keys.removeIf { it.startsWith(prefix) }
    }

    fun invalidateCachedStreamUrl(videoId: String, itag: Int) {
        val key = buildCacheKey(videoId, itag)
        streamUrlCache.remove(key)
    }

    fun markStreamClientFailed(videoId: String, clientKey: String?, httpStatusCode: Int?) {
        if (httpStatusCode != 403 && httpStatusCode != 429) return
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return
        val scopedKey = buildFailedClientKey(videoId, normalizedClientKey)
        failedStreamClientsUntil[scopedKey] =
            System.currentTimeMillis() + FAILED_CLIENT_BACKOFF_MS
    }

    private fun isStreamClientTemporarilyBlocked(videoId: String, clientKey: String?): Boolean {
        val normalizedClientKey = normalizeStreamClientKey(clientKey)
        if (normalizedClientKey.isEmpty()) return false

        val scopedKey = buildFailedClientKey(videoId, normalizedClientKey)
        val until = failedStreamClientsUntil[scopedKey] ?: return false
        if (until <= System.currentTimeMillis()) {
            failedStreamClientsUntil.remove(scopedKey)
            return false
        }
        return true
    }

    fun markPreferredClientFailed(videoId: String, client: PlayerStreamClient, httpStatusCode: Int?) {
        markStreamClientFailed(videoId, client.name, httpStatusCode)
    }
    private fun normalizeStreamClientKey(clientKey: String?): String {
        return clientKey?.trim()?.takeIf { it.isNotBlank() }?.uppercase(Locale.US).orEmpty()
    }

    private fun buildFailedClientKey(videoId: String, clientKey: String): String {
        return "$videoId:${normalizeStreamClientKey(clientKey)}"
    }

    data class PlaybackData(
        val audioConfig: PlayerResponse.PlayerConfig.AudioConfig?,
        val videoDetails: PlayerResponse.VideoDetails?,
        val playbackTracking: PlayerResponse.PlaybackTracking?,
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )
    /**
     * Custom player response intended to use for playback with in-flight request deduplication.
     */
    suspend fun playerResponseForPlayback(
        videoId: String,
        playlistId: String? = null,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        preferredStreamClient: PlayerStreamClient = PlayerStreamClient.ANDROID_VR,
        // if provided, this preference overrides ConnectivityManager.isActiveNetworkMetered
        networkMetered: Boolean? = null,
        avoidCodecs: Set<String> = emptySet(),
        dataSaver: Boolean = false,
        trackTitle: String? = null,
        trackArtist: String? = null,
        trackDuration: Int? = null,
    ): Result<PlaybackData> = coroutineScope {
        val dedupeKey = "$videoId:$audioQuality:${preferredStreamClient.name}:$dataSaver"
        
        var isOriginator = false
        val deferred = inFlightResolutions.computeIfAbsent(dedupeKey) {
            isOriginator = true
            async(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                    playerResponseForPlaybackOnce(
                        videoId = videoId,
                        playlistId = playlistId,
                        audioQuality = audioQuality,
                        connectivityManager = connectivityManager,
                        preferredStreamClient = preferredStreamClient,
                        networkMetered = networkMetered,
                        avoidCodecs = avoidCodecs,
                        dataSaver = dataSaver,
                        trackTitle = trackTitle,
                        trackArtist = trackArtist,
                        trackDuration = trackDuration,
                    )
                }
            }
        }

        try {
            deferred.await()
        } finally {
            if (isOriginator) {
                inFlightResolutions.remove(dedupeKey)
            }
        }
    }

    private suspend fun playerResponseForPlaybackOnce(
        videoId: String,
        playlistId: String?,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        preferredStreamClient: PlayerStreamClient,
        networkMetered: Boolean?,
        avoidCodecs: Set<String>,
        dataSaver: Boolean = false,
        trackTitle: String? = null,
        trackArtist: String? = null,
        trackDuration: Int? = null,
    ): PlaybackData {
        Timber.tag(logTag).i("Fetching player response for videoId: $videoId, playlistId: $playlistId")
        val signatureTimestamp = getSignatureTimestampOrNull(videoId)
        Timber.tag(logTag).v("Signature timestamp: $signatureTimestamp")

        val isLoggedIn = YouTube.cookie != null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        Timber.tag(logTag).v("Session authentication status: ${if (isLoggedIn) "Logged in" else "Not logged in"} (sessionId=${sessionId.orEmpty()})")

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var streamPlayerResponse: PlayerResponse? = null

        val orderedFallbackClients =
            (
                    if (isLoggedIn) {
                        STREAM_FALLBACK_CLIENTS.filter { it.loginSupported } + STREAM_FALLBACK_CLIENTS.filterNot { it.loginSupported }
                    } else {
                        STREAM_FALLBACK_CLIENTS.toList()
                    }
                    ).distinct()

        val preferredYouTubeClient =
            when (preferredStreamClient) {
                PlayerStreamClient.ANDROID_VR -> if (audioQuality == AudioQuality.OPUS) ANDROID_MUSIC else IOS
                PlayerStreamClient.WEB_REMIX -> WEB_REMIX
                PlayerStreamClient.IOS -> if (audioQuality == AudioQuality.OPUS) ANDROID_MUSIC else IOS
                PlayerStreamClient.MOBILE -> ANDROID_MUSIC
                PlayerStreamClient.TVHTML5 -> TVHTML5_SIMPLY_EMBEDDED_PLAYER
                PlayerStreamClient.ANDROID_MUSIC -> ANDROID_MUSIC
            }

        val metadataClient =
            preferredYouTubeClient.takeIf { preferredStreamClient == PlayerStreamClient.ANDROID_VR } ?: MAIN_CLIENT

        Timber.tag(logTag).i("Fetching metadata and preferred stream responses in parallel using clients: ${metadataClient.clientName} and ${preferredYouTubeClient.clientName}")
        val (metadataPlayerResponse, preferredPlayerResponse) = coroutineScope {
            val metadataDeferred = async {
                runCatching {
                    YouTube.player(videoId, playlistId, metadataClient, signatureTimestamp).getOrThrow()
                }.getOrNull()
            }
            val preferredDeferred = if (preferredYouTubeClient != metadataClient) {
                async {
                    runCatching {
                        YouTube.player(videoId, playlistId, preferredYouTubeClient, signatureTimestamp).getOrThrow()
                    }.getOrNull()
                }
            } else null

            metadataDeferred.await() to preferredDeferred?.await()
        }

        val audioConfig = metadataPlayerResponse?.playerConfig?.audioConfig
        val videoDetails = metadataPlayerResponse?.videoDetails
        val playbackTracking = metadataPlayerResponse?.playbackTracking
        val expectedDurationMs = videoDetails?.lengthSeconds?.toLongOrNull()?.takeIf { it > 0 }?.times(1000L)

        if ((audioQuality == AudioQuality.LOSSLESS || audioQuality == AudioQuality.SAAVN) && !dataSaver) {
            val title = videoDetails?.title?.takeIf { it.isNotBlank() } ?: trackTitle.orEmpty()
            val artist = videoDetails?.author?.takeIf { it.isNotBlank() } ?: trackArtist.orEmpty()
            val durationSecs = videoDetails?.lengthSeconds?.toIntOrNull() ?: trackDuration ?: -1
            Timber.tag(logTag).i("[LOSSLESS_PIPELINE] ========================================================")
            Timber.tag(logTag).i("[LOSSLESS_PIPELINE] User preference: $audioQuality")
            Timber.tag(logTag).i("[LOSSLESS_PIPELINE] Target Track: '$title' by '$artist' (videoId=$videoId, duration=${durationSecs}s)")
            
            val losslessResult = LosslessStreamResolver.resolve(
                videoId = videoId,
                title = title,
                artist = artist,
                durationSeconds = durationSecs,
                isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered,
                allowHighResLossy = true,
            )
            if (losslessResult != null) {
                Timber.tag(logTag).i("[LOSSLESS_PIPELINE] -> SUCCESS: Stream resolved: ${losslessResult.streamUrl} (source=${losslessResult.source})")
                Timber.tag(logTag).i("[LOSSLESS_PIPELINE] ========================================================")
                return PlaybackData(
                    audioConfig = audioConfig,
                    videoDetails = videoDetails,
                    playbackTracking = playbackTracking,
                    format = losslessResult.format,
                    streamUrl = losslessResult.streamUrl,
                    streamExpiresInSeconds = losslessResult.expiresInSeconds,
                )
            } else {
                Timber.tag(logTag).w("[LOSSLESS_PIPELINE] -> FALLBACK TRIGGERED: No external catalog source available for videoId=$videoId ('$title' by '$artist').")
                Timber.tag(logTag).w("[LOSSLESS_PIPELINE] -> Gracefully routing to standard YouTube stream pipeline.")
                Timber.tag(logTag).i("[LOSSLESS_PIPELINE] ========================================================")
            }
        }

        // 1. Primary Direct High-Fidelity Audio Stream Resolution via NewPipe
        // Bypasses YouTube bot-guard / signature blocks on InnerTube clients and extracts genuine Opus & AAC streams
        val newPipeAudio = resolveAudioWithNewPipe(
            videoId = videoId,
            audioQuality = audioQuality,
            isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered,
            dataSaver = dataSaver,
            avoidCodecs = avoidCodecs,
        )
        if (newPipeAudio != null) {
            val (audioFormat, audioUrl, extractedDetails) = newPipeAudio
            val effectiveVideoDetails = videoDetails ?: extractedDetails
            Timber.tag(logTag).i("Direct YouTube audio stream resolved via NewPipe: itag=${audioFormat.itag}, mime=${audioFormat.mimeType} for videoId=$videoId")
            streamUrlCache[buildCacheKey(videoId, audioFormat.itag)] =
                CachedStreamUrl(
                    url = audioUrl,
                    expiresAtMs = System.currentTimeMillis() + (21600 * 1000L),
                )
            return PlaybackData(
                audioConfig = audioConfig,
                videoDetails = effectiveVideoDetails,
                playbackTracking = playbackTracking,
                format = audioFormat,
                streamUrl = audioUrl,
                streamExpiresInSeconds = 21600,
            )
        }

        val streamClients =
            buildList {
                if (audioQuality == AudioQuality.OPUS) {
                    add(if (preferredYouTubeClient == IOS) ANDROID_MUSIC else preferredYouTubeClient)
                    add(ANDROID_MUSIC)
                    add(WEB_REMIX)
                    add(TVHTML5_SIMPLY_EMBEDDED_PLAYER)
                    add(TVHTML5)
                    add(ANDROID_VR_1_61_48)
                    add(WEB)
                    addAll(orderedFallbackClients)
                } else if (audioQuality == AudioQuality.SAAVN) {
                    add(preferredYouTubeClient)
                    add(IOS)
                    add(ANDROID_MUSIC)
                    add(WEB_REMIX)
                    addAll(orderedFallbackClients)
                } else {
                    add(preferredYouTubeClient)
                    addAll(orderedFallbackClients)
                }
                if (preferredYouTubeClient != MAIN_CLIENT) add(MAIN_CLIENT)
            }.distinct().filterNot { client ->
                val blocked = isStreamClientTemporarilyBlocked(videoId, client.clientName)
                if (blocked) {
                    Timber.tag(logTag).w("Temporarily blocked stream client for $videoId: ${client.clientName}")
                }
                blocked
            }

        val botDetectedClients = mutableSetOf<String>()

        for ((index, client) in streamClients.withIndex()) {
            format = null
            streamUrl = null
            streamExpiresInSeconds = null
            streamPlayerResponse = null

            Timber.tag(logTag).v(
                "Trying ${if (client == MAIN_CLIENT) "MAIN_CLIENT" else "fallback client"} ${index + 1}/${streamClients.size}: ${client.clientName}"
            )

            if (client != MAIN_CLIENT && client.loginRequired && !isLoggedIn) {
                Timber.tag(logTag).w("Skipping client ${client.clientName} - requires login but user is not logged in")
                continue
            }

            streamPlayerResponse = when (client) {
                metadataClient -> metadataPlayerResponse
                preferredYouTubeClient -> preferredPlayerResponse
                else -> {
                    Timber.tag(logTag).i("Fetching player response for fallback client: ${client.clientName}")
                    YouTube.player(videoId, playlistId, client, signatureTimestamp).getOrNull()
                }
            }

            if (streamPlayerResponse == null) continue

            if (streamPlayerResponse.playabilityStatus.status != "OK") {
                val reason = streamPlayerResponse.playabilityStatus.reason.orEmpty()
                val isBotDetection = isBotDetectionError(reason)
                Timber.tag(logTag).w(
                    "Player response status not OK: ${streamPlayerResponse.playabilityStatus.status}, reason: $reason, botDetection: $isBotDetection"
                )
                if (isBotDetection) {
                    botDetectedClients.add(client.clientName)

                    val scopedKey = buildFailedClientKey(videoId, client.clientName)
                    failedStreamClientsUntil[scopedKey] =
                        System.currentTimeMillis() + FAILED_CLIENT_BACKOFF_MS
                }
                continue
            }

            val isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered
            val candidates =
                selectAudioFormatCandidates(
                    streamPlayerResponse,
                    audioQuality,
                    isMetered,
                    avoidCodecs = avoidCodecs,
                    dataSaver = dataSaver,
                )

            if (candidates.isEmpty()) continue

            val hasPreferredCodec = candidates.any {
                val c = extractCodec(it.mimeType)?.lowercase().orEmpty()
                when (audioQuality) {
                    AudioQuality.OPUS -> c.contains("opus") || it.itag in listOf(249, 250, 251)
                    AudioQuality.SAAVN -> c.contains("mp4a") || c.contains("aac") || it.itag in listOf(139, 140, 141)
                    AudioQuality.LOSSLESS -> true
                }
            }
            if (!hasPreferredCodec && index < streamClients.size - 1) {
                Timber.tag(logTag).d("Client ${client.clientName} does not have preferred codec for $audioQuality, trying next client")
                continue
            }

            var selectedFormat: PlayerResponse.StreamingData.Format? = null
            var selectedUrl: String? = null
            val isFallbackClient = client != preferredYouTubeClient && client != metadataClient

            for (candidate in candidates.asSequence().take(6)) {
                if (isLoggedIn && expectedDurationMs != null && isLikelyPreview(candidate, expectedDurationMs)) continue
                val cacheKey = buildCacheKey(videoId, candidate.itag)
                val cached = streamUrlCache[cacheKey]
                val candidateUrl =
                    if (cached != null && cached.expiresAtMs > System.currentTimeMillis()) {
                        cached.url
                    } else {
                        findUrlOrNull(candidate, videoId, client)
                    } ?: continue

                val streamCandidate = com.mudassir131.yt.playback.recovery.StreamCandidate.create(
                    videoId = videoId,
                    client = client,
                    format = candidate,
                    streamUrl = candidateUrl,
                    expiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds ?: 21600,
                )

                if (isFallbackClient) {
                    val isValid = com.mudassir131.yt.playback.recovery.StreamCandidate.validateSelectively(
                        streamCandidate,
                        isSuspiciousOrFallback = true
                    )
                    if (!isValid) {
                        Timber.tag(logTag).w("Fallback candidate ${streamCandidate.candidateId} failed preflight check, skipping")
                        continue
                    }
                }

                selectedFormat = candidate
                selectedUrl = candidateUrl
                break
            }

            if (selectedFormat == null || selectedUrl == null) continue

            format = selectedFormat
            streamUrl = selectedUrl
            streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds

            if (streamExpiresInSeconds == null) {
                streamPlayerResponse = null
                continue
            }

            Timber.tag(logTag).i("Format found: ${format.mimeType}, bitrate: ${format.bitrate}")
            Timber.tag(logTag).v("Stream expires in: $streamExpiresInSeconds seconds")
            break
        }

        if (streamPlayerResponse == null || format == null || streamUrl == null || streamExpiresInSeconds == null) {
            val candidateTitle = videoDetails?.title?.takeIf { it.isNotBlank() } ?: trackTitle.orEmpty()
            val candidateArtist = videoDetails?.author?.takeIf { it.isNotBlank() } ?: trackArtist.orEmpty()
            val candidateDuration = videoDetails?.lengthSeconds?.toIntOrNull() ?: trackDuration ?: -1

            var resolvedTitle = candidateTitle
            var resolvedArtist = candidateArtist
            var resolvedDuration = candidateDuration

            // If metadata is completely missing, attempt to fetch basic track info via YouTube.queue
            if (resolvedTitle.isBlank()) {
                val queueItem = runCatching {
                    YouTube.queue(listOf(videoId)).getOrNull()?.firstOrNull()
                }.getOrNull()
                if (queueItem != null) {
                    resolvedTitle = queueItem.title
                    resolvedArtist = queueItem.artists.joinToString { it.name }
                    resolvedDuration = queueItem.duration ?: -1
                }
            }

            if (resolvedTitle.isNotBlank()) {
                Timber.tag(logTag).w("All YouTube stream clients failed or triggered bot detection. Attempting high-fidelity online stream fallback for '$resolvedTitle' by '$resolvedArtist'...")
                val fallbackResult = LosslessStreamResolver.resolve(
                    videoId = videoId,
                    title = resolvedTitle,
                    artist = resolvedArtist,
                    durationSeconds = resolvedDuration,
                    isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered,
                    allowHighResLossy = true,
                )
                if (fallbackResult != null) {
                    Timber.tag(logTag).i("Fallback stream resolved successfully: ${fallbackResult.streamUrl} (source=${fallbackResult.source})")
                    return PlaybackData(
                        audioConfig = audioConfig,
                        videoDetails = videoDetails,
                        playbackTracking = playbackTracking,
                        format = fallbackResult.format,
                        streamUrl = fallbackResult.streamUrl,
                        streamExpiresInSeconds = fallbackResult.expiresInSeconds,
                    )
                }
            }

            if (botDetectedClients.isNotEmpty()) {
                Timber.tag(logTag).e("Bot detection triggered on clients: $botDetectedClients - all clients failed")
                throw PlaybackException(
                    "Sign in to confirm you're not a bot",
                    null,
                    PlaybackException.ERROR_CODE_REMOTE_ERROR
                )
            }
            Timber.tag(logTag).e("Bad stream player response - all clients failed")
            throw Exception("Bad stream player response")
        }

        if (streamPlayerResponse.playabilityStatus.status != "OK") {
            val errorReason = streamPlayerResponse.playabilityStatus.reason
            Timber.tag(logTag).e("Playability status not OK: $errorReason")
            throw PlaybackException(
                errorReason,
                null,
                PlaybackException.ERROR_CODE_REMOTE_ERROR
            )
        }

        if (streamExpiresInSeconds == null) {
            Timber.tag(logTag).e("Missing stream expire time")
            throw Exception("Missing stream expire time")
        }

        if (format == null) {
            Timber.tag(logTag).e("Could not find suitable format for quality: $audioQuality. Available formats from last client: ${streamPlayerResponse.streamingData?.adaptiveFormats?.filter { it.isAudio }?.map { "${it.mimeType} @ ${it.bitrate}bps (itag: ${it.itag})" }}")
            throw Exception("Could not find format for quality: $audioQuality")
        }

        if (streamUrl == null) {
            Timber.tag(logTag).e("Could not find stream url for format: ${format.mimeType}, itag: ${format.itag}")
            throw Exception("Could not find stream url")
        }

        Timber.tag(logTag).i("Successfully obtained playback data with format: ${format.mimeType}, bitrate: ${format.bitrate}")

        streamUrlCache[buildCacheKey(videoId, format.itag)] =
            CachedStreamUrl(
                url = streamUrl,
                expiresAtMs = System.currentTimeMillis() + (streamExpiresInSeconds * 1000L),
            )

        return PlaybackData(
            audioConfig,
            videoDetails,
            playbackTracking,
            format,
            streamUrl,
            streamExpiresInSeconds,
        )
    }
    /**
     * Simple player response intended to use for metadata only.
     * Stream URLs of this response might not work so don't use them.
     */
    suspend fun playerResponseForMetadata(
        videoId: String,
        playlistId: String? = null,
    ): Result<PlayerResponse> {
        Timber.tag(logTag).i("Fetching metadata-only player response for videoId: $videoId using MAIN_CLIENT: ${MAIN_CLIENT.clientName}")
        return YouTube.player(videoId, playlistId, client = MAIN_CLIENT)
            .onSuccess { Timber.tag(logTag).d("Successfully fetched metadata") }
            .onFailure { Timber.tag(logTag).e(it, "Failed to fetch metadata") }
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
        // optional override from user preference; if non-null, use this instead of ConnectivityManager
        networkMetered: Boolean? = null,
        avoidCodecs: Set<String> = emptySet(),
    ): PlayerResponse.StreamingData.Format? {
        val isMetered = networkMetered ?: connectivityManager.isActiveNetworkMetered
        return selectAudioFormatCandidates(
            playerResponse,
            audioQuality,
            isMetered,
            avoidCodecs = avoidCodecs,
        ).firstOrNull()
    }

    private fun selectAudioFormatCandidates(
        playerResponse: PlayerResponse,
        audioQuality: AudioQuality,
        networkMetered: Boolean,
        avoidCodecs: Set<String> = emptySet(),
        dataSaver: Boolean = false,
    ): List<PlayerResponse.StreamingData.Format> {
        Timber.tag(logTag).i("Finding format with audioQuality: $audioQuality, network metered: $networkMetered, dataSaver: $dataSaver")

        val audioFormats =
            playerResponse.streamingData?.adaptiveFormats
                ?.asSequence()
                ?.filter { it.isAudio && it.bitrate > 0 }
                ?.filter { it.url != null || it.signatureCipher != null || it.cipher != null }
                ?.filter { format ->
                    val codec = extractCodec(format.mimeType)?.lowercase()
                    codec == null || codec !in avoidCodecs
                }
                ?.toList()
                .orEmpty()

        if (audioFormats.isEmpty()) return emptyList()

        val targetBitrateBps =
            if (dataSaver) {
                64_000
            } else {
                when (audioQuality) {
                    AudioQuality.SAAVN -> 160_000
                    AudioQuality.OPUS -> 320_000
                    AudioQuality.LOSSLESS -> 1_411_200
                }
            }

        val preferHigher =
            compareByDescending<PlayerResponse.StreamingData.Format> { modeCodecRank(audioQuality, extractCodec(it.mimeType)) }
                .thenByDescending { it.bitrate }
                .thenByDescending { it.url != null }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val preferLowerAboveTarget =
            compareByDescending<PlayerResponse.StreamingData.Format> { modeCodecRank(audioQuality, extractCodec(it.mimeType)) }
                .thenBy { it.bitrate }
                .thenByDescending { it.url != null }
                .thenByDescending { it.audioSampleRate ?: 0 }

        val candidates =
            if (targetBitrateBps == null) {
                audioFormats.sortedWith(preferHigher)
            } else {
                val belowOrEqual = audioFormats.filter { it.bitrate <= targetBitrateBps }
                if (belowOrEqual.isNotEmpty()) {
                    belowOrEqual.sortedWith(preferHigher)
                } else {
                    val aboveOrEqual = audioFormats.filter { it.bitrate >= targetBitrateBps }
                    if (aboveOrEqual.isNotEmpty()) aboveOrEqual.sortedWith(preferLowerAboveTarget)
                    else audioFormats.sortedWith(preferHigher)
                }
            }

        Timber.tag(logTag)
            .v(
                "Available audio formats: ${
                    candidates.take(12).map {
                        val codec = extractCodec(it.mimeType)
                        val direct = if (it.url != null) "direct" else "cipher"
                        "${it.mimeType} ($direct, codec=${codec ?: "unknown"}) @ ${it.bitrate}bps"
                    }
                }"
            )

        return candidates
    }

    private fun extractCodec(mimeType: String): String? {
        val match = Regex("""codecs="([^"]+)"""").find(mimeType) ?: return null
        return match.groupValues.getOrNull(1)?.split(",")?.firstOrNull()?.trim()
    }

    private fun codecRank(codec: String?): Int =
        when {
            codec.isNullOrBlank() -> 0
            codec.contains("opus", ignoreCase = true) -> 3
            codec.contains("mp4a", ignoreCase = true) -> 2
            else -> 1
        }

    private fun modeCodecRank(audioQuality: AudioQuality, codec: String?): Int =
        when (audioQuality) {
            AudioQuality.OPUS -> when {
                codec?.contains("opus", ignoreCase = true) == true -> 5
                codec?.contains("mp4a", ignoreCase = true) == true -> 2
                else -> codecRank(codec)
            }
            AudioQuality.SAAVN -> when {
                codec?.contains("mp4a", ignoreCase = true) == true -> 5
                codec?.contains("opus", ignoreCase = true) == true -> 3
                else -> codecRank(codec)
            }
            AudioQuality.LOSSLESS -> when {
                codec?.contains("alac", ignoreCase = true) == true -> 6
                codec?.contains("flac", ignoreCase = true) == true -> 5
                codec?.contains("wav", ignoreCase = true) == true -> 5
                codec?.contains("opus", ignoreCase = true) == true -> 4
                codec?.contains("mp4a", ignoreCase = true) == true -> 3
                else -> codecRank(codec)
            }
        }
    private fun isLikelyPreview(
        format: PlayerResponse.StreamingData.Format,
        expectedDurationMs: Long,
    ): Boolean {
        val approx = format.approxDurationMs?.toLongOrNull() ?: return false
        if (expectedDurationMs < 90_000L) return false
        return approx in 1L..(minOf(90_000L, (expectedDurationMs * 9L) / 10L))
    }
    /**
     * Wrapper around the [NewPipeUtils.getSignatureTimestamp] function which reports exceptions
     */
    private fun getSignatureTimestampOrNull(
        videoId: String
    ): Int? {
        cachedSignatureTimestamp?.let { return it }
        Timber.tag(logTag).i("Getting signature timestamp for videoId: $videoId")
        val timestamp = NewPipeUtils.getSignatureTimestamp(videoId)
            .onSuccess { Timber.tag(logTag).i("Signature timestamp obtained: $it") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get signature timestamp")
                reportException(it)
            }
            .getOrNull()
        if (timestamp != null) {
            cachedSignatureTimestamp = timestamp
        }
        return timestamp
    }
    /**
     * Wrapper around the [NewPipeUtils.getStreamUrl] function which reports exceptions.
     * Also patches cver to match the client version.
     */
    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
    ): String? {
        Timber.tag(logTag).i("Finding stream URL for format: ${format.mimeType}, videoId: $videoId")
        var url = NewPipeUtils.getStreamUrl(format, videoId, client)
            .onSuccess { Timber.tag(logTag).i("Stream URL obtained successfully") }
            .onFailure {
                Timber.tag(logTag).e(it, "Failed to get stream URL")
                reportException(it)
            }
            .getOrNull() ?: return null

        // Patch cver in the URL to match the client we actually used
        if (client != null) {
            url = StreamClientUtils.patchClientVersion(url, client.clientVersion)
        }

        return url
    }

    private fun buildCacheKey(videoId: String, itag: Int): String {
        return "$videoId:$itag"
    }

    private fun isBotDetectionError(reason: String): Boolean {
        val lower = reason.lowercase(Locale.US)
        return "sign in" in lower ||
                "bot" in lower ||
                "confirm" in lower && "not a" in lower ||
                "verify" in lower && "human" in lower
    }

    fun isBotDetectionException(error: PlaybackException): Boolean {
        val message = error.message.orEmpty()
        if (isBotDetectionError(message)) return true
        var cause: Throwable? = error.cause
        while (cause != null) {
            if (isBotDetectionError(cause.message.orEmpty())) return true
            cause = cause.cause
        }
        return false
    }

    private data class NewPipeAudioResult(
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val videoDetails: PlayerResponse.VideoDetails?,
    )

    private fun resolveAudioWithNewPipe(
        videoId: String,
        audioQuality: AudioQuality,
        isMetered: Boolean,
        dataSaver: Boolean,
        avoidCodecs: Set<String> = emptySet(),
    ): NewPipeAudioResult? {
        return try {
            NewPipeUtils.ensureInitialized()
            val service = ServiceList.YouTube
            val extractor = service.getStreamExtractor("https://www.youtube.com/watch?v=$videoId") as YoutubeStreamExtractor
            extractor.fetchPage()

            val streams = extractor.audioStreams.orEmpty().filter { !it.url.isNullOrBlank() }
            if (streams.isEmpty()) return null

            // Prioritize streams based on user's audio quality preference
            // AudioQuality.OPUS -> Opus (itag 251 @ 160kbps, 250, 249) > AAC (itag 140, 139)
            // AudioQuality.SAAVN -> AAC (itag 140, 139) > Opus (itag 251, etc.)
            // AudioQuality.LOSSLESS -> Opus 251 (highest quality available on YouTube) > AAC 140
            val sortedStreams = streams.filter { stream ->
                val codec = stream.codec?.lowercase().orEmpty()
                avoidCodecs.none { codec.contains(it.lowercase()) }
            }.sortedWith(
                compareByDescending<AudioStream> { stream ->
                    val isOpus = stream.codec?.contains("opus", ignoreCase = true) == true || stream.itag in listOf(249, 250, 251)
                    val isAac = stream.codec?.contains("mp4a", ignoreCase = true) == true || stream.itag in listOf(139, 140)
                    when (audioQuality) {
                        AudioQuality.OPUS -> if (isOpus) 10 else if (isAac) 5 else 1
                        AudioQuality.SAAVN -> if (isAac) 10 else if (isOpus) 5 else 1
                        AudioQuality.LOSSLESS -> if (isOpus) 10 else if (isAac) 5 else 1
                    }
                }.thenByDescending { stream ->
                    val br = if (stream.averageBitrate > 0) {
                        if (stream.averageBitrate < 1000) stream.averageBitrate * 1000 else stream.averageBitrate
                    } else if (stream.itag == 251) 160_000 else 128_000
                    if (dataSaver) -br else br
                }
            )

            val selectedStream = sortedStreams.firstOrNull() ?: streams.firstOrNull() ?: return null
            val streamUrl = selectedStream.url ?: return null
            val isOpus = selectedStream.codec?.contains("opus", ignoreCase = true) == true || selectedStream.itag in listOf(249, 250, 251)
            val mimeType = if (isOpus) "audio/webm; codecs=\"opus\"" else "audio/mp4; codecs=\"mp4a.40.2\""
            val rawBitrate = selectedStream.averageBitrate
            val bitrateBps = if (rawBitrate > 0) {
                if (rawBitrate < 1000) rawBitrate * 1000 else rawBitrate
            } else if (selectedStream.itag == 251) 160_000 else 128_000

            val format = PlayerResponse.StreamingData.Format(
                itag = if (selectedStream.itag > 0) selectedStream.itag else if (isOpus) 251 else 140,
                url = streamUrl,
                mimeType = mimeType,
                bitrate = bitrateBps,
                width = null,
                height = null,
                contentLength = null,
                quality = "tiny",
                fps = null,
                qualityLabel = null,
                averageBitrate = bitrateBps,
                audioQuality = if (selectedStream.itag == 251 || bitrateBps >= 150_000) "AUDIO_QUALITY_MEDIUM" else "AUDIO_QUALITY_LOW",
                approxDurationMs = (extractor.length * 1000L).toString(),
                audioSampleRate = if (isOpus) 48000 else 44100,
                audioChannels = 2,
                loudnessDb = null,
                lastModified = null,
                signatureCipher = null,
                cipher = null,
            )

            val fallbackDetails = PlayerResponse.VideoDetails(
                videoId = videoId,
                title = extractor.name.orEmpty(),
                author = extractor.uploaderName.orEmpty(),
                channelId = extractor.uploaderUrl.orEmpty(),
                lengthSeconds = extractor.length.toString(),
                musicVideoType = null,
                viewCount = "0",
                thumbnail = com.mudassir131.yt.innertube.models.Thumbnails(emptyList()),
            )

            NewPipeAudioResult(format, streamUrl, fallbackDetails)
        } catch (e: Exception) {
            Timber.tag(logTag).w(e, "NewPipe audio stream extraction failed for $videoId")
            null
        }
    }
}

