package com.mudassir131.yt.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun RootScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        action?.invoke()
    }
}

@Composable
fun AnimatedHeaderAction(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(120), label = "header-action")
    Surface(
        modifier = modifier
            .size(48.dp)
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.padding(12.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Shared direction-aware root header state. Negative content motion hides the header; reversing
 * the content motion reveals it immediately, without waiting for the list to return to item zero.
 */
@Stable
class AutoHidingHeaderState internal constructor() {
    var offsetPx by mutableFloatStateOf(0f)
        private set
    private var maxHeightPx = 1f

    internal fun updateHeight(heightPx: Float) {
        maxHeightPx = heightPx.coerceAtLeast(1f)
        offsetPx = offsetPx.coerceIn(-maxHeightPx, 0f)
    }

    fun reveal() {
        offsetPx = 0f
    }

    private fun moveBy(delta: Float): Float {
        if (abs(delta) < 0.35f) return 0f
        val previousOffset = offsetPx
        offsetPx = (offsetPx + delta).coerceIn(-maxHeightPx, 0f)
        return offsetPx - previousOffset
    }

    private suspend fun settle(velocityY: Float) {
        val target = when {
            velocityY < -500f -> -maxHeightPx
            velocityY > 500f -> 0f
            offsetPx < -maxHeightPx * 0.48f -> -maxHeightPx
            else -> 0f
        }
        animate(
            initialValue = offsetPx,
            targetValue = target,
            animationSpec = tween(180),
        ) { value, _ -> offsetPx = value }
    }

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val consumedY = moveBy(available.y)
            return Offset(x = 0f, y = consumedY)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            settle(consumed.y + available.y)
            return Velocity.Zero
        }
    }
}

@Composable
fun rememberAutoHidingHeaderState(): AutoHidingHeaderState {
    return remember { AutoHidingHeaderState() }
}

/**
 * Native root-screen shell. Header and content are measured as one vertical hierarchy. Collapsing
 * the header changes the content's real placement instead of translating two independently
 * scrolling layers over each other. Player/navigation bottom insets remain owned by scrollable
 * content through the compact local inset override.
 */
@Composable
fun AutoHidingRootScaffold(
    modifier: Modifier = Modifier,
    state: AutoHidingHeaderState = rememberAutoHidingHeaderState(),
    header: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val statusBarHeight = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

    val playerInsets = LocalPlayerAwareWindowInsets.current
    val compactInsets = playerInsets.only(
        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
    )

    Layout(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(state.nestedScrollConnection),
        content = {
            Surface(
                color = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = statusBarHeight),
                    content = header,
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalPlayerAwareWindowInsets provides compactInsets) {
                    content()
                }
            }
        },
    ) { measurables, constraints ->
        val headerPlaceable = measurables[0].measure(constraints.copy(minHeight = 0))
        state.updateHeight(headerPlaceable.height.toFloat())

        val visibleHeaderHeight =
            (headerPlaceable.height + state.offsetPx.roundToInt())
                .coerceIn(0, headerPlaceable.height)
        val safeContentTop = maxOf(visibleHeaderHeight, with(density) { statusBarHeight.roundToPx() })
        val contentPlaceable = measurables[1].measure(
            constraints.copy(
                minHeight = 0,
                maxHeight = (constraints.maxHeight - safeContentTop).coerceAtLeast(0),
            ),
        )

        layout(constraints.maxWidth, constraints.maxHeight) {
            headerPlaceable.placeRelative(0, state.offsetPx.roundToInt())
            contentPlaceable.placeRelative(0, safeContentTop)
        }
    }
}
