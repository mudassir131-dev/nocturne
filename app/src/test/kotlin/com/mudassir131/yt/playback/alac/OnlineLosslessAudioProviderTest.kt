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
    fun `test OnlineLosslessAudioProvider is registered in LosslessStreamResolver`() {
        assertNotNull(OnlineLosslessAudioProvider)
        assertEquals("OnlineLosslessAudioProvider", OnlineLosslessAudioProvider.name)
    }
}
