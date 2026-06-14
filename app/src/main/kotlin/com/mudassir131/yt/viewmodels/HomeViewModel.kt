/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.PlaylistItem
import com.mudassir131.yt.innertube.models.WatchEndpoint
import com.mudassir131.yt.innertube.models.YTItem
import com.mudassir131.yt.innertube.models.filterExplicit
import com.mudassir131.yt.innertube.models.filterVideo
import com.mudassir131.yt.innertube.pages.ExplorePage
import com.mudassir131.yt.innertube.pages.HomePage
import com.mudassir131.yt.innertube.utils.completed
import com.mudassir131.yt.innertube.utils.parseCookieString
import com.mudassir131.yt.constants.HideExplicitKey
import com.mudassir131.yt.constants.HideVideoKey
import com.mudassir131.yt.constants.InnerTubeCookieKey
import com.mudassir131.yt.constants.QuickPicks
import com.mudassir131.yt.constants.QuickPicksKey
import com.mudassir131.yt.constants.YtmSyncKey
import com.mudassir131.yt.constants.ContentFilterMode
import com.mudassir131.yt.constants.ContentFilterModeKey
import com.mudassir131.yt.utils.filterSongsByContentMode
import com.mudassir131.yt.utils.filterLocalItemsByContentMode
import com.mudassir131.yt.utils.filterYTItemsByContentMode
import com.mudassir131.yt.db.MusicDatabase
import com.mudassir131.yt.db.entities.*
import com.mudassir131.yt.extensions.toEnum
import com.mudassir131.yt.models.SimilarRecommendation
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.get
import com.mudassir131.yt.utils.SyncUtils
import com.mudassir131.yt.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val syncUtils: SyncUtils,
    val forYouEngine: com.mudassir131.yt.utils.ForYouSuggestionEngine,
) : ViewModel() {
    val isRefreshing = MutableStateFlow(false)
    val isLoading = MutableStateFlow(false)
    private val isInitialLoadComplete = MutableStateFlow(false)
    val contentFilterMode = context.dataStore.data.map {
        it[ContentFilterModeKey].toEnum(ContentFilterMode.GLOBAL)
    }.distinctUntilChanged()

    private val _forYouSuggestions = MutableStateFlow<List<com.mudassir131.yt.innertube.models.SongItem>?>(null)
    val forYouSuggestions = combine(_forYouSuggestions, contentFilterMode) { list, mode ->
        list?.filterYTItemsByContentMode(mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val quickPicksEnum = context.dataStore.data.map {
        it[QuickPicksKey].toEnum(QuickPicks.QUICK_PICKS)
    }.distinctUntilChanged()

    private val _quickPicks = MutableStateFlow<List<Song>?>(null)
    val quickPicks = combine(_quickPicks, contentFilterMode) { list, mode ->
        list?.filterSongsByContentMode(mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _forgottenFavorites = MutableStateFlow<List<Song>?>(null)
    val forgottenFavorites = combine(_forgottenFavorites, contentFilterMode) { list, mode ->
        list?.filterSongsByContentMode(mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _keepListening = MutableStateFlow<List<LocalItem>?>(null)
    val keepListening = combine(_keepListening, contentFilterMode) { list, mode ->
        list?.filterLocalItemsByContentMode(mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _similarRecommendations = MutableStateFlow<List<SimilarRecommendation>?>(null)
    val similarRecommendations = combine(_similarRecommendations, contentFilterMode) { list, mode ->
        list?.mapNotNull { rec ->
            val filteredItems = rec.items.filterYTItemsByContentMode(mode)
            if (filteredItems.isNotEmpty() || mode == ContentFilterMode.GLOBAL) {
                rec.copy(items = filteredItems)
            } else {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _accountPlaylists = MutableStateFlow<List<PlaylistItem>?>(null)
    val accountPlaylists = combine(_accountPlaylists, contentFilterMode) { list, mode ->
        list?.filterYTItemsByContentMode(mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _homePage = MutableStateFlow<HomePage?>(null)
    val homePage = combine(_homePage, contentFilterMode) { page, mode ->
        page?.copy(
            sections = page.sections.mapNotNull { section ->
                val filteredItems = section.items.filterYTItemsByContentMode(mode)
                if (filteredItems.isNotEmpty() || mode == ContentFilterMode.GLOBAL) {
                    section.copy(items = filteredItems)
                } else {
                    null
                }
            }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _explorePage = MutableStateFlow<ExplorePage?>(null)
    val explorePage = combine(_explorePage, contentFilterMode) { page, mode ->
        page?.copy(
            newReleaseAlbums = page.newReleaseAlbums.filterYTItemsByContentMode(mode)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val selectedChip = MutableStateFlow<HomePage.Chip?>(null)
    private val previousHomePage = MutableStateFlow<HomePage?>(null)

    val recentActivity = MutableStateFlow<List<YTItem>?>(null)
    val recentPlaylistsDb = MutableStateFlow<List<Playlist>?>(null)

    private val _allLocalItems = MutableStateFlow<List<LocalItem>>(emptyList())
    val allLocalItems = combine(_allLocalItems, contentFilterMode) { list, mode ->
        list.filterLocalItemsByContentMode(mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _allYtItems = MutableStateFlow<List<YTItem>>(emptyList())
    val allYtItems = combine(_allYtItems, contentFilterMode) { list, mode ->
        list.filterYTItemsByContentMode(mode)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Account display info
    val accountName = MutableStateFlow<String?>(null)
    val accountImageUrl = MutableStateFlow<String?>(null)
    
    // Track last processed cookie to avoid unnecessary updates
    private var lastProcessedCookie: String? = null
    
    // Track if we're currently processing account data
    private var isProcessingAccountData = false
    private var wasLoggedIn = false

    private fun filterHomeChips(chips: List<HomePage.Chip>?): List<HomePage.Chip>? {
        return chips?.filterNot { it.title.contains("podcasts", ignoreCase = true) }
    }

    private suspend fun getQuickPicks(){
        when (quickPicksEnum.first()) {
            QuickPicks.QUICK_PICKS -> _quickPicks.value = database.quickPicks().first().shuffled().take(20)
            QuickPicks.LAST_LISTEN -> songLoad()
        }
    }

    private suspend fun load() {
        if (isLoading.value) return
        isLoading.value = true
        
        try {
            supervisorScope {
                val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                val hideVideo = context.dataStore.get(HideVideoKey, false)
                val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2

                launch { getQuickPicks() }
                launch { _forgottenFavorites.value = database.forgottenFavorites().first().shuffled().take(20) }
                launch {
                    try {
                        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                        val hideVideo = context.dataStore.get(HideVideoKey, false)
                        _forYouSuggestions.value = forYouEngine.getSuggestions(hideExplicit, hideVideo)
                    } catch (_: Exception) {}
                }
                
                launch {
                    val keepListeningSongs = database.mostPlayedSongs(fromTimeStamp, limit = 15, offset = 5)
                        .first().shuffled().take(10)
                    val keepListeningAlbums = database.mostPlayedAlbums(fromTimeStamp, limit = 8, offset = 2)
                        .first().filter { it.album.thumbnailUrl != null }.shuffled().take(5)
                    val keepListeningArtists = database.mostPlayedArtists(fromTimeStamp)
                        .first().filter { it.artist.isYouTubeArtist && it.artist.thumbnailUrl != null }
                        .shuffled().take(5)
                    _keepListening.value = (keepListeningSongs + keepListeningAlbums + keepListeningArtists).shuffled()
                }

                launch {
                        YouTube.home().onSuccess { page ->
                        _homePage.value = page.copy(
                            chips = filterHomeChips(page.chips),
                            sections = page.sections.map { section ->
                                section.copy(items = section.items.filterExplicit(hideExplicit).filterVideo(hideVideo))
                            }
                        )
                    }.onFailure { reportException(it) }
                }

                launch {
                    YouTube.explore().onSuccess { page ->
                        val artists: MutableMap<Int, String> = mutableMapOf()
                        val favouriteArtists: MutableMap<Int, String> = mutableMapOf()
                        database.allArtistsByPlayTime().first().let { list ->
                            var favIndex = 0
                            for ((artistsIndex, artist) in list.withIndex()) {
                                artists[artistsIndex] = artist.id
                                if (artist.artist.bookmarkedAt != null) {
                                    favouriteArtists[favIndex] = artist.id
                                    favIndex++
                                }
                            }
                        }
                        _explorePage.value = page.copy(
                            newReleaseAlbums = page.newReleaseAlbums
                                .sortedBy { album ->
                                    val artistIds = album.artists.orEmpty().mapNotNull { it.id }
                                    val firstArtistKey = artistIds.firstNotNullOfOrNull { artistId ->
                                        if (artistId in favouriteArtists.values) {
                                            favouriteArtists.entries.firstOrNull { it.value == artistId }?.key
                                        } else {
                                            artists.entries.firstOrNull { it.value == artistId }?.key
                                        }
                                    } ?: Int.MAX_VALUE
                                    firstArtistKey
                                }.filterExplicit(hideExplicit)
                        )
                    }.onFailure { reportException(it) }
                }
            }

            _allLocalItems.value = (_quickPicks.value.orEmpty() + _forgottenFavorites.value.orEmpty() + _keepListening.value.orEmpty())
                .filter { it is Song || it is Album }

            viewModelScope.launch(Dispatchers.IO) {
                loadSimilarRecommendations()
            }

            _allYtItems.value = _similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                    _homePage.value?.sections?.flatMap { it.items }.orEmpty()
                    
            isInitialLoadComplete.value = true
        } catch (e: Exception) {
            reportException(e)
        } finally {
            isLoading.value = false
        }
    }

    private suspend fun loadSimilarRecommendations() {
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideo = context.dataStore.get(HideVideoKey, false)
        val fromTimeStamp = System.currentTimeMillis() - 86400000 * 7 * 2
        
        val artistRecommendations = database.mostPlayedArtists(fromTimeStamp, limit = 10).first()
            .filter { it.artist.isYouTubeArtist }
            .shuffled().take(3)
            .mapNotNull {
                val items = mutableListOf<YTItem>()
                YouTube.artist(it.id).onSuccess { page ->
                    items += page.sections.getOrNull(page.sections.size - 2)?.items.orEmpty()
                    items += page.sections.lastOrNull()?.items.orEmpty()
                }
                SimilarRecommendation(
                    title = it,
                    items = items.filterExplicit(hideExplicit).filterVideo(hideVideo).shuffled().ifEmpty { return@mapNotNull null }
                )
            }

        val songRecommendations = database.mostPlayedSongs(fromTimeStamp, limit = 10).first()
            .filter { it.album != null }
            .shuffled().take(2)
            .mapNotNull { song ->
                val endpoint = YouTube.next(WatchEndpoint(videoId = song.id)).getOrNull()?.relatedEndpoint
                    ?: return@mapNotNull null
                val page = YouTube.related(endpoint).getOrNull() ?: return@mapNotNull null
                SimilarRecommendation(
                    title = song,
                    items = (page.songs.shuffled().take(8) +
                            page.albums.shuffled().take(4) +
                            page.artists.shuffled().take(4) +
                            page.playlists.shuffled().take(4))
                        .filterExplicit(hideExplicit).filterVideo(hideVideo)
                        .shuffled()
                        .ifEmpty { return@mapNotNull null }
                )
            }

        _similarRecommendations.value = (artistRecommendations + songRecommendations).shuffled()
        
        _allYtItems.value = _similarRecommendations.value?.flatMap { it.items }.orEmpty() +
                _homePage.value?.sections?.flatMap { it.items }.orEmpty()
    }

    private suspend fun songLoad() {
        val song = database.events().first().firstOrNull()?.song
        if (song != null) {
            if (database.hasRelatedSongs(song.id)) {
                val relatedSongs = database.getRelatedSongs(song.id).first().shuffled().take(20)
                _quickPicks.value = relatedSongs
            }
        }
    }

    private val _isLoadingMore = MutableStateFlow(false)
    fun loadMoreYouTubeItems(continuation: String?) {
        if (continuation == null || _isLoadingMore.value) return
        val hideExplicit = context.dataStore.get(HideExplicitKey, false)
        val hideVideo = context.dataStore.get(HideVideoKey, false)

        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingMore.value = true
            val nextSections = YouTube.home(continuation).getOrNull() ?: run {
                _isLoadingMore.value = false
                return@launch
            }

            _homePage.value = nextSections.copy(
                chips = _homePage.value?.chips,
                sections = (_homePage.value?.sections.orEmpty() + nextSections.sections).map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideo(hideVideo))
                }
            )
            _isLoadingMore.value = false
        }
    }

    fun toggleChip(chip: HomePage.Chip?) {
        if (chip == null || chip == selectedChip.value && previousHomePage.value != null) {
            _homePage.value = previousHomePage.value
            previousHomePage.value = null
            selectedChip.value = null
            return
        }

        if (selectedChip.value == null) {
            previousHomePage.value = _homePage.value
        }

        viewModelScope.launch(Dispatchers.IO) {
            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
            val hideVideo = context.dataStore.get(HideVideoKey, false)
            val nextSections = YouTube.home(params = chip?.endpoint?.params).getOrNull() ?: return@launch

            _homePage.value = nextSections.copy(
                chips = _homePage.value?.chips,
                sections = nextSections.sections.map { section ->
                    section.copy(items = section.items.filterExplicit(hideExplicit).filterVideo(hideVideo))
                }
            )
            selectedChip.value = chip
        }
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing.value = true
            load()
            isRefreshing.value = false
        }
    }

    fun refreshAccountData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isProcessingAccountData) return@launch
            
            isProcessingAccountData = true
            try {
                val cookie = context.dataStore.get(InnerTubeCookieKey, "")
                if (cookie.isNotEmpty()) {
                    YouTube.cookie = cookie
                    
                    YouTube.accountInfo().onSuccess { info ->
                        accountName.value = info.name
                        accountImageUrl.value = info.thumbnailUrl
                    }.onFailure {
                        timber.log.Timber.w(it, "Failed to fetch account info")
                    }

                    launch {
                        YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                            val lists = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
                            _accountPlaylists.value = lists
                        }.onFailure {
                            timber.log.Timber.w(it, "Failed to fetch playlists")
                        }
                    }
                } else {
                    accountName.value = "Guest"
                    accountImageUrl.value = null
                    _accountPlaylists.value = null
                }
            } finally {
                isProcessingAccountData = false
            }
        }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            load()
        }

        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(3000)
            
            syncUtils.cleanupDuplicatePlaylists()
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            context.dataStore.data
                .map { it[InnerTubeCookieKey] }
                .distinctUntilChanged()
                .collect { cookie ->
                    if (isProcessingAccountData) return@collect
                    
                    lastProcessedCookie = cookie
                    isProcessingAccountData = true
                    
                    try {
                        val isLoggedIn = cookie?.let { "SAPISID" in parseCookieString(it) } ?: false
                        val loginTransition = isLoggedIn && !wasLoggedIn
                        wasLoggedIn = isLoggedIn
                        
                        if (isLoggedIn && cookie != null && cookie.isNotEmpty()) {
                            try {
                                YouTube.cookie = cookie
                            } catch (e: Exception) {
                                timber.log.Timber.e(e, "Failed to set YouTube cookie")
                                return@collect
                            }

                            if (loginTransition) {
                                launch {
                                    try {
                                        if (context.dataStore.get(YtmSyncKey, true)) {
                                            syncUtils.performFullSync()
                                        }
                                    } catch (e: Exception) {
                                        Timber.e(e, "Error during login-triggered sync")
                                        reportException(e)
                                    }
                                }
                            }
                            
                            kotlinx.coroutines.delay(100)
                            
                            try {
                                YouTube.accountInfo().onSuccess { info ->
                                    accountName.value = info.name
                                    accountImageUrl.value = info.thumbnailUrl
                                }.onFailure { e ->
                                    timber.log.Timber.w(e, "Failed to fetch account info")
                                }
                            } catch (e: Exception) {
                                timber.log.Timber.e(e, "Exception fetching account info")
                            }

                            launch {
                                try {
                                    YouTube.library("FEmusic_liked_playlists").completed().onSuccess {
                                        val lists = it.items.filterIsInstance<PlaylistItem>().filterNot { it.id == "SE" }
                                        _accountPlaylists.value = lists
                                    }.onFailure { e ->
                                        timber.log.Timber.w(e, "Failed to fetch account playlists")
                                    }
                                } catch (e: Exception) {
                                    timber.log.Timber.e(e, "Exception fetching account playlists")
                                }
                            }
                        } else {
                            accountName.value = "Guest"
                            accountImageUrl.value = null
                            _accountPlaylists.value = null
                        }
                    } catch (e: Exception) {
                        timber.log.Timber.e(e, "Error processing cookie change")
                        accountName.value = "Guest"
                        accountImageUrl.value = null
                        _accountPlaylists.value = null
                    } finally {
                        isProcessingAccountData = false
                    }
                }
        }
    }
}
