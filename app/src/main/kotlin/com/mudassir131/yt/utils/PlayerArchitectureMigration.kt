package com.mudassir131.yt.utils

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mudassir131.yt.constants.AppleMusicInspiredKey
import com.mudassir131.yt.constants.GlassBlurIntensityKey
import com.mudassir131.yt.constants.GlassDynamicTintKey
import com.mudassir131.yt.constants.GlassEffectsKey
import com.mudassir131.yt.constants.GlassPerformanceModeKey
import com.mudassir131.yt.constants.GlassQualityModeKey
import com.mudassir131.yt.constants.GlassTransparencyKey
import com.mudassir131.yt.constants.PlayerDesignStyleKey

private val LegacyPlayerExperienceKey = stringPreferencesKey("playerExperience")
private val PlayerArchitectureMigratedKey = booleanPreferencesKey("playerArchitectureMigratedV3")

/**
 * Collapses stale player selections before any composable reads them.
 * Cinematic is now the implicit default; only the optional Apple boolean remains.
 */
object PlayerArchitectureMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[PlayerArchitectureMigratedKey] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val migrated = mutablePreferencesOf().apply { this += currentData }
        // Force Apple Music inspired player to default ON
        migrated[AppleMusicInspiredKey] = true

        migrated.remove(LegacyPlayerExperienceKey)
        migrated.remove(PlayerDesignStyleKey)
        migrated.remove(GlassEffectsKey)
        migrated.remove(GlassQualityModeKey)
        migrated.remove(GlassBlurIntensityKey)
        migrated.remove(GlassTransparencyKey)
        migrated.remove(GlassDynamicTintKey)
        migrated.remove(GlassPerformanceModeKey)
        migrated[PlayerArchitectureMigratedKey] = true
        return migrated
    }

    override suspend fun cleanUp() = Unit
}
