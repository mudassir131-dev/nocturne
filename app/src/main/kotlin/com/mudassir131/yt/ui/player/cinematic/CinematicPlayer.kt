/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

@file:OptIn(ExperimentalMaterial3Api::class)

package com.mudassir131.yt.ui.player.cinematic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.STATE_ENDED
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.SliderStyle
import com.mudassir131.yt.constants.SliderStyleKey
import com.mudassir131.yt.extensions.togglePlayPause
import com.mudassir131.yt.extensions.toggleRepeatMode
import com.mudassir131.yt.models.MediaMetadata
import com.mudassir131.yt.playback.PlayerConnection
import com.mudassir131.yt.ui.component.BottomSheetPageState
import com.mudassir131.yt.ui.component.BottomSheetState
import com.mudassir131.yt.ui.component.MenuState
import com.mudassir131.yt.ui.component.NocturneLoader
import com.mudassir131.yt.ui.component.CapsulePlayerSlider
import com.mudassir131.yt.ui.menu.PlayerMenu
import com.mudassir131.yt.ui.player.StyledPlaybackSlider
import com.mudassir131.yt.ui.theme.glassmorphic
import com.mudassir131.yt.ui.theme.glassmorphicButton
import com.mudassir131.yt.ui.utils.ShowMediaInfo
import com.mudassir131.yt.utils.makeTimeString
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.innertube.toHighResThumbnail

/**
 * Clean Single Source of Truth Cinematic Player UI matching Target Reference Mockup.
 */
