/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.material3.MaterialTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mudassir131.yt.innertube.models.AlbumItem
import com.mudassir131.yt.innertube.models.ArtistItem
import com.mudassir131.yt.innertube.models.PlaylistItem
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.innertube.utils.parseCookieString
import com.mudassir131.yt.LocalDatabase
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.LocalPlayerConnection
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.InnerTubeCookieKey
import com.mudassir131.yt.constants.DisableBlurKey
import com.mudassir131.yt.constants.ShowHomeCategoryChipsKey
import com.mudassir131.yt.constants.ProfileNameKey
import com.mudassir131.yt.db.entities.Album
import com.mudassir131.yt.db.entities.Artist
import com.mudassir131.yt.db.entities.Playlist
import com.mudassir131.yt.db.entities.Song
import com.mudassir131.yt.models.toMediaMetadata
import com.mudassir131.yt.playback.queues.LocalAlbumRadio
import com.mudassir131.yt.playback.queues.YouTubeAlbumRadio
import com.mudassir131.yt.playback.queues.YouTubeQueue
import com.mudassir131.yt.ui.component.ChipsRow
import com.mudassir131.yt.ui.component.AutoHidingRootScaffold
import com.mudassir131.yt.ui.component.NocturneDynamicScreen
import com.mudassir131.yt.ui.component.HideOnScrollFAB
import com.mudassir131.yt.ui.component.LocalBottomSheetPageState
import com.mudassir131.yt.ui.component.LocalMenuState
import com.mudassir131.yt.ui.component.NavigationTitle
import com.mudassir131.yt.ui.utils.SnapLayoutInfoProvider
import com.mudassir131.yt.utils.rememberPreference
import com.mudassir131.yt.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val quickPicks by viewModel.quickPicks.collectAsState()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsState()
    val keepListening by viewModel.keepListening.collectAsState()
    val homePage by viewModel.homePage.collectAsState()
    val explorePage by viewModel.explorePage.collectAsState()
    val forYouSuggestions by viewModel.forYouSuggestions.collectAsState()

    val allLocalItems by viewModel.allLocalItems.collectAsState()
    val allYtItems by viewModel.allYtItems.collectAsState()
    val selectedChip by viewModel.selectedChip.collectAsState()

    val isLoading: Boolean by viewModel.isLoading.collectAsState()
    val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsState()
    val accountImageUrl by viewModel.accountImageUrl.collectAsState()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val (showHomeCategoryChips) = rememberPreference(ShowHomeCategoryChipsKey, true)
    val (profileName) = rememberPreference(ProfileNameKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }
    val url = if (isLoggedIn) accountImageUrl else null
    val displayName = remember(profileName, accountName) {
        profileName.ifBlank { accountName?.takeIf { it.isNotBlank() } ?: "Listener" }
    }
    val curatedFeatures = remember(homePage) {
        homePage?.sections.orEmpty()
            .flatMap { section ->
                section.items.filterIsInstance<PlaylistItem>().map { playlist ->
                    CuratedFeature(category = section.title, playlist = playlist)
                }
            }
            .distinctBy { it.playlist.id }
            .take(24)
    }

    val scope = rememberCoroutineScope()
    val lazylistState = rememberLazyListState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { lazylistState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                val len = lazylistState.layoutInfo.totalItemsCount
                if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                    viewModel.loadMoreYouTubeItems(homePage?.continuation)
                }
            }
    }

    if (selectedChip != null) {
        BackHandler {

            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(showHomeCategoryChips, selectedChip) {
        if (!showHomeCategoryChips && selectedChip != null) {
            viewModel.toggleChip(selectedChip)
        }
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }


    NocturneDynamicScreen(disableBlur = disableBlur) {
        AutoHidingRootScaffold(
            header = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HomePersonalizedHeader(
                        name = displayName,
                        navController = navController,
                    )
                    if (showHomeCategoryChips) {
                        ChipsRow(
                            chips = homePage?.chips.orEmpty().map { it to it.title },
                            currentValue = selectedChip,
                            onValueUpdate = { viewModel.toggleChip(it) },
                        )
                    }
                }
            },
        ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh
                )
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val forgottenFavoritesSnapLayoutInfoProvider = remember(forgottenFavoritesLazyGridState) {
                SnapLayoutInfoProvider(
                    lazyGridState = forgottenFavoritesLazyGridState,
                    positionInLayout = { layoutSize, itemSize ->
                        (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                    }
                )
            }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues()
            ) {
                if (curatedFeatures.isNotEmpty()) {
                    item(key = "curated_trending") {
                        CuratedTrendingCarousel(
                            features = curatedFeatures,
                            navController = navController,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }

                quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
            /*
                item {
                    NavigationTitle(
                        title = stringResource(R.string.quick_picks),
                        modifier = Modifier.animateItem()
                    )
                }
            */

                item {
                    QuickPicksSection(
                        quickPicks = picks,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic
                    )
                }
            }


            quickPicks?.takeIf { it.isNotEmpty() }?.let { picks ->
                item {
                    QuickPicksListSection(
                        quickPicks = picks,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            keepListening?.takeIf { it.isNotEmpty() }?.let { items ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.keep_listening),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    KeepListeningSection(
                        keepListening = items,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        scope = scope
                    )
                }
            }

            AccountPlaylistsContainer(
                viewModel = viewModel,
                accountName = accountName,
                accountImageUrl = url,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )

            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { favorites ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.forgotten_favorites),
                        modifier = Modifier.animateItem()
                    )
                }

                item {
                    ForgottenFavoritesSection(
                        forgottenFavorites = favorites,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        horizontalLazyGridItemWidth = horizontalLazyGridItemWidth,
                        lazyGridState = forgottenFavoritesLazyGridState,
                        snapLayoutInfoProvider = forgottenFavoritesSnapLayoutInfoProvider,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic
                    )
                }
            }

            SimilarRecommendationsContainer(
                viewModel = viewModel,
                mediaMetadata = mediaMetadata,
                isPlaying = isPlaying,
                navController = navController,
                playerConnection = playerConnection,
                menuState = menuState,
                haptic = haptic,
                scope = scope
            )

            homePage?.sections?.forEach { section ->
                val isCommunity = section.title?.contains("community", ignoreCase = true) == true ||
                    section.title?.contains("From the", ignoreCase = true) == true ||
                    section.title?.contains("Trending", ignoreCase = true) == true &&
                    section.items.all { it is com.mudassir131.yt.innertube.models.PlaylistItem }

                if (isCommunity) {
                    item {
                        CommunityPlaylistsSection(
                            section = section,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic,
                            scope = scope,
                            modifier = Modifier.animateItem()
                        )
                    }
                } else {
                    item {
                        HomePageSectionTitle(
                            section = section,
                            navController = navController,
                            modifier = Modifier.animateItem()
                        )
                    }

                    item {
                        HomePageSectionContent(
                            section = section,
                            mediaMetadata = mediaMetadata,
                            isPlaying = isPlaying,
                            navController = navController,
                            playerConnection = playerConnection,
                            menuState = menuState,
                            haptic = haptic,
                            scope = scope
                        )
                    }
                }
            }

            if (isLoading || homePage?.continuation != null && homePage?.sections?.isNotEmpty() == true) {
                item {
                    HomeLoadingShimmer(modifier = Modifier.animateItem())
                }
            }

            forYouSuggestions?.takeIf { it.isNotEmpty() }?.let { suggestions ->
                item {
                    ForYouSection(
                        suggestions = suggestions,
                        mediaMetadata = mediaMetadata,
                        isPlaying = isPlaying,
                        navController = navController,
                        playerConnection = playerConnection,
                        menuState = menuState,
                        haptic = haptic,
                        modifier = Modifier.animateItem()
                    )
                }
            }

            explorePage?.moodAndGenres?.let { genres ->
                item {
                    NavigationTitle(
                        title = stringResource(R.string.mood_and_genres),
                        onClick = { navController.navigate("mood_and_genres") },
                        modifier = Modifier.animateItem()
                    )
                }
                item {
                    MoodAndGenresSection(
                        moodAndGenres = genres,
                        navController = navController
                    )
                }
            }

            if (isMoodAndGenresLoading) {
                item {
                    MoodAndGenresLoadingShimmer(modifier = Modifier.animateItem())
                }
            }
            }

            HideOnScrollFAB(
                visible = allLocalItems.isNotEmpty() || allYtItems.isNotEmpty(),
                lazyListState = lazylistState,
                icon = R.drawable.shuffle,
                onClick = {
                    val local = when {
                        allLocalItems.isNotEmpty() && allYtItems.isNotEmpty() -> Random.nextFloat() < 0.5
                        allLocalItems.isNotEmpty() -> true
                        else -> false
                    }
                    scope.launch(Dispatchers.Main) {
                        if (local) {
                            when (val luckyItem = allLocalItems.random()) {
                                is Song -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                is Album -> {
                                    val albumWithSongs = withContext(Dispatchers.IO) {
                                        database.albumWithSongs(luckyItem.id).first()
                                    }
                                    albumWithSongs?.let {
                                        playerConnection.playQueue(LocalAlbumRadio(it))
                                    }
                                }
                                is Artist -> {}
                                is Playlist -> {}
                            }
                        } else {
                            when (val luckyItem = allYtItems.random()) {
                                is SongItem -> playerConnection.playQueue(YouTubeQueue.radio(luckyItem.toMediaMetadata()))
                                is AlbumItem -> playerConnection.playQueue(YouTubeAlbumRadio(luckyItem.playlistId))
                                is ArtistItem -> luckyItem.radioEndpoint?.let {
                                    playerConnection.playQueue(YouTubeQueue(it))
                                }
                                is PlaylistItem -> luckyItem.playEndpoint?.let {
                                    playerConnection.playQueue(YouTubeQueue(it))
                                }
                            }
                        }
                    }
                }
            )

            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        }
        }
    }
}
