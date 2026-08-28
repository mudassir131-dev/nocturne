/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.enhancement.ai

import com.mudassir131.yt.playback.enhancement.model.AudioCharacteristics
import com.mudassir131.yt.playback.enhancement.model.EnhancementMode
import com.mudassir131.yt.playback.enhancement.model.EnhancementProfile
import com.mudassir131.yt.playback.enhancement.model.ProfileSource
import kotlin.math.max

/**
 * Intelligent deterministic local DSP parameter generator.
 *
 * Used when:
 * 1. No API key is configured
 * 2. User selects "Local Only"
 * 3. Device is offline
 * 4. AI request times out or returns malformed response
 *
 * Implements audio engineering heuristic rules derived from extracted spectral & dynamic characteristics.
 */
class LocalFallbackEnhancementProvider : AiEnhancementProvider {

    override suspend fun optimizeProfile(
        characteristics: AudioCharacteristics,
        trackTitle: String?,
        trackArtist: String?,
        mode: EnhancementMode,
        apiKey: String,
    ): Result<EnhancementProfile> {
        val base = EnhancementProfile.forMode(mode)

        // 1. Low-Mid Mud Control (250 - 500 Hz buildup detection)
        val mudCorrectionDb = when {
            characteristics.lowMidEnergy > 0.28f -> -1.8f
            characteristics.lowMidEnergy > 0.22f -> -1.0f
            characteristics.lowMidEnergy < 0.12f -> 0.0f
            else -> -0.5f
        }

        // 2. High-Frequency Air & Presence Adaptation
        val needsBrightness = characteristics.spectralCentroidHz < 2200.0f || characteristics.highEnergy < 0.07f
        val isAlreadySharp = characteristics.spectralCentroidHz > 4500.0f || characteristics.highEnergy > 0.20f

        val presenceCorrectionDb = when {
            isAlreadySharp -> -0.5f
            needsBrightness -> 1.2f
            else -> 0.0f
        }

        val airCorrectionDb = when {
            isAlreadySharp -> -0.8f
            needsBrightness -> 1.5f
            else -> 0.2f
        }

        // 3. Dynamic Bass Management Adaptation
        val hasExcessiveBass = characteristics.subBassEnergy > 0.22f || characteristics.bassEnergy > 0.35f
        val isBassLight = characteristics.bassEnergy < 0.12f && characteristics.subBassEnergy < 0.08f

        val lowShelfAdjustDb = when {
            hasExcessiveBass -> -0.5f // Don't statically boost heavy bass
            isBassLight -> 1.5f      // Gently restore lean bass
            else -> 0.5f
        }

        val bassDynamicFactor = when {
            hasExcessiveBass -> 0.45f // Strong dynamic protection
            characteristics.crestFactorDb < 8.0f -> 0.35f // Low dynamic range track
            else -> 0.20f
        }

        // 4. Harmonic Enhancement & Distortion Guard
        val isClipped = characteristics.estimatedClipping > 0.02f
        val harmonicAmount = when {
            isClipped -> 0.01f // Disengage exciter on pre-distorted/clipped source
            characteristics.highEnergy < 0.08f -> 0.08f // Add pleasant warmth to dull source
            else -> base.harmonicAmount
        }

        // 5. Dynamics & Compression Adjustment
        val compressionAdjust = when {
            characteristics.estimatedCompression > 0.70f -> 0.05f // Avoid over-squashing compressed audio
            characteristics.dynamicRangeDb > 25.0f -> 0.20f        // Gentle control on high-dynamic tracks
            else -> base.compressionAmount
        }

        val calculatedProfile = EnhancementProfile(
            lowShelfDb = (base.lowShelfDb + lowShelfAdjustDb).coerceIn(-4.0f, 4.0f),
            lowMidDb = (base.lowMidDb + mudCorrectionDb).coerceIn(-4.0f, 1.0f),
            presenceDb = (base.presenceDb + presenceCorrectionDb).coerceIn(-2.0f, 3.5f),
            airDb = (base.airDb + airCorrectionDb).coerceIn(-2.0f, 4.0f),
            bassDynamicAmount = bassDynamicFactor.coerceIn(0.10f, 0.80f),
            compressionAmount = compressionAdjust.coerceIn(0.0f, 0.50f),
            harmonicAmount = harmonicAmount.coerceIn(0.0f, 0.15f),
            stereoAmount = base.stereoAmount,
            limiterCeilingDb = -1.0f,
            confidence = 0.85f,
            mode = mode,
            source = ProfileSource.LOCAL_FALLBACK,
            timestampMs = System.currentTimeMillis(),
        )

        return Result.success(calculatedProfile.sanitize())
    }
}
