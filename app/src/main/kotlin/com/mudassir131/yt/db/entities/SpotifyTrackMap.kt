/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.db.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Global Spotify → YouTube match cache, shared across every playlist import.
 *
 * The match-quality columns let a cache hit be reused *with* the confidence it originally earned,
 * rather than reappearing as an unscored match.
 */
@Immutable
@Entity(
    tableName = "spotify_track_map",
    indices = [
        Index(value = ["spotifyTrackId"], unique = true),
        Index(value = ["songId"])
    ]
)
data class SpotifyTrackMap(
    @PrimaryKey val spotifyTrackId: String,
    val songId: String,
    val title: String,
    val artist: String = "",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    // Added in schema 30. Non-null columns added under an AutoMigration need a defaultValue.
    @ColumnInfo(defaultValue = "")
    val album: String = "",
    @ColumnInfo(defaultValue = "0")
    val durationMs: Long = 0,
    @ColumnInfo(defaultValue = "0")
    val matchConfidence: Float = 0f,
    @ColumnInfo(defaultValue = "INNERTUBE")
    val matchSource: String = "INNERTUBE"
)
