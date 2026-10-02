/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import androidx.compose.runtime.Immutable
import com.mudassir131.yt.ui.appleplayer.liveart.CanvasArtwork

@Immutable
sealed interface VisualContent {
    @Immutable
    data class MusicVideo(
        val videoId: String,
        val title: String?,
        val artist: String?,
        val thumbnailUrl: String?,
        val durationMs: Long?,
        val streamUrl: String? = null,
        val qualityLabel: String? = null,
        val itag: Int? = null,
        val availableQualities: List<VideoQualityOption> = emptyList(),
    ) : VisualContent

    @Immutable
    data class Canvas(
        val artwork: CanvasArtwork,
        val primaryUrl: String?,
        val fallbackUrl: String?,
    ) : VisualContent

    @Immutable
    data class Artwork(
        val url: String?,
    ) : VisualContent

    data object None : VisualContent
}

enum class VisualMode {
    MUSIC_VIDEO,
    CANVAS,
    ARTWORK,
    NONE,
}

@Immutable
data class VideoQualityOption(
    val itag: Int,
    val label: String,
    val height: Int,
    val isAuto: Boolean = false,
)

@Immutable
data class VisualResolutionResult(
    val musicVideo: VisualContent.MusicVideo? = null,
    val canvas: VisualContent.Canvas? = null,
    val artwork: VisualContent.Artwork = VisualContent.Artwork(null),
    val activeMode: VisualMode = VisualMode.ARTWORK,
) {
    val hasVideo: Boolean get() = musicVideo != null && !musicVideo.streamUrl.isNullOrBlank()
    val hasCanvas: Boolean get() = canvas != null && (!canvas.primaryUrl.isNullOrBlank() || !canvas.fallbackUrl.isNullOrBlank())
    val hasBothVideoAndCanvas: Boolean get() = hasVideo && hasCanvas
}
