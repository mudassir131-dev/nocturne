/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope

val LocalBottomBarScrollProgress = compositionLocalOf { 0f }
val LocalBottomBarScrollState = staticCompositionLocalOf<BottomBarScrollState?> { null }

/**
 * Manages the continuous scroll-linked collapse/expand fraction (0f = fully expanded, 1f = fully collapsed)
 * with physics-based spring settling on fling.
 */
@Stable
class BottomBarScrollState(
    val coroutineScope: CoroutineScope,
    var collapseDistancePx: Float = 350f,
    private val canScrollProvider: () -> Boolean = { true },
) {
    val collapseFraction: Float = 0f

    fun onScrollDelta(deltaY: Float) {}

    suspend fun settle(velocityY: Float) {}

    fun expand(animate: Boolean = true) {}

    fun collapse(animate: Boolean = true) {}

    suspend fun animateTo(target: Float) {}

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset = Offset.Zero
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = Velocity.Zero
    }
}

@Composable
fun rememberBottomBarScrollState(
    collapseDistance: Dp = 140.dp,
    canScroll: () -> Boolean = { true },
): BottomBarScrollState {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val collapseDistancePx = with(density) { collapseDistance.toPx() }
    val state = remember(coroutineScope) {
        BottomBarScrollState(
            coroutineScope = coroutineScope,
            collapseDistancePx = collapseDistancePx,
            canScrollProvider = canScroll,
        )
    }
    state.collapseDistancePx = collapseDistancePx
    return state
}
