/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDateTime

/**
 * One Spotify track as it landed in a Nocturne playlist, with the outcome of matching it to YouTube.
 *
 * This exists as its own table because [youtubeVideoId] has to be nullable: an unmatched track can't
 * live in `playlist_song_map`, which has a foreign key to `song`. Keeping the unmatched rows here is
 * what makes manual review possible instead of the track silently disappearing from the import.
 */
@Immutable
@Entity(
    tableName = "spotify_import_track",
    primaryKeys = ["playlistId", "spotifyTrackId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["matchStatus"]),
        Index(value = ["spotifyTrackId"])
    ]
)
data class SpotifyImportTrackEntity(
    val playlistId: String,
    val spotifyTrackId: String,
    val trackName: String,
    val artist: String = "",
    val album: String = "",
    /** Spotify reports milliseconds; the rest of the app works in seconds. Stored as given. */
    val durationMs: Long = 0,
    val position: Int = 0,
    /** Null exactly when [matchStatus] is [STATUS_UNMATCHED]. */
    val youtubeVideoId: String? = null,
    val matchConfidence: Float = 0f,
    val matchStatus: String = STATUS_UNMATCHED,
    val matchSource: String = SOURCE_NONE,
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        const val STATUS_MATCHED = "MATCHED"
        const val STATUS_UNMATCHED = "UNMATCHED"

        /** Matched through YouTube Data API v3 `search.list` with a duration check. */
        const val SOURCE_DATA_API = "DATA_API"

        /** Matched through the keyless InnerTube search, same scoring. */
        const val SOURCE_INNERTUBE = "INNERTUBE"

        /** Reused an earlier match from `spotify_track_map`. */
        const val SOURCE_CACHE = "CACHE"

        /** Nothing cleared the confidence threshold. */
        const val SOURCE_NONE = "NONE"
    }
}
