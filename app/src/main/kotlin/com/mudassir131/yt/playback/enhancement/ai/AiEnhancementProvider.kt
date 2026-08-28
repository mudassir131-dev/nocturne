/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.enhancement.ai

import com.mudassir131.yt.playback.enhancement.model.AudioCharacteristics
import com.mudassir131.yt.playback.enhancement.model.EnhancementMode
import com.mudassir131.yt.playback.enhancement.model.EnhancementProfile

/**
 * Modular interface for AI Enhancement Parameter Optimizers.
 *
 * Enforces:
 * 1. The AI acts ONLY as an audio mastering engineer / DSP tuner.
 * 2. It never returns or processes raw audio.
 * 3. It returns structured, sanitized DSP control parameters.
 */
interface AiEnhancementProvider {

    /**
     * Analyzes compact audio characteristics and generates an optimal DSP enhancement profile.
     *
     * @param characteristics Extracted audio measurements (RMS, peak, crest factor, frequency energy, etc.)
     * @param trackTitle Track title for genre/contextual guidance (optional)
     * @param trackArtist Track artist for stylistic reference (optional)
     * @param mode Target enhancement mode (Natural, Clear, Detailed, Hi-Res Feel, Studio)
     * @param apiKey User's BYOK secret API key
     * @return Result containing sanitized [EnhancementProfile] or an Exception upon failure.
     */
    suspend fun optimizeProfile(
        characteristics: AudioCharacteristics,
        trackTitle: String?,
        trackArtist: String?,
        mode: EnhancementMode,
        apiKey: String,
    ): Result<EnhancementProfile>
}
