package com.mudassir131.yt.ui.appleplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleArtworkUrlTest {
    @Test
    fun googleMusicArtworkIsRequestedAtDonorResolution() {
        assertEquals(
            "https://lh3.googleusercontent.com/example=w1200-h1200",
            "https://lh3.googleusercontent.com/example=w544-h544-p-l90-rj"
                .toAppleExpandedArtworkUrl(),
        )
    }

    @Test
    fun youtubeVideoArtworkPrefersMaxResolutionWithSafeFallback() {
        val preferred = "https://i.ytimg.com/vi/id/hqdefault.jpg".toAppleExpandedArtworkUrl()
        assertEquals("https://i.ytimg.com/vi/id/maxresdefault.jpg", preferred)
        assertEquals("https://i.ytimg.com/vi/id/hqdefault.jpg", preferred.appleExpandedArtworkFallback())
    }

    @Test
    fun unrecognizedProviderKeepsOriginalArtworkUrl() {
        val original = "https://cdn.example.com/original-cover.jpg"
        assertEquals(original, original.toAppleExpandedArtworkUrl())
        assertNull(original.appleExpandedArtworkFallback())
    }
}
