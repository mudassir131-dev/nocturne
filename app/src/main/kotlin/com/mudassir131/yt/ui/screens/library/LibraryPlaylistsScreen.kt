/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.mudassir131.yt.ui.theme.PlayerColorExtractor
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mudassir131.yt.innertube.utils.parseCookieString
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.CONTENT_TYPE_HEADER
import com.mudassir131.yt.constants.CONTENT_TYPE_PLAYLIST
import com.mudassir131.yt.constants.GridItemSize
import com.mudassir131.yt.constants.GridItemsSizeKey
import com.mudassir131.yt.constants.GridThumbnailHeight
import com.mudassir131.yt.constants.InnerTubeCookieKey
import com.mudassir131.yt.constants.LibraryViewType
import com.mudassir131.yt.constants.PlaylistSortDescendingKey
import com.mudassir131.yt.constants.PlaylistSortType
import com.mudassir131.yt.constants.PlaylistSortTypeKey
import com.mudassir131.yt.constants.PlaylistViewTypeKey
import com.mudassir131.yt.constants.ShowLikedPlaylistKey
import com.mudassir131.yt.constants.ShowDownloadedPlaylistKey
import com.mudassir131.yt.constants.ShowTopPlaylistKey
import com.mudassir131.yt.constants.ShowCachedPlaylistKey
import com.mudassir131.yt.constants.UseNewLibraryDesignKey
import com.mudassir131.yt.constants.YtmSyncKey
import com.mudassir131.yt.constants.DisableBlurKey
import com.mudassir131.yt.constants.PlaylistTagsFilterKey
import com.mudassir131.yt.db.entities.Playlist
import com.mudassir131.yt.db.entities.PlaylistEntity
import com.mudassir131.yt.ui.component.CreatePlaylistDialog
import com.mudassir131.yt.ui.component.HideOnScrollFAB
import com.mudassir131.yt.ui.component.LibraryPlaylistGridItem
import com.mudassir131.yt.ui.component.LibraryPlaylistListItem
import com.mudassir131.yt.ui.utils.LocalListItemShape
import com.mudassir131.yt.ui.utils.getGroupShape
import com.mudassir131.yt.ui.component.LocalMenuState
import com.mudassir131.yt.ui.component.PlaylistGridItem
import com.mudassir131.yt.ui.component.PlaylistListItem
import com.mudassir131.yt.ui.component.SortHeader
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.utils.rememberPreference
import com.mudassir131.yt.viewmodels.LibraryPlaylistsViewModel
import com.mudassir131.yt.LocalDatabase
import com.mudassir131.yt.extensions.move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryPlaylistsScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryPlaylistsViewModel = hiltViewModel(),
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val coroutineScope = rememberCoroutineScope()

    var viewType by rememberEnumPreference(PlaylistViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) = rememberEnumPreference(
        PlaylistSortTypeKey,
        PlaylistSortType.CUSTOM
    )
    val (sortDescending, onSortDescendingChange) = rememberPreference(
        PlaylistSortDescendingKey,
        true
    )
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val useNewLibraryDesign by rememberPreference(UseNewLibraryDesignKey, false)


    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }
    val database = LocalDatabase.current
    val filteredPlaylistIds by database.playlistIdsByTags(
        if (selectedTagIds.isEmpty()) emptyList() else selectedTagIds.toList()
    ).collectAsState(initial = emptyList())

    val playlists by viewModel.allPlaylists.collectAsState()

    val visiblePlaylists = playlists.filter { playlist ->
        val name = playlist.playlist.name ?: ""
        val matchesName = !name.contains("episode", ignoreCase = true)
        val matchesTags = selectedTagIds.isEmpty() || playlist.id in filteredPlaylistIds
        matchesName && matchesTags
    }

    val topSize by viewModel.topValue.collectAsState(initial = 50)

    val likedPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.liked)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.offline)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.my_top) + " $topSize"
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachePlaylist =
        Playlist(
            playlist = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                name = stringResource(R.string.cached_playlist)
            ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, true)

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val canEnterReorderMode = sortType == PlaylistSortType.CUSTOM && selectedTagIds.isEmpty()
    var reorderEnabled by rememberSaveable { mutableStateOf(false) }
    val canReorderPlaylists = canEnterReorderMode && reorderEnabled
    val listHeaderItems =
        2 +
            (if (showLiked) 1 else 0) +
            (if (showDownloaded) 1 else 0) +
            (if (showTop) 1 else 0) +
            (if (showCached) 1 else 0)
    val mutableVisiblePlaylists = remember { mutableStateListOf<Playlist>() }
    var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val reorderableState = rememberReorderableLazyListState(
        lazyListState = lazyListState,
        scrollThresholdPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues(),
    ) { from, to ->
        if (!canReorderPlaylists) return@rememberReorderableLazyListState
        if (from.index < listHeaderItems || to.index < listHeaderItems) return@rememberReorderableLazyListState

        val fromIndex = from.index - listHeaderItems
        val toIndex = to.index - listHeaderItems

        if (fromIndex !in mutableVisiblePlaylists.indices || toIndex !in mutableVisiblePlaylists.indices) {
            return@rememberReorderableLazyListState
        }

        val currentDragInfo = dragInfo
        dragInfo =
            if (currentDragInfo == null) {
                fromIndex to toIndex
            } else {
                currentDragInfo.first to toIndex
            }

        mutableVisiblePlaylists.move(fromIndex, toIndex)
    }

    LaunchedEffect(visiblePlaylists, canReorderPlaylists, reorderableState.isAnyItemDragging, dragInfo) {
        if (!canReorderPlaylists) {
            mutableVisiblePlaylists.clear()
            mutableVisiblePlaylists.addAll(visiblePlaylists)
            return@LaunchedEffect
        }

        if (!reorderableState.isAnyItemDragging && dragInfo == null) {
            mutableVisiblePlaylists.clear()
            mutableVisiblePlaylists.addAll(visiblePlaylists)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging, canReorderPlaylists) {
        if (!canReorderPlaylists || reorderableState.isAnyItemDragging) return@LaunchedEffect

        dragInfo ?: return@LaunchedEffect
        database.transaction {
            mutableVisiblePlaylists.forEachIndexed { index, playlist ->
                setPlaylistCustomOrder(playlist.id, index)
            }
        }
        dragInfo = null
    }
    
    LaunchedEffect(canEnterReorderMode) {
        if (!canEnterReorderMode) reorderEnabled = false
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn = remember(innerTubeCookie) {
        "SAPISID" in parseCookieString(innerTubeCookie)
    }

    val (ytmSync) = rememberPreference(YtmSyncKey, true)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.sync()
            }
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    val surfaceColor = MaterialTheme.colorScheme.surface

    var showChoiceDialog by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    if (showChoiceDialog) {
        com.mudassir131.yt.ui.component.PlaylistActionChoiceDialog(
            onDismiss = { showChoiceDialog = false },
            onCreateClick = { showCreatePlaylistDialog = true },
            onImportClick = { showImportPlaylistDialog = true }
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            initialTextFieldValue = initialTextFieldValue,
            allowSyncing = allowSyncing
        )
    }

    if (showImportPlaylistDialog) {
        com.mudassir131.yt.ui.component.PlaylistImportDialog(
            onDismiss = { showImportPlaylistDialog = false }
        )
    }

    val headerContent = @Composable {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        PlaylistSortType.CREATE_DATE -> R.string.sort_by_create_date
                        PlaylistSortType.NAME -> R.string.sort_by_name
                        PlaylistSortType.SONG_COUNT -> R.string.sort_by_song_count
                        PlaylistSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        PlaylistSortType.CUSTOM -> R.string.sort_by_custom
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            Text(
                text = pluralStringResource(
                    R.plurals.n_playlist,
                    playlists.size,
                    playlists.size
                ),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            if (canEnterReorderMode) {
                IconButton(
                    onClick = { reorderEnabled = !reorderEnabled },
                    modifier = Modifier.padding(start = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(if (reorderEnabled) R.drawable.lock_open else R.drawable.lock),
                        contentDescription = null,
                    )
                }
            }

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(start = 6.dp, end = 6.dp),
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        },
                    ),
                    contentDescription = null,
                )
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val pullRefreshState = rememberPullToRefreshState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                state = pullRefreshState,
                isRefreshing = isRefreshing,
                onRefresh = { if (ytmSync) viewModel.sync() }
            ),
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                val autoPlaylists = remember(showLiked, showDownloaded, showTop, showCached, likedPlaylist, downloadPlaylist, topPlaylist, cachePlaylist, topSize) {
                    listOfNotNull(
                        if (showLiked) (likedPlaylist to "auto_playlist/liked") else null,
                        if (showDownloaded) (downloadPlaylist to "auto_playlist/downloaded") else null,
                        if (showTop) (topPlaylist to "top_playlist/$topSize") else null,
                        if (showCached) (cachePlaylist to "cache_playlist/cached") else null,
                    )
                }

                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (autoPlaylists.isNotEmpty()) {
                        item(
                            key = "autoPlaylistsGroup",
                            contentType = CONTENT_TYPE_PLAYLIST,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .animateItem()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    autoPlaylists.forEachIndexed { index, (playlist, route) ->
                                        val isFirst = index == 0
                                        val isLast = index == autoPlaylists.size - 1
                                        val itemShape = when {
                                            isFirst && isLast -> RoundedCornerShape(24.dp)
                                            isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                                            isLast -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                            else -> androidx.compose.ui.graphics.RectangleShape
                                        }

                                        PlaylistListItem(
                                            playlist = playlist,
                                            autoPlaylist = true,
                                            shape = itemShape,
                                            containerColor = Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { navController.navigate(route) },
                                        )

                                        if (index < autoPlaylists.size - 1) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(start = 76.dp, end = 16.dp),
                                                thickness = 0.5.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (visiblePlaylists.isEmpty()) {
                        item {
                        }
                    }

                    if (canReorderPlaylists) {
                        itemsIndexed(
                            items = mutableVisiblePlaylists,
                            key = { _, item -> item.id },
                            contentType = { _, _ -> CONTENT_TYPE_PLAYLIST },
                        ) { index, playlist ->
                            ReorderableItem(
                                state = reorderableState,
                                key = playlist.id,
                            ) {
                                CompositionLocalProvider(
                                    LocalListItemShape provides getGroupShape(index, mutableVisiblePlaylists.size)
                                ) {
                                    LibraryPlaylistListItem(
                                        navController = navController,
                                        menuState = menuState,
                                        coroutineScope = coroutineScope,
                                        playlist = playlist,
                                        useNewDesign = useNewLibraryDesign,
                                        showDragHandle = true,
                                        dragHandleModifier = Modifier.draggableHandle(),
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = visiblePlaylists,
                            key = { _, it -> it.id },
                            contentType = { _, _ -> CONTENT_TYPE_PLAYLIST },
                        ) { index, playlist ->
                            CompositionLocalProvider(
                                LocalListItemShape provides getGroupShape(index, visiblePlaylists.size)
                            ) {
                                LibraryPlaylistListItem(
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    playlist = playlist,
                                    useNewDesign = useNewLibraryDesign,
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyListState,
                    icon = R.drawable.add,
                    onClick = {
                        showChoiceDialog = true
                    },
                )
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                    GridCells.Adaptive(
                        minSize = GridThumbnailHeight + if (gridItemSize == GridItemSize.BIG) 24.dp else (-24).dp,
                    ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (showLiked) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = likedPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/liked")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showDownloaded) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = downloadPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("auto_playlist/downloaded")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showTop) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = topPlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("top_playlist/$topSize")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (showCached) {
                        item(
                            key = "cachePlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistGridItem(
                                playlist = cachePlaylist,
                                fillMaxWidth = true,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            navController.navigate("cache_playlist/cached")
                                        },
                                    )
                                    .animateItem(),
                            )
                        }
                    }

                    if (visiblePlaylists.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                        }
                    }

                    items(
                        items = visiblePlaylists,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { playlist ->
                        LibraryPlaylistGridItem(
                            navController = navController,
                            menuState = menuState,
                            coroutineScope = coroutineScope,
                            playlist = playlist,
                            modifier = Modifier.animateItem()
                        )
                    }
                }

                HideOnScrollFAB(
                    lazyListState = lazyGridState,
                    icon = R.drawable.add,
                    onClick = {
                        showChoiceDialog = true
                    },
                )
            }
        }

        PullToRefreshDefaults.Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues()),
        )
    }
}

