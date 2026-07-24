package com.mudassir131.yt.utils

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.mudassir131.yt.constants.HasCompletedOnboardingKey
import com.mudassir131.yt.constants.LastWelcomedVersionKey
import com.mudassir131.yt.constants.WelcomeShownKey

private val OnboardingMigrationCompleteKey = booleanPreferencesKey("onboardingPersistenceMigratedV1")

/**
 * Existing installations already dismissed Nocturne's legacy welcome surface.
 * Treat those users as onboarded so an app update never forces a new profile flow.
 */
object OnboardingMigration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[OnboardingMigrationCompleteKey] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val migrated = mutablePreferencesOf().apply { this += currentData }
        if (currentData[HasCompletedOnboardingKey] == null) {
            val isEstablishedInstall =
                currentData[WelcomeShownKey] == true ||
                    !currentData[LastWelcomedVersionKey].isNullOrBlank() ||
                    currentData.asMap().isNotEmpty()
            migrated[HasCompletedOnboardingKey] = isEstablishedInstall
        }
        migrated[OnboardingMigrationCompleteKey] = true
        return migrated
    }

    override suspend fun cleanUp() = Unit
}
