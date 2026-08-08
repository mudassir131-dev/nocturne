/*
 * Nocturne - by Mudassir
 * Apple-style player integration derived from Echo Music (GPL-3.0).
 * See NOTICE-ECHO.md and docs/ECHO_INTEGRATION.md.
 */
package com.mudassir131.yt.ui.appleplayer

import android.app.Activity
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.core.view.WindowCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.mudassir131.yt.LocalPlayerConnection
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.ApplePlayerDataSaverKey
import com.mudassir131.yt.constants.ApplePlayerBackgroundStyle
import com.mudassir131.yt.constants.ApplePlayerBackgroundStyleKey
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.db.entities.LyricsEntity
import com.mudassir131.yt.extensions.metadata
import com.mudassir131.yt.models.MediaMetadata
import com.mudassir131.yt.ui.appleplayer.liveart.AppleLiveArtworkResolver
import com.mudassir131.yt.ui.appleplayer.liveart.CanvasArtwork
import com.mudassir131.yt.ui.appleplayer.liveart.CanvasArtworkPlayer
import com.mudassir131.yt.ui.appleplayer.lyrics.AppleLyricsPipeline
import com.mudassir131.yt.ui.appleplayer.lyrics.AppleLyricsResult
import com.mudassir131.yt.ui.appleplayer.lyrics.AppleLyricsTimingParser
import com.mudassir131.yt.ui.appleplayer.lyrics.AppleLyricsView
import com.mudassir131.yt.ui.component.BottomSheet
import com.mudassir131.yt.ui.component.BottomSheetState
import com.mudassir131.yt.ui.component.LocalBottomSheetPageState
import com.mudassir131.yt.ui.component.LocalMenuState
import com.mudassir131.yt.ui.component.PlayerSliderTrack
import com.mudassir131.yt.ui.component.rememberBottomSheetState
import com.mudassir131.yt.ui.menu.LyricsMenu
import com.mudassir131.yt.ui.menu.PlayerMenu
import com.mudassir131.yt.ui.player.Queue
import com.mudassir131.yt.ui.player.SleepTimerDialog
import com.mudassir131.yt.ui.utils.ShowMediaInfo
import com.mudassir131.yt.utils.makeTimeString
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Isolated Apple-style player. It deliberately depends only on Nocturne's public
 * [com.mudassir131.yt.playback.PlayerConnection] contract and does not call legacy player UI.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ApplePlayerHost(
    state: BottomSheetState,
    navController: NavController,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val connection = LocalPlayerConnection.current ?: return
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val menuState = LocalMenuState.current
    val adapter = remember(connection) { NocturneApplePlayerAdapter(connection) }
    val metadata by adapter.metadata.collectAsState()
    val currentSong by adapter.currentSong.collectAsState(initial = null)
    val currentLyrics by adapter.currentLyrics.collectAsState(initial = null)
    val currentFormat by adapter.currentFormat.collectAsState(initial = null)
    val playbackState by adapter.playbackState.collectAsState()
    val isPlaying by adapter.isPlaying.collectAsState()
    val canSkipPrevious by adapter.canSkipPrevious.collectAsState()
    val canSkipNext by adapter.canSkipNext.collectAsState()
    val dataSaver by rememberPreference(ApplePlayerDataSaverKey, defaultValue = false)
    val playerBackgroundStyle by rememberPreference(
        ApplePlayerBackgroundStyleKey,
        defaultValue = ApplePlayerBackgroundStyle.APPLE_MUSIC.name,
    )
    val availableOutputs = rememberAppleAudioOutputs()
    val connectedOutputName = availableOutputs.firstOrNull { it.isExternal }?.name
    val formatLoading = playbackState == Player.STATE_BUFFERING ||
        (metadata != null && currentFormat == null)

    var position by rememberSaveable(metadata?.id) {
        mutableLongStateOf(connection.player.currentPosition.coerceAtLeast(0L))
    }
    var duration by rememberSaveable(metadata?.id) {
        mutableLongStateOf(connection.player.duration.validDuration())
    }
    var draggedPosition by remember { mutableStateOf<Long?>(null) }
    var showAudioOutput by rememberSaveable { mutableStateOf(false) }
    var showSleepTimer by rememberSaveable { mutableStateOf(false) }
    var sleepTimerActive by remember { mutableStateOf(connection.service.sleepTimer.isActive) }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }
    var liveArtwork by remember(metadata?.id) { mutableStateOf<CanvasArtwork?>(null) }
    var showLyrics by rememberSaveable { mutableStateOf(false) }
    var lyricsFullScreen by rememberSaveable { mutableStateOf(false) }
    var lyricsToolsOpened by rememberSaveable(metadata?.id) { mutableStateOf(false) }
    var lyricsLoading by remember(metadata?.id) { mutableStateOf(false) }
    var lyricsResult by remember(metadata?.id) { mutableStateOf<AppleLyricsResult?>(null) }
    val systemInDarkTheme = isSystemInDarkTheme()

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = 0.dp,
        expandedBound = state.expandedBound,
        collapsedBound = 1.dp,
        initialAnchor = 1,
    )

    DisposableEffect(state.isExpanded, systemInDarkTheme) {
        val window = (context as? Activity)?.window
        if (window != null && state.isExpanded) {
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        }
        onDispose {
            if (window != null) {
                WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !systemInDarkTheme
            }
        }
    }

    LaunchedEffect(metadata?.id, dataSaver) {
        liveArtwork = if (dataSaver) {
            null
        } else {
            metadata?.let { withContext(Dispatchers.IO) { AppleLiveArtworkResolver.resolve(it) } }
        }
    }

    LaunchedEffect(showLyrics, metadata?.id) {
        val current = metadata
        if (showLyrics && current != null && lyricsResult == null) {
            lyricsLoading = true
            lyricsResult = withContext(Dispatchers.IO) {
                AppleLyricsPipeline.resolve(context.applicationContext, current)
            }
            lyricsLoading = false
        }
    }

    LaunchedEffect(lyricsToolsOpened, currentLyrics?.lyrics, metadata?.id) {
        val storedLyrics = currentLyrics?.lyrics
        if (
            lyricsToolsOpened &&
            !storedLyrics.isNullOrBlank() &&
            storedLyrics != LyricsEntity.LYRICS_NOT_FOUND
        ) {
            lyricsResult = AppleLyricsResult(
                provider = "Nocturne",
                raw = storedLyrics,
                lines = AppleLyricsTimingParser.parse(storedLyrics),
            )
            lyricsLoading = false
        }
    }

    LaunchedEffect(metadata?.id, isPlaying) {
        while (isActive) {
            position = connection.player.currentPosition.coerceAtLeast(0L)
            duration = connection.player.duration.validDuration()
            delay(if (isPlaying) 250 else 750)
        }
    }

    LaunchedEffect(sleepTimerActive) {
        while (sleepTimerActive && isActive) {
            val remaining = if (connection.service.sleepTimer.pauseWhenSongEnd) {
                connection.player.duration.validDuration() - connection.player.currentPosition.coerceAtLeast(0L)
            } else {
                connection.service.sleepTimer.triggerTime - System.currentTimeMillis()
            }
            sleepTimerTimeLeft = remaining.coerceAtLeast(0L)
            if (remaining <= 0L) sleepTimerActive = false
            delay(1000L)
        }
    }

    val openPlayerMenu: () -> Unit = {
        metadata?.let { current ->
            menuState.show {
                PlayerMenu(
                    mediaMetadata = current,
                    navController = navController,
                    playerBottomSheetState = state,
                    showVolumeControl = false,
                    onShowDetailsDialog = {
                        bottomSheetPageState.show { ShowMediaInfo(current.id) }
                    },
                    onDismiss = menuState::dismiss,
                )
            }
        }
        Unit
    }

    val openLyricsMenu: () -> Unit = {
        metadata?.let { current ->
            lyricsToolsOpened = true
            menuState.show {
                LyricsMenu(
                    lyricsProvider = {
                        currentLyrics ?: lyricsResult?.let { result ->
                            LyricsEntity(current.id, result.raw)
                        }
                    },
                    mediaMetadataProvider = { current },
                    onDismiss = menuState::dismiss,
                )
            }
        }
        Unit
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = Color.Unspecified,
        onDismiss = adapter::stopAndClear,
        collapsedContent = {
            AppleMiniPlayer(
                metadata = metadata,
                isPlaying = isPlaying,
                canSkipPrevious = canSkipPrevious,
                canSkipNext = canSkipNext,
                pureBlack = pureBlack,
                onExpand = state::expandSoft,
                onTogglePlayback = adapter::togglePlayback,
                onPrevious = adapter::previous,
                onNext = adapter::next,
            )
        },
    ) {
        AppleExpandedPlayer(
            metadata = metadata,
            liveArtwork = liveArtwork,
            showLyrics = showLyrics,
            lyricsFullScreen = lyricsFullScreen,
            lyricsResult = lyricsResult,
            lyricsLoading = lyricsLoading,
            currentFormat = currentFormat,
            formatLoading = formatLoading,
            connectedOutputName = connectedOutputName,
            playerBackgroundStyle = playerBackgroundStyle.toApplePlayerBackgroundStyle(),
            liked = currentSong?.song?.liked == true,
            isPlaying = isPlaying,
            position = draggedPosition ?: position,
            duration = duration,
            onLike = adapter::toggleFavorite,
            onPlayerActions = openPlayerMenu,
            onLyricsActions = openLyricsMenu,
            onSeek = { draggedPosition = it },
            onSeekFinished = {
                draggedPosition?.let(connection.player::seekTo)
                draggedPosition = null
            },
            onArtistClick = {
                metadata?.artists?.firstOrNull()?.id?.takeIf(String::isNotBlank)?.let { artistId ->
                    state.collapseSoft()
                    navController.navigate("artist/$artistId")
                }
            },
            onLyricsSeek = adapter::seekTo,
            onPrevious = adapter::previous,
            onTogglePlayback = adapter::togglePlayback,
            onNext = adapter::next,
            onLyrics = {
                if (showLyrics) lyricsFullScreen = false
                showLyrics = !showLyrics
            },
            onToggleLyricsFullScreen = { lyricsFullScreen = !lyricsFullScreen },
            onAudioOutput = { showAudioOutput = true },
            onQueue = queueSheetState::expandSoft,
            sleepTimerActive = sleepTimerActive,
            sleepTimerTimeLeft = sleepTimerTimeLeft,
            onSleepTimer = {
                if (sleepTimerActive) {
                    connection.service.sleepTimer.clear()
                    sleepTimerActive = false
                } else {
                    showSleepTimer = true
                }
            },
        )

        val queueTextColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface
        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            navController = navController,
            backgroundColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
            onBackgroundColor = queueTextColor,
            TextBackgroundColor = queueTextColor,
            textButtonColor = MaterialTheme.colorScheme.secondary,
            iconButtonColor = MaterialTheme.colorScheme.onSecondary,
            onShowLyrics = {
                queueSheetState.collapseSoft()
                showLyrics = true
            },
            pureBlack = pureBlack,
        )
    }

    if (showAudioOutput) {
        AppleAudioOutputSheet(onDismiss = { showAudioOutput = false })
    }

    if (showSleepTimer) {
        SleepTimerDialog(
            onDismiss = { showSleepTimer = false },
            onConfirm = { minutes ->
                connection.service.sleepTimer.start(minutes)
                sleepTimerActive = true
                showSleepTimer = false
            },
            onEndOfSong = {
                connection.service.sleepTimer.start(-1)
                sleepTimerActive = true
                showSleepTimer = false
            },
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppleExpandedPlayer(
    metadata: MediaMetadata?,
    liveArtwork: CanvasArtwork?,
    showLyrics: Boolean,
    lyricsFullScreen: Boolean,
    lyricsResult: AppleLyricsResult?,
    lyricsLoading: Boolean,
    currentFormat: FormatEntity?,
    formatLoading: Boolean,
    connectedOutputName: String?,
    playerBackgroundStyle: ApplePlayerBackgroundStyle,
    liked: Boolean,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onLike: () -> Unit,
    onPlayerActions: () -> Unit,
    onLyricsActions: () -> Unit,
    onSeek: (Long) -> Unit,
    onArtistClick: () -> Unit,
    onSeekFinished: () -> Unit,
    onLyricsSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlayback: () -> Unit,
    onNext: () -> Unit,
    onLyrics: () -> Unit,
    onToggleLyricsFullScreen: () -> Unit,
    onAudioOutput: () -> Unit,
    onQueue: () -> Unit,
    sleepTimerActive: Boolean,
    sleepTimerTimeLeft: Long,
    onSleepTimer: () -> Unit,
) {
    val playPauseRoundness by animateDpAsState(
        targetValue = if (isPlaying) 24.dp else 36.dp,
        animationSpec = tween(durationMillis = 90, easing = LinearEasing),
        label = "playPauseRoundness",
    )

    Box(Modifier.fillMaxSize().background(Color(0xFF202023))) {
        AppleMusicBackdrop(
            metadata = metadata,
            liveArtwork = liveArtwork,
            isPlaying = isPlaying,
            showLyrics = showLyrics,
            backgroundStyle = playerBackgroundStyle,
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = { fadeIn().togetherWith(fadeOut()) },
                label = "Lyrics",
                modifier = Modifier.weight(1f),
            ) { lyricsVisible ->
                if (lyricsVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                    ) {
                        AppleLyricsView(
                            result = lyricsResult,
                            positionMs = position,
                            loading = lyricsLoading,
                            modifier = Modifier.padding(horizontal = 24.dp),
                            onSeek = onLyricsSeek,
                        )
                    }
                } else {
                    Spacer(Modifier.fillMaxSize())
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            ) {
                AnimatedContent(targetState = showLyrics, label = "ThumbnailAnimation") { lyricsVisible ->
                    if (lyricsVisible) {
                        Row {
                            AsyncImage(
                                model = metadata?.thumbnailUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(3.dp)),
                            )
                            Spacer(Modifier.width(12.dp))
                        }
                    } else {
                        Spacer(Modifier.width(0.dp))
                    }
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = metadata?.title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = metadata?.artists?.joinToString { it.name }.orEmpty(),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            enabled = metadata?.artists?.firstOrNull()?.id?.isNotBlank() == true,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onArtistClick,
                        ),
                    )
                }

                Spacer(Modifier.width(12.dp))

                AppleRoundActionButton(
                    icon = if (showLyrics) R.drawable.fullscreen else R.drawable.more_vert,
                    contentDescription = if (showLyrics) "Full screen lyrics" else "More",
                    onClick = if (showLyrics) onToggleLyricsFullScreen else onPlayerActions,
                )

                Spacer(Modifier.width(12.dp))

                AppleRoundActionButton(
                    icon = if (showLyrics) R.drawable.more_horiz else if (liked) R.drawable.favorite else R.drawable.favorite_border,
                    contentDescription = if (showLyrics) "Lyrics options" else "Favorite",
                    onClick = if (showLyrics) onLyricsActions else onLike,
                )
            }

            Spacer(Modifier.height(20.dp))

            val trackInteractionSource = remember { MutableInteractionSource() }
            val isTrackDragged by trackInteractionSource.collectIsDraggedAsState()
            val isTrackPressed by trackInteractionSource.collectIsPressedAsState()
            val trackHeight by animateDpAsState(
                targetValue = if (isTrackDragged || isTrackPressed) 16.dp else 10.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
                label = "appleProgressTrackHeight",
            )

            Slider(
                value = position.coerceIn(0L, duration.coerceAtLeast(1L)).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = duration > 0L,
                interactionSource = trackInteractionSource,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White.copy(alpha = 0.7f),
                    activeTickColor = Color.White.copy(alpha = 0.7f),
                    thumbColor = Color.White.copy(alpha = 0.7f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                    disabledActiveTrackColor = Color.White.copy(alpha = 0.7f),
                    disabledInactiveTrackColor = Color.White.copy(alpha = 0.4f),
                    disabledThumbColor = Color.White.copy(alpha = 0.7f),
                ),
                thumb = { Spacer(Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White.copy(alpha = 0.7f),
                            activeTickColor = Color.White.copy(alpha = 0.7f),
                            inactiveTrackColor = Color.White.copy(alpha = 0.4f),
                            inactiveTickColor = Color.White.copy(alpha = 0.4f),
                        ),
                        trackHeight = trackHeight,
                    )
                },
                modifier = Modifier.padding(horizontal = 32.dp),
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp),
            ) {
                Text(
                    text = makeTimeString(position),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                AppleQualityBadge(
                    format = currentFormat,
                    isLoading = formatLoading,
                    sleepTimerActive = sleepTimerActive,
                    sleepTimerTimeLeft = sleepTimerTimeLeft,
                )

                Text(
                    text = if (duration > 0L) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = !lyricsFullScreen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                ) {
                    Box(Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onPrevious,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.apple_skip_previous),
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(playPauseRoundness))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onTogglePlayback,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(if (isPlaying) R.drawable.pause_applemusic else R.drawable.play_applemusic),
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            colorFilter = ColorFilter.tint(Color.White),
                            modifier = Modifier.size(72.dp),
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .align(Alignment.Center)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onNext,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.apple_skip_next),
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !lyricsFullScreen && !connectedOutputName.isNullOrBlank(),
                enter = slideInVertically(animationSpec = tween(260)) { it } + fadeIn(tween(180)),
                exit = slideOutVertically(animationSpec = tween(200)) { it } + fadeOut(tween(140)),
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.bluetooth),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = connectedOutputName.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            AnimatedVisibility(visible = !lyricsFullScreen) {
                AppleBottomActions(
                    showLyrics = showLyrics,
                    sleepTimerActive = sleepTimerActive,
                    onQueue = onQueue,
                    onAudioOutput = onAudioOutput,
                    onSleepTimer = onSleepTimer,
                    onLyrics = onLyrics,
                )
            }
        }
    }
}

