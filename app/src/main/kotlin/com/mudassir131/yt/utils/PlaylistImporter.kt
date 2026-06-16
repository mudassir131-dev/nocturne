/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import android.content.Context
import android.net.Uri
import com.mudassir131.yt.db.MusicDatabase
import com.mudassir131.yt.db.entities.PlaylistEntity
import com.mudassir131.yt.db.entities.PlaylistSongMap
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.models.toMediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.json.JSONObject
import org.jsoup.Jsoup
import java.time.LocalDateTime
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object PlaylistImporter {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://www.google.com")
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP Error: ${response.code}")
            return@withContext response.body?.string() ?: ""
        }
    }

    private suspend fun resolveRedirect(url: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()
            httpClient.newCall(request).execute().use { response ->
                return@withContext response.request.url.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext url
        }
    }

    suspend fun importPlaylist(
        database: MusicDatabase,
        url: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedUrl = url.trim()
            val resolvedUrl = resolveRedirect(trimmedUrl)
            
            if (resolvedUrl.contains("youtube.com") || resolvedUrl.contains("youtu.be")) {
                val uri = Uri.parse(resolvedUrl)
                val playlistId = uri.getQueryParameter("list")
                    ?: return@runCatching Result.failure<String>(IllegalArgumentException("Invalid YouTube Playlist URL")).getOrThrow()
                
                val playlistPage = YouTube.playlist(playlistId).getOrThrow()
                val playlistName = playlistPage.playlist.title ?: "Imported YouTube Playlist"
                
                val newPlaylistId = UUID.randomUUID().toString()
                database.withTransaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )

                    playlistPage.songs.forEachIndexed { index, songItem ->
                        val metadata = songItem.toMediaMetadata()
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
            } else if (resolvedUrl.contains("spotify.com/playlist/")) {
                val playlistId = resolvedUrl.substringAfter("playlist/").substringBefore("?").substringBefore("/")
                val html = fetchHtml("https://open.spotify.com/embed/playlist/$playlistId")
                val doc = Jsoup.parse(html)

                val nextDataScript = doc.getElementById("__NEXT_DATA__")
                val jsonText = nextDataScript?.data()?.trim() ?: ""

                var playlistName = "Imported Spotify Playlist"
                val tracks = mutableListOf<Pair<String, String>>()

                if (jsonText.isNotEmpty()) {
                    try {
                        val root = JSONObject(jsonText)
                        val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
                        val state = pageProps?.optJSONObject("state")
                        val entity = state?.optJSONObject("data")?.optJSONObject("entity")
                        if (entity != null) {
                            playlistName = entity.optString("name", "Imported Spotify Playlist").takeIf { it.isNotEmpty() } ?: "Imported Spotify Playlist"
                            val trackList = entity.optJSONArray("trackList")
                            if (trackList != null) {
                                for (i in 0 until trackList.length()) {
                                    val trackObj = trackList.getJSONObject(i)
                                    val title = trackObj.optString("title")
                                    val subtitle = trackObj.optString("subtitle")
                                    if (title.isNotEmpty()) {
                                        tracks.add(title to subtitle)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (tracks.isEmpty()) {
                    return@runCatching Result.failure<String>(Exception("No tracks found in Spotify playlist")).getOrThrow()
                }

                val newPlaylistId = UUID.randomUUID().toString()
                database.withTransaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )
                }

                // Resolve all tracks in parallel
                val deferreds = tracks.mapIndexed { index, (songName, artistName) ->
                    async {
                        val query = "$songName $artistName"
                        val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        val songItem = searchResult?.items?.firstOrNull() as? SongItem
                        if (songItem != null) {
                            Triple(index, songItem.toMediaMetadata(), songItem.setVideoId)
                        } else {
                            null
                        }
                    }
                }

                val results = deferreds.awaitAll().filterNotNull()

                database.withTransaction {
                    results.forEach { (index, metadata, setVideoId) ->
                        insert(metadata)
                        insert(
                            PlaylistSongMap(
                                playlistId = newPlaylistId,
                                songId = metadata.id,
                                position = index,
                                setVideoId = setVideoId
                            )
                        )
                    }
                }
                return@runCatching playlistName
            } else if (resolvedUrl.contains("music.apple.com/")) {
                val html = fetchHtml(resolvedUrl)
                val doc = Jsoup.parse(html)

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
                database.withTransaction {
                    insert(
                        PlaylistEntity(
                            id = newPlaylistId,
                            name = playlistName,
                            bookmarkedAt = LocalDateTime.now(),
                            isEditable = true
                        )
                    )
                }

                // Resolve all tracks in parallel
                val deferreds = tracks.mapIndexed { index, (songName, artistName) ->
                    async {
                        val query = "$songName $artistName"
                        val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                        val songItem = searchResult?.items?.firstOrNull() as? SongItem
                        if (songItem != null) {
                            Triple(index, songItem.toMediaMetadata(), songItem.setVideoId)
                        } else {
                            null
                        }
                    }
                }

                val results = deferreds.awaitAll().filterNotNull()

                database.withTransaction {
                    results.forEach { (index, metadata, setVideoId) ->
                        insert(metadata)
                        insert(
                            PlaylistSongMap(
                                playlistId = newPlaylistId,
                                songId = metadata.id,
                                position = index,
                                setVideoId = setVideoId
                            )
                        )
                    }
                }
                return@runCatching playlistName
            } else {
                return@runCatching Result.failure<String>(IllegalArgumentException("Unsupported Playlist URL")).getOrThrow()
            }
        }
    }
}
