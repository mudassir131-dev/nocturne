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
import com.mudassir131.yt.ui.component.VeluneLoader
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status bar ke baad 48px (~24dp) gap
        Spacer(Modifier.height(24.dp))

        // 1. TOP HEADER ("Now Playing" + Subtitle)
        Text(
            text = "Now Playing",
            style = MaterialTheme.typography.titleSmall,
            color = textBackgroundColor.copy(alpha = 0.70f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        val headerSubtitle = queueTitle ?: mediaMetadata.album?.title ?: artistLine
        if (headerSubtitle.isNotBlank()) {
            Text(
                text = headerSubtitle,
                style = MaterialTheme.typography.titleMedium,
                color = textBackgroundColor.copy(alpha = 0.85f),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .basicMarquee()
            )
        }

        // Dynamic spacer to absorb vertical height before artwork
        Spacer(Modifier.weight(1f))

        // 2. THUMBNAIL IMAGE — square (1:1), width = 85%, rounded corners (24dp)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
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

        // Fixed gap from artwork to title Y ≈ 36dp
        Spacer(Modifier.height(36.dp))

        // 3. ROW: Left song title + artist (vertically centered Column), Right 3 transparent action icons
        Row(
            modifier = Modifier.fillMaxWidth(0.88f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
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

            Spacer(Modifier.width(12.dp))

            // Right 3 transparent action buttons: Share, Heart, More
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Share
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
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
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = "Share",
                        tint = textBackgroundColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Favorite / Heart
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                playerConnection.toggleLike()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (currentSongLiked) R.drawable.favorite else R.drawable.favorite_border
                        ),
                        contentDescription = "Favorite",
                        tint = if (currentSongLiked) MaterialTheme.colorScheme.primary else textBackgroundColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // More (...)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
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
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_horiz),
                        contentDescription = "More Options",
                        tint = textBackgroundColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Uske niche 24px (~12dp) gap
        Spacer(Modifier.height(12.dp))

        // 4. PROGRESS SLIDER (CapsulePlayerSlider with thick active track and | line vertical handle)
        val safeDuration = if (duration <= 0L || duration == C.TIME_UNSET) 0f else duration.toFloat()
        val safePosition = (sliderPosition ?: position).toFloat().coerceIn(0f, safeDuration.coerceAtLeast(0f))

        Column(
            modifier = Modifier.fillMaxWidth(0.88f)
        ) {
            CapsulePlayerSlider(
                value = safePosition,
                valueRange = 0f..safeDuration.coerceAtLeast(0f),
                onValueChange = { onSliderValueChange(it.toLong()) },
                onValueChangeFinished = onSliderValueChangeFinished,
                activeColor = textButtonColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textBackgroundColor.copy(alpha = 0.75f)
                )
                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textBackgroundColor.copy(alpha = 0.75f)
                )
            }
        }

        // Uske niche 32px (~16dp) gap
        Spacer(Modifier.height(16.dp))

        // 5. PLAYBACK CONTROLS: Compact middle alignment with fixed 16dp spacing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle (44dp)
            val smallShape = RoundedCornerShape(14.dp)
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
                    .size(44.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Previous (52dp)
            val mediumShape = RoundedCornerShape(16.dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerConnection.seekToPrevious()
                },
                enabled = canSkipPrevious,
                shape = mediumShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(52.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Play / Pause (74dp wide × 70dp high Solid Pastel Accent Container)
            val playShape = RoundedCornerShape(playPauseCorner)
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
                    .size(width = 74.dp, height = 70.dp)
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
                        VeluneLoader(size = 32.dp, color = Color.Black)
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
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Next (52dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    playerConnection.seekToNext()
                },
                enabled = canSkipNext,
                shape = mediumShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.14f),
                modifier = Modifier
                    .size(52.dp)
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Repeat (44dp)
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
                    .size(44.dp)
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
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 32px (~16dp) gap
        Spacer(Modifier.height(16.dp))

        // 6. BOTTOM ACTION ROW: [ Queue ]  (🌙)  [ Lyrics ] centered compactly with 12dp spacing
        val bottomPillShape = RoundedCornerShape(50)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Queue Pill (width 110dp, height 40dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    queueSheetState.expandSoft()
                },
                shape = bottomPillShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .width(110.dp)
                    .height(40.dp)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = bottomPillShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.12f)
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
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Queue",
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Sleep Timer Circle (40dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onShowSleepTimer()
                },
                shape = CircleShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .size(40.dp)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = CircleShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.12f)
                            )
                        } else Modifier
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = "Sleep Timer",
                        tint = textBackgroundColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Lyrics Pill (width 110dp, height 40dp)
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    lyricsSheetState.expandSoft()
                },
                shape = bottomPillShape,
                color = if (isGlassActive) Color.Transparent else textBackgroundColor.copy(alpha = 0.12f),
                modifier = Modifier
                    .width(110.dp)
                    .height(40.dp)
                    .then(
                        if (isGlassActive) {
                            Modifier.glassmorphicButton(
                                isGlassActive = true,
                                shape = bottomPillShape,
                                baseColor = textBackgroundColor.copy(alpha = 0.12f)
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
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Lyrics",
                        color = textBackgroundColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Bottom system gesture / navigation padding
        Spacer(Modifier.navigationBarsPadding().height(16.dp))
    }
}
