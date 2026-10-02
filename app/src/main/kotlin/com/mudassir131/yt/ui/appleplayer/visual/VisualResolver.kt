/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import com.mudassir131.yt.models.MediaMetadata
import com.mudassir131.yt.ui.appleplayer.liveart.AppleLiveArtworkResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

object VisualResolver {
    private const val TAG = "VisualResolver"

    suspend fun resolve(
        metadata: MediaMetadata,
        dataSaver: Boolean = false,
        isMetered: Boolean = false,
    ): VisualResolutionResult = withContext(Dispatchers.IO) {
        val artwork = VisualContent.Artwork(metadata.thumbnailUrl)

        if (dataSaver) {
            Timber.tag(TAG).d("DataSaver enabled: skipping video/canvas resolution, using artwork")
            return@withContext VisualResolutionResult(
                musicVideo = null,
                canvas = null,
                artwork = artwork,
                activeMode = VisualMode.ARTWORK,
            )
        }

        try {
            coroutineScope {
                val videoDeferred = async {
                    runCatching {
                        MusicVideoResolver.resolve(metadata, isMetered = isMetered, dataSaver = dataSaver)
                    }.getOrNull()
                }

                val canvasDeferred = async {
                    runCatching {
                        AppleLiveArtworkResolver.resolve(metadata)
                    }.getOrNull()
                }

                val musicVideo = videoDeferred.await()
                val canvasArtwork = canvasDeferred.await()

                val canvasContent = canvasArtwork?.let {
                    val primary = it.preferredAnimationUrl?.takeIf { u -> u.isNotBlank() }
                    val fallback = it.videoUrl?.takeIf { u -> u.isNotBlank() }
                    if (primary != null || fallback != null) {
                        VisualContent.Canvas(
                            artwork = it,
                            primaryUrl = primary,
                            fallbackUrl = fallback,
                        )
                    } else null
                }

                val initialMode = when {
                    canvasContent != null -> VisualMode.CANVAS
                    musicVideo != null && !musicVideo.streamUrl.isNullOrBlank() -> VisualMode.MUSIC_VIDEO
                    else -> VisualMode.ARTWORK
                }

                Timber.tag(TAG).d(
                    "Resolved visual for '${metadata.title}': hasVideo=${musicVideo != null}, hasCanvas=${canvasContent != null}, mode=$initialMode"
                )

                VisualResolutionResult(
                    musicVideo = musicVideo,
                    canvas = canvasContent,
                    artwork = artwork,
                    activeMode = initialMode,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error during visual resolution, falling back to artwork")
            VisualResolutionResult(
                musicVideo = null,
                canvas = null,
                artwork = artwork,
                activeMode = VisualMode.ARTWORK,
            )
        }
    }
}