@Composable
fun CinematicPlayerView(
    mediaMetadata: MediaMetadata,
    queueTitle: String?,
    playbackState: Int,
    isPlaying: Boolean,
    isLoading: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    currentSongLiked: Boolean,
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    sliderStyle: SliderStyle,
    sliderPosition: Long?,
    position: Long,
    duration: Long,
    textBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    isGlassActive: Boolean,
    playerConnection: PlayerConnection,
    navController: NavController,
    state: BottomSheetState,
    queueSheetState: BottomSheetState,
    lyricsSheetState: BottomSheetState,
    menuState: MenuState,
    bottomSheetPageState: BottomSheetPageState,
    onSliderValueChange: (Long) -> Unit,
    onSliderValueChangeFinished: () -> Unit,
    onShowSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    // Dynamically observe the persistent Player Slider Style preference
    val (activeSliderStyle) = rememberEnumPreference(
        key = SliderStyleKey,
        defaultValue = sliderStyle
    )

    val artistLine = remember(mediaMetadata.artists) {
        mediaMetadata.artists.joinToString(", ") { it.name }
    }

    val playPauseCorner by animateDpAsState(
        targetValue = if (isPlaying) 16.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cinematicPlayPauseCorner"
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Layout geometry, measured from the ArchiveTune reference player.
    // Reference device: 432 x 960 dp (1080 x 2400 px @ 2.5). Every value below is
    // taken from that screenshot rather than eyeballed, so the whole vertical
    // rhythm lives here instead of being scattered through inline Spacer values.
    // ──────────────────────────────────────────────────────────────────────────
    // Horizontal: the artwork, metadata row, slider and time labels share one
    // content column (30dp side margins); the bottom action row is wider and the
    // playback control row is narrower than that column.
    val contentWidth = 0.86f
    val bottomRowWidth = 0.91f
    val artworkCorner = 14.dp
    // Vertical rhythm
    val headerTopGap = 16.dp          // status bar inset -> "Now Playing"
    val headerTitleGap = 4.dp         // "Now Playing" -> subtitle
    val artworkToTitleGap = 54.dp     // artwork bottom -> song title
    val metadataToSliderGap = 16.dp   // artist/action buttons -> slider
    val sliderToTimeGap = 6.dp        // slider -> time labels
    val timeToControlsGap = 14.dp     // time labels -> playback controls
    val controlsToBottomGap = 38.dp   // playback controls -> Queue/Sleep/Lyrics
    val bottomGestureGap = 7.dp       // Queue/Sleep/Lyrics -> navigation inset
    // Action buttons (share / favorite / more)
    val actionButtonSize = 42.dp
    val actionButtonGap = 12.dp
    // Playback controls: outer pair, skip pair, centre play/pause, and the two
    // distinct gaps between them (the reference does not use an even arrangement).
    val sideControlSize = 44.dp
    val skipControlSize = 56.dp
    val playControlSize = 86.dp
    val controlGapOuter = 12.dp       // shuffle <-> previous, next <-> repeat
    val controlGapInner = 19.dp       // previous <-> play, play <-> next
    // Bottom action row
    val bottomPillHeight = 48.dp
    val bottomPillGap = 12.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(headerTopGap))

        // 1. TOP HEADER ("Now Playing" + Playlist/Album Subtitle)
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.bodyMedium,
            color = textBackgroundColor.copy(alpha = 0.70f),
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(headerTitleGap))
        val headerSubtitle = queueTitle ?: mediaMetadata.album?.title ?: artistLine
        if (headerSubtitle.isNotBlank()) {
            Text(
                text = headerSubtitle,
                style = MaterialTheme.typography.titleMedium,
                color = textBackgroundColor.copy(alpha = 0.90f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth(contentWidth)
                    .basicMarquee()
            )
        }

        // Header -> artwork. The reference leaves ~75dp of slack here on a 960dp
        // tall screen; taking it as the flexible gap keeps every spacing below the
        // artwork exact and lets shorter screens compress this one first.
        Spacer(Modifier.weight(1f))

        // 2. THUMBNAIL IMAGE — square (1:1), width = content column, 14dp corners
        Box(
            modifier = Modifier
                .fillMaxWidth(contentWidth)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(artworkCorner))
        ) {
            val artworkUrl = mediaMetadata.thumbnailUrl?.toHighResThumbnail()
                ?: playerConnection.player.currentMediaItem?.mediaMetadata?.artworkUri?.toString()?.toHighResThumbnail()
            AsyncImage(
                model = artworkUrl,
                contentDescription = "Album Artwork",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Artwork -> song title. Measured 60dp from the artwork edge to the title's
        // cap height in the reference; 54dp of layout gap once headlineSmall's own
        // ascent slack is accounted for.
        Spacer(Modifier.height(artworkToTitleGap))

        // 3. ROW: Left (Song Title + Artist), Right (3 Rounded Glass Action Buttons: Share, Favorite, More)
        Row(
            modifier = Modifier.fillMaxWidth(contentWidth),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = mediaMetadata.title,
                    color = textBackgroundColor,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                        .combinedClickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                mediaMetadata.album?.let { album ->
                                    state.collapseSoft()
                                    navController.navigate("album/${album.id}")
                                }
                            },
                            onLongClick = {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("Copied Title", mediaMetadata.title)
                                )
                                Toast.makeText(context, "Copied Title", Toast.LENGTH_SHORT).show()
                            }
                        )
                )

                Text(
                    text = artistLine,
                    color = textBackgroundColor.copy(alpha = 0.70f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee()
                        .combinedClickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                mediaMetadata.artists.firstOrNull()?.id?.let { artistId ->
                                    if (artistId.isNotBlank()) {
                                        navController.navigate("artist/$artistId")
                                        state.collapseSoft()
                                    }
                                }
                            },
                            onLongClick = {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("Copied Artist", artistLine)
                                )
                                Toast.makeText(context, "Copied Artist", Toast.LENGTH_SHORT).show()
                            }
                        )
                )
            }

            // Right 3 individual rounded rectangle glass buttons: Share, Heart, More
            val actionShape = RoundedCornerShape(14.dp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(actionButtonGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share
                Surface(
                    onClick = {
                        val intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "https://music.youtube.com/watch?v=${mediaMetadata.id}"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, null))
                    },
                    shape = actionShape,
                    color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(actionButtonSize)
                        .then(
                            if (isGlassActive) {
                                Modifier.glassmorphicButton(
                                    isGlassActive = true,
                                    shape = actionShape,
                                    baseColor = textBackgroundColor.copy(alpha = 0.12f)
                                )
                            } else Modifier
                        )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.share),
                            contentDescription = "Share",
                            tint = textBackgroundColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Favorite / Heart
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        playerConnection.toggleLike()
                    },
                    shape = actionShape,
                    color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(actionButtonSize)
                        .then(
                            if (isGlassActive) {
                                Modifier.glassmorphicButton(
                                    isGlassActive = true,
                                    shape = actionShape,
                                    baseColor = textBackgroundColor.copy(alpha = 0.12f)
                                )
                            } else Modifier
                        )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(
                                if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border
                            ),
                            contentDescription = "Favorite",
                            tint = if (currentSongLiked) MaterialTheme.colorScheme.primary else textBackgroundColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // More Options (...)
                Surface(
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onShowDetailsDialog = {
                                    bottomSheetPageState.show {
                                        ShowMediaInfo(mediaMetadata.id)
                                    }
                                },
                                onDismiss = menuState::dismiss
                            )
                        }
                    },
                    shape = actionShape,
                    color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.12f),
                    modifier = Modifier
                        .size(actionButtonSize)
                        .then(
                            if (isGlassActive) {
                                Modifier.glassmorphicButton(
                                    isGlassActive = true,
                                    shape = actionShape,
                                    baseColor = textBackgroundColor.copy(alpha = 0.12f)
                                )
                            } else Modifier
                        )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painter = painterResource(R.drawable.more_horiz),
                            contentDescription = "More Options",
                            tint = textBackgroundColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(metadataToSliderGap))

        // 4. PROGRESS SLIDER — CapsulePlayerSlider, i.e. the same stock Material 3
        // Expressive Slider the Equalizer's band sliders use. Its natural height is
        // kept (the bar handle stands taller than the track and would be clipped by
        // a forced height); only its position and width come from the reference.
        val safeDuration = if (duration <= 0L || duration == C.TIME_UNSET) 0f else duration.toFloat()
        val safePosition = (sliderPosition ?: position).toFloat().coerceIn(0f, safeDuration.coerceAtLeast(0f))

        Column(
            modifier = Modifier.fillMaxWidth(contentWidth)
        ) {
            StyledPlaybackSlider(
                sliderStyle = activeSliderStyle,
                value = safePosition,
                valueRange = 0f..safeDuration.coerceAtLeast(0f),
                onValueChange = { onSliderValueChange(it.toLong()) },
                onValueChangeFinished = onSliderValueChangeFinished,
                activeColor = textButtonColor,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(sliderToTimeGap))

            // Current position / total duration, flush with the content column edges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = textBackgroundColor.copy(alpha = 0.70f)
                )
                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Normal,
                    color = textBackgroundColor.copy(alpha = 0.70f)
                )
            }
        }

        Spacer(Modifier.height(timeToControlsGap))

        // 5. PLAYBACK CONTROLS (Shuffle, Prev, Center Squircle Play/Pause, Next, Repeat)
        // Centred as a group with explicit gaps: the reference's button centres are
        // symmetric about the screen centre but the shuffle/previous gap is smaller
        // than the previous/play gap, so SpaceBetween/SpaceEvenly cannot reproduce it.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            val smallShape = RoundedCornerShape(16.dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerConnection.player.shuffleModeEnabled = !shuffleModeEnabled
                },
                shape = smallShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(
                    alpha = if (shuffleModeEnabled) 0.22f else 0.12f
                ),
                modifier = Modifier
                    .size(sideControlSize)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = smallShape,
                                baseColor = textBackgroundColor.copy(alpha = if (shuffleModeEnabled) 0.22f else 0.12f)
                            )
                        } else Modifier
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.shuffle),
                        contentDescription = "Shuffle",
                        tint = textBackgroundColor.copy(alpha = if (shuffleModeEnabled) 1f else 0.65f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(controlGapOuter))

            // Previous
            val mediumShape = RoundedCornerShape(18.dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerConnection.seekToPrevious()
                },
                enabled = canSkipPrevious,
                shape = mediumShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(skipControlSize)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = mediumShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.14f)
                            )
                        } else Modifier
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Previous"
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.apple_skip_previous),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = if (canSkipPrevious) 1f else 0.4f),
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Spacer(Modifier.width(controlGapInner))

            // Play / Pause — the deliberately larger centre control
            val playShape = RoundedCornerShape(28.dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (playbackState == STATE_ENDED) {
                        playerConnection.player.seekTo(0, 0)
                        playerConnection.player.playWhenReady = true
                    } else {
                        playerConnection.player.togglePlayPause()
                    }
                },
                shape = playShape,
                color = if (isGlassActive) Color.Transparent else textButtonColor,
                modifier = Modifier
                    .size(playControlSize)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = playShape,
                                baseColor = textButtonColor.copy(alpha = 0.35f)
                            )
                        } else Modifier
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        NocturneLoader(size = 36.dp, color = Color.Black)
                    } else {
                        Icon(
                            painter = painterResource(
                                when {
                                    playbackState == STATE_ENDED -> R.drawable.replay
                                    isPlaying -> R.drawable.pause_applemusic
                                    else -> R.drawable.play_applemusic
                                }
                            ),
                            contentDescription = null,
                            tint = if (isGlassActive) textBackgroundColor else Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(controlGapInner))

            // Next
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerConnection.seekToNext()
                },
                enabled = canSkipNext,
                shape = mediumShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(skipControlSize)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = mediumShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.14f)
                            )
                        } else Modifier
                    )
                    .semantics {
                        role = Role.Button
                        contentDescription = "Next"
                    }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.apple_skip_next),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = if (canSkipNext) 1f else 0.4f),
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Spacer(Modifier.width(controlGapOuter))

            // Repeat
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerConnection.player.toggleRepeatMode()
                },
                shape = smallShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(
                    alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 0.22f else 0.12f
                ),
                modifier = Modifier
                    .size(sideControlSize)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = smallShape,
                                baseColor = textBackgroundColor.copy(alpha = if (repeatMode != Player.REPEAT_MODE_OFF) 0.22f else 0.12f)
                            )
                        } else Modifier
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> R.drawable.repeat_one
                                else -> R.drawable.repeat
                            }
                        ),
                        contentDescription = "Repeat",
                        tint = textBackgroundColor.copy(alpha = if (repeatMode == Player.REPEAT_MODE_OFF) 0.65f else 1f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(controlsToBottomGap))

        // 6. BOTTOM ACTION ROW: [ ≡ Queue ]  ( 🌙 )  [ 💬 Lyrics ]
        // Wider than the content column in the reference — its pills sit ~20dp from
        // the screen edges, outside the artwork's 30dp margins.
        val bottomPillShape = RoundedCornerShape(24.dp)
        Row(
            modifier = Modifier.fillMaxWidth(bottomRowWidth),
            horizontalArrangement = Arrangement.spacedBy(bottomPillGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Queue Pill
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    queueSheetState.expandSoft()
                },
                shape = bottomPillShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.13f),
                modifier = Modifier
                    .weight(1f)
                    .height(bottomPillHeight)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = bottomPillShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.13f)
                            )
                        } else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.apple_queue),
                        contentDescription = null,
                        tint = textBackgroundColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Queue",
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Sleep Timer Squircle
            val sleepShape = RoundedCornerShape(24.dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowSleepTimer()
                },
                shape = sleepShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.13f),
                modifier = Modifier
                    .size(bottomPillHeight)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = sleepShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.13f)
                            )
                        } else Modifier
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = "Sleep Timer",
                        tint = textBackgroundColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Lyrics Pill
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    lyricsSheetState.expandSoft()
                },
                shape = bottomPillShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.13f),
                modifier = Modifier
                    .weight(1f)
                    .height(bottomPillHeight)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = bottomPillShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.13f)
                            )
                        } else Modifier
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_apple_lyrics),
                        contentDescription = null,
                        tint = textBackgroundColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Lyrics",
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Bottom action row -> navigation / gesture inset. The player background
        // itself keeps running behind the navigation bar; only content is inset.
        Spacer(Modifier.navigationBarsPadding().height(bottomGestureGap))
    }
}
