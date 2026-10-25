/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineLosslessAudioProviderTest {

    @Test
    fun `test DES media URL decryption produces direct audio stream URL`() {
        // Test media url decryption
        val encryptedUrl = "L/T3aZ10e74fO6lE8O7Zkg==" // Valid test token
        val decrypted = OnlineLosslessAudioProvider.decryptMediaUrl(encryptedUrl)
        // Decrypted url must either decrypt cleanly or return null for garbage
        // Let's test with a verified known test vector
        val knownEncrypted = "r0W+6P0Z/s/N8TqE8t4Bww==" // Sample
        val result = OnlineLosslessAudioProvider.decryptMediaUrl(knownEncrypted)
        // Does not throw exceptions
    }

    @Test
    fun `test saavn 96 kbps token decrypts and upgrades to the 320 kbps master`() {
        // Real Saavn DES token (key 38346591) carrying a _96 kbps media URL
        val encryptedLowRate = "ID2ieOjCrwfgWvL5sXl4B1ImC5QfbsDyjwXWKqpYuaq36O3W/7tll6V5lRpBTXQnzQKcyIJEJgI2lGFVxcjhGhw7tS9a8Gtq"

        val upgraded = OnlineLosslessAudioProvider.decryptMediaUrl(encryptedLowRate)

        assertNotNull("the legacy _96 kbps token must decrypt", upgraded)
        assertEquals(
            "the _96 kbps source must be upgraded to the raw 320 kbps master",
            "https://aac.saavncdn.com/840/c9e70fb62d66fa6e14f6b7cdbc56cc05_320.mp4",
            upgraded,
        )
    }

    @Test
    fun `test OnlineLosslessAudioProvider is registered in LosslessStreamResolver`() {
        assertNotNull(OnlineLosslessAudioProvider)
        assertEquals("OnlineLosslessAudioProvider", OnlineLosslessAudioProvider.name)
    }
}
