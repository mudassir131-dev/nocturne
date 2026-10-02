/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import com.mudassir131.yt.R
import com.mudassir131.yt.utils.makeTimeString
import kotlinx.coroutines.delay

@Composable
fun FullscreenVideoPlayer(
    musicVideo: VisualContent.MusicVideo,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onDismiss: () -> Unit,
    onEnterPip: () -> Unit = {},
    onSelectQuality: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    BackHandler {
        onDismiss()
    }

    // Configure system bars for immersive video experience
    DisposableEffect(Unit) {
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }

        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        onDispose {
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = originalOrientation
        }
    }

    var showControls by remember { mutableStateOf(true) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var isLandscape by remember { mutableStateOf(false) }
    var draggedPosition by remember { mutableStateOf<Long?>(null) }
    var isMuted by remember { mutableStateOf(true) }

    // Auto-hide controls
    LaunchedEffect(showControls, isPlaying, showQualityMenu, draggedPosition) {
        if (showControls && isPlaying && !showQualityMenu && draggedPosition == null) {
            delay(3500L)
            showControls = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { showControls = !showControls },
                ),
        ) {
            // Full Video Player
            MusicVideoPlayer(
                musicVideo = musicVideo,
                isPlaying = isPlaying,
                position = draggedPosition ?: position,
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                muted = isMuted,
                modifier = Modifier.fillMaxSize(),
                onPlaybackFailed = onDismiss,
            )

            // Sleek Apple-inspired Controls Overlay
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(tween(200)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f),
                                ),
                            ),
                        ),
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Exit Fullscreen Button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_back),
                                contentDescription = "Exit Fullscreen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp),
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        // Title & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = musicVideo.title.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.basicMarquee(),
                            )
                            if (!musicVideo.artist.isNullOrBlank()) {
                                Text(
                                    text = musicVideo.artist,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        // Mute / Unmute
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable { isMuted = !isMuted },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(if (isMuted) R.drawable.volume_off else R.drawable.volume_up),
                                contentDescription = if (isMuted) "Unmute Video Audio" else "Mute Video Audio",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        // Quality Selector
                        val availableQualities = musicVideo.availableQualities
                        if (availableQualities.isNotEmpty() && onSelectQuality != null) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                        .clickable { showQualityMenu = true }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = musicVideo.qualityLabel ?: "Quality",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                        ),
                                    )
                                }

                                DropdownMenu(
                                    expanded = showQualityMenu,
                                    onDismissRequest = { showQualityMenu = false },
                                    modifier = Modifier.background(Color(0xFF2C2C2E)),
                                ) {
                                    availableQualities.forEach { option ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = option.label,
                                                    color = Color.White,
                                                    fontWeight = if (option.label == musicVideo.qualityLabel) FontWeight.Bold else FontWeight.Normal,
                                                )
                                            },
                                            onClick = {
                                                showQualityMenu = false
                                                onSelectQuality(option.itag)
                                            },
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))
                        }

                        // Picture in Picture
                        val supportsPip = remember(context) {
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
                        }
                        if (supportsPip) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .clickable(onClick = onEnterPip),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.desktop_windows),
                                    contentDescription = "Picture-in-Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            Spacer(Modifier.width(8.dp))
                        }

                        // Orientation Toggle
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    isLandscape = !isLandscape
                                    activity?.requestedOrientation = if (isLandscape) {
                                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.motion_photos_on),
                                contentDescription = "Rotate",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    // Center Playback Controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                    ) {
                        // Replay 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onSeek((position - 10_000L).coerceAtLeast(0L)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_replay_10),
                                contentDescription = "Rewind 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }

                        // Play / Pause
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable(onClick = onTogglePlayPause),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp),
                            )
                        }

                        // Forward 10s
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                                .clickable { onSeek((position + 10_000L).coerceAtMost(duration)) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_forward_10),
                                contentDescription = "Forward 10 seconds",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }

                    // Bottom Bar with Timeline Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    ) {
                        val currentPos = draggedPosition ?: position
                        val totalDuration = duration.coerceAtLeast(1L)

                        Slider(
                            value = currentPos.coerceIn(0L, totalDuration).toFloat(),
                            onValueChange = { draggedPosition = it.toLong() },
                            onValueChangeFinished = {
                                draggedPosition?.let(onSeek)
                                draggedPosition = null
                            },
                            valueRange = 0f..totalDuration.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = makeTimeString(currentPos),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                            Text(
                                text = if (duration > 0L) makeTimeString(duration) else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
