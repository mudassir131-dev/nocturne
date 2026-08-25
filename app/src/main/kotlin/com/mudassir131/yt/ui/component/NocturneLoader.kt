/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Universal Nocturne Loading Indicator - Android 16 / Pixel Expressive Morphing Scalloped Flower Loader
 *
 * Scales dynamically for small (14dp-24dp), medium (28dp-40dp), and large (48dp-64dp) loading indicators
 * across all screens, players, lyrics, dialogs, and menus.
 */
@Composable
fun NocturneLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color? = null,
) {
    PixelAndroid16Loader(
        modifier = modifier,
        size = size,
        color = color,
        numLobes = 10
    )
}

@Deprecated("Use NocturneLoader instead", ReplaceWith("NocturneLoader(modifier, size, color)"))
@Composable
fun VeluneLoader(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color? = null,
) {
    NocturneLoader(modifier, size, color)
}
