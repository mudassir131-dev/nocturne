/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    private val animatable = Animatable(0f)

    var collapseFraction: Float by mutableFloatStateOf(0f)
        private set

    private fun updateFraction(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        if (clamped != collapseFraction) {
            collapseFraction = clamped
            coroutineScope.launch {
                animatable.snapTo(clamped)
            }
        }
    }

    fun onScrollDelta(deltaY: Float) {
        if (!canScrollProvider()) return
        if (abs(deltaY) < 0.2f) return
        val distance = collapseDistancePx.coerceAtLeast(100f)
        // deltaY < 0 means user dragged finger up (scrolling down), collapsing bottom bar (fraction increases)
        // deltaY > 0 means user dragged finger down (scrolling up), expanding bottom bar (fraction decreases)
        val deltaFraction = -deltaY / distance
        updateFraction(collapseFraction + deltaFraction)
    }

    suspend fun settle(velocityY: Float) {
        if (!canScrollProvider()) return
        val target = when {
            velocityY < -500f -> 1f // Fast downward scroll -> collapse
            velocityY > 500f -> 0f  // Fast upward scroll -> expand
            collapseFraction > 0.45f -> 1f
            else -> 0f
        }
        animateTo(target)
    }

    fun expand(animate: Boolean = true) {
        if (animate) {
            coroutineScope.launch {
                animateTo(0f)
            }
        } else {
            collapseFraction = 0f
            coroutineScope.launch {
                animatable.snapTo(0f)
            }
        }
    }

    fun collapse(animate: Boolean = true) {
        if (animate) {
            coroutineScope.launch {
                animateTo(1f)
            }
        } else {
            collapseFraction = 1f
            coroutineScope.launch {
                animatable.snapTo(1f)
            }
        }
    }

    suspend fun animateTo(target: Float) {
        val targetClamped = target.coerceIn(0f, 1f)
        animatable.snapTo(collapseFraction)
        animatable.animateTo(
            targetValue = targetClamped,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ) {
            collapseFraction = value
        }
    }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (canScrollProvider()) {
                onScrollDelta(available.y)
            }
            return Offset.Zero
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            if (canScrollProvider()) {
                settle(consumed.y + available.y)
            }
            return Velocity.Zero
        }
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
