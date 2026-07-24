/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.lyrics

import android.content.Context

interface LyricsProvider {
    val id: LyricsProviderId
    val name: String
    val timingCapabilities: Set<LyricsTimingCapability>
        get() = setOf(LyricsTimingCapability.PLAIN, LyricsTimingCapability.LINE_SYNCED)

    fun isEnabled(context: Context): Boolean

    suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String>

    suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        getLyrics(id, title, artist, album, duration).onSuccess(callback)
    }
}
