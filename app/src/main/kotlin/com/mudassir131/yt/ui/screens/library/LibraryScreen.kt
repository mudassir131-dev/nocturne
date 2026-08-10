/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.mudassir131.yt.LocalDatabase
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.ChipSortTypeKey
import com.mudassir131.yt.constants.DisableBlurKey
import com.mudassir131.yt.constants.LibraryFilter
import com.mudassir131.yt.constants.PlaylistTagsFilterKey
import com.mudassir131.yt.constants.ShowTagsInLibraryKey
import com.mudassir131.yt.ui.component.ChipsRow
import com.mudassir131.yt.ui.component.TagsFilterChips
import com.mudassir131.yt.ui.component.RootScreenHeader
import com.mudassir131.yt.ui.component.AnimatedHeaderAction
import com.mudassir131.yt.ui.component.NocturneDynamicScreen
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.utils.rememberPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.PLAYLISTS)
    LaunchedEffect(filterType) {
        if (filterType == LibraryFilter.LIBRARY) filterType = LibraryFilter.PLAYLISTS
    }

    val (disableBlur) = rememberPreference(DisableBlurKey, true)

    val database = LocalDatabase.current
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    val selectedPrimaryFilter =
        if (filterType == LibraryFilter.LIBRARY) LibraryFilter.PLAYLISTS else filterType
    val primaryFilters = @Composable {
        ChipsRow(
            chips =
            listOf(
                LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists),
                LibraryFilter.SONGS to stringResource(R.string.filter_songs),
                LibraryFilter.ALBUMS to stringResource(R.string.filter_albums),
                LibraryFilter.ARTISTS to stringResource(R.string.filter_artists),
            ),
            currentValue = selectedPrimaryFilter,
            onValueUpdate = { filterType = it },
        )
    }
    val filterContent = @Composable {
        if (showTagsInLibrary) {
            TagsFilterChips(
                database = database,
                selectedTags = selectedTagIds,
                onTagToggle = { tag ->
                    val newTags = if (tag.id in selectedTagIds) {
                        selectedTagIds - tag.id
                    } else {
                        selectedTagIds + tag.id
                    }
                    onSelectedTagsFilterChange(newTags.joinToString(","))
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    NocturneDynamicScreen(disableBlur = disableBlur) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(Modifier.fillMaxWidth()) {
                RootScreenHeader(
                    title = stringResource(R.string.filter_library),
                    action = {
                        AnimatedHeaderAction(
                            icon = R.drawable.settings,
                            contentDescription = "Settings",
                            onClick = { navController.navigate("settings") },
                        )
                    },
                )
                primaryFilters()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (filterType) {
                    LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
                    LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
                    LibraryFilter.SONGS -> LibrarySongsScreen(
                        navController,
                        { filterType = LibraryFilter.PLAYLISTS })

                    LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                        navController,
                        { filterType = LibraryFilter.PLAYLISTS })

                    LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                        navController,
                        { filterType = LibraryFilter.PLAYLISTS })
                }
            }
        }
    }
}
