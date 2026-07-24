package com.mudassir131.yt.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mudassir131.yt.R
import com.mudassir131.yt.LocalPlayerConnection
import com.mudassir131.yt.LocalDatabase
import com.mudassir131.yt.db.entities.PlaylistEntity
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.PlaylistItem
import com.mudassir131.yt.innertube.utils.completed
import com.mudassir131.yt.models.toMediaMetadata
import com.mudassir131.yt.innertube.toHighResThumbnail
import com.mudassir131.yt.playback.queues.YouTubeQueue
import com.mudassir131.yt.ui.component.AnimatedHeaderAction
import com.mudassir131.yt.ui.menu.AddToPlaylistDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

data class CuratedFeature(
    val category: String,
    val playlist: PlaylistItem,
)

private val CuratedHeroEdgePadding = 12.dp

@Composable
fun HomePersonalizedHeader(
    name: String,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) now = LocalDateTime.now()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            now = LocalDateTime.now()
        }
    }
    val greeting = when (now.hour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Hi, $name",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 39.sp),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        AnimatedHeaderAction(
            icon = R.drawable.favorite_border,
            contentDescription = "Favorites",
            onClick = { navController.navigate("auto_playlist/liked") },
        )
        Spacer(Modifier.width(8.dp))
        AnimatedHeaderAction(
            icon = R.drawable.settings,
            contentDescription = "Settings",
            onClick = { navController.navigate("settings") },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CuratedTrendingCarousel(
    features: List<CuratedFeature>,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    if (features.isEmpty()) return
    val playerConnection = LocalPlayerConnection.current
    val pagerState = rememberPagerState(pageCount = { features.size })
    val isInteracting by remember { derivedStateOf { pagerState.isScrollInProgress } }

    LaunchedEffect(pagerState.currentPage, isInteracting, features.size) {
        if (!isInteracting && features.size > 1) {
            delay(6_500)
            pagerState.animateScrollToPage(
                page = (pagerState.currentPage + 1) % features.size,
                animationSpec = tween(560),
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Curated & trending",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = CuratedHeroEdgePadding,
                end = CuratedHeroEdgePadding,
                top = 8.dp,
                bottom = 8.dp,
            ),
        )
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = CuratedHeroEdgePadding),
            pageSpacing = 12.dp,
            key = { features[it].playlist.id },
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp),
        ) { page ->
            val feature = features[page]
            val selected = pagerState.currentPage == page
            val scale by animateFloatAsState(if (selected) 1f else 0.965f, tween(220), label = "curated-depth")
            CuratedCard(
                feature = feature,
                onOpen = {
                    navController.navigate(
                        "curated/${feature.playlist.id}?category=${Uri.encode(feature.category)}" +
                            "&title=${Uri.encode(feature.playlist.title)}" +
                            "&thumbnail=${Uri.encode(feature.playlist.thumbnail.orEmpty())}",
                    )
                },
                onPlay = {
                    feature.playlist.playEndpoint?.let { endpoint ->
                        playerConnection?.playQueue(YouTubeQueue(endpoint))
                    } ?: navController.navigate("online_playlist/${feature.playlist.id}")
                },
                modifier = Modifier.scale(scale),
            )
        }
        if (features.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(features.size.coerceAtMost(10)) { index ->
                    val selected = pagerState.currentPage % 10 == index
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CuratedCard(
    feature: CuratedFeature,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val dbPlaylist by database.playlistByBrowseId(feature.playlist.id).collectAsState(initial = null)
    var showAddDialog by rememberSaveable(feature.playlist.id) { mutableStateOf(false) }
    val isFavorite = dbPlaylist?.playlist?.bookmarkedAt != null

    AddToPlaylistDialog(
        isVisible = showAddDialog,
        onGetSong = { _ ->
            val media = YouTube.playlist(feature.playlist.id).completed().getOrNull()
                ?.songs.orEmpty().map { it.toMediaMetadata() }
            database.transaction { media.forEach(::insert) }
            media.map { it.id }
        },
        onDismiss = { showAddDialog = false },
        onAddComplete = { count, _ ->
            Toast.makeText(
                context,
                "$count song${if (count == 1) "" else "s"} added",
                Toast.LENGTH_SHORT,
            ).show()
        },
    )

    val palette = remember(feature.category, feature.playlist.title) {
        curatedPalette("${feature.category} ${feature.playlist.title}")
    }
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(28.dp),
        color = palette.first(),
        tonalElevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(palette)),
        ) {
            AsyncImage(
                model = feature.playlist.thumbnail?.toHighResThumbnail(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.48f),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(palette.first(), palette.first().copy(alpha = 0.96f), Color.Transparent),
                            endX = 900f,
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.69f)
                    .padding(20.dp),
            ) {
                Text(
                    text = feature.playlist.title,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = feature.category,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 7.dp),
                )
                feature.playlist.songCountText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.68f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Surface(
                    onClick = onPlay,
                    shape = CircleShape,
                    color = Color.White,
                    contentColor = Color.Black,
                    modifier = Modifier.size(54.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = "Play",
                        modifier = Modifier.padding(15.dp),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp),
            ) {
                CuratedSecondaryAction(
                    icon = if (isFavorite) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = if (isFavorite) "Remove favorite" else "Favorite collection",
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            database.transaction {
                                val current = dbPlaylist?.playlist
                                if (current == null) {
                                    insert(
                                        PlaylistEntity(
                                            name = feature.playlist.title,
                                            browseId = feature.playlist.id,
                                            thumbnailUrl = feature.playlist.thumbnail,
                                            isEditable = false,
                                            remoteSongCount = feature.playlist.songCountText
                                                ?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                                            playEndpointParams = feature.playlist.playEndpoint?.params,
                                            shuffleEndpointParams = feature.playlist.shuffleEndpoint?.params,
                                            radioEndpointParams = feature.playlist.radioEndpoint?.params,
                                        ).toggleLike(),
                                    )
                                } else {
                                    update(current, feature.playlist)
                                    update(current.toggleLike())
                                }
                            }
                        }
                    },
                )
                CuratedSecondaryAction(
                    icon = R.drawable.playlist_add,
                    contentDescription = "Add collection to playlist",
                    onClick = { showAddDialog = true },
                )
            }
        }
    }
}

@Composable
private fun CuratedSecondaryAction(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.34f),
        contentColor = Color.White,
        modifier = Modifier.size(42.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.padding(10.dp),
        )
    }
}

private fun curatedPalette(text: String): List<Color> {
    val lower = text.lowercase()
    val pair = when {
        "punjabi" in lower -> Color(0xFF9A3412) to Color(0xFFF59E0B)
        "bollywood" in lower || "hindi" in lower -> Color(0xFF4C1D95) to Color(0xFFDB2777)
        "romance" in lower || "love" in lower -> Color(0xFF9F1239) to Color(0xFFFB7185)
        "chill" in lower || "relax" in lower -> Color(0xFF075985) to Color(0xFF22D3EE)
        "workout" in lower || "party" in lower -> Color(0xFF7C2D12) to Color(0xFFEF4444)
        "devotional" in lower -> Color(0xFF92400E) to Color(0xFFFBBF24)
        "hip" in lower || "rap" in lower -> Color(0xFF111827) to Color(0xFF7C3AED)
        "international" in lower || "global" in lower || "pop" in lower -> Color(0xFF1E3A8A) to Color(0xFF8B5CF6)
        else -> Color(0xFF312E81) to Color(0xFF0F766E)
    }
    return listOf(pair.first, pair.second.copy(alpha = 0.88f))
}
