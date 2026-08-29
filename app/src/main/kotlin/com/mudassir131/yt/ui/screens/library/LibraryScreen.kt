/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.screens.library

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mudassir131.yt.LocalDatabase
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.ChipSortTypeKey
import com.mudassir131.yt.constants.DisableBlurKey
import com.mudassir131.yt.constants.LibraryFilter
import com.mudassir131.yt.constants.PlaylistTagsFilterKey
import com.mudassir131.yt.constants.ShowTagsInLibraryKey
import com.mudassir131.yt.ui.component.AnimatedHeaderAction
import com.mudassir131.yt.ui.component.ChipsRow
import com.mudassir131.yt.ui.component.CreatePlaylistDialog
import com.mudassir131.yt.ui.component.NocturneDynamicScreen
import com.mudassir131.yt.ui.component.PlaylistActionChoiceDialog
import com.mudassir131.yt.ui.component.PlaylistImportDialog
import com.mudassir131.yt.ui.component.RootScreenHeader
import com.mudassir131.yt.ui.component.SpotifyPlaylistImportDialog
import com.mudassir131.yt.ui.component.TagsFilterChips
import com.mudassir131.yt.utils.PlaylistImporter
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

@Composable
fun LibraryScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = LocalDatabase.current

    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.PLAYLISTS)
    LaunchedEffect(filterType) {
        if (filterType == LibraryFilter.LIBRARY) filterType = LibraryFilter.PLAYLISTS
    }

    val (disableBlur) = rememberPreference(DisableBlurKey, false)
    val (showTagsInLibrary) = rememberPreference(ShowTagsInLibraryKey, true)
    val (selectedTagsFilter, onSelectedTagsFilterChange) = rememberPreference(PlaylistTagsFilterKey, "")
    val selectedTagIds = remember(selectedTagsFilter) {
        selectedTagsFilter.split(",").filter { it.isNotBlank() }.toSet()
    }

    var showChoiceDialog by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportSpotifyPlaylistDialog by rememberSaveable { mutableStateOf(false) }

    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val appContext = context.applicationContext
        Toast.makeText(appContext, "Importing playlist from CSV in background...", Toast.LENGTH_SHORT).show()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val displayName = runCatching {
                    var name: String? = null
                    if (uri.scheme == "content") {
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (idx >= 0) name = cursor.getString(idx)
                            }
                        }
                    }
                    name?.substringBeforeLast('.')
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Imported CSV Playlist"

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val reader = InputStreamReader(stream, Charsets.UTF_8)
                    val summary = PlaylistImporter.importFromCsv(
                        database = database,
                        csvReader = reader,
                        playlistName = displayName,
                    )
                    withContext(Dispatchers.Main) {
                        val msg = appContext.getString(
                            R.string.import_summary,
                            summary.playlistName,
                            summary.matched,
                            summary.unmatched,
                        )
                        Toast.makeText(appContext, msg, Toast.LENGTH_LONG).show()
                        summary.quotaNotice?.let { notice ->
                            Toast.makeText(appContext, notice, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMsg = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "Failed to import CSV"
                    Toast.makeText(appContext, "CSV Import Failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    if (showChoiceDialog) {
        PlaylistActionChoiceDialog(
            onDismiss = { showChoiceDialog = false },
            onCreateClick = { showCreatePlaylistDialog = true },
            onImportYouTubeClick = { showImportPlaylistDialog = true },
            onImportSpotifyClick = { showImportSpotifyPlaylistDialog = true },
            onImportCsvClick = {
                importCsvLauncher.launch(
                    arrayOf(
                        "text/csv",
                        "text/x-csv",
                        "text/comma-separated-values",
                        "text/x-comma-separated-values",
                        "application/csv",
                        "application/x-csv",
                        "application/vnd.ms-excel",
                        "text/plain",
                        "text/*",
                        "application/octet-stream",
                        "*/*",
                    )
                )
            },
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            allowSyncing = true,
        )
    }

    if (showImportPlaylistDialog) {
        PlaylistImportDialog(
            onDismiss = { showImportPlaylistDialog = false },
        )
    }

    if (showImportSpotifyPlaylistDialog) {
        SpotifyPlaylistImportDialog(
            onDismiss = { showImportSpotifyPlaylistDialog = false },
        )
    }

    val selectedPrimaryFilter =
        if (filterType == LibraryFilter.LIBRARY) LibraryFilter.PLAYLISTS else filterType
    val primaryFilters = @Composable {
        ChipsRow(
            chips = listOf(
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }

    NocturneDynamicScreen(disableBlur = disableBlur) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
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
                    .weight(1f),
            ) {
                when (filterType) {
                    LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
                    LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
                    LibraryFilter.SONGS -> LibrarySongsScreen(
                        navController,
                        { filterType = LibraryFilter.PLAYLISTS },
                    )

                    LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                        navController,
                        { filterType = LibraryFilter.PLAYLISTS },
                    )

                    LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                        navController,
                        { filterType = LibraryFilter.PLAYLISTS },
                    )
                }
            }
        }
    }
}
