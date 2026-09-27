package com.mudassir131.yt.ui.appleplayer.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudassir131.yt.LocalPlayerConnection
import com.mudassir131.yt.constants.DisableBlurKey
import com.mudassir131.yt.constants.LyricsAnimationStyle
import com.mudassir131.yt.constants.LyricsAnimationStyleKey
import com.mudassir131.yt.constants.LyricsClickKey
import com.mudassir131.yt.constants.LyricsLineSpacingKey
import com.mudassir131.yt.constants.LyricsScrollKey
import com.mudassir131.yt.constants.LyricsTextPositionKey
import com.mudassir131.yt.constants.LyricsTextSizeKey
import com.mudassir131.yt.ui.component.AppleInstrumentalDots
import com.mudassir131.yt.ui.component.NocturneLoader
import com.mudassir131.yt.ui.screens.settings.LyricsPosition
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

private const val ManualFollowDelayMs = 3_500L
private const val LargeSeekDistance = 8
private val AppleLyricsEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
private const val APPLE_INSTRUMENTAL_TAG = "♫♫♫"

private fun insertAppleInstrumentalBreaks(lines: List<AppleLyricsLine>): List<AppleLyricsLine> {
    if (lines.isEmpty()) return lines
    val firstLine = lines.firstOrNull() ?: return lines
    if (firstLine.startMs == null) return lines

    val result = mutableListOf<AppleLyricsLine>()

    // 1. Intro Instrumental break (at least 3.5s before first lyric line)
    if (firstLine.startMs >= 3500L) {
        result.add(
            AppleLyricsLine(
                text = APPLE_INSTRUMENTAL_TAG,
                startMs = 0L,
                endMs = firstLine.startMs,
                words = emptyList(),
            ),
        )
    }

    // 2. Interlude Instrumental breaks (gap between lines >= 7.0s)
    for (i in lines.indices) {
        val current = lines[i]
        result.add(current)
        if (i < lines.lastIndex) {
            val next = lines[i + 1]
            val currentEnd = current.endMs ?: (current.startMs?.plus(3500L))
            if (currentEnd != null && next.startMs != null) {
                val gap = next.startMs - currentEnd
                if (gap >= 7000L) {
                    result.add(
                        AppleLyricsLine(
                            text = APPLE_INSTRUMENTAL_TAG,
                            startMs = currentEnd,
                            endMs = next.startMs,
                            words = emptyList(),
                        ),
                    )
                }
            }
        }
    }
    return result
}

