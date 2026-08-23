/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.recovery

import com.mudassir131.yt.innertube.models.YouTubeClient
import com.mudassir131.yt.innertube.models.response.PlayerResponse
import com.mudassir131.yt.utils.StreamClientUtils
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Deterministic stream candidate representation.
 */
data class StreamCandidate(
    val candidateId: String,
    val videoId: String,
    val client: YouTubeClient,
    val format: PlayerResponse.StreamingData.Format,
    val streamUrl: String,
    val expiresAtMs: Long,
    val resolvedAtMs: Long = System.currentTimeMillis(),
    val isPreflightValidated: Boolean = false,
) {
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= (expiresAtMs - 15_000L) // 15s grace period

    companion object {
        fun create(
            videoId: String,
            client: YouTubeClient,
            format: PlayerResponse.StreamingData.Format,
            streamUrl: String,
            expiresInSeconds: Int,
        ): StreamCandidate {
            val candidateId = "${client.clientName}:$videoId:${format.itag}"
            val expiresAtMs = System.currentTimeMillis() + (expiresInSeconds * 1000L)
            return StreamCandidate(
                candidateId = candidateId,
                videoId = videoId,
                client = client,
                format = format,
                streamUrl = streamUrl,
                expiresAtMs = expiresAtMs,
            )
        }

        private val preflightClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(2500, TimeUnit.MILLISECONDS)
                .readTimeout(2500, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .build()
        }

        /**
         * Selectively validates a stream candidate.
         * Normal preferred candidates skip preflight to avoid doubling HTTP overhead.
         * Only suspicious or fallback recovery candidates undergo a lightweight HEAD/Range check.
         */
        fun validateSelectively(
            candidate: StreamCandidate,
            isSuspiciousOrFallback: Boolean,
        ): Boolean {
            if (candidate.isExpired) {
                Timber.tag("StreamCandidate").w("Candidate ${candidate.candidateId} rejected: already expired")
                return false
            }

            if (!isSuspiciousOrFallback) {
                // Let ExoPlayer handle standard loading naturally
                return true
            }

            return runCatching {
                val clientParam = candidate.client.clientName
                val userAgent = StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

                val requestBuilder = Request.Builder()
                    .url(candidate.streamUrl)
                    .header("User-Agent", userAgent)
                    .header("Range", "bytes=0-1")
                    .head()

                originReferer.origin?.let { requestBuilder.header("Origin", it) }
                originReferer.referer?.let { requestBuilder.header("Referer", it) }

                preflightClient.newCall(requestBuilder.build()).execute().use { response ->
                    val isOk = response.isSuccessful || response.code == 206
                    if (!isOk) {
                        Timber.tag("StreamCandidate").w(
                            "Selective preflight failed for ${candidate.candidateId} with HTTP ${response.code}"
                        )
                    }
                    isOk
                }
            }.getOrElse { e ->
                Timber.tag("StreamCandidate").w(e, "Selective preflight exception for ${candidate.candidateId}")
                false
            }
        }
    }
}