@Composable
private fun AppleMusicBackdrop(
    metadata: MediaMetadata?,
    liveArtwork: CanvasArtwork?,
    isPlaying: Boolean,
    showLyrics: Boolean,
    backgroundStyle: ApplePlayerBackgroundStyle,
) {
    val context = LocalContext.current
    val preferredArtworkUrl = remember(metadata?.id, metadata?.thumbnailUrl) {
        metadata?.thumbnailUrl?.toAppleExpandedArtworkUrl()
    }
    var resolvedArtworkUrl by remember(metadata?.id, preferredArtworkUrl) {
        mutableStateOf(preferredArtworkUrl)
    }
    val artworkRequest = remember(context, metadata?.id, resolvedArtworkUrl) {
        ImageRequest.Builder(context)
            .data(resolvedArtworkUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    // A blurred RenderEffect layer can be created before AsyncImage has a painter.
    // On some devices that empty layer stays cached until the player is reopened.
    // Wait for Coil success before creating the blur layer. The source thumbnail is
    // normally already cached by the list or mini-player, so its artwork-derived
    // colour appears immediately while the high-resolution foreground loads.
    val blurArtworkUrl = metadata?.thumbnailUrl ?: resolvedArtworkUrl
    val blurArtworkRequest = remember(context, metadata?.id, blurArtworkUrl) {
        ImageRequest.Builder(context)
            .data(blurArtworkUrl)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    var blurArtworkReady by remember(metadata?.id, blurArtworkUrl) {
        mutableStateOf(false)
    }

    val useFallbackArtwork = {
        resolvedArtworkUrl?.appleExpandedArtworkFallback()?.let { fallback ->
            resolvedArtworkUrl = fallback
        }
        Unit
    }
    val clearArtworkAlpha by animateFloatAsState(
        targetValue = if (showLyrics) 0f else 1f,
        animationSpec = tween(500),
        label = "clearArtworkAlpha",
    )

    Box(Modifier.fillMaxSize()) {
        if (backgroundStyle == ApplePlayerBackgroundStyle.SOLID) {
            Box(Modifier.fillMaxSize().background(Color(0xFF202023)))
        } else if (backgroundStyle == ApplePlayerBackgroundStyle.APPLE_MUSIC) {
            AsyncImage(
                model = blurArtworkRequest,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { blurArtworkReady = true },
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (blurArtworkReady) Modifier.blur(150.dp)
                        else Modifier.alpha(0f),
                    ),
            )
        }

        if (backgroundStyle != ApplePlayerBackgroundStyle.SOLID) {
            val artworkModifier = if (backgroundStyle == ApplePlayerBackgroundStyle.ARTWORK) {
                Modifier.fillMaxSize().alpha(clearArtworkAlpha)
            } else {
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.65f)
                    .alpha(clearArtworkAlpha)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.00f to Color.Black,
                                    0.75f to Color.Black,
                                    0.92f to Color.Black.copy(alpha = 0.4f),
                                    1.00f to Color.Transparent,
                                ),
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
            }
            Box(modifier = artworkModifier) {
                AsyncImage(
                    model = artworkRequest,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onError = { useFallbackArtwork() },
                    modifier = Modifier.fillMaxSize(),
                )
                CanvasArtworkPlayer(
                    primaryUrl = liveArtwork?.preferredAnimationUrl,
                    fallbackUrl = liveArtwork?.videoUrl,
                    isPlaying = isPlaying,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.4f),
                    ),
                ),
            ),
        )
    }
}

