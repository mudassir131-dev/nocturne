/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Player slider color configuration for consistent styling across all slider types
 * 
 * This object provides standardized color schemes for Default, Squiggly, and Slim sliders
 * used in the music player interface, ensuring visual consistency and proper contrast.
 */
object PlayerSliderColors {

    private fun resolveActive(color: Color, isLight: Boolean): Color {
        // If caller explicitly passed White (e.g. Apple Music player), preserve pure White!
        if (color == Color.White) return Color.White
        return if (isLight && color.luminance() > 0.45f) Color(0xFF2C343D) else color
    }

    private fun resolveInactive(color: Color, isLight: Boolean, alpha: Float): Color {
        if (color == Color.White) return Color.White.copy(alpha = alpha)
        return if (isLight) Color(0xFFD6DBE2) else Color.White.copy(alpha = alpha)
    }

    /**
     * Standard slider colors for all slider types
     * 
     * @param activeColor Color for active track, ticks, and thumb
     * @param inactiveAlpha Alpha transparency for inactive track (default: 0.15f for subtle appearance)
     * @return SliderColors configuration
     */
    @Composable
    fun getSliderColors(
        activeColor: Color,
        inactiveAlpha: Float = 0.15f
    ): SliderColors {
        val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.45f
        val active = resolveActive(activeColor, isLight)
        val inactive = resolveInactive(activeColor, isLight, inactiveAlpha)
        return SliderDefaults.colors(
            activeTrackColor = active,
            activeTickColor = active,
            thumbColor = active,
            inactiveTrackColor = inactive
        )
    }

    /**
     * Default slider colors using button color scheme
     * 
     * @param buttonColor The active button color from player theme
     * @return SliderColors configuration for default slider
     */
    @Composable
    fun standardSliderColors(buttonColor: Color): SliderColors {
        return getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.INACTIVE_TRACK_ALPHA
        )
    }

    /**
     * Squiggly slider colors using button color scheme
     * 
     * @param buttonColor The active button color from player theme
     * @return SliderColors configuration for squiggly slider
     */
    @Composable
    fun wavySliderColors(buttonColor: Color): SliderColors {
        val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.45f
        val active = resolveActive(buttonColor, isLight)
        val inactive = resolveInactive(buttonColor, isLight, Config.INACTIVE_TRACK_ALPHA)
        val inactiveTick = resolveInactive(buttonColor, isLight, Config.INACTIVE_TICK_ALPHA)
        return SliderDefaults.colors(
            activeTrackColor = active,
            activeTickColor = active,
            thumbColor = Color.Transparent,
            inactiveTrackColor = inactive,
            inactiveTickColor = inactiveTick
        )
    }

    @Composable
    fun thickSliderColors(buttonColor: Color): SliderColors {
        return getSliderColors(
            activeColor = buttonColor,
            inactiveAlpha = Config.THICK_INACTIVE_TRACK_ALPHA
        )
    }

    @Composable
    fun circularSliderColors(buttonColor: Color): SliderColors {
        val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.45f
        val active = resolveActive(buttonColor, isLight)
        val inactive = resolveInactive(buttonColor, isLight, Config.INACTIVE_TRACK_ALPHA)
        return SliderDefaults.colors(
            activeTrackColor = active,
            activeTickColor = active,
            thumbColor = active,
            inactiveTrackColor = inactive
        )
    }

    @Composable
    fun simpleSliderColors(buttonColor: Color): SliderColors {
        val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.45f
        val active = resolveActive(buttonColor, isLight)
        val inactive = resolveInactive(buttonColor, isLight, Config.SIMPLE_INACTIVE_TRACK_ALPHA)
        return SliderDefaults.colors(
            activeTrackColor = active.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            activeTickColor = active.copy(alpha = Config.SIMPLE_ACTIVE_TRACK_ALPHA),
            thumbColor = Color.Transparent,
            inactiveTrackColor = inactive,
            inactiveTickColor = inactive
        )
    }

    /**
     * Configuration constants for slider colors
     */
    object Config {
        /** Alpha transparency for inactive track - subtle white appearance */
        const val INACTIVE_TRACK_ALPHA = 0.15f

        const val THICK_INACTIVE_TRACK_ALPHA = 0.2f

        const val SIMPLE_ACTIVE_TRACK_ALPHA = 0.8f

        const val SIMPLE_INACTIVE_TRACK_ALPHA = 0.1f
        
        /** Alpha transparency for inactive ticks */
        const val INACTIVE_TICK_ALPHA = 0.2f
        
        /** Default active color when no theme color is available */
        val DEFAULT_ACTIVE_COLOR = Color(0xFF1976D2)
        
        /** Default inactive color when no theme color is available */
        val DEFAULT_INACTIVE_COLOR = Color.White.copy(alpha = INACTIVE_TRACK_ALPHA)
    }
}