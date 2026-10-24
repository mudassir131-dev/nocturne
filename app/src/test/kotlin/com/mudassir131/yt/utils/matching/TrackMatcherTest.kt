/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils.matching

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Artist verification is the gate that keeps a same-titled song by a different performer
 * from being served as the searched track (the reported "12 Saal" / "Brown Rang" wrong-song
 * bug). These tests pin exactly which names count as the same performer.
 */
class TrackMatcherTest {

    @Test
    fun `different performers must never verify even when edit distance is suspiciously close`() {
        // levenshteinRatio("bilal saeed", "sarmad qadeer") ~ 0.39 - above the 0.35 title floor
        // this bug slipped through - yet these are two different people.
        assertFalse(TrackMatcher.artistVerified("Bilal Saeed", "Sarmad Qadeer"))
        assertFalse(TrackMatcher.artistVerified("Yo Yo Honey Singh", "Rahul Vaidya"))
        assertFalse(TrackMatcher.artistVerified("Sonu Nigam", "Sonu Kakkar"))
    }

    @Test
    fun `billings that fold into one another verify`() {
        assertTrue(TrackMatcher.artistVerified("Bilal Saeed", "Bilal Saeed"))
        assertTrue(TrackMatcher.artistVerified("Yo Yo Honey Singh", "Honey Singh"))
        assertTrue(TrackMatcher.artistVerified("Honey Singh", "Yo Yo Honey Singh"))
        assertTrue(
            "A full multi-artist CSV field must contain the searched performer",
            TrackMatcher.artistVerified("Arijit Singh", "Arijit Singh, Shashaa Tirupati"),
        )
        assertTrue(
            "A shortened billing of the same performer must verify",
            TrackMatcher.artistVerified("Pritam Chakraborty", "Pritam"),
        )
    }

    @Test
    fun `spelling variants of the same performer verify`() {
        assertTrue(TrackMatcher.artistVerified("Armaan Malik", "Arman Malik"))
        assertTrue(TrackMatcher.artistVerified("A.R. Rahman", "A R Rahman"))
    }

    @Test
    fun `blank sides can never verify`() {
        assertFalse(TrackMatcher.artistVerified("", "Bilal Saeed"))
        assertFalse(TrackMatcher.artistVerified("Bilal Saeed", ""))
        assertFalse(TrackMatcher.artistVerified("   ", "   "))
    }
}
