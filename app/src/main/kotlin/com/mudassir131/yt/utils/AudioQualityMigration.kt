package com.mudassir131.yt.utils

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.mudassir131.yt.constants.AudioQuality
import com.mudassir131.yt.constants.AudioQualityKey
import com.mudassir131.yt.constants.PlayerVolumeKey

private val AudioQualityMigratedKey = booleanPreferencesKey("audioQualityMigratedToSourcesV1")

/** Maps the removed bitrate presets onto the two source/codec modes before preferences are read. */
object AudioQualityMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[AudioQualityMigratedKey] != true

    override suspend fun migrate(currentData: Preferences): Preferences =
        mutablePreferencesOf().apply {
            this += currentData
            this[AudioQualityKey] = when (currentData[AudioQualityKey]) {
                AudioQuality.SAAVN.name -> AudioQuality.SAAVN.name
                // LOW used the AAC-oriented compatibility tier; all other legacy modes preferred
                // the best available stream and therefore migrate to the explicit Opus path.
                "LOW" -> AudioQuality.SAAVN.name
                else -> AudioQuality.LOSSLESS.name
            }
            // Volume is now owned by Android STREAM_MUSIC; prevent a stale in-player multiplier
            // from attenuating the device volume a second time.
            this[PlayerVolumeKey] = 1f
            this[AudioQualityMigratedKey] = true
        }

    override suspend fun cleanUp() = Unit
}
