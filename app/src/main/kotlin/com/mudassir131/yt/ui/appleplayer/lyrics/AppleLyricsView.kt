package com.mudassir131.yt.ui.appleplayer.lyrics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.LocalPlayerConnection
import com.mudassir131.yt.ui.component.VeluneLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

private const val ManualFollowDelayMs = 3_500L
private const val LargeSeekDistance = 8
private val AppleLyricsEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

@Composable
fun AppleLyricsView(
    result: AppleLyricsResult?,
    positionMs: Long,
    loading: Boolean,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit,
) {
    if (loading || result == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) {
                VeluneLoader(size = 52.dp, color = Color.White)
            } else {
                Text("Lyrics unavailable", color = Color.White.copy(alpha = .7f))
            }
        }
        return
    }

    val lines = result.lines
    val playerConnection = LocalPlayerConnection.current
    val latestExternalPosition by rememberUpdatedState(positionMs)
    var fluidPositionMs by remember(result.raw) { mutableLongStateOf(positionMs) }

    // Keep lyric animation frame-accurate without recomposing the whole Apple player per frame.
    // A large difference indicates that the user is actively scrubbing.
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
            delay(16L)
        }
    }

    val renderedPositionMs = if (result.isLineSynced) fluidPositionMs else positionMs
    val activeIndex = remember(lines, renderedPositionMs) {
        lines.activeLineIndex(renderedPositionMs)
    }
    val listState = rememberLazyListState()
    val isUserDragging by listState.interactionSource.collectIsDraggedAsState()
    val focusOffsetPx = with(LocalDensity.current) { 120.dp.roundToPx() }
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
    LaunchedEffect(activeIndex, autoFollow, result.isLineSynced) {
        if (!result.isLineSynced || !autoFollow || activeIndex !in lines.indices) return@LaunchedEffect
        val distance = if (lastFollowedIndex < 0) Int.MAX_VALUE else abs(activeIndex - lastFollowedIndex)
        if (distance > LargeSeekDistance) {
            // A large seek should not animate through every intervening lyric.
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
                items = lines,
                key = { index, line -> "${index}_${line.startMs ?: 0L}_${line.text.hashCode()}" },
                contentType = { _, line -> if (line.words.isEmpty()) "line" else "word_line" },
            ) { index, line ->
                val active = index == activeIndex
                AppleLyricLine(
                    line = line,
                    active = active,
                    activePositionMs = renderedPositionMs.takeIf { active },
                    onClick = line.startMs?.let { start ->
                        {
                            autoFollow = true
                            lastFollowedIndex = -1
                            onSeek(start)
                        }
                    },
                )
            }
        }

        if (!autoFollow && result.isLineSynced) {
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

@Composable
private fun AppleLyricLine(
    line: AppleLyricsLine,
    active: Boolean,
    activePositionMs: Long?,
    onClick: (() -> Unit)?,
) {
    val color by animateColorAsState(
        targetValue = if (active) Color.White else Color.White.copy(alpha = .38f),
        animationSpec = tween(260, easing = AppleLyricsEasing),
        label = "lyric-emphasis",
    )
    val emphasis by animateFloatAsState(
        targetValue = if (active) 1f else .985f,
        animationSpec = tween(280, easing = AppleLyricsEasing),
        label = "lyric-scale",
    )

    Text(
        text = remember(line, activePositionMs) {
            buildAnnotatedString {
                if (line.words.isEmpty() || activePositionMs == null) {
                    append(line.text)
                } else {
                    line.words.forEach { word ->
                        val progress = when {
                            activePositionMs < word.startMs -> 0f
                            word.endMs == null || word.endMs <= word.startMs -> 1f
                            activePositionMs >= word.endMs -> 1f
                            else -> (activePositionMs - word.startMs).toFloat() / (word.endMs - word.startMs)
                        }
                        pushStyle(
                            SpanStyle(
                                color = Color.White.copy(alpha = .32f + .68f * progress),
                                fontWeight = if (progress > 0f) FontWeight.Bold else FontWeight.SemiBold,
                            ),
                        )
                        append(word.text)
                        pop()
                    }
                }
            }
        },
        color = color,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
        modifier = Modifier
            .padding(vertical = 12.dp)
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base }
            .graphicsLayer {
                scaleX = emphasis
                scaleY = emphasis
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, .5f)
            }
    )
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
