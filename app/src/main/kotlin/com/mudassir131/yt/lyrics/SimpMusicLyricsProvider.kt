/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.lyrics

import android.content.Context
import com.mudassir131.yt.constants.EnableSimpMusicLyricsKey
import com.mudassir131.yt.simpmusic.SimpMusicLyrics
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.get

object SimpMusicLyricsProvider : LyricsProvider {
    override val name: String = "Nocturne"

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableSimpMusicLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = SimpMusicLyrics.getLyrics(videoId = id, duration = duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        SimpMusicLyrics.getAllLyrics(videoId = id, duration = duration, callback = callback)
    }
}

