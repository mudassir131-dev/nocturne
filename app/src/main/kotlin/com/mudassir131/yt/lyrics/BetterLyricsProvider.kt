/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.lyrics

import android.content.Context
import com.mudassir131.yt.betterlyrics.BetterLyrics
import com.mudassir131.yt.constants.EnableBetterLyricsKey
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.get

import com.mudassir131.yt.utils.GlobalLog
import android.util.Log

object BetterLyricsProvider : LyricsProvider {
    override val id = LyricsProviderId.BETTER_LYRICS
    override val timingCapabilities = setOf(
        LyricsTimingCapability.PLAIN,
        LyricsTimingCapability.LINE_SYNCED,
        LyricsTimingCapability.WORD_SYNCED,
    )
    init {
        BetterLyrics.logger = { message ->
            GlobalLog.append(Log.INFO, "BetterLyrics", message)
        }
    }

    override val name = "BetterLyrics"

    override fun isEnabled(context: Context): Boolean = context.dataStore[EnableBetterLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = BetterLyrics.getLyrics(title = title, artist = artist, album = null, durationSeconds = duration)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) {
        BetterLyrics.getAllLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSeconds = duration,
            callback = callback,
        )
    }
}
