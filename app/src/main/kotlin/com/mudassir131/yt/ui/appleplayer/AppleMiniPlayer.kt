/*
 * Nocturne - by Mudassir
 * Adapted from Echo Music's FloatingMiniPlayer at
 * 6bea1dbf25c5761707ac0d5c9f3ce32d589be7aa (GPL-3.0).
 */
package com.mudassir131.yt.ui.appleplayer

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.SwipeSensitivityKey
import com.mudassir131.yt.constants.SwipeThumbnailKey
import com.mudassir131.yt.models.MediaMetadata
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt

/** Echo's expanded floating-tab accessory, adapted only at the playback boundary. */
@Composable
fun AppleMiniPlayer(
    metadata: MediaMetadata?,
    isPlaying: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    pureBlack: Boolean,
    onExpand: () -> Unit,
    onTogglePlayback: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeEnabled by rememberPreference(SwipeThumbnailKey, true)
    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    var dragStartedAt by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val swipeAnimation = remember {
        spring<Float>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow,
        )
    }
    val autoSwipeThreshold = remember(swipeSensitivity) {
        (600 / (1f + exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "appleMiniPlayerPressScale",
    )

    val shape = RoundedCornerShape(100)
    val backgroundColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp)
            .then(
                if (swipeEnabled) {
                    Modifier.pointerInput(canSkipPrevious, canSkipNext, swipeSensitivity) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragStartedAt = System.currentTimeMillis()
                                totalDragDistance = 0f
                            },
                            onDragCancel = {
                                coroutineScope.launch { offsetX.animateTo(0f, swipeAnimation) }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val adjustedDrag = if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                val swipingRight = adjustedDrag > 0
                                val swipingLeft = adjustedDrag < 0
                                val mayMoveLeft = swipingLeft && canSkipNext
                                val mayMoveRight = swipingRight && canSkipPrevious
                                val mayReturnToCenter =
                                    (swipingRight && !canSkipPrevious && offsetX.value < 0) ||
                                        (swipingLeft && !canSkipNext && offsetX.value > 0)

                                if (mayMoveLeft || mayMoveRight || mayReturnToCenter) {
                                    totalDragDistance += abs(adjustedDrag)
                                    coroutineScope.launch { offsetX.snapTo(offsetX.value + adjustedDrag) }
                                }
                            },
                            onDragEnd = {
                                val dragDuration = System.currentTimeMillis() - dragStartedAt
                                val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                val currentOffset = offsetX.value
                                val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f
                                val shouldChangeSong =
                                    (abs(currentOffset) > 50f && velocity > velocityThreshold) ||
                                        abs(currentOffset) > autoSwipeThreshold

                                if (shouldChangeSong) {
                                    if (currentOffset > 0 && canSkipPrevious) onPrevious()
                                    else if (currentOffset <= 0 && canSkipNext) onNext()
                                }
                                coroutineScope.launch { offsetX.animateTo(0f, swipeAnimation) }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .shadow(12.dp, shape, clip = false)
                .background(backgroundColor, shape)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onExpand,
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            AsyncImage(
                model = metadata?.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = metadata?.title.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metadata?.artists?.joinToString { it.name }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            IconButton(
                onClick = onTogglePlayback,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.skip_next),
                    contentDescription = "Next",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
