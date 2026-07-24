package com.mudassir131.yt.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import kotlin.math.max

/**
 * One cached, screen-sized visual canvas for Nocturne's root destinations.
 *
 * The theme surface is always painted first. When blur effects are enabled, the dynamic Material
 * palette is layered across the complete viewport so headers, filters, content, and empty space
 * all belong to the same composition. This intentionally owns no insets: the foreground screen
 * remains responsible for safe interactive placement while the background can stay edge-to-edge.
 */
@Composable
fun NocturneDynamicScreen(
    disableBlur: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val secondaryContainer = MaterialTheme.colorScheme.secondaryContainer

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                if (disableBlur) {
                    onDrawBehind { drawRect(surface) }
                } else {
                    val width = size.width
                    val height = size.height
                    val span = max(width, height)
                    val layers = listOf(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.34f), Color.Transparent),
                            center = Offset(width * 0.12f, height * 0.08f),
                            radius = span * 0.48f,
                        ),
                        Brush.radialGradient(
                            colors = listOf(secondary.copy(alpha = 0.30f), Color.Transparent),
                            center = Offset(width * 0.88f, height * 0.20f),
                            radius = span * 0.52f,
                        ),
                        Brush.radialGradient(
                            colors = listOf(tertiary.copy(alpha = 0.24f), Color.Transparent),
                            center = Offset(width * 0.22f, height * 0.52f),
                            radius = span * 0.50f,
                        ),
                        Brush.radialGradient(
                            colors = listOf(primaryContainer.copy(alpha = 0.22f), Color.Transparent),
                            center = Offset(width * 0.82f, height * 0.64f),
                            radius = span * 0.54f,
                        ),
                        Brush.radialGradient(
                            colors = listOf(secondaryContainer.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(width * 0.45f, height * 0.92f),
                            radius = span * 0.58f,
                        ),
                    )
                    val readabilityVeil = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            surface.copy(alpha = 0.08f),
                            surface.copy(alpha = 0.24f),
                        ),
                        startY = height * 0.42f,
                        endY = height,
                    )

                    onDrawBehind {
                        drawRect(surface)
                        layers.forEach { drawRect(it) }
                        drawRect(readabilityVeil)
                    }
                }
            },
        content = content,
    )
}
