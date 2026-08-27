/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

/**
 * Metadata for a resolved lossless audio stream (ALAC/FLAC/PCM).
 */
data class ResolvedLosslessStream(
    val url: String,
    val mimeType: String = "audio/mp4",
    val codec: String = "alac",
    val bitDepth: Int = 16,
    val sampleRate: Int = 44100,
    val channels: Int = 2,
    val bitrate: Int? = null,
    val contentLength: Long? = null,
    val durationSeconds: Int? = null,
    val sourceName: String = "alac",
    val expiresInSeconds: Int = 86400,
)

/**
 * Interface for lossless audio stream providers.
 */
interface LosslessAudioProvider {
    val name: String

    /**
     * Attempts to resolve a lossless ALAC / FLAC stream for the given song.
     * Returns null if not available.
     */
    suspend fun resolve(
        videoId: String,
        title: String,
        artist: String,
        durationSeconds: Int = -1,
    ): ResolvedLosslessStream?
}
