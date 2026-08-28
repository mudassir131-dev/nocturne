/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

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
import java.util.regex.Pattern

object PlaylistImporter {

    const val MAX_IMPORT_SONGS = 5000
    const val SPOTIFY_MAX_IMPORT_SONGS = 3000

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
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/json")
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Referer", "https://open.spotify.com/")
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
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .build()
            httpClient.newCall(request).execute().use { response ->
                return@withContext response.request.url.toString()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext url
        }
    }

    private fun extractAccessTokenFromHtml(html: String): String? {
        // Look for session script tag
        val sessionMatch = Pattern.compile("<script id=\"session\"[^>]*>([\\s\\S]*?)</script>").matcher(html)
        if (sessionMatch.find()) {
            val sessionJson = sessionMatch.group(1)?.trim() ?: ""
            if (sessionJson.isNotEmpty()) {
                val token = runCatching { JSONObject(sessionJson).optString("accessToken") }.getOrNull()
                if (!token.isNullOrEmpty()) return token
            }
        }
        // Fallback regex search for accessToken
        val regex = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"")
        val matcher = regex.matcher(html)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return null
    }

    internal suspend fun fetchSpotifyTracks(playlistId: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        var playlistName = "Imported Spotify Playlist"
        val tracks = mutableListOf<Pair<String, String>>()

        // 1. Obtain Spotify access token
        var accessToken: String? = null
        try {
            val tokenReq = Request.Builder()
                .url("https://open.spotify.com/get_access_token?reason=transport&productType=web_player")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .header("Referer", "https://open.spotify.com/")
                .header("Sec-Fetch-Dest", "empty")
                .header("Sec-Fetch-Mode", "cors")
                .header("Sec-Fetch-Site", "same-origin")
                .build()
            httpClient.newCall(tokenReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val body = resp.body?.string() ?: ""
                    val tokenJson = JSONObject(body)
                    accessToken = tokenJson.optString("accessToken").takeIf { it.isNotEmpty() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If direct token call was blocked, extract token from Spotify webpage HTML
        if (accessToken.isNullOrEmpty()) {
            try {
                val pageHtml = fetchHtml("https://open.spotify.com/playlist/$playlistId")
                accessToken = extractAccessTokenFromHtml(pageHtml)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fetch tracks using Spotify Web API with full pagination up to SPOTIFY_MAX_IMPORT_SONGS (3,000)
        if (!accessToken.isNullOrEmpty()) {
            try {
                // Fetch playlist name
                val nameReq = Request.Builder()
                    .url("https://api.spotify.com/v1/playlists/$playlistId?fields=name")
                    .header("Authorization", "Bearer $accessToken")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                    .build()
                httpClient.newCall(nameReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val body = resp.body?.string() ?: ""
                        val pObj = JSONObject(body)
                        val name = pObj.optString("name")
                        if (name.isNotEmpty()) {
                            playlistName = name
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Paginate in 100-item batches up to SPOTIFY_MAX_IMPORT_SONGS (3,000 songs)
            var offset = 0
            var hasMore = true
            while (hasMore && tracks.size < SPOTIFY_MAX_IMPORT_SONGS) {
                val limit = minOf(100, SPOTIFY_MAX_IMPORT_SONGS - tracks.size)
                try {
                    val tracksReq = Request.Builder()
                        .url("https://api.spotify.com/v1/playlists/$playlistId/tracks?offset=$offset&limit=$limit&fields=items(track(name,artists(name))),next,total")
                        .header("Authorization", "Bearer $accessToken")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                        .build()

                    val respBody = httpClient.newCall(tracksReq).execute().use { resp ->
                        if (resp.isSuccessful) resp.body?.string() else null
                    }

                    if (respBody.isNullOrEmpty()) {
                        hasMore = false
                        break
                    }

                    val tracksJson = JSONObject(respBody)
                    val items = tracksJson.optJSONArray("items")
                    if (items == null || items.length() == 0) {
                        hasMore = false
                        break
                    }

                    for (i in 0 until items.length()) {
                        if (tracks.size >= SPOTIFY_MAX_IMPORT_SONGS) break
                        val itemObj = items.optJSONObject(i) ?: continue
                        val trackObj = itemObj.optJSONObject("track") ?: continue
                        val title = trackObj.optString("name")
                        val artistsArr = trackObj.optJSONArray("artists")
                        val artist = if (artistsArr != null && artistsArr.length() > 0) {
                            artistsArr.getJSONObject(0).optString("name")
                        } else {
                            ""
                        }
                        if (title.isNotEmpty()) {
                            tracks.add(title to artist)
                        }
                    }

                    val next = tracksJson.optString("next")
                    if (next.isNullOrEmpty() || next == "null") {
                        hasMore = false
                    } else {
                        offset += items.length()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    hasMore = false
                }
            }
        }

        // 3. Fallback to existing embed scraping if tracks is still empty
        if (tracks.isEmpty()) {
            try {
                val html = fetchHtml("https://open.spotify.com/embed/playlist/$playlistId")
                val doc = Jsoup.parse(html)

                val nextDataScript = doc.getElementById("__NEXT_DATA__")
                val jsonText = nextDataScript?.data()?.trim() ?: ""

                if (jsonText.isNotEmpty()) {
                    val root = JSONObject(jsonText)
                    val pageProps = root.optJSONObject("props")?.optJSONObject("pageProps")
                    val state = pageProps?.optJSONObject("state")
                    val entity = state?.optJSONObject("data")?.optJSONObject("entity")
                    if (entity != null) {
                        playlistName = entity.optString("name", playlistName).takeIf { it.isNotEmpty() } ?: playlistName
                        val trackList = entity.optJSONArray("trackList")
                        if (trackList != null) {
                            for (i in 0 until trackList.length()) {
                                if (tracks.size >= SPOTIFY_MAX_IMPORT_SONGS) break
                                val trackObj = trackList.getJSONObject(i)
                                val title = trackObj.optString("title")
                                val subtitle = trackObj.optString("subtitle")
                                if (title.isNotEmpty()) {
                                    tracks.add(title to subtitle)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        Pair(playlistName, tracks.take(SPOTIFY_MAX_IMPORT_SONGS))
    }

    internal suspend fun fetchAppleMusicTracks(resolvedUrl: String): Pair<String, List<Pair<String, String>>> = withContext(Dispatchers.IO) {
        val html = try {
            fetchHtml(resolvedUrl)
        } catch (e: Exception) {
            // Apple Music is optional, provide a clean, descriptive message if WAF blocks it
            throw Exception("Apple Music block: ${e.localizedMessage}. Please try Spotify/YouTube.")
        }
        val doc = Jsoup.parse(html)
        var playlistName = doc.title().replace(" on Apple Music", "").trim()
        val tracks = mutableListOf<Pair<String, String>>()

        val scripts = doc.select("script[type=application/ld+json]")
        for (script in scripts) {
            val jsonText = script.data().trim()
            try {
                val root = JSONObject(jsonText)
                val type = root.optString("@type")
                if (type == "MusicPlaylist" || root.has("track") || root.has("itemListElement")) {
                    playlistName = root.optString("name", playlistName)
                    val trackArray = root.optJSONArray("track") ?: root.optJSONArray("itemListElement")
                    if (trackArray != null) {
                        for (i in 0 until trackArray.length()) {
                            if (tracks.size >= MAX_IMPORT_SONGS) break
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
                    if (tracks.isNotEmpty()) break
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback for Apple Music page scraping if JSON-LD wasn't fully parsed
        if (tracks.isEmpty()) {
            val songElements = doc.select("div.songs-list-row, li.songs-list-row, div[data-testid='track-cell'], div[role='row']")
            for (el in songElements) {
                if (tracks.size >= MAX_IMPORT_SONGS) break
                val title = el.select("div.songs-list-row__song-name, .songs-list-row__song-name, div[data-testid='track-title']").text().trim()
                val artist = el.select("a.songs-list-row__link, .songs-list-row__by-line, div[data-testid='track-subtitle']").text().trim()
                if (title.isNotEmpty()) {
                    tracks.add(title to artist)
                }
            }
        }

        Pair(playlistName, tracks.take(MAX_IMPORT_SONGS))
    }

    suspend fun importPlaylist(
        database: MusicDatabase,
        url: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val trimmedUrl = url.trim()
            val resolvedUrl = if (trimmedUrl.contains("spotify.link")) {
                resolveRedirect(trimmedUrl)
            } else {
                trimmedUrl
            }
            
            // Extract YouTube playlist ID from URL or use raw ID if provided
            val youtubePlaylistId = when {
                resolvedUrl.contains("youtube.com") || resolvedUrl.contains("youtu.be") -> {
                    val uri = Uri.parse(resolvedUrl)
                    uri.getQueryParameter("list")
                }
                resolvedUrl.startsWith("PL") || resolvedUrl.startsWith("OLAK") -> {
                    resolvedUrl
                }
                else -> null
            }
            
            if (youtubePlaylistId != null) {
                val initialPage = YouTube.playlist(youtubePlaylistId).getOrThrow()
                val playlistName = initialPage.playlist.title ?: "Imported YouTube Playlist"
                
                val allSongs = initialPage.songs.toMutableList()
                var continuation = initialPage.songsContinuation ?: initialPage.continuation
                val seenContinuations = mutableSetOf<String>()

                while (continuation != null && allSongs.size < MAX_IMPORT_SONGS) {
                    if (continuation in seenContinuations) break
                    seenContinuations.add(continuation)

                    val continuationPage = YouTube.playlistContinuation(continuation).getOrNull() ?: break
                    if (continuationPage.songs.isEmpty()) break

                    allSongs.addAll(continuationPage.songs)
                    continuation = continuationPage.continuation
                }

                val finalSongs = allSongs.take(MAX_IMPORT_SONGS)

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

                    finalSongs.forEachIndexed { index, songItem ->
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
                val (playlistName, tracks) = fetchSpotifyTracks(playlistId)

                if (tracks.isEmpty()) {
                    return@runCatching Result.failure<String>(Exception("No tracks found in Spotify playlist")).getOrThrow()
                }

                val finalTracks = tracks.take(SPOTIFY_MAX_IMPORT_SONGS)

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

                // Resolve all tracks in chunks of 5 to avoid connection flooding/rate-limiting
                val results = mutableListOf<Triple<Int, com.mudassir131.yt.models.MediaMetadata, String?>>()
                val chunks = finalTracks.mapIndexed { index, pair -> index to pair }.chunked(5)
                for (chunk in chunks) {
                    val deferreds = chunk.map { (index, pair) ->
                        async {
                            runCatching {
                                val (songName, artistName) = pair
                                val query = "$songName $artistName".trim()
                                val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                val songItem = searchResult?.items?.firstOrNull() as? SongItem
                                if (songItem != null) {
                                    Triple(index, songItem.toMediaMetadata(), songItem.setVideoId)
                                } else {
                                    null
                                }
                            }.getOrNull()
                        }
                    }
                    results.addAll(deferreds.awaitAll().filterNotNull())
                }

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
                val (playlistName, tracks) = fetchAppleMusicTracks(resolvedUrl)

                if (tracks.isEmpty()) {
                    return@runCatching Result.failure<String>(Exception("No tracks found in Apple Music playlist")).getOrThrow()
                }

                val finalTracks = tracks.take(MAX_IMPORT_SONGS)

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

                // Resolve all tracks in chunks of 5 to avoid connection flooding/rate-limiting
                val results = mutableListOf<Triple<Int, com.mudassir131.yt.models.MediaMetadata, String?>>()
                val chunks = finalTracks.mapIndexed { index, pair -> index to pair }.chunked(5)
                for (chunk in chunks) {
                    val deferreds = chunk.map { (index, pair) ->
                        async {
                            runCatching {
                                val (songName, artistName) = pair
                                val query = "$songName $artistName".trim()
                                val searchResult = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrNull()
                                val songItem = searchResult?.items?.firstOrNull() as? SongItem
                                if (songItem != null) {
                                    Triple(index, songItem.toMediaMetadata(), songItem.setVideoId)
                                } else {
                                    null
                                }
                            }.getOrNull()
                        }
                    }
                    results.addAll(deferreds.awaitAll().filterNotNull())
                }

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
