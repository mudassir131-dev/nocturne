/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Apple Music inspired 3-dot instrumental/solo animation.
 *
 * Appears during instrumental preludes, guitar solos, or beat breaks where
 * vocals are silent. The 3 dots animate in a wave-pulse rhythm, with an
 * anticipatory countdown when approaching the next vocal line.
 */
@Composable
fun AppleInstrumentalDots(
    isActive: Boolean,
    timeRemainingMs: Long? = null,
    baseColor: Color = Color.White,
    dotSize: Dp = 9.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotsWave")

    // Wave pulse progress for continuous breathing
    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotsWaveProgress"
    )

    // Check if we are in the 3.5-second countdown to the next vocal line
    val isCountdown = isActive && timeRemainingMs != null && timeRemainingMs in 0L..3500L

    Row(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = when (horizontalAlignment) {
            Alignment.Start -> Arrangement.spacedBy(10.dp, Alignment.Start)
            Alignment.CenterHorizontally -> Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            Alignment.End -> Arrangement.spacedBy(10.dp, Alignment.End)
            else -> Arrangement.spacedBy(10.dp)
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (dotIndex in 0..2) {
            val (dotScale, dotAlpha) = if (!isActive) {
                // Inactive state (dimmed and static)
                0.85f to 0.28f
            } else if (isCountdown && timeRemainingMs != null) {
                // Countdown mode: light up sequentially (Dot 0 at 3.5s, Dot 1 at 2.3s, Dot 2 at 1.1s)
                val dotActivationThreshold = when (dotIndex) {
                    0 -> 3500L
                    1 -> 2300L
                    else -> 1100L
                }
                if (timeRemainingMs <= dotActivationThreshold) {
                    1.28f to 1.0f
                } else {
                    0.85f to 0.35f
                }
            } else {
                // Wave pulse mode: phase offset per dot
                val phaseOffset = dotIndex * 0.22f
                val dotPhase = (waveProgress - phaseOffset + 1f) % 1f

                // Smooth bell-curve pulse
                val pulse = when {
                    dotPhase < 0.4f -> FastOutSlowInEasing.transform(dotPhase / 0.4f)
                    dotPhase < 0.8f -> 1f - FastOutSlowInEasing.transform((dotPhase - 0.4f) / 0.4f)
                    else -> 0f
                }
                val scale = 0.88f + (0.35f * pulse)
                val alpha = 0.35f + (0.65f * pulse)
                scale to alpha
            }

            Box(
                modifier = Modifier.size(dotSize * 1.6f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(dotSize * dotScale)) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    // Subtle radial bloom when dot is at peak brightness
                    if (isActive && dotAlpha > 0.75f) {
                        drawCircle(
                            color = baseColor.copy(alpha = 0.22f * dotAlpha),
                            radius = radius * 1.55f,
                            center = center
                        )
                    }

                    // Main dot circle
                    drawCircle(
                        color = baseColor.copy(alpha = dotAlpha),
                        radius = radius,
                        center = center
                    )
                }
            }
        }
    }
}
