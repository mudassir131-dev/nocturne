/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



@file:OptIn(ExperimentalCoroutinesApi::class)

package com.mudassir131.yt.viewmodels

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.exoplayer.offline.Download
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.constants.AlbumFilter
import com.mudassir131.yt.constants.AlbumFilterKey
import com.mudassir131.yt.constants.AlbumSortDescendingKey
import com.mudassir131.yt.constants.AlbumSortType
import com.mudassir131.yt.constants.AlbumSortTypeKey
import com.mudassir131.yt.constants.ArtistFilter
import com.mudassir131.yt.constants.ArtistFilterKey
import com.mudassir131.yt.constants.ArtistSongSortDescendingKey
import com.mudassir131.yt.constants.ArtistSongSortType
import com.mudassir131.yt.constants.ArtistSongSortTypeKey
import com.mudassir131.yt.constants.ArtistSortDescendingKey
import com.mudassir131.yt.constants.ArtistSortType
import com.mudassir131.yt.constants.ArtistSortTypeKey
import com.mudassir131.yt.constants.HideExplicitKey
import com.mudassir131.yt.constants.HideVideoKey
import com.mudassir131.yt.constants.LibraryFilter
import com.mudassir131.yt.constants.PlaylistSortDescendingKey
import com.mudassir131.yt.constants.PlaylistSortType
import com.mudassir131.yt.constants.PlaylistSortDescendingKey
import com.mudassir131.yt.constants.PlaylistSortTypeKey
import com.mudassir131.yt.constants.SongFilter
import com.mudassir131.yt.constants.SongFilterKey
import com.mudassir131.yt.constants.SongSortDescendingKey
import com.mudassir131.yt.constants.SongSortType
import com.mudassir131.yt.constants.SongSortTypeKey
import com.mudassir131.yt.constants.TopSize
import com.mudassir131.yt.constants.ContentFilterMode
import com.mudassir131.yt.constants.ContentFilterModeKey
import com.mudassir131.yt.utils.filterSongsByContentMode
import com.mudassir131.yt.utils.filterLocalItemsByContentMode
import com.mudassir131.yt.db.MusicDatabase
import com.mudassir131.yt.db.entities.Song
import com.mudassir131.yt.extensions.filterExplicit
import com.mudassir131.yt.extensions.filterExplicitAlbums
import com.mudassir131.yt.extensions.reversed
import com.mudassir131.yt.extensions.toEnum
import com.mudassir131.yt.playback.DownloadUtil
import com.mudassir131.yt.utils.SyncUtils
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.get
import com.mudassir131.yt.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.Collator
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LibrarySongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val allSongs =
        context.dataStore.data
            .map {
                val filterSort = Triple(
                    it[SongFilterKey].toEnum(SongFilter.LIKED),
                    it[SongSortTypeKey].toEnum(SongSortType.CREATE_DATE),
                    (it[SongSortDescendingKey] ?: true)
                )
                val hideExplicit = it[HideExplicitKey] ?: false
                val hideVideo = it[HideVideoKey] ?: false
                val mode = it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
                Pair(Pair(filterSort, hideExplicit), Pair(hideVideo, mode))
            }.distinctUntilChanged()
            .flatMapLatest { (firstPart, secondPart) ->
                val (filterSort, hideExplicit) = firstPart
                val (hideVideo, mode) = secondPart
                val (filter, sortType, descending) = filterSort
                when (filter) {
                    SongFilter.LIBRARY -> database.songs(sortType, descending, hideVideo)
                        .map { it.filterExplicit(hideExplicit).filterSongsByContentMode(mode) }
                    SongFilter.LIKED -> database.likedSongs(sortType, descending, hideVideo)
                        .map { it.filterExplicit(hideExplicit).filterSongsByContentMode(mode) }
                    SongFilter.DOWNLOADED ->
                        downloadUtil.downloads.flatMapLatest { downloads ->
                            database
                                .allSongs()
                                .flowOn(Dispatchers.IO)
                                .map { songs ->
                                    songs.filter { song: Song ->
                                        downloads[song.id]?.state == Download.STATE_COMPLETED
                                    }
                                }.map { songs ->
                                    when (sortType) {
                                        SongSortType.CREATE_DATE -> songs.sortedBy { song: Song ->
                                            downloads[song.id]?.updateTimeMs ?: 0L
                                        }

                                        SongSortType.NAME -> songs.sortedBy { song: Song -> song.song.title }
                                        SongSortType.ARTIST -> {
                                            val collator =
                                                Collator.getInstance(Locale.getDefault())
                                            collator.strength = Collator.PRIMARY
                                            songs
                                                .sortedWith(
                                                    compareBy(collator) { song: Song ->
                                                        song.artists.joinToString("") { artist -> artist.name }
                                                    },
                                                ).groupBy { it.album?.title }
                                                .flatMap { (_, songsByAlbum) ->
                                                    songsByAlbum.sortedBy { album ->
                                                        album.artists.joinToString(
                                                            "",
                                                        ) { artist -> artist.name }
                                                    }
                                                }
                                        }

                                        SongSortType.PLAY_TIME -> songs.sortedBy { song: Song -> song.song.totalPlayTime }
                                    }.reversed(descending).filterExplicit(hideExplicit).filterSongsByContentMode(mode)
                                }
                        }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refresh(filter: SongFilter) {
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                when (filter) {
                    SongFilter.LIKED -> syncUtils.syncLikedSongs()
                    SongFilter.LIBRARY -> syncUtils.syncLibrarySongs()
                    SongFilter.DOWNLOADED -> Unit
                }
            } catch (e: Exception) {
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun syncLikedSongs() {
        refresh(SongFilter.LIKED)
    }

    fun syncLibrarySongs() {
        refresh(SongFilter.LIBRARY)
    }
}

@HiltViewModel
class LibraryArtistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val allArtists =
        context.dataStore.data
            .map {
                val filter = it[ArtistFilterKey].toEnum(ArtistFilter.LIKED)
                val sortType = it[ArtistSortTypeKey].toEnum(ArtistSortType.CREATE_DATE)
                val descending = it[ArtistSortDescendingKey] ?: true
                val mode = it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
                Pair(Triple(filter, sortType, descending), mode)
            }.distinctUntilChanged()
            .flatMapLatest { (triple, mode) ->
                val (filter, sortType, descending) = triple
                when (filter) {
                    ArtistFilter.LIBRARY -> database.artists(sortType, descending)
                    ArtistFilter.LIKED -> database.artistsBookmarked(sortType, descending)
                }.map { it.filterLocalItemsByContentMode(mode) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refresh(filter: ArtistFilter) {
        if (filter != ArtistFilter.LIKED) return
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                syncUtils.syncArtistsSubscriptions()
            } catch (e: Exception) {
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun sync() {
        refresh(ArtistFilter.LIKED)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allArtists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null || Duration.between(
                            it.lastUpdateTime,
                            LocalDateTime.now()
                        ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryAlbumsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    downloadUtil: DownloadUtil,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    val allAlbums =
        context.dataStore.data
            .map {
                val filter = it[AlbumFilterKey].toEnum(AlbumFilter.LIKED)
                val sortType = it[AlbumSortTypeKey].toEnum(AlbumSortType.CREATE_DATE)
                val descending = it[AlbumSortDescendingKey] ?: true
                val hideExplicit = it[HideExplicitKey] ?: false
                val mode = it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
                Pair(Triple(filter, sortType, descending), Pair(hideExplicit, mode))
            }.distinctUntilChanged()
            .flatMapLatest { (triple, pair) ->
                val (filter, sortType, descending) = triple
                val (hideExplicit, mode) = pair
                when (filter) {
                    AlbumFilter.DOWNLOADED ->
                        downloadUtil.downloads.flatMapLatest { downloads ->
                            database.allSongs()
                                .flowOn(Dispatchers.IO)
                                .map { songs ->
                                    songs
                                        .filter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }
                                        .mapNotNull { it.song.albumId }
                                        .toSet()
                                }.flatMapLatest { downloadedAlbumIds ->
                                    database.albumsByIds(downloadedAlbumIds, sortType, descending)
                                        .map { albums -> albums.filterExplicitAlbums(hideExplicit).filterLocalItemsByContentMode(mode) }
                                }
                        }
                    
                        AlbumFilter.DOWNLOADED_FULL ->
                            downloadUtil.downloads.flatMapLatest { downloads ->
                                database.allSongs()
                                    .flowOn(Dispatchers.IO)
                                    .map { songs ->
                                        songs
                                            .filter { song -> downloads[song.id]?.state == Download.STATE_COMPLETED }
                                            .mapNotNull { song -> song.song.albumId?.let { albumId -> albumId to song } }
                                            .groupBy({ it.first }, { it.second })
                                            .mapValues { (_, songList) -> songList.size }
                                    }.flatMapLatest { downloadedCountByAlbum ->
                                        database.albumsByIds(downloadedCountByAlbum.keys, sortType, descending)
                                            .map { albums ->
                                                albums.filter { album ->
                                                    val totalSongsInAlbum = album.album.songCount
                                                    val downloadedSongsCount = downloadedCountByAlbum[album.album.id] ?: 0
                                                    totalSongsInAlbum > 0 && downloadedSongsCount >= totalSongsInAlbum
                                                }.filterExplicitAlbums(hideExplicit).filterLocalItemsByContentMode(mode)
                                            }
                                    }
                            }
                    AlbumFilter.LIBRARY -> database.albums(sortType, descending).map { it.filterExplicitAlbums(hideExplicit).filterLocalItemsByContentMode(mode) }
                    AlbumFilter.LIKED -> database.albumsLiked(sortType, descending).map { it.filterExplicitAlbums(hideExplicit).filterLocalItemsByContentMode(mode) }
                }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refresh(filter: AlbumFilter) {
        if (filter != AlbumFilter.LIKED) return
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                syncUtils.syncLikedAlbums()
            } catch (e: Exception) {
                reportException(e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun sync() {
        refresh(AlbumFilter.LIKED)
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            allAlbums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryPlaylistsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val allPlaylists =
        context.dataStore.data
            .map {
                val sortType = it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CUSTOM)
                val descending = it[PlaylistSortDescendingKey] ?: true
                val mode = it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
                Pair(Pair(sortType, descending), mode)
            }.distinctUntilChanged()
            .flatMapLatest { (sortDesc, mode) ->
                val (sortType, descending) = sortDesc
                database.playlists(sortType, descending).map { it.filterLocalItemsByContentMode(mode) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun sync() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            syncUtils.syncSavedPlaylists()
            syncUtils.syncAutoSyncPlaylists()
            _isRefreshing.value = false
        }
    }

    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
}

@HiltViewModel
class ArtistSongsViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val artistId = savedStateHandle.get<String>("artistId")!!
    val artist =
        database
            .artist(artistId)
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val songs =
        context.dataStore.data
            .map {
                val sortType = it[ArtistSongSortTypeKey].toEnum(ArtistSongSortType.CREATE_DATE)
                val descending = it[ArtistSongSortDescendingKey] ?: true
                val hideExplicit = it[HideExplicitKey] ?: false
                val mode = it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
                Pair(Triple(sortType, descending, hideExplicit), mode)
            }.distinctUntilChanged()
            .flatMapLatest { (triple, mode) ->
                val (sortType, descending, hideExplicit) = triple
                database.artistSongs(artistId, sortType, descending)
                    .map { it.filterExplicit(hideExplicit).filterSongsByContentMode(mode) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}

@HiltViewModel
class LibraryMixViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    database: MusicDatabase,
    private val syncUtils: SyncUtils,
) : ViewModel() {
    val syncAllLibrary = {
         viewModelScope.launch(Dispatchers.IO) {
             try {
                 syncUtils.performFullSync()
             } catch (e: Exception) {
                 timber.log.Timber.e(e, "Error during manual sync")
             }
         }
    }
    val topValue =
        context.dataStore.data
            .map { it[TopSize] ?: "50" }
            .distinctUntilChanged()
    var artists = context.dataStore.data
        .map { it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL) }
        .distinctUntilChanged()
        .flatMapLatest { mode ->
            database.artistsBookmarked(ArtistSortType.CREATE_DATE, true)
                .map { it.filterLocalItemsByContentMode(mode) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var albums = context.dataStore.data
        .map {
            val hideExplicit = it[HideExplicitKey] ?: false
            val mode = it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
            hideExplicit to mode
        }
        .distinctUntilChanged()
        .flatMapLatest { (hideExplicit, mode) ->
            database.albumsLiked(AlbumSortType.CREATE_DATE, true)
                .map { it.filterExplicitAlbums(hideExplicit).filterLocalItemsByContentMode(mode) }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    var playlists =
        context.dataStore.data
            .map {
                val sortType = it[PlaylistSortTypeKey].toEnum(PlaylistSortType.CUSTOM)
                val descending = it[PlaylistSortDescendingKey] ?: true
                val mode = it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
                Triple(sortType, descending, mode)
            }.distinctUntilChanged()
            .flatMapLatest { (sortType, descending, mode) ->
                database.playlists(sortType, descending)
                    .map { it.filterLocalItemsByContentMode(mode) }
            }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            albums.collect { albums ->
                albums
                    .filter {
                        it.album.songCount == 0
                    }.forEach { album ->
                        YouTube
                            .album(album.id)
                            .onSuccess { albumPage ->
                                database.query {
                                    update(album.album, albumPage, album.artists)
                                }
                            }.onFailure {
                                reportException(it)
                                if (it.message?.contains("NOT_FOUND") == true) {
                                    database.query {
                                        delete(album.album)
                                    }
                                }
                            }
                    }
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            artists.collect { artists ->
                artists
                    .map { it.artist }
                    .filter {
                        it.thumbnailUrl == null ||
                                Duration.between(
                                    it.lastUpdateTime,
                                    LocalDateTime.now(),
                                ) > Duration.ofDays(10)
                    }.forEach { artist ->
                        YouTube.artist(artist.id).onSuccess { artistPage ->
                            database.query {
                                update(artist, artistPage)
                            }
                        }
                    }
            }
        }
    }
}

@HiltViewModel
class LibraryViewModel
@Inject
constructor() : ViewModel() {
    private val curScreen = mutableStateOf(LibraryFilter.LIBRARY)
    val filter: MutableState<LibraryFilter> = curScreen
}
