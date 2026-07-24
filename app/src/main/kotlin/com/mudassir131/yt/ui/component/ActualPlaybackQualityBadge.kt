package com.mudassir131.yt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudassir131.yt.db.entities.FormatEntity

/** Labels the resolved stream, never merely the selected preference. */
fun FormatEntity.actualPlaybackQualityLabel(): String {
    val source = playbackUrl.orEmpty()
    val codec = codecs.lowercase()
    return when {
        source.contains("saavn", ignoreCase = true) -> "SAAVN"
        codec.contains("opus") || mimeType.contains("opus", ignoreCase = true) -> "OPUS"
        codec.contains("mp4a") -> "AAC"
        codec.isNotBlank() -> codecs.substringBefore('.').uppercase()
        else -> mimeType.substringAfter('/', "AUDIO").uppercase()
    }
}

@Composable
fun ActualPlaybackQualityBadge(
    format: FormatEntity?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val label = remember(format) { format?.actualPlaybackQualityLabel().orEmpty() }
    if (label.isBlank()) return
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.08f))
            .border(0.5.dp, color.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
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
