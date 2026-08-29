/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Immutable
@Entity(tableName = "spotify_import_progress")
data class SpotifyImportProgressEntity(
    @PrimaryKey val spotifyPlaylistId: String,
    val playlistId: String,
    val playlistName: String,
    val nextUrl: String?,
    val processedCount: Int = 0,
    val status: String = "IN_PROGRESS",
    val maxTracks: Int = 3000,
    val lastUpdated: LocalDateTime = LocalDateTime.now()
)