@Composable
fun AppleLyricsView(
    result: AppleLyricsResult?,
    positionMs: Long,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit,
) {
    // Dynamic preferences from Appearance Settings
    val (lyricsPosition) = rememberEnumPreference(LyricsTextPositionKey, LyricsPosition.LEFT)
    val (lyricsAnimation) = rememberEnumPreference(LyricsAnimationStyleKey, LyricsAnimationStyle.APPLE)
    val (lyricsClick) = rememberPreference(LyricsClickKey, true)
    val (lyricsScroll) = rememberPreference(LyricsScrollKey, true)
    val (lyricsTextSize) = rememberPreference(LyricsTextSizeKey, 26f)
    val (lyricsLineSpacing) = rememberPreference(LyricsLineSpacingKey, 1.3f)
    val (disableBlur) = rememberPreference(DisableBlurKey, defaultValue = true)

    if (loading || result == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) {
                NocturneLoader(size = 52.dp, color = Color.White)
            } else {
                Text("Lyrics unavailable", color = Color.White.copy(alpha = .7f))
            }
        }
        return
    }

    val displayLines = remember(result.lines) {
        if (result.isLineSynced) insertAppleInstrumentalBreaks(result.lines) else result.lines
    }
    val playerConnection = LocalPlayerConnection.current
    val latestExternalPosition by rememberUpdatedState(positionMs)
    var fluidPositionMs by remember(result.raw) { mutableLongStateOf(positionMs) }

    LaunchedEffect(result.raw, result.isLineSynced, playerConnection) {
        if (!result.isLineSynced) return@LaunchedEffect
        while (isActive) {
            val external = latestExternalPosition
            val playerPosition = playerConnection?.player?.currentPosition
            fluidPositionMs = if (playerPosition != null && abs(external - playerPosition) < 1_000L) {
                playerPosition + 150L
            } else {
                external
            }
            delay(32L) // 30fps position clock is perfectly smooth while reducing CPU overhead
        }
    }

    val renderedPositionMs = if (result.isLineSynced) fluidPositionMs else positionMs
    val activeIndex by remember(displayLines, renderedPositionMs) {
        derivedStateOf {
            displayLines.activeLineIndex(renderedPositionMs)
        }
    }
    val listState = rememberLazyListState()
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    val focusOffsetPx = with(LocalDensity.current) { 140.dp.roundToPx() }
    var autoFollow by rememberSaveable(result.raw) { mutableStateOf(true) }
    var lastFollowedIndex by remember(result.raw) { mutableStateOf(-1) }

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) {
            autoFollow = false
        } else if (!autoFollow) {
            delay(ManualFollowDelayMs)
            autoFollow = true
        }
    }

    // Playback ticks only update the active line. Scrolling is launched when the index changes.
    LaunchedEffect(activeIndex, autoFollow, result.isLineSynced, lyricsScroll) {
        if (!lyricsScroll || !result.isLineSynced || !autoFollow || activeIndex !in displayLines.indices) return@LaunchedEffect
        val distance = if (lastFollowedIndex < 0) Int.MAX_VALUE else abs(activeIndex - lastFollowedIndex)
        if (distance > LargeSeekDistance) {
            listState.scrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
        listState.animateScrollToItem(activeIndex, -focusOffsetPx)
        lastFollowedIndex = activeIndex
    }

    Box(modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 120.dp, horizontal = 18.dp),
        ) {
            itemsIndexed(
                items = displayLines,
                key = { index, line -> "${index}_${line.startMs ?: 0L}_${line.text.hashCode()}" },
                contentType = { _, line ->
                    if (line.text == APPLE_INSTRUMENTAL_TAG) "instrumental"
                    else if (line.words.isEmpty()) "line" else "word_line"
                },
            ) { index, line ->
                val active = index == activeIndex
                val distance = if (active) 0 else abs(index - activeIndex)

                if (line.text == APPLE_INSTRUMENTAL_TAG) {
                    val timeRemaining = if (line.endMs != null && active) {
                        (line.endMs - renderedPositionMs).coerceAtLeast(0L)
                    } else null

                    val dotsAlignment = when (lyricsPosition) {
                        LyricsPosition.LEFT -> Alignment.Start
                        LyricsPosition.CENTER -> Alignment.CenterHorizontally
                        LyricsPosition.RIGHT -> Alignment.End
                    }

                    AppleInstrumentalDots(
                        isActive = active,
                        timeRemainingMs = timeRemaining,
                        baseColor = Color.White,
                        horizontalAlignment = dotsAlignment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = (10f * lyricsLineSpacing).dp),
                    )
                } else {
                    AppleLyricLine(
                        line = line,
                        active = active,
                        distance = distance,
                        isUserDragging = isUserDragging,
                        disableBlur = disableBlur,
                        activePositionMs = renderedPositionMs.takeIf { active },
                        positionStyle = lyricsPosition,
                        animationStyle = lyricsAnimation,
                        textSizeSp = lyricsTextSize,
                        lineSpacing = lyricsLineSpacing,
                        clickableEnabled = lyricsClick,
                        onClick = if (lyricsClick) {
                            line.startMs?.let { start ->
                                {
                                    autoFollow = true
                                    lastFollowedIndex = -1
                                    onSeek(start)
                                }
                            }
                        } else null,
                    )
                }
            }
        }

        if (!autoFollow && result.isLineSynced && lyricsScroll) {
            FilledTonalButton(
                onClick = {
                    autoFollow = true
                    lastFollowedIndex = -1
                },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            ) {
                Text("Follow lyrics")
            }
        }
    }
}

