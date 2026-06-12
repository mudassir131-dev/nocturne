/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Velune custom loading animation - animated V that pulses and rotates
 */
@Composable
fun VeluneLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color? = null,
) {
    val accentColor = color ?: MaterialTheme.colorScheme.primary

    val infiniteTransition = rememberInfiniteTransition(label = "velune_loader")

    // Pulsing scale animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Rotation animation
    val rotation by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    // Alpha pulse
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val strokeWidth = w * 0.08f
            val scaleX = w / 100f
            val scaleY = h / 100f

            rotate(rotation, pivot = Offset(w / 2f, h / 2f)) {
                scale(scale, pivot = Offset(w / 2f, h / 2f)) {
                    val path = Path().apply {
                        // Left curve
                        moveTo(32f * scaleX, 62f * scaleY)
                        lineTo(32f * scaleX, 44f * scaleY)
                        arcTo(
                            rect = Rect(
                                left = 32f * scaleX,
                                top = 32f * scaleY,
                                right = 56f * scaleX,
                                bottom = 56f * scaleY
                            ),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        lineTo(56f * scaleX, 50f * scaleY)

                        // Right curve
                        moveTo(68f * scaleX, 38f * scaleY)
                        lineTo(68f * scaleX, 56f * scaleY)
                        arcTo(
                            rect = Rect(
                                left = 44f * scaleX,
                                top = 44f * scaleY,
                                right = 68f * scaleX,
                                bottom = 68f * scaleY
                            ),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        lineTo(44f * scaleX, 50f * scaleY)
                    }

                    drawPath(
                        path = path,
                        color = accentColor.copy(alpha = alpha),
                        style = Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
