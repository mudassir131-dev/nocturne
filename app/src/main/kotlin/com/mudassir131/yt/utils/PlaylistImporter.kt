/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import android.content.Context
import com.mudassir131.yt.db.MusicDatabase
import com.mudassir131.yt.db.entities.PlaylistEntity
import com.mudassir131.yt.db.entities.PlaylistSongMap
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.util.UUID

object PlaylistImporter {

    suspend fun importPlaylist(
        database: MusicDatabase,
        url: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedUrl = url.trim()
            if (trimmedUrl.contains("youtube.com") || trimmedUrl.contains("youtu.be")) {
                val playlistId = extractQueryParameter(trimmedUrl, "list")
                    ?: return@runCatching Result.failure<String>(IllegalArgumentException("Invalid YouTube Playlist URL")).getOrThrow()
                
                val playlistPage = YouTube.playlist(playlistId).getOrThrow()
                val playlistName = playlistPage.playlist.title ?: "Imported YouTube Playlist"
                
                val newPlaylistId = UUID.randomUUID().toString()
                database.transaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )
                }

                playlistPage.songs.forEachIndexed { index, songItem ->
                    val metadata = songItem.toMediaMetadata()
                    database.transaction {
                        insert(metadata)
                        insert(
                            PlaylistSongMap(
                                playlistId = newPlaylistId,
                                songId = songItem.id,
                                position = index,
                                setVideoId = songItem.setVideoId
                            )
                        )
                    }
                }
                return@runCatching playlistName
            } else if (trimmedUrl.contains("spotify.com/playlist/")) {
                val playlistId = trimmedUrl.substringAfter("playlist/").substringBefore("?").substringBefore("/")
                val doc = Jsoup.connect("https://open.spotify.com/playlist/$playlistId")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(15000)
                    .get()

                var playlistName = doc.title().replace(" | Spotify", "").trim()
                val tracks = mutableListOf<Pair<String, String>>()

                val scripts = doc.select("script[type=application/ld+json]")
                for (script in scripts) {
                    val jsonText = script.data().trim()
                    try {
                        val root = JSONObject(jsonText)
                        val type = root.optString("@type")
                        if (type == "MusicPlaylist" || root.has("track")) {
                            playlistName = root.optString("name", playlistName)
                            val trackArray = root.optJSONArray("track") ?: root.optJSONArray("itemListElement")
                            if (trackArray != null) {
                                for (i in 0 until trackArray.length()) {
                                    val trackObj = trackArray.getJSONObject(i)
                                    val item = if (trackObj.optString("@type") == "ListItem") {
                                        trackObj.optJSONObject("item")
                                    } else {
                                        trackObj
                                    }
                                    if (item != null) {
                                        val name = item.optString("name")
                                        val artistObj = item.optJSONObject("byArtist")
                                        val artistName = artistObj?.optString("name") ?: ""
                                        if (name.isNotEmpty()) {
                                            tracks.add(name to artistName)
                                        }
                                    }
                                }
                            }
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (tracks.isEmpty()) {
                    return@runCatching Result.failure<String>(Exception("No tracks found in Spotify playlist")).getOrThrow()
                }

                val newPlaylistId = UUID.randomUUID().toString()
                database.transaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )
                }

                tracks.forEachIndexed { index, (songName, artistName) ->
                    val query = "$songName $artistName"
                    val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    val songItem = searchResult?.items?.firstOrNull() as? SongItem
                    if (songItem != null) {
                        val metadata = songItem.toMediaMetadata()
                        database.transaction {
                            insert(metadata)
                            insert(
                                PlaylistSongMap(
                                    playlistId = newPlaylistId,
                                    songId = songItem.id,
                                    position = index,
                                    setVideoId = songItem.setVideoId
                                )
                            )
                        }
                    }
                }
                return@runCatching playlistName
            } else if (trimmedUrl.contains("music.apple.com/")) {
                val doc = Jsoup.connect(trimmedUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com")
                    .timeout(15000)
                    .get()

                var playlistName = doc.title().replace(" on Apple Music", "").trim()
                val tracks = mutableListOf<Pair<String, String>>()

                val scripts = doc.select("script[type=application/ld+json]")
                for (script in scripts) {
                    val jsonText = script.data().trim()
                    try {
                        val root = JSONObject(jsonText)
                        val type = root.optString("@type")
                        if (type == "MusicPlaylist" || root.has("track")) {
                            playlistName = root.optString("name", playlistName)
                            val trackArray = root.optJSONArray("track") ?: root.optJSONArray("itemListElement")
                            if (trackArray != null) {
                                for (i in 0 until trackArray.length()) {
                                    val trackObj = trackArray.getJSONObject(i)
                                    val item = if (trackObj.optString("@type") == "ListItem") {
                                        trackObj.optJSONObject("item")
                                    } else {
                                        trackObj
                                    }
                                    if (item != null) {
                                        val name = item.optString("name")
                                        val artistObj = item.optJSONObject("byArtist")
                                        val artistName = artistObj?.optString("name") ?: ""
                                        if (name.isNotEmpty()) {
                                            tracks.add(name to artistName)
                                        }
                                    }
                                }
                            }
                            break
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Fallback for Apple Music page scraping if JSON-LD wasn't fully parsed
                if (tracks.isEmpty()) {
                    val songElements = doc.select("div.songs-list-row, li.songs-list-row")
                    for (el in songElements) {
                        val title = el.select("div.songs-list-row__song-name, .songs-list-row__song-name").text().trim()
                        val artist = el.select("a.songs-list-row__link, .songs-list-row__by-line").text().trim()
                        if (title.isNotEmpty()) {
                            tracks.add(title to artist)
                        }
                    }
                }

                if (tracks.isEmpty()) {
                    return@runCatching Result.failure<String>(Exception("No tracks found in Apple Music playlist")).getOrThrow()
                }

                val newPlaylistId = UUID.randomUUID().toString()
                database.transaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )
                }

                tracks.forEachIndexed { index, (songName, artistName) ->
                    val query = "$songName $artistName"
                    val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                    val songItem = searchResult?.items?.firstOrNull() as? SongItem
                    if (songItem != null) {
                        val metadata = songItem.toMediaMetadata()
                        database.transaction {
                            insert(metadata)
                            insert(
                                PlaylistSongMap(
                                    playlistId = newPlaylistId,
                                    songId = songItem.id,
                                    position = index,
                                    setVideoId = songItem.setVideoId
                                )
                            )
                        }
                    }
                }
                return@runCatching playlistName
            } else {
                return@runCatching Result.failure<String>(IllegalArgumentException("Unsupported Playlist URL")).getOrThrow()
            }
        }.getOrThrow()
    }

    private fun extractQueryParameter(url: String, key: String): String? {
        val query = url.substringAfter("?", "")
        if (query.isEmpty()) return null
        val params = query.split("&")
        for (param in params) {
            val parts = param.split("=")
            if (parts.size == 2 && parts[0] == key) {
                return parts[1]
            }
        }
        return null
    }
}