private fun String.toApplePlayerBackgroundStyle(): ApplePlayerBackgroundStyle =
    ApplePlayerBackgroundStyle.entries.firstOrNull { it.name == this }
        ?: ApplePlayerBackgroundStyle.APPLE_MUSIC

@Composable
private fun AppleRoundActionButton(
    icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AppleQualityBadge(
    format: FormatEntity?,
    isLoading: Boolean,
    sleepTimerActive: Boolean,
    sleepTimerTimeLeft: Long,
) {
    val formatText = remember(format) {
        format?.let {
            val codec = it.codecs.takeIf(String::isNotBlank)?.uppercase()
                ?: it.mimeType.substringAfter("/", it.mimeType).uppercase()
            val bitrate = it.bitrate.takeIf { value -> value > 0 }?.let { value -> "${value / 1000} kbps" }
            listOfNotNull(codec.takeIf(String::isNotBlank), bitrate).joinToString(" • ")
        }.orEmpty()
    }
    if (!sleepTimerActive && formatText.isBlank() && !isLoading) return

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        if (sleepTimerActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.sleep_timer),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = makeTimeString(sleepTimerTimeLeft),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
        } else if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(11.dp),
                    color = Color.White.copy(alpha = 0.82f),
                    strokeWidth = 1.4.dp,
                )
                Text(
                    text = "Loading",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp,
                    ),
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                )
            }
        } else {
            Text(
                text = formatText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp,
                ),
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppleBottomActions(
    showLyrics: Boolean,
    sleepTimerActive: Boolean,
    onQueue: () -> Unit,
    onAudioOutput: () -> Unit,
    onSleepTimer: () -> Unit,
    onLyrics: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)),
    ) {
        TextButton(onClick = onQueue) {
            Icon(
                painter = painterResource(R.drawable.apple_queue),
                contentDescription = "Playing Next",
                modifier = Modifier.size(24.dp),
                tint = Color.White,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier.width(116.dp),
        ) {
            ToggleButton(
                checked = false,
                onCheckedChange = { onAudioOutput() },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.height(40.dp).weight(1f),
                contentPadding = PaddingValues(0.dp),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    checkedContainerColor = Color.White.copy(alpha = 0.4f),
                    checkedContentColor = Color.White,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.speaker_apple),
                    contentDescription = "Audio Output",
                    modifier = Modifier.size(24.dp),
                )
            }

            ToggleButton(
                checked = sleepTimerActive,
                onCheckedChange = { onSleepTimer() },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.height(40.dp).weight(1f),
                contentPadding = PaddingValues(0.dp),
                colors = ToggleButtonDefaults.toggleButtonColors(
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    checkedContainerColor = Color.White.copy(alpha = 0.4f),
                    checkedContentColor = Color.White,
                ),
            ) {
                Icon(
                    painter = painterResource(R.drawable.sleep_timer),
                    contentDescription = "Sleep Timer",
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        TextButton(onClick = onLyrics) {
            Icon(
                painter = painterResource(R.drawable.apple_music_me),
                contentDescription = if (showLyrics) "Now Playing" else "Lyrics",
                modifier = Modifier.size(24.dp),
                tint = Color.White,
            )
        }
    }
}

private fun Long.validDuration(): Long = if (this == C.TIME_UNSET || this < 0L) 0L else this
