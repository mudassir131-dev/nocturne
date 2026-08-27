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
        connection.player.seekTo(index, 0L)
        connection.player.play()
    }
    fun stopAndClear() = connection.service.stopAndClearPlayback()
}
