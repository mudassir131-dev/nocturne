/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val InstagramVerifiedBlue = Color(0xFF0095F6)

/**
 * Authentic Instagram / Meta 8-point scalloped verified badge with center checkmark
 */
@Composable
fun InstagramVerifiedBadge(
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
    badgeColor: Color = InstagramVerifiedBlue,
    checkColor: Color = Color.White,
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height
            val centerX = w / 2f
            val centerY = h / 2f
            val radius = w / 2f

            // 1. Draw 8-pointed scalloped Instagram rosette badge
            val badgePath = Path()
            val numPoints = 8
            val outerRadius = radius * 0.98f
            val innerRadius = radius * 0.82f

            for (i in 0 until numPoints * 2) {
                val angle = (i * PI / numPoints) - (PI / 2)
                val r = if (i % 2 == 0) outerRadius else innerRadius
                val x = centerX + r.toFloat() * cos(angle).toFloat()
                val y = centerY + r.toFloat() * sin(angle).toFloat()

                if (i == 0) {
                    badgePath.moveTo(x, y)
                } else {
                    badgePath.lineTo(x, y)
                }
            }
            badgePath.close()

            drawPath(
                path = badgePath,
                color = badgeColor
            )

            // 2. Draw crisp white checkmark
            val checkPath = Path().apply {
                moveTo(w * 0.32f, h * 0.51f)
                lineTo(w * 0.45f, h * 0.65f)
                lineTo(w * 0.70f, h * 0.38f)
            }

            drawPath(
                path = checkPath,
                color = checkColor,
                style = Stroke(
                    width = w * 0.13f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
