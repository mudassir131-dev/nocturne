/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.playback.queues

import androidx.media3.common.MediaItem
import com.mudassir131.yt.extensions.ExtraIsMusicVideo
import com.mudassir131.yt.extensions.metadata
import com.mudassir131.yt.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaItem>

    data class Status(
        val title: String?,
        val items: List<MediaItem>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) =
            if (enabled) {
                copy(
                    items = items.filterExplicit(),
                )
            } else {
                this
            }
        fun filterVideo(enabled: Boolean = true) =
            if (enabled) {
                copy(
                    items = items.filterVideo(),
                )
            } else {
                this
            }
    }
}

fun List<MediaItem>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.metadata?.explicit == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterVideo(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.mediaMetadata.extras?.getBoolean(ExtraIsMusicVideo, false) == true
        }
    } else {
        this
    }

fun List<MediaItem>.filterByContentMode(mode: com.mudassir131.yt.constants.ContentFilterMode): List<MediaItem> =
    if (mode == com.mudassir131.yt.constants.ContentFilterMode.GLOBAL) {
        this
    } else {
        filter { mediaItem ->
            val title = mediaItem.mediaMetadata.title?.toString().orEmpty()
            val artistName = mediaItem.mediaMetadata.artist?.toString().orEmpty()
            com.mudassir131.yt.utils.ContentFilter.matches(title, artistName, mode)
        }
    }

fun Queue.Status.filterByContentMode(mode: com.mudassir131.yt.constants.ContentFilterMode): Queue.Status =
    if (mode == com.mudassir131.yt.constants.ContentFilterMode.GLOBAL) {
        this
    } else {
        copy(items = items.filterByContentMode(mode))
    }

