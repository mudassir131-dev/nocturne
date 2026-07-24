package com.mudassir131.yt.ui.screens.playlist

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil3.compose.AsyncImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Size
import coil3.toBitmap
import com.mudassir131.yt.LocalDatabase
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.LocalPlayerConnection
import com.mudassir131.yt.R
import com.mudassir131.yt.db.entities.PlaylistEntity
import com.mudassir131.yt.extensions.togglePlayPause
import com.mudassir131.yt.innertube.models.PlaylistItem
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.innertube.models.WatchEndpoint
import com.mudassir131.yt.innertube.toHighResThumbnail
import com.mudassir131.yt.models.toMediaMetadata
import com.mudassir131.yt.playback.queues.YouTubeQueue
import com.mudassir131.yt.ui.component.IconButton
import com.mudassir131.yt.ui.component.LocalMenuState
import com.mudassir131.yt.ui.component.YouTubeListItem
import com.mudassir131.yt.ui.menu.AddToPlaylistDialog
import com.mudassir131.yt.ui.menu.YouTubeSongMenu
import com.mudassir131.yt.ui.theme.PlayerColorExtractor
import com.mudassir131.yt.ui.theme.ArtworkPaletteCache
import com.mudassir131.yt.ui.utils.backToMain
import com.mudassir131.yt.viewmodels.OnlinePlaylistViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CuratedDetailScreen(
    navController: NavController,
    category: String?,
    fallbackTitle: String?,
    fallbackThumbnail: String?,
    viewModel: OnlinePlaylistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val playlist by viewModel.playlist.collectAsState()
    val songs by viewModel.playlistSongs.collectAsState()
    val dbPlaylist by viewModel.dbPlaylist.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val title = playlist?.title ?: fallbackTitle.orEmpty()
    val heroUrl = playlist?.thumbnail ?: fallbackThumbnail
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    var palette by remember(heroUrl) { mutableStateOf(listOf(primary, surface)) }

    LaunchedEffect(heroUrl, surface) {
        if (heroUrl.isNullOrBlank()) return@LaunchedEffect
        ArtworkPaletteCache[heroUrl]?.let {
            palette = it
            return@LaunchedEffect
        }
        val request = ImageRequest.Builder(context)
            .data(heroUrl)
            .size(Size(PlayerColorExtractor.Config.IMAGE_SIZE, PlayerColorExtractor.Config.IMAGE_SIZE))
            .allowHardware(false)
            .build()
        val bitmap = runCatching { context.imageLoader.execute(request).image?.toBitmap() }.getOrNull()
        if (bitmap != null) {
            val extracted = withContext(Dispatchers.Default) {
                val source = Palette.from(bitmap)
                    .maximumColorCount(PlayerColorExtractor.Config.MAX_COLOR_COUNT)
                    .resizeBitmapArea(PlayerColorExtractor.Config.BITMAP_AREA)
                    .generate()
                PlayerColorExtractor.extractGradientColors(source, surface.toArgb())
            }
            if (extracted.isNotEmpty()) {
                ArtworkPaletteCache[heroUrl] = extracted
                palette = extracted
            }
        }
    }

    AddToPlaylistDialog(
        isVisible = showAddDialog,
        onGetSong = { _ ->
            val media = songs.map(SongItem::toMediaMetadata)
            database.transaction { media.forEach(::insert) }
            media.map { it.id }
        },
        onDismiss = { showAddDialog = false },
        onAddComplete = { count, _ ->
            Toast.makeText(context, "$count song${if (count == 1) "" else "s"} added", Toast.LENGTH_SHORT).show()
        },
    )

    val showCompactTitle by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 280
        }
    }
    val heroOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 360
        }
    }

    Box(Modifier.fillMaxSize().background(surface)) {
        LazyColumn(
            state = listState,
            contentPadding = LocalPlayerAwareWindowInsets.current
                .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                .asPaddingValues(),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "curated_hero") {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                            .graphicsLayer {
                                val progress = (heroOffset / 700f).coerceIn(0f, 0.12f)
                                scaleX = 1f + progress
                                scaleY = 1f + progress
                                translationY = heroOffset * 0.18f
                            },
                    ) {
                        if (!heroUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = heroUrl.toHighResThumbnail(),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(Brush.linearGradient(palette)))
                        }
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(
                                    0f to Color.Transparent,
                                    0.48f to palette.first().copy(alpha = 0.22f),
                                    1f to surface,
                                ),
                            ),
                        )
                    }

                    Column(Modifier.padding(horizontal = 20.dp)) {
                        category?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            title,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        playlist?.author?.name?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        playlist?.songCountText?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        CuratedActionRow(
                            playlist = playlist,
                            isFavorite = dbPlaylist?.playlist?.bookmarkedAt != null,
                            onFavorite = {
                                val currentPlaylist = playlist ?: return@CuratedActionRow
                                scope.launch(Dispatchers.IO) {
                                    database.transaction {
                                        val current = dbPlaylist?.playlist
                                        if (current == null) {
                                            insert(
                                                PlaylistEntity(
                                                    name = currentPlaylist.title,
                                                    browseId = currentPlaylist.id,
                                                    thumbnailUrl = currentPlaylist.thumbnail,
                                                    isEditable = currentPlaylist.isEditable,
                                                    playEndpointParams = currentPlaylist.playEndpoint?.params,
                                                    shuffleEndpointParams = currentPlaylist.shuffleEndpoint?.params,
                                                    radioEndpointParams = currentPlaylist.radioEndpoint?.params,
                                                ).toggleLike(),
                                            )
                                        } else {
                                            update(current, currentPlaylist)
                                            update(current.toggleLike())
                                        }
                                    }
                                }
                            },
                            onAdd = { showAddDialog = true },
                            onPlay = {
                                playlist?.playEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                            },
                            onShuffle = {
                                playlist?.shuffleEndpoint?.let { playerConnection.playQueue(YouTubeQueue(it)) }
                            },
                        )
                        Text(
                            text = "Songs",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
                        )
                    }
                }
            }

            if (isLoading && songs.isEmpty()) {
                item { Text("Loading collection…", modifier = Modifier.padding(24.dp)) }
            }
            items(songs, key = { it.id }) { song ->
                YouTubeListItem(
                    item = song,
                    isActive = mediaMetadata?.id == song.id,
                    isPlaying = isPlaying,
                    trailingContent = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    YouTubeSongMenu(song, navController, menuState::dismiss)
                                }
                            },
                            onLongClick = {},
                        ) {
                            Icon(painterResource(R.drawable.more_vert), contentDescription = "More")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = {
                                if (mediaMetadata?.id == song.id) {
                                    playerConnection.player.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue(WatchEndpoint(videoId = song.id), song.toMediaMetadata()),
                                    )
                                }
                            },
                            onLongClick = {
                                menuState.show { YouTubeSongMenu(song, navController, menuState::dismiss) }
                            },
                        ),
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Surface(
                onClick = navController::navigateUp,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                modifier = Modifier.size(46.dp),
            ) {
                Icon(painterResource(R.drawable.arrow_back), "Back", modifier = Modifier.padding(11.dp))
            }
            AnimatedVisibility(showCompactTitle, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun CuratedActionRow(
    playlist: PlaylistItem?,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    onAdd: () -> Unit,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        CuratedAction(
            icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
            label = if (isFavorite) "Saved" else "Favorite",
            onClick = onFavorite,
            modifier = Modifier.weight(1f),
        )
        CuratedAction(R.drawable.playlist_add, "Add", onAdd, Modifier.weight(1f))
        if (playlist?.shuffleEndpoint != null) {
            CuratedAction(R.drawable.shuffle, "Shuffle", onShuffle, Modifier.weight(1f), primary = true)
        } else {
            CuratedAction(R.drawable.play, "Play", onPlay, Modifier.weight(1f), primary = true)
        }
    }
}

@Composable
private fun CuratedAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    primary: Boolean = false,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = if (primary) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
        modifier = modifier.height(48.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.size(6.dp))
        Text(label, maxLines = 1)
    }
}
