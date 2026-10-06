package com.mudassir131.yt.ui.appleplayer

import com.mudassir131.yt.extensions.togglePlayPause
import com.mudassir131.yt.extensions.toggleRepeatMode
import com.mudassir131.yt.playback.PlayerConnection

/** The only playback-facing API used by the transplanted Apple experience. */
class NocturneApplePlayerAdapter(
    private val connection: PlayerConnection,
) {
    val player get() = connection.player
    val metadata get() = connection.mediaMetadata
    val currentSong get() = connection.currentSong
    val currentLyrics get() = connection.currentLyrics
    val currentFormat get() = connection.currentFormat
    val currentFormatInfo get() = connection.currentFormatInfo
    val playbackState get() = connection.playbackState
    val isPlaying get() = connection.isPlaying
    val canSkipPrevious get() = connection.canSkipPrevious
    val canSkipNext get() = connection.canSkipNext
    val queue get() = connection.queueWindows
    val queueIndex get() = connection.currentWindowIndex
    val shuffle get() = connection.shuffleModeEnabled
    val repeat get() = connection.repeatMode

    fun togglePlayback() = connection.player.togglePlayPause()
    fun previous() = connection.seekToPrevious()
    fun next() = connection.seekToNext()
    fun toggleFavorite() = connection.toggleLike()
    fun toggleShuffle() {
        connection.player.shuffleModeEnabled = !connection.player.shuffleModeEnabled
    }
    fun toggleRepeat() = connection.player.toggleRepeatMode()
    fun seekTo(positionMs: Long) = connection.player.seekTo(positionMs)
    fun playQueueIndex(index: Int) {
        val window = connection.queueWindows.value.getOrNull(index)
        val targetIndex = if (window != null) {
            val timeline = connection.player.currentTimeline
            (0 until timeline.windowCount).indexOfFirst {
                timeline.getWindow(it, androidx.media3.common.Timeline.Window()).uid == window.uid
            }.takeIf { it != -1 } ?: (0 until player.mediaItemCount).indexOfFirst {
                player.getMediaItemAt(it).mediaId == window.mediaItem.mediaId
            }.takeIf { it != -1 } ?: index.coerceIn(0, (player.mediaItemCount - 1).coerceAtLeast(0))
        } else {
            index.coerceIn(0, (player.mediaItemCount - 1).coerceAtLeast(0))
        }
        connection.player.seekToDefaultPosition(targetIndex)
        if (connection.player.playbackState == androidx.media3.common.Player.STATE_IDLE) {
            connection.player.prepare()
        }
        connection.player.play()
    }
    fun stopAndClear() = connection.service.stopAndClearPlayback()
}
