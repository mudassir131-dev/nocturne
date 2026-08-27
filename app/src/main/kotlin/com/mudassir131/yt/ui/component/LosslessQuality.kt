/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.playback.alac.AudioFormatInfo
import com.mudassir131.yt.playback.alac.toAudioFormatInfo

enum class LosslessQuality(val label: String) {
    LOSSLESS("Lossless"),
    HI_RES_LOSSLESS("Hi-Res Lossless"),
}

/**
 * Accurately determines if the resolved playback stream is genuine Lossless or Hi-Res Lossless.
 * Returns null for lossy formats (Opus, AAC, MP3, Vorbis, etc.).
 */
fun FormatEntity.losslessQuality(): LosslessQuality? {
    val info = toAudioFormatInfo()
    return if (info.isLossless) {
        if (info.isHiRes) LosslessQuality.HI_RES_LOSSLESS else LosslessQuality.LOSSLESS
    } else {
        null
    }
}

fun AudioFormatInfo.losslessQuality(): LosslessQuality? {
    return if (isLossless) {
        if (isHiRes) LosslessQuality.HI_RES_LOSSLESS else LosslessQuality.LOSSLESS
    } else {
        null
    }
}

/**
 * Original minimalist soundwave / audio resolution badge icon inspired by high-fidelity audio aesthetics.
 */
@Composable
fun LosslessIcon(
    color: Color,
    isHiRes: Boolean = false,
    modifier: Modifier = Modifier,
    size: Dp = 11.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        drawLosslessWaveform(color, isHiRes)
    }
}

private fun DrawScope.drawLosslessWaveform(color: Color, isHiRes: Boolean) {
    val w = size.width
    val h = size.height
    val barCount = if (isHiRes) 4 else 3
    val spacing = w * 0.14f
    val totalSpacing = spacing * (barCount - 1)
    val barWidth = (w - totalSpacing) / barCount

    // Symmetrical high-fidelity acoustic wave heights
    val heights = if (isHiRes) {
        floatArrayOf(0.45f, 1.0f, 0.75f, 0.45f)
    } else {
        floatArrayOf(0.55f, 1.0f, 0.55f)
    }

    for (i in 0 until barCount) {
        val barH = h * heights[i]
        val x = i * (barWidth + spacing)
        val y = (h - barH) / 2f
        drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(barWidth, barH),
            cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
        )
    }
}