private data class AppleKaraokeWordData(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

@Composable
private fun AppleLyricLine(
    line: AppleLyricsLine,
    active: Boolean,
    distance: Int,
    isUserDragging: Boolean,
    disableBlur: Boolean,
    activePositionMs: Long?,
    positionStyle: LyricsPosition,
    animationStyle: LyricsAnimationStyle,
    textSizeSp: Float,
    lineSpacing: Float,
    clickableEnabled: Boolean,
    onClick: (() -> Unit)?,
) {
    // 1. Color animation based on distance / active state
    val targetColor = when {
        active -> Color.White
        distance == 1 -> Color.White.copy(alpha = 0.55f)
        distance == 2 -> Color.White.copy(alpha = 0.40f)
        else -> Color.White.copy(alpha = 0.30f)
    }

    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 260, easing = AppleLyricsEasing),
        label = "lyric-color",
    )

    // 2. Scale / emphasis
    val targetScale = when {
        active -> 1.02f
        distance == 1 -> 0.98f
        distance == 2 -> 0.97f
        else -> 0.96f
    }
    val emphasis by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 260, easing = AppleLyricsEasing),
        label = "lyric-scale",
    )

    // 3. Slide translation for slide animation style
    val slideOffset by animateDpAsState(
        targetValue = if (active && animationStyle == LyricsAnimationStyle.SLIDE) 8.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "lyric-slide",
    )

    // 4. Optical Depth of field blur matching Apple Music video
    val blurRadius = when {
        active -> 0.dp
        isUserDragging -> 2.5.dp
        distance == 1 -> 4.5.dp
        distance == 2 -> 8.dp
        else -> 12.dp
    }

    // Alignment and transform origin from user settings
    val textAlign = when (positionStyle) {
        LyricsPosition.LEFT -> TextAlign.Start
        LyricsPosition.CENTER -> TextAlign.Center
        LyricsPosition.RIGHT -> TextAlign.End
    }

    val transformOrigin = when (positionStyle) {
        LyricsPosition.LEFT -> TransformOrigin(0f, .5f)
        LyricsPosition.CENTER -> TransformOrigin(.5f, .5f)
        LyricsPosition.RIGHT -> TransformOrigin(1f, .5f)
    }

    val baseModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = (8f * lineSpacing).dp)
        .padding(start = if (animationStyle == LyricsAnimationStyle.SLIDE) slideOffset else 0.dp)
        .then(if (blurRadius > 0.dp) Modifier.blur(radius = blurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded) else Modifier)
        .let { base -> if (clickableEnabled && onClick != null) base.clickable(onClick = onClick) else base }
        .graphicsLayer {
            scaleX = emphasis
            scaleY = emphasis
            this.transformOrigin = transformOrigin
        }

    if (!active || activePositionMs == null) {
        Text(
            text = line.text,
            color = color,
            textAlign = textAlign,
            style = TextStyle(
                fontSize = textSizeSp.sp,
                lineHeight = (textSizeSp * lineSpacing).sp,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = baseModifier,
        )
    } else {
        val wordsToRender = remember(line) {
            if (line.words.isNotEmpty()) {
                line.words.map { w ->
                    AppleKaraokeWordData(
                        text = w.text,
                        startMs = w.startMs ?: 0L,
                        endMs = w.endMs ?: ((w.startMs ?: 0L) + 500L),
                    )
                }
            } else {
                val rawWords = line.text.split(Regex("\\s+")).filter(String::isNotBlank)
                val totalChars = rawWords.sumOf { it.length }.coerceAtLeast(1)
                val start = line.startMs ?: 0L
                val end = line.endMs ?: (start + 3500L)
                val lineDur = (end - start).coerceAtLeast(1000L)
                var runningTime = start
                rawWords.map { w ->
                    val wDur = (lineDur * w.length / totalChars).coerceAtLeast(150L)
                    val wStart = runningTime
                    val wEnd = runningTime + wDur
                    runningTime = wEnd
                    AppleKaraokeWordData(text = w, startMs = wStart, endMs = wEnd)
                }
            }
        }

        val horizontalAlignment = when (positionStyle) {
            LyricsPosition.LEFT -> Alignment.Start
            LyricsPosition.CENTER -> Alignment.CenterHorizontally
            LyricsPosition.RIGHT -> Alignment.End
        }

        FlowRow(
            modifier = baseModifier,
            horizontalArrangement = Arrangement.spacedBy(6.dp, horizontalAlignment),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            wordsToRender.forEach { wordData ->
                AppleKaraokeWord(
                    text = wordData.text,
                    startMs = wordData.startMs,
                    endMs = wordData.endMs,
                    activePositionMs = activePositionMs,
                    fontSize = textSizeSp.sp,
                    lineHeight = (textSizeSp * lineSpacing).sp,
                    baseColor = Color.White,
                    inactiveAlpha = 0.38f,
                )
            }
        }
    }
}

@Composable
private fun AppleKaraokeWord(
    text: String,
    startMs: Long,
    endMs: Long,
    activePositionMs: Long,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    baseColor: Color,
    inactiveAlpha: Float = 0.38f,
    modifier: Modifier = Modifier,
) {
    val duration = (endMs - startMs).coerceAtLeast(1L)
    val elapsed = activePositionMs - startMs
    val progress = (elapsed.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

    when {
        progress >= 1f -> {
            // 1. Completed word: single text element in bright solid color
            Text(
                text = text,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = baseColor,
                fontWeight = FontWeight.Bold,
                modifier = modifier,
            )
        }
        progress <= 0f -> {
            // 2. Not yet sung word: single text element in dim color
            Text(
                text = text,
                fontSize = fontSize,
                lineHeight = lineHeight,
                color = baseColor.copy(alpha = inactiveAlpha),
                fontWeight = FontWeight.Bold,
                modifier = modifier,
            )
        }
        else -> {
            // 3. Actively singing word (0 < progress < 1):
            // Both layers share the EXACT SAME font metrics and style
            Box(modifier = modifier) {
                Text(
                    text = text,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    color = baseColor.copy(alpha = inactiveAlpha),
                    fontWeight = FontWeight.Bold,
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithContent {
                            drawContent()
                            val totalWidth = size.width
                            val fillWidth = totalWidth * progress
                            val featherPx = 18f
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
                ) {
                    Text(
                        text = text,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = baseColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

private fun List<AppleLyricsLine>.activeLineIndex(positionMs: Long): Int {
    if (isEmpty()) return -1
    var low = 0
    var high = lastIndex
    var answer = -1
    while (low <= high) {
        val middle = (low + high).ushr(1)
        val start = this[middle].startMs
        if (start != null && start <= positionMs) {
            answer = middle
            low = middle + 1
        } else {
            high = middle - 1
        }
    }
    return answer.coerceAtLeast(0)
}
