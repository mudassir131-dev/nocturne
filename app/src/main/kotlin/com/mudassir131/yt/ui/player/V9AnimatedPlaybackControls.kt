/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 *
 * Adapted for Nocturne by Mudassir
 */

package com.mudassir131.yt.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.mudassir131.yt.R

private enum class V9PlaybackButtonType { NONE, PREVIOUS, PLAY_PAUSE, NEXT }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun V9AnimatedPlaybackControls(
    isPlayingProvider: () -> Boolean,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    baseWeight: Float = 1f,
    expansionWeight: Float = 1.15f,
    compressionWeight: Float = 0.65f,
    pressAnimationSpec: AnimationSpec<Float>,
    releaseDelay: Long = 220L,
    playPauseCornerPlaying: Dp = 50.dp,
    playPauseCornerPaused: Dp = 24.dp,
    colorOtherButtons: Color = MaterialTheme.colorScheme.secondaryContainer,
    colorPlayPause: Color = MaterialTheme.colorScheme.primary,
    tintPlayPauseIcon: Color = MaterialTheme.colorScheme.onPrimary,
    tintOtherIcons: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    colorPreviousButton: Color = colorOtherButtons,
    colorNextButton: Color = colorOtherButtons,
    tintPreviousIcon: Color = tintOtherIcons,
    tintNextIcon: Color = tintOtherIcons,
    playPauseIconSize: Dp = 38.dp,
    iconSize: Dp = 30.dp,
) {
    val isPlaying = isPlayingProvider()
    var lastClicked by remember { mutableStateOf<V9PlaybackButtonType?>(null) }
    var clickTrigger by remember { mutableStateOf(0) }
    val latestIsPlayingProvider by rememberUpdatedState(newValue = isPlayingProvider)
    val latestLastClicked by rememberUpdatedState(newValue = lastClicked)
    val isPlayPauseLocked =
        lastClicked == V9PlaybackButtonType.NEXT || lastClicked == V9PlaybackButtonType.PREVIOUS
    var playPauseVisualState by remember { mutableStateOf(isPlaying) }
    var pendingPlayPauseState by remember { mutableStateOf<Boolean?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    val motionScheme = remember { MotionScheme.expressive() }
    val defaultSpatialDpSpec = remember { motionScheme.defaultSpatialSpec<Dp>() }

    LaunchedEffect(lastClicked, clickTrigger) {
        if (lastClicked != null) {
            val delayTime = when (lastClicked) {
                V9PlaybackButtonType.NEXT, V9PlaybackButtonType.PREVIOUS -> 600L
                else -> releaseDelay
            }
            delay(delayTime)
            lastClicked = null
        }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            pendingPlayPauseState = true
            return@LaunchedEffect
        }

        val shouldDelay = latestLastClicked != V9PlaybackButtonType.PLAY_PAUSE
        if (shouldDelay) {
            delay(releaseDelay)
        }
        if (!latestIsPlayingProvider()) {
            pendingPlayPauseState = false
        }
    }

    LaunchedEffect(isPlayPauseLocked, pendingPlayPauseState) {
        if (!isPlayPauseLocked) {
            pendingPlayPauseState?.let {
                playPauseVisualState = it
                pendingPlayPauseState = null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            fun weightFor(button: V9PlaybackButtonType): Float = when (lastClicked) {
                button -> expansionWeight
                null -> baseWeight
                else -> compressionWeight
            }

            val prevWeight by animateFloatAsState(
                targetValue = weightFor(V9PlaybackButtonType.PREVIOUS),
                animationSpec = pressAnimationSpec,
                label = "prevWeight"
            )
            Box(
                modifier = Modifier
                    .weight(prevWeight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colorPreviousButton)
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastClicked = V9PlaybackButtonType.PREVIOUS
                        clickTrigger++
                        onPrevious()
                    },
                contentAlignment = Alignment.Center
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (lastClicked == V9PlaybackButtonType.PREVIOUS) 1.15f else 1.0f,
                    animationSpec = pressAnimationSpec,
                    label = "prevScale"
                )
                Icon(
                    painter = painterResource(R.drawable.apple_skip_previous),
                    contentDescription = "Previous",
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    tint = tintPreviousIcon
                )
            }

            val playPauseWeight by animateFloatAsState(
                targetValue = weightFor(V9PlaybackButtonType.PLAY_PAUSE) * 1.35f,
                animationSpec = pressAnimationSpec,
                label = "playPauseWeight"
            )
            val currentPlayPauseCorner by animateDpAsState(
                targetValue = if (playPauseVisualState) playPauseCornerPlaying else playPauseCornerPaused,
                animationSpec = defaultSpatialDpSpec,
                label = "playPauseCorner"
            )

            Box(
                modifier = Modifier
                    .weight(playPauseWeight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(currentPlayPauseCorner))
                    .background(colorPlayPause)
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastClicked = V9PlaybackButtonType.PLAY_PAUSE
                        clickTrigger++
                        playPauseVisualState = !playPauseVisualState
                        onPlayPause()
                    },
                contentAlignment = Alignment.Center
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (lastClicked == V9PlaybackButtonType.PLAY_PAUSE) 1.12f else 1.0f,
                    animationSpec = pressAnimationSpec,
                    label = "playPauseScale"
                )
                Crossfade(
                    targetState = playPauseVisualState,
                    animationSpec = tween(220),
                    label = "playPauseIcon"
                ) { playing ->
                    Icon(
                        painter = painterResource(if (playing) R.drawable.pause_applemusic else R.drawable.play_applemusic),
                        contentDescription = if (playing) "Pause" else "Play",
                        modifier = Modifier
                            .size(playPauseIconSize)
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        tint = tintPlayPauseIcon
                    )
                }
            }

            val nextWeight by animateFloatAsState(
                targetValue = weightFor(V9PlaybackButtonType.NEXT),
                animationSpec = pressAnimationSpec,
                label = "nextWeight"
            )
            Box(
                modifier = Modifier
                    .weight(nextWeight)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(colorNextButton)
                    .clickable {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        lastClicked = V9PlaybackButtonType.NEXT
                        clickTrigger++
                        onNext()
                    },
                contentAlignment = Alignment.Center
            ) {
                val scale by animateFloatAsState(
                    targetValue = if (lastClicked == V9PlaybackButtonType.NEXT) 1.15f else 1.0f,
                    animationSpec = pressAnimationSpec,
                    label = "nextScale"
                )
                Icon(
                    painter = painterResource(R.drawable.apple_skip_next),
                    contentDescription = "Next",
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer(scaleX = scale, scaleY = scale),
                    tint = tintNextIcon
                )
            }
        }
    }
}
