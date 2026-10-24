/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.mudassir131.yt.constants.AudioQuality
import com.mudassir131.yt.constants.AudioQualityKey
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The retuned default is YouTube Opus end to end: the stamped legacy LOSSLESS default
 * (which mixed Saavn AAC and YouTube Opus per song) flips exactly once; an explicit
 * SAAVN choice and an unset preference are both left alone.
 */
class AudioQualityOpusDefaultMigrationTest {

    @Test
    fun `stamped legacy LOSSLESS default flips to OPUS`() = runBlocking {
        val before = mutablePreferencesOf(AudioQualityKey to AudioQuality.LOSSLESS.name)
        val after = AudioQualityOpusDefaultMigration.migrate(before)

        assertEquals(AudioQuality.OPUS.name, after[AudioQualityKey])
    }

    @Test
    fun `explicitly chosen SAAVN is preserved`() = runBlocking {
        val before = mutablePreferencesOf(AudioQualityKey to AudioQuality.SAAVN.name)
        val after = AudioQualityOpusDefaultMigration.migrate(before)

        assertEquals(AudioQuality.SAAVN.name, after[AudioQualityKey])
    }

    @Test
    fun `unset preference stays unset so the code default applies`() = runBlocking {
        val after = AudioQualityOpusDefaultMigration.migrate(mutablePreferencesOf())

        assertNull(after[AudioQualityKey])
    }

    @Test
    fun `migration runs exactly once`() = runBlocking {
        val after = AudioQualityOpusDefaultMigration.migrate(mutablePreferencesOf())

        assertFalse(AudioQualityOpusDefaultMigration.shouldMigrate(after))
    }
}
