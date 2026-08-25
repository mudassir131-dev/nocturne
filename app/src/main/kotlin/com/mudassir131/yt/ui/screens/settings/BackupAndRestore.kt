/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R
import com.mudassir131.yt.db.entities.Song
import com.mudassir131.yt.ui.component.IconButton
import com.mudassir131.yt.ui.menu.AddToPlaylistDialogOnline
import com.mudassir131.yt.ui.menu.LoadingScreen
import com.mudassir131.yt.ui.utils.backToMain
import com.mudassir131.yt.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val CSV_MIME_TYPES =
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
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAndRestore(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
    viewModel: BackupRestoreViewModel = hiltViewModel(),
) {
    var importedTitle by remember { mutableStateOf("") }
    val importedSongs = remember { mutableStateListOf<Song>() }
    var showChoosePlaylistDialogOnline by rememberSaveable { mutableStateOf(false) }
    var isProgressStarted by rememberSaveable { mutableStateOf(false) }
    var progressStatus by remember { mutableStateOf("") }
    var progressPercentage by rememberSaveable { mutableIntStateOf(0) }
    
    val backupRestoreProgress by viewModel.backupRestoreProgress.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val backupLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) {
                viewModel.backup(context, uri)
            }
        }
        
    val restoreLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                viewModel.restore(context, uri)
            }
        }
        
    val importPlaylistFromCsv =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            coroutineScope.launch {
                val result = viewModel.importPlaylistFromCsv(context, uri)
                importedSongs.clear()
                importedSongs.addAll(result)

                if (importedSongs.isNotEmpty()) {
                    showChoosePlaylistDialogOnline = true
                }
            }
        }
        
    val importM3uLauncherOnline = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val result = viewModel.loadM3UOnline(context, uri)
            importedSongs.clear()
            importedSongs.addAll(result)

            if (importedSongs.isNotEmpty()) {
                showChoosePlaylistDialogOnline = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore)) },
                navigationIcon = {
                    IconButton(
                        onClick = navController::navigateUp,
                        onLongClick = navController::backToMain,
                    ) {
                        Icon(
                            painterResource(R.drawable.arrow_back),
                            contentDescription = null,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                ),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
        ) {
            item {
                androidx.compose.material3.Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth()) {
                        val items = listOf(
                            Triple(painterResource(R.drawable.backup), "Backup", {
                                val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                                backupLauncher.launch(
                                    "${context.getString(R.string.app_name)}_${
                                        LocalDateTime.now().format(formatter)
                                    }.backup"
                                )
                            }),
                            Triple(painterResource(R.drawable.restore), "Restore", {
                                restoreLauncher.launch(arrayOf("application/octet-stream"))
                            }),
                            Triple(painterResource(R.drawable.playlist_add), "Import a \"m3u\" playlists", {
                                importM3uLauncherOnline.launch(arrayOf("audio/*"))
                            }),
                            Triple(painterResource(R.drawable.playlist_add), "Import a \"csv\" playlist", {
                                importPlaylistFromCsv.launch(CSV_MIME_TYPES)
                            })
                        )

                        items.forEachIndexed { index, (icon, title, onClick) ->
                            val isFirst = index == 0
                            val isLast = index == items.size - 1
                            val shape = when {
                                isFirst && isLast -> RoundedCornerShape(24.dp)
                                isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                                isLast -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                else -> androidx.compose.ui.graphics.RectangleShape
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(shape)
                                    .clickable(onClick = onClick)
                                    .padding(vertical = 16.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (index < items.size - 1) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    AddToPlaylistDialogOnline(
        isVisible = showChoosePlaylistDialogOnline,
        allowSyncing = false,
        initialTextFieldValue = importedTitle,
        songs = importedSongs,
        onDismiss = { showChoosePlaylistDialogOnline = false },
        onProgressStart = { newVal -> isProgressStarted = newVal },
        onPercentageChange = { newPercentage -> progressPercentage = newPercentage },
        onStatusChange = { progressStatus = it }
    )

    LaunchedEffect(progressPercentage, isProgressStarted) {
        if (isProgressStarted && progressPercentage == 99) {
            delay(10000)
            if (progressPercentage == 99) {
                isProgressStarted = false
                progressPercentage = 0
            }
        }
    }

    LoadingScreen(
        isVisible = backupRestoreProgress != null || isProgressStarted,
        value = backupRestoreProgress?.percent ?: progressPercentage,
        title = backupRestoreProgress?.title,
        stepText = backupRestoreProgress?.step ?: progressStatus,
        indeterminate = backupRestoreProgress?.indeterminate ?: false,
    )
}
