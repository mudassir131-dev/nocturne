/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.recovery

import timber.log.Timber

/**
 * Structured, sanitized diagnostic logger for playback events and failure recovery.
 *
 * CRITICAL SECURITY / PRIVACY RULE:
 * This logger NEVER records full signed media URLs, query signatures, tokens, auth headers, or cookies.
 */
object PlaybackDiagnostics {
    private const val TAG = "PlaybackDiag"

    data class DiagnosticEvent(
        val videoId: String,
        val client: String?,
        val itag: Int?,
        val bitrate: Int?,
        val mimeType: String?,
        val category: PlaybackFailureCategory?,
        val errorCode: Int?,
        val httpStatus: Int?,
        val positionMs: Long?,
        val bufferedPositionMs: Long?,
        val retryCount: Int,
        val sessionGeneration: Long,
        val message: String? = null,
    )

    fun logEvent(event: DiagnosticEvent) {
        val details = buildString {
            append("videoId=${event.videoId}")
            event.client?.let { append(" client=$it") }
            event.itag?.let { append(" itag=$it") }
            event.bitrate?.let { append(" bitrate=${it}bps") }
            event.mimeType?.let { append(" mime=$it") }
            event.category?.let { append(" category=${it.name}") }
            event.errorCode?.let { append(" errCode=${event.errorCode}") }
            event.httpStatus?.let { append(" http=${event.httpStatus}") }
            event.positionMs?.let { append(" pos=${it}ms") }
            event.bufferedPositionMs?.let { append(" buf=${it}ms") }
            append(" retry=${event.retryCount}")
            append(" sessionGen=${event.sessionGeneration}")
            event.message?.let { append(" msg=\"$it\"") }
        }
        Timber.tag(TAG).i(details)
    }

    fun logRecoveryStart(
        videoId: String,
        category: PlaybackFailureCategory,
        attempt: Int,
        positionMs: Long,
        sessionGen: Long,
    ) {
        Timber.tag(TAG).w(
            "RECOVERY_START: videoId=$videoId, category=${category.name}, attempt=$attempt, pos=${positionMs}ms, sessionGen=$sessionGen"
        )
    }

    fun logRecoverySuccess(
        videoId: String,
        attempt: Int,
        restoredPositionMs: Long,
        actualPositionMs: Long,
        sessionGen: Long,
    ) {
        Timber.tag(TAG).i(
            "RECOVERY_SUCCESS: videoId=$videoId, attempt=$attempt, restoredPos=${restoredPositionMs}ms, actualPos=${actualPositionMs}ms, sessionGen=$sessionGen"
        )
    }

    fun logRecoveryFailed(
        videoId: String,
        category: PlaybackFailureCategory,
        attempts: Int,
        sessionGen: Long,
        reason: String,
    ) {
        Timber.tag(TAG).e(
            "RECOVERY_FAILED: videoId=$videoId, category=${category.name}, totalAttempts=$attempts, sessionGen=$sessionGen, reason=$reason"
        )
    }

    fun logStallDetected(
        videoId: String,
        stallDurationMs: Long,
        positionMs: Long,
        bufferedMs: Long,
        sessionGen: Long,
    ) {
        Timber.tag(TAG).w(
            "STALL_DETECTED: videoId=$videoId, stallDuration=${stallDurationMs}ms, pos=${positionMs}ms, buf=${bufferedMs}ms, sessionGen=$sessionGen"
        )
    }
}
