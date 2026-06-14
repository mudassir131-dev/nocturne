/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import com.mudassir131.yt.constants.ContentFilterMode
import com.mudassir131.yt.db.entities.Song
import com.mudassir131.yt.db.entities.LocalItem
import com.mudassir131.yt.db.entities.Album
import com.mudassir131.yt.db.entities.Artist
import com.mudassir131.yt.db.entities.Playlist
import com.mudassir131.yt.innertube.models.YTItem
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.innertube.models.AlbumItem
import com.mudassir131.yt.innertube.models.ArtistItem
import com.mudassir131.yt.innertube.models.PlaylistItem

object ContentFilter {

    private val QURAN_KEYWORDS = listOf(
        "quran", "qur'an", "surah", "recitation", "telawah", "tilawat", "tilawah", "qari", "reciter",
        "mishary", "al-sudais", "al-shuraim", "al-muaiqly", "al-ghamdi", "al-hudhaify", "minshawi",
        "abdul basit", "husary", "al-afasy", "sudais", "shuraim", "muaiqly", "ghamdi", "hudhaify",
        "ayat", "ayah", "telawat", "tarteel", "tajweed", "surat", "telawat", "quranic", "koran"
    )

    private val NASHEED_KEYWORDS = listOf(
        "nasheed", "nashid", "anachid", "vocals only", "vocal only", "no music", "islamic song",
        "islamic vocal", "maher zain", "sami yusuf", "harris j", "humood alkhuder", "ahmed bukhatir",
        "mishary alafasy nasheed", "vocalless", "acapella", "naat", "hamd", "darood", "salawat", 
        "nasheeds", "nashids", "nasheed v", "nasheed a", "vocals-only", "vocal-only"
    )

    fun matchesQuranic(title: String, artistName: String): Boolean {
        val lowerTitle = title.lowercase()
        val lowerArtist = artistName.lowercase()
        return QURAN_KEYWORDS.any { lowerTitle.contains(it) || lowerArtist.contains(it) }
    }

    fun matchesNasheed(title: String, artistName: String): Boolean {
        val lowerTitle = title.lowercase()
        val lowerArtist = artistName.lowercase()
        return NASHEED_KEYWORDS.any { lowerTitle.contains(it) || lowerArtist.contains(it) }
    }

    fun matches(title: String, artistName: String, mode: ContentFilterMode): Boolean {
        return when (mode) {
            ContentFilterMode.GLOBAL -> true
            ContentFilterMode.QURANIC -> matchesQuranic(title, artistName)
            ContentFilterMode.NASHEED -> matchesNasheed(title, artistName)
        }
    }
}

// Extension to filter list of local Song entities
fun List<Song>.filterSongsByContentMode(mode: ContentFilterMode): List<Song> {
    if (mode == ContentFilterMode.GLOBAL) return this
    return filter { song ->
        val artistName = song.artists.joinToString(" ") { it.name }
        ContentFilter.matches(song.title, artistName, mode)
    }
}

// Extension to filter list of local items (Song, Album, Artist, Playlist)
fun <T : LocalItem> List<T>.filterLocalItemsByContentMode(mode: ContentFilterMode): List<T> {
    if (mode == ContentFilterMode.GLOBAL) return this
    return filter { item ->
        when (item) {
            is Song -> {
                val artistName = item.artists.joinToString(" ") { it.name }
                ContentFilter.matches(item.title, artistName, mode)
            }
            is Album -> {
                val artistName = item.artists.joinToString(" ") { it.name }
                ContentFilter.matches(item.title, artistName, mode)
            }
            is Artist -> {
                ContentFilter.matches(item.title, item.title, mode)
            }
            is Playlist -> {
                ContentFilter.matches(item.title, "", mode)
            }
            else -> true
        }
    }
}

// Extension to filter list of online YTItem subclasses
fun <T : YTItem> List<T>.filterYTItemsByContentMode(mode: ContentFilterMode): List<T> {
    if (mode == ContentFilterMode.GLOBAL) return this
    return filter { item ->
        when (item) {
            is SongItem -> {
                val artistName = item.artists.joinToString(" ") { it.name }
                ContentFilter.matches(item.title, artistName, mode)
            }
            is AlbumItem -> {
                val artistName = item.artists?.joinToString(" ") { it.name }.orEmpty()
                ContentFilter.matches(item.title, artistName, mode)
            }
            is PlaylistItem -> {
                val authorName = item.author?.name.orEmpty()
                ContentFilter.matches(item.title, authorName, mode)
            }
            is ArtistItem -> {
                ContentFilter.matches(item.title, item.title, mode)
            }
            else -> true
        }
    }
}
