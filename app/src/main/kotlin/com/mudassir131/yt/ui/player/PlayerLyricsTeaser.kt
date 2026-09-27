/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudassir131.yt.R
import com.mudassir131.yt.db.entities.LyricsEntity
import com.mudassir131.yt.lyrics.LyricsUtils
import com.mudassir131.yt.models.MediaMetadata
import com.mudassir131.yt.ui.appleplayer.lyrics.AppleLyricsResult

private val AppleSlideEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

@Immutable
data class TeaserWord(
    val text: String,
    val startMs: Long,
    val endMs: Long?,
)

@Immutable
data class TeaserLine(
    val startMs: Long,
    val endMs: Long?,
    val text: String,
    val words: List<TeaserWord> = emptyList(),
)

/**
 * Apple Music inspired synced lyrics slider teaser with buttery smooth vertical slide transitions.
 * Accurately matches the video animation:
 * - When lines change, old line slides UP out while new line slides UP in from below.
 * - The chevron '>' is bound to the line row, preventing horizontal snapping/jitter.
 * - During loading/solo breaks, displays "♫ Feel that build ›" with an animated gentle pulse.
 * - Tapping anywhere immediately expands the full lyrics section.
 */
@Composable
fun PlayerLyricsTeaser(
    mediaMetadata: MediaMetadata?,
    position: Long,
    duration: Long,
    lyricsEntity: LyricsEntity? = null,
    appleLyricsResult: AppleLyricsResult? = null,
    textBackgroundColor: Color = Color.White,
    onShowLyrics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 1. Parse / normalize lines from either AppleLyricsResult or LyricsEntity
    val teaserLines: List<TeaserLine> = remember(appleLyricsResult?.raw, lyricsEntity?.lyrics, mediaMetadata?.id) {
        val appleLines = appleLyricsResult?.lines
        if (appleLines != null && appleLines.isNotEmpty()) {
            appleLines.mapNotNull { line ->
                val start = line.startMs ?: return@mapNotNull null
                TeaserLine(
                    startMs = start,
                    endMs = line.endMs,
                    text = line.text,
                    words = line.words.map { w ->
                        TeaserWord(w.text, w.startMs, w.endMs)
                    },
                )
            }
        } else {
            val raw = lyricsEntity?.lyrics?.trim()
            if (raw.isNullOrBlank() || raw == LyricsEntity.LYRICS_NOT_FOUND) {
                emptyList()
            } else if (raw.startsWith("[")) {
                LyricsUtils.parseLyrics(raw).mapIndexed { idx, entry ->
                    TeaserLine(
                        startMs = entry.time,
                        endMs = null,
                        text = entry.text,
                        words = entry.words?.map { w ->
                            TeaserWord(
                                text = w.text,
                                startMs = (w.startTime * 1000).toLong(),
                                endMs = (w.endTime * 1000).toLong(),
                            )
                        } ?: emptyList(),
                    )
                }
            } else if (LyricsUtils.isTtml(raw)) {
                LyricsUtils.parseTtml(raw, mediaMetadata?.duration ?: (duration / 1000).toInt()).map { entry ->
                    TeaserLine(
                        startMs = entry.time,
                        endMs = null,
                        text = entry.text,
                        words = entry.words?.map { w ->
                            TeaserWord(
                                text = w.text,
                                startMs = (w.startTime * 1000).toLong(),
                                endMs = (w.endTime * 1000).toLong(),
                            )
                        } ?: emptyList(),
                    )
                }
            } else {
                raw.lines().filter(String::isNotBlank).mapIndexed { index, line ->
                    TeaserLine(index * 5000L, null, line)
                }
            }
        }
    }

    val firstLineStart = teaserLines.firstOrNull()?.startMs ?: 0L

    // 2. Active line index via derived state to prevent frame-by-frame recomposition spam
    val activeLineIndex by remember(teaserLines, position) {
        derivedStateOf {
            if (teaserLines.isEmpty()) -1
            else findTeaserLineIndex(teaserLines, position)
        }
    }

    // 3. Check for instrumental prelude or interlude
    val isInstrumental by remember(activeLineIndex, position, teaserLines) {
        derivedStateOf {
            if (teaserLines.isEmpty() || activeLineIndex < 0) return@derivedStateOf true
            // Intro: at least 3.0s before first line
            if (firstLineStart >= 3000L && position < (firstLineStart - 600L)) return@derivedStateOf true

            // Interlude: check if there's a gap >= 7.0s between current and next line
            if (activeLineIndex in 0 until teaserLines.lastIndex) {
                val curr = teaserLines[activeLineIndex]
                val next = teaserLines[activeLineIndex + 1]
                val gap = next.startMs - curr.startMs
                if (gap >= 7000L && position >= (curr.startMs + 3800L) && position < (next.startMs - 500L)) {
                    return@derivedStateOf true
                }
            }
            val currText = teaserLines.getOrNull(activeLineIndex)?.text.orEmpty().trim()
            currText.isBlank() || isInstrumentalTag(currText)
        }
    }

    // 4. Gentle pulse for instrumental vibe state
    val infiniteTransition = rememberInfiniteTransition(label = "teaserPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "teaserPulseAlpha",
    )

    // The targetKey ONLY changes when the active line changes or switches to instrumental!
    val activeLineKey = if (isInstrumental) -1 else activeLineIndex

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = textBackgroundColor),
            ) { onShowLyrics() }
            .padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        AnimatedContent(
            targetState = activeLineKey,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInVertically(animationSpec = tween(260, easing = AppleSlideEasing)) { it } +
                        fadeIn(animationSpec = tween(220, easing = LinearEasing)))
                        .togetherWith(
                            slideOutVertically(animationSpec = tween(240, easing = AppleSlideEasing)) { -it } +
                                fadeOut(animationSpec = tween(180, easing = LinearEasing)),
                        )
                } else {
                    (slideInVertically(animationSpec = tween(260, easing = AppleSlideEasing)) { -it } +
                        fadeIn(animationSpec = tween(220, easing = LinearEasing)))
                        .togetherWith(
                            slideOutVertically(animationSpec = tween(240, easing = AppleSlideEasing)) { it } +
                                fadeOut(animationSpec = tween(180, easing = LinearEasing)),
                        )
                }
            },
            label = "teaserLineAnimatedContent",
            modifier = Modifier.fillMaxWidth(),
        ) { targetIndex ->
            if (targetIndex < 0 || targetIndex !in teaserLines.indices) {
                // Instrumental / Searching State: "♫ Feel that build >"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = textBackgroundColor.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Feel that build",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 0.2.sp,
                        ),
                        color = textBackgroundColor.copy(alpha = pulseAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.navigate_next),
                        contentDescription = "Open Lyrics",
                        tint = textBackgroundColor.copy(alpha = 0.70f * pulseAlpha),
                        modifier = Modifier.size(16.dp),
                    )
                }
            } else {
                val currentLine = teaserLines[targetIndex]
                ActiveTeaserLineText(
                    line = currentLine,
                    position = position,
                    textColor = textBackgroundColor,
                )
            }
        }
    }
}

