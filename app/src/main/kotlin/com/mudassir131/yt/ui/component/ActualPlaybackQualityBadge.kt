/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.playback.alac.AudioFormatInfo
import com.mudassir131.yt.playback.alac.toAudioFormatInfo

/** Labels the resolved stream, never merely the selected preference. */
fun FormatEntity.actualPlaybackQualityLabel(): String {
    return toAudioFormatInfo().qualityLabel
}

@Composable
fun ActualPlaybackQualityBadge(
    format: FormatEntity?,
    color: Color,
    modifier: Modifier = Modifier,
    formatInfo: AudioFormatInfo? = null,
) {
    val resolvedInfo = remember(format, formatInfo) {
        formatInfo ?: format?.toAudioFormatInfo()
    }
    val label = remember(resolvedInfo) { resolvedInfo?.qualityLabel.orEmpty() }
    val lossless = remember(resolvedInfo) { resolvedInfo?.losslessQuality() }
    if (label.isBlank()) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (lossless != null) {
                LosslessIcon(
                    color = color.copy(alpha = 0.82f),
                    isHiRes = lossless == LosslessQuality.HI_RES_LOSSLESS || lossless == LosslessQuality.BIT_PERFECT,
                    size = 10.dp,
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = color.copy(alpha = 0.82f),
                maxLines = 1,
            )
        }
    }
}
