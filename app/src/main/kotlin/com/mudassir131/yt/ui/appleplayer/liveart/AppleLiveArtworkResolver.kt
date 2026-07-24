/*
 * Adapted from Echo Music's live-artwork resolution pipeline (GPL-3.0).
 * See NOTICE-ECHO.md.
 */
package com.mudassir131.yt.ui.appleplayer.liveart

import com.mudassir131.yt.models.MediaMetadata
import kotlinx.coroutines.CancellationException

/**
 * Sequential resolution is intentional: it makes provider precedence stable and
 * avoids Echo's former first-response race changing artwork between launches.
 */
object AppleLiveArtworkResolver {
    suspend fun resolve(metadata: MediaMetadata): CanvasArtwork? {
        val artist = metadata.artists.firstOrNull()?.name.orEmpty()
        if (metadata.title.isBlank() || artist.isBlank()) return null

        return try {
            EchoMusicCanvasProvider.getBySongArtist(metadata.title, artist)
                ?: TidalCanvasProvider.getBySongArtist(
                    song = metadata.title,
                    artist = artist,
                    album = metadata.album?.title,
                )
                ?: AppleMusicCanvasProvider.getBySongArtist(
                    song = metadata.title,
                    artist = artist,
                    album = metadata.album?.title,
                )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            null
        }
    }
}
