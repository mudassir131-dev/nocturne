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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nocturne custom loading animation - animated Pill-Infinity logo that pulses and rotates
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
            val strokeWidth = w * 0.07f
            val waveStrokeWidth = w * 0.05f
            val scaleX = w / 100f
            val scaleY = h / 100f

            rotate(rotation, pivot = Offset(w / 2f, h / 2f)) {
                scale(scale, pivot = Offset(w / 2f, h / 2f)) {
                    
                    // 1. Draw back U-shapes (rotated +45 degrees)
                    rotate(45f, pivot = Offset(w / 2f, h / 2f)) {
                        // Top U-shape
                        val topPath = Path().apply {
                            moveTo(36f * scaleX, 40f * scaleY)
                            lineTo(36f * scaleX, 36f * scaleY)
                            arcTo(
                                rect = Rect(
                                    left = 36f * scaleX,
                                    top = 22f * scaleY,
                                    right = 64f * scaleX,
                                    bottom = 50f * scaleY
                                ),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false
                            )
                            lineTo(64f * scaleX, 40f * scaleY)
                        }
                        
                        // Bottom U-shape
                        val bottomPath = Path().apply {
                            moveTo(64f * scaleX, 60f * scaleY)
                            lineTo(64f * scaleX, 64f * scaleY)
                            arcTo(
                                rect = Rect(
                                    left = 36f * scaleX,
                                    top = 50f * scaleY,
                                    right = 64f * scaleX,
                                    bottom = 78f * scaleY
                                ),
                                startAngleDegrees = 0f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false
                            )
                            lineTo(36f * scaleX, 60f * scaleY)
                        }

                        drawPath(
                            path = topPath,
                            color = accentColor.copy(alpha = alpha),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        drawPath(
                            path = bottomPath,
                            color = accentColor.copy(alpha = alpha),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // 2. Draw front complete capsule & vertical waveform (rotated -45 degrees)
                    rotate(-45f, pivot = Offset(w / 2f, h / 2f)) {
                        // Main Capsule Outline
                        drawRoundRect(
                            color = accentColor.copy(alpha = alpha),
                            topLeft = Offset(36f * scaleX, 22f * scaleY),
                            size = Size(28f * scaleX, 56f * scaleY),
                            cornerRadius = CornerRadius(14f * scaleX, 14f * scaleY),
                            style = Stroke(
                                width = strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // 5 Vertical Soundwave Lines inside front capsule
                        val waveColor = accentColor.copy(alpha = alpha)
                        
                        // Line 1 (x = 42)
                        drawLine(
                            color = waveColor,
                            start = Offset(42f * scaleX, 47f * scaleY),
                            end = Offset(42f * scaleX, 53f * scaleY),
                            strokeWidth = waveStrokeWidth,
                            cap = StrokeCap.Round
                        )
                        // Line 2 (x = 46)
                        drawLine(
                            color = waveColor,
                            start = Offset(46f * scaleX, 44f * scaleY),
                            end = Offset(46f * scaleX, 56f * scaleY),
                            strokeWidth = waveStrokeWidth,
                            cap = StrokeCap.Round
                        )
                        // Line 3 (x = 50, Center)
                        drawLine(
                            color = waveColor,
                            start = Offset(50f * scaleX, 40f * scaleY),
                            end = Offset(50f * scaleX, 60f * scaleY),
                            strokeWidth = waveStrokeWidth,
                            cap = StrokeCap.Round
                        )
                        // Line 4 (x = 54)
                        drawLine(
                            color = waveColor,
                            start = Offset(54f * scaleX, 44f * scaleY),
                            end = Offset(54f * scaleX, 56f * scaleY),
                            strokeWidth = waveStrokeWidth,
                            cap = StrokeCap.Round
                        )
                        // Line 5 (x = 58)
                        drawLine(
                            color = waveColor,
                            start = Offset(58f * scaleX, 47f * scaleY),
                            end = Offset(58f * scaleX, 53f * scaleY),
                            strokeWidth = waveStrokeWidth,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
