package com.mudassir131.yt.ui.theme

import androidx.compose.ui.graphics.Color
import java.util.LinkedHashMap

/** Small process-local LRU for artwork palettes; Coil remains responsible for bitmap caching. */
object ArtworkPaletteCache {
    private const val MaxEntries = 48
    private val values = object : LinkedHashMap<String, List<Color>>(MaxEntries, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<Color>>?): Boolean =
            size > MaxEntries
    }

    @Synchronized
    operator fun get(key: String): List<Color>? = values[key]

    @Synchronized
    operator fun set(key: String, colors: List<Color>) {
        if (colors.isNotEmpty()) values[key] = colors
    }
}
