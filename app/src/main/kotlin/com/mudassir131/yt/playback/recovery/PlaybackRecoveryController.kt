/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.recovery

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.mudassir131.yt.utils.YTPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.random.Random

enum class RecoveryState {
    IDLE,
    RECOVERING,
    COOLDOWN,
    FAILED,
}

class PlaybackRecoveryController(
    private val scope: CoroutineScope,
    private val getPlayer: () -> ExoPlayer?,
    private val onClearTrackCache: (String) -> Unit,
    private val onUnrecoverableFailure: (mediaId: String, error: PlaybackException?) -> Unit,
) {
    private val sessionCounter = AtomicLong(1L)
    @Volatile
    var currentSessionGeneration: Long = sessionCounter.get()
        private set

    private val _recoveryState = MutableStateFlow(RecoveryState.IDLE)
    val recoveryState = _recoveryState.asStateFlow()

    private val _isRecovering = MutableStateFlow(false)
    val isRecovering = _isRecovering.asStateFlow()

    private val retryAttemptsByMedia = ConcurrentHashMap<String, Int>()
    private var activeRecoveryJob: Job? = null
    private val recoveryLock = Any()

    companion object {
        const val MAX_RETRIES = 4
        private const val BASE_BACKOFF_MS = 350L
        private const val COOLDOWN_DURATION_MS = 6000L
    }

    fun startNewSession(newMediaId: String?): Long {
        synchronized(recoveryLock) {
            activeRecoveryJob?.cancel()
            activeRecoveryJob = null
            _recoveryState.value = RecoveryState.IDLE
            _isRecovering.value = false
            currentSessionGeneration = sessionCounter.incrementAndGet()
            if (newMediaId != null) {
                // Keep only the active media in retry tracking
                retryAttemptsByMedia.keys.retainAll(setOf(newMediaId))
            } else {
                retryAttemptsByMedia.clear()
            }
            return currentSessionGeneration
        }
    }

    fun triggerRecovery(
        mediaId: String,
        error: PlaybackException?,
        category: PlaybackFailureCategory,
        forced: Boolean = false,
    ) {
        val player = getPlayer() ?: return
        val currentGen = currentSessionGeneration

        synchronized(recoveryLock) {
            if (_recoveryState.value == RecoveryState.RECOVERING && !forced) {
                Timber.tag("RecoveryCtrl").d("Recovery already in progress for session $currentGen, coalescing request")
                return
            }

            if (_recoveryState.value == RecoveryState.COOLDOWN && !forced) {
                Timber.tag("RecoveryCtrl").d("Recovery in cooldown for session $currentGen, ignoring duplicate trigger")
                return
            }

            val currentAttempts = retryAttemptsByMedia.getOrDefault(mediaId, 0)
            if (currentAttempts >= MAX_RETRIES && !forced) {
                PlaybackDiagnostics.logRecoveryFailed(
                    mediaId,
                    category,
                    currentAttempts,
                    currentGen,
                    "Retry budget exhausted ($MAX_RETRIES attempts)"
                )
                _recoveryState.value = RecoveryState.FAILED
                _isRecovering.value = false
                onUnrecoverableFailure(mediaId, error)
                return
            }

            val nextAttempt = currentAttempts + 1
            retryAttemptsByMedia[mediaId] = nextAttempt

            val savedPositionMs = player.currentPosition.coerceAtLeast(0L)
            val wasPlaying = player.playWhenReady

            _recoveryState.value = RecoveryState.RECOVERING
            _isRecovering.value = true

            PlaybackDiagnostics.logRecoveryStart(mediaId, category, nextAttempt, savedPositionMs, currentGen)

            activeRecoveryJob?.cancel()
            activeRecoveryJob = scope.launch(Dispatchers.Main) {
                executeRecovery(
                    mediaId = mediaId,
                    category = category,
                    attempt = nextAttempt,
                    savedPositionMs = savedPositionMs,
                    wasPlaying = wasPlaying,
                    sessionGen = currentGen,
                    originalError = error,
                )
            }
        }
    }

    private suspend fun executeRecovery(
        mediaId: String,
        category: PlaybackFailureCategory,
        attempt: Int,
        savedPositionMs: Long,
        wasPlaying: Boolean,
        sessionGen: Long,
        originalError: PlaybackException?,
    ) {
        // Compute backoff with jitter
        val backoffMs = if (category == PlaybackFailureCategory.TRANSIENT_NETWORK) {
            (BASE_BACKOFF_MS * (1 shl (attempt - 1))) + Random.nextLong(100, 300)
        } else {
            Random.nextLong(50, 150)
        }

        delay(backoffMs)

        // Session cancellation check
        if (sessionGen != currentSessionGeneration) {
            Timber.tag("RecoveryCtrl").d("Recovery aborted: session generation changed ($sessionGen -> $currentSessionGeneration)")
            return
        }

        // Invalidate cached URL for this track
        withContext(Dispatchers.IO) {
            YTPlayerUtils.invalidateCachedStreamUrls(mediaId)
            onClearTrackCache(mediaId)
        }

        val player = getPlayer()
        if (player == null || sessionGen != currentSessionGeneration) return

        // In-place reprepare on existing ExoPlayer instance
        try {
            player.seekTo(savedPositionMs)
            player.prepare()
            if (wasPlaying) {
                player.playWhenReady = true
            }

            // Monitor position restoration
            delay(1200L)
            if (sessionGen != currentSessionGeneration) return

            val actualPos = player.currentPosition
            PlaybackDiagnostics.logRecoverySuccess(mediaId, attempt, savedPositionMs, actualPos, sessionGen)

            synchronized(recoveryLock) {
                if (sessionGen == currentSessionGeneration) {
                    _recoveryState.value = RecoveryState.COOLDOWN
                    _isRecovering.value = false
                }
            }

            delay(COOLDOWN_DURATION_MS)
            synchronized(recoveryLock) {
                if (sessionGen == currentSessionGeneration && _recoveryState.value == RecoveryState.COOLDOWN) {
                    _recoveryState.value = RecoveryState.IDLE
                    // Reset retry attempts on verified continuous playback
                    if (player.isPlaying) {
                        retryAttemptsByMedia.remove(mediaId)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag("RecoveryCtrl").e(e, "Exception during recovery execution for $mediaId")
            synchronized(recoveryLock) {
                if (sessionGen == currentSessionGeneration) {
                    if (attempt >= MAX_RETRIES) {
                        _recoveryState.value = RecoveryState.FAILED
                        _isRecovering.value = false
                        onUnrecoverableFailure(mediaId, originalError)
                    } else {
                        _recoveryState.value = RecoveryState.IDLE
                        _isRecovering.value = false
                    }
                }
            }
        }
    }

    fun onPlaybackProgressConfirmed(mediaId: String) {
        // Once playback is healthy and progressing, clear failure counts
        retryAttemptsByMedia.remove(mediaId)
        if (_recoveryState.value == RecoveryState.COOLDOWN || _recoveryState.value == RecoveryState.RECOVERING) {
            _recoveryState.value = RecoveryState.IDLE
            _isRecovering.value = false
        }
    }
}
