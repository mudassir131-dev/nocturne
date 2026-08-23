/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.recovery

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Multi-signal buffer stall watchdog that differentiates genuine stream starvation
 * from normal ExoPlayer buffering.
 */
class PlaybackWatchdog(
    private val scope: CoroutineScope,
    private val getPlayer: () -> ExoPlayer?,
    private val recoveryController: PlaybackRecoveryController,
) {
    private var watchdogJob: Job? = null
    private var bufferingStartedAtMs: Long = 0L
    private var lastRecordedBufferedPositionMs: Long = -1L
    private var lastBufferedAdvanceAtMs: Long = 0L

    companion object {
        private const val CHECK_INTERVAL_MS = 1000L
        private const val STALL_THRESHOLD_MS = 8500L // 8.5s without any buffer advance
    }

    fun start() {
        stop()
        watchdogJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkPlaybackHealth()
            }
        }
    }

    fun stop() {
        watchdogJob?.cancel()
        watchdogJob = null
        resetBufferingTracking()
    }

    private fun resetBufferingTracking() {
        bufferingStartedAtMs = 0L
        lastRecordedBufferedPositionMs = -1L
        lastBufferedAdvanceAtMs = 0L
    }

    private fun checkPlaybackHealth() {
        val player = getPlayer() ?: return
        val currentMediaId = player.currentMediaItem?.mediaId ?: return
        val now = System.currentTimeMillis()

        val isBuffering = player.playbackState == Player.STATE_BUFFERING
        val shouldBePlaying = player.playWhenReady

        if (!isBuffering || !shouldBePlaying) {
            resetBufferingTracking()
            return
        }

        if (bufferingStartedAtMs == 0L) {
            bufferingStartedAtMs = now
            lastRecordedBufferedPositionMs = player.bufferedPosition
            lastBufferedAdvanceAtMs = now
            return
        }

        val currentBufferedPos = player.bufferedPosition
        if (currentBufferedPos > lastRecordedBufferedPositionMs + 500L) {
            // Buffer is actively loading chunks, network is progressing normally
            lastRecordedBufferedPositionMs = currentBufferedPos
            lastBufferedAdvanceAtMs = now
            return
        }

        val stallDuration = now - lastBufferedAdvanceAtMs
        if (stallDuration >= STALL_THRESHOLD_MS) {
            if (recoveryController.recoveryState.value == RecoveryState.IDLE) {
                PlaybackDiagnostics.logStallDetected(
                    videoId = currentMediaId,
                    stallDurationMs = stallDuration,
                    positionMs = player.currentPosition,
                    bufferedMs = player.bufferedPosition,
                    sessionGen = recoveryController.currentSessionGeneration,
                )
                resetBufferingTracking()
                recoveryController.triggerRecovery(
                    mediaId = currentMediaId,
                    error = null,
                    category = PlaybackFailureCategory.BUFFER_STALL,
                )
            }
        }
    }
}