/**
 * Isolated text renderer with attached trailing chevron '>' so the whole line
 * animates with ultra-smooth karaoke fill and soft gradient mask effects.
 * Masks ONLY the text bounds for perfect progress scaling with zero double text.
 */
@Composable
private fun ActiveTeaserLineText(
    line: TeaserLine,
    position: Long,
    textColor: Color,
) {
    val progress by remember(line, position) {
        derivedStateOf {
            calculateTeaserKaraokeProgress(line, position)
        }
    }

    val teaserTextStyle = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.2.sp,
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.weight(1f, fill = false),
            contentAlignment = Alignment.CenterStart,
        ) {
            // 1. Inactive Base Layer (dim text, determines the exact layout bounds)
            Text(
                text = line.text,
                style = teaserTextStyle,
                color = textColor.copy(alpha = 0.38f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            // 2. Active Masked Layer (matches parent text bounds pixel-for-pixel)
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            if (progress < 1f) {
                                val totalWidth = size.width
                                val fillWidth = totalWidth * progress
                                val featherPx = 24f
                                val solidFraction = ((fillWidth - featherPx * 0.4f) / totalWidth).coerceIn(0f, 1f)
                                val endFraction = ((fillWidth + featherPx * 0.6f) / totalWidth).coerceIn(0f, 1f)

                                val maskBrush = Brush.horizontalGradient(
                                    0f to Color.Black,
                                    solidFraction to Color.Black,
                                    endFraction to Color.Transparent,
                                    1f to Color.Transparent,
                                )
                                drawRect(
                                    brush = maskBrush,
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        }
                ) {
                    Text(
                        text = line.text,
                        style = teaserTextStyle,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            painter = painterResource(R.drawable.navigate_next),
            contentDescription = "Open Lyrics",
            tint = textColor.copy(alpha = if (progress >= 1f) 1.0f else 0.38f),
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun calculateTeaserKaraokeProgress(line: TeaserLine, position: Long): Float {
    if (line.words.isNotEmpty()) {
        val firstStart = line.words.first().startMs
        val lastEnd = line.words.last().endMs ?: (line.words.last().startMs + 500L)
        if (position < firstStart) return 0f
        if (position >= lastEnd) return 1f

        val totalChars = line.text.length.coerceAtLeast(1)
        var charAcc = 0
        for (w in line.words) {
            val wStart = w.startMs
            val wEnd = w.endMs ?: (w.startMs + 400L)
            val wLen = w.text.length
            val startF = charAcc.toFloat() / totalChars
            val endF = (charAcc + wLen).toFloat() / totalChars
            charAcc += wLen + 1

            if (position in wStart..wEnd) {
                val dur = (wEnd - wStart).coerceAtLeast(1L)
                val wProg = (position - wStart).toFloat() / dur.toFloat()
                return (startF + (endF - startF) * wProg).coerceIn(0f, 1f)
            } else if (position < wStart) {
                return startF
            }
        }
        return 1f
    } else {
        val start = line.startMs
        val end = line.endMs ?: (line.startMs + 3500L)
        val dur = (end - start).coerceAtLeast(1000L)
        return ((position - start).toFloat() / dur.toFloat()).coerceIn(0f, 1f)
    }
}

private fun findTeaserLineIndex(lines: List<TeaserLine>, position: Long): Int {
    if (lines.isEmpty()) return -1
    val leadMs = 150L
    val target = position + leadMs
    var low = 0
    var high = lines.lastIndex
    var answer = -1
    while (low <= high) {
        val mid = (low + high) ushr 1
        if (lines[mid].startMs <= target) {
            answer = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return answer.coerceAtLeast(0)
}

private fun isInstrumentalTag(text: String): Boolean {
    val lower = text.lowercase().trim()
    return lower.startsWith("instrumental") ||
        lower.startsWith("[music") ||
        lower.startsWith("(music") ||
        lower.contains("solo") ||
        lower == "♫♫♫"
}
