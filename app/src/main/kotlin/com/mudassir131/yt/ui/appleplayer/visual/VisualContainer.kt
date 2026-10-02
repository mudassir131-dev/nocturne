/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.appleplayer.liveart.CanvasArtworkPlayer
import kotlinx.coroutines.delay
import kotlin.math.abs

@Composable
fun VisualContainer(
    visualResult: VisualResolutionResult,
    activeMode: VisualMode,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    artworkRequest: ImageRequest,
    useFallbackArtwork: () -> Unit,
    modifier: Modifier = Modifier,
    onModeChange: (VisualMode) -> Unit,
    onEnterFullscreen: () -> Unit = {},
    onEnterPip: () -> Unit = {},
    onSelectQuality: ((Int) -> Unit)? = null,
) {
    val context = LocalContext.current
    val supportsPip = remember(context) {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    var showOverlayControls by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }

    // Auto-hide controls after 3.5 seconds
    LaunchedEffect(showOverlayControls, showQualityMenu) {
        if (showOverlayControls && !showQualityMenu) {
            delay(3500L)
            showOverlayControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { showOverlayControls = !showOverlayControls },
            ),
    ) {
        // 1. Base Layer: Artwork image (always available as backdrop & seamless fallback)
        AsyncImage(
            model = artworkRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            onError = { useFallbackArtwork() },
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Middle Layer: Canvas (when mode is CANVAS and canvas exists)
        if (activeMode == VisualMode.CANVAS && visualResult.canvas != null) {
            CanvasArtworkPlayer(
                primaryUrl = visualResult.canvas.primaryUrl,
                fallbackUrl = visualResult.canvas.fallbackUrl,
                isPlaying = isPlaying,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 3. Top Playback Layer: Full Music Video (when mode is MUSIC_VIDEO and video exists)
        if (activeMode == VisualMode.MUSIC_VIDEO && visualResult.musicVideo != null) {
            MusicVideoPlayer(
                musicVideo = visualResult.musicVideo,
                isPlaying = isPlaying,
                position = position,
                modifier = Modifier.fillMaxSize(),
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT,
                onPlaybackFailed = {
                    // Graceful fallback: Canvas if available, otherwise Artwork
                    val fallbackMode = if (visualResult.hasCanvas) VisualMode.CANVAS else VisualMode.ARTWORK
                    onModeChange(fallbackMode)
                },
            )
        }

        // 4. Subtle Apple-style segmented toggle pill: [ Video | Canvas ] (positioned below status bar)
        if (visualResult.hasBothVideoAndCanvas) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            ) {
                SegmentedVisualToggle(
                    activeMode = activeMode,
                    onModeChange = onModeChange,
                )
            }
        }

        // 5. Minimal Video Floating Action Controls (Fullscreen, PiP, Quality)
        AnimatedVisibility(
            visible = (activeMode == VisualMode.MUSIC_VIDEO && (showOverlayControls || !visualResult.hasBothVideoAndCanvas)),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(250)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Quality Selector (if available)
                val currentQuality = visualResult.musicVideo?.qualityLabel
                val availableQualities = visualResult.musicVideo?.availableQualities.orEmpty()
                if (availableQualities.isNotEmpty() && onSelectQuality != null) {
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { showQualityMenu = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = currentQuality ?: "HD",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
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
                                            fontWeight = if (option.label == currentQuality) FontWeight.Bold else FontWeight.Normal,
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
                }

                // Picture-in-Picture Button (if supported)
                if (supportsPip) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(onClick = onEnterPip),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.desktop_windows),
                            contentDescription = "Picture in Picture",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                // Fullscreen Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(onClick = onEnterFullscreen),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.fullscreen),
                        contentDescription = "Enter Fullscreen",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedVisualToggle(
    activeMode: VisualMode,
    onModeChange: (VisualMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetIndex = if (activeMode == VisualMode.MUSIC_VIDEO) 0f else 1f
    val animatedIndex by animateFloatAsState(
        targetValue = targetIndex,
        animationSpec = spring(
            dampingRatio = 0.72f,
            stiffness = 320f,
        ),
        label = "SegmentedVisualTogglePill",
    )
    val lag = abs(targetIndex - animatedIndex)
    val stretchScaleX = 1f + lag * 0.14f
    val stretchScaleY = 1f - lag * 0.07f

    val shape = RoundedCornerShape(20.dp)
    val tabWidth = 76.dp
    val tabHeight = 32.dp

    BoxWithConstraints(
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.55f))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), shape)
            .padding(3.dp),
    ) {
        val indicatorOffset = tabWidth * animatedIndex

        // Sliding Capsule Indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .size(width = tabWidth, height = tabHeight)
                .graphicsLayer {
                    scaleX = stretchScaleX
                    scaleY = stretchScaleY
                }
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .border(
                    width = 0.5.dp,
                    color = Color.White.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(16.dp),
                )
        )

        // Labels
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isVideo = activeMode == VisualMode.MUSIC_VIDEO
            val isCanvas = activeMode == VisualMode.CANVAS

            Box(
                modifier = Modifier
                    .size(width = tabWidth, height = tabHeight)
                    .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onModeChange(VisualMode.MUSIC_VIDEO)
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Video",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isVideo) FontWeight.Bold else FontWeight.Medium,
                        color = if (isVideo) Color.White else Color.White.copy(alpha = 0.65f),
                    ),
                )
            }

            Box(
                modifier = Modifier
                    .size(width = tabWidth, height = tabHeight)
                    .clip(RoundedCornerShape(16.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    onModeChange(VisualMode.CANVAS)
                },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Canvas",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = if (isCanvas) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCanvas) Color.White else Color.White.copy(alpha = 0.65f),
                    ),
                )
            }
        }
    }
}

