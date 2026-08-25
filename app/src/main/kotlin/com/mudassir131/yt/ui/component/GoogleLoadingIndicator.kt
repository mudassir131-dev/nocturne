/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Android 16 / Google Pixel Material 3 Expressive Loading Indicator.
 *
 * Implements the Android 16 / Pixel continuous morphing scalloped starburst / flower
 * shape with fluid rotation and breathing elasticity as seen in Android 16 System UI & Google apps.
 */
@Composable
fun PixelAndroid16Loader(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color? = null,
    numLobes: Int = 10,
) {
    val accentColor = color ?: MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "pixel_android16_loader")

    // Smooth continuous rotation
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Breathing pulse scale (bouncy elastic feel)
    val scalePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1800
                0.88f at 0 using FastOutSlowInEasing
                1.06f at 900 using FastOutSlowInEasing
                0.88f at 1800 using FastOutSlowInEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )

    // Morphing scallop amplitude (depth of petals)
    val scallopDepth by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0.12f at 0 using CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                0.22f at 700 using CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
                0.12f at 1400 using CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "scallop_depth"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w / 2f
            val centerY = h / 2f
            val maxR = (w.coerceAtMost(h) / 2f) * 0.92f
            val baseRadius = maxR * (1f - scallopDepth)
            val amplitude = maxR * scallopDepth

            rotate(degrees = rotation, pivot = Offset(centerX, centerY)) {
                scale(scale = scalePulse, pivot = Offset(centerX, centerY)) {
                    val path = Path()
                    val numPoints = 180
                    for (i in 0 until numPoints) {
                        val angle = (i.toFloat() / numPoints) * 2f * PI.toFloat()
                        // 10-lobed smooth sinusoidal scallop curve
                        val r = baseRadius + amplitude * cos(numLobes * angle)
                        val x = centerX + r * cos(angle)
                        val y = centerY + r * sin(angle)

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            path.lineTo(x, y)
                        }
                    }
                    path.close()

                    // Solid Material 3 expressive filled shape
                    drawPath(
                        path = path,
                        color = accentColor
                    )
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for GoogleLoadingIndicator
 */
@Composable
fun GoogleLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color? = null,
    strokeWidth: Dp? = null,
    isMultiColor: Boolean = false,
    singleColor: Color? = null,
) {
    PixelAndroid16Loader(
        modifier = modifier,
        size = size,
        color = singleColor ?: color,
        numLobes = 10
    )
}

/**
 * Pixel / Android 16 Pulsing wave dots
 */
@Composable
fun GoogleExpressiveDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    spacing: Dp = 6.dp,
    color: Color? = null,
) {
    val accentColor = color ?: MaterialTheme.colorScheme.primary
    val infiniteTransition = rememberInfiniteTransition(label = "expressive_dots")

    val animations = (0 until 4).map { index ->
        val delay = index * 160
        infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.25f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1300
                    0.5f at 0
                    0.5f at delay
                    1.25f at (delay + 300).coerceAtMost(1150) using FastOutSlowInEasing
                    0.5f at (delay + 600).coerceAtMost(1300) using FastOutSlowInEasing
                    0.5f at 1300
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "dot_scale_$index"
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0 until 4).forEach { index ->
            val scale by animations[index]
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
            ) {
                Canvas(modifier = Modifier.size(dotSize)) {
                    drawCircle(color = accentColor.copy(alpha = (scale * 0.8f).coerceIn(0.4f, 1f)))
                }
            }
        }
    }
}
