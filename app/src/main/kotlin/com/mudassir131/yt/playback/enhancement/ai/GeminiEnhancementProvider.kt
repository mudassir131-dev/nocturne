/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.enhancement.ai

import com.mudassir131.yt.playback.enhancement.model.AudioCharacteristics
import com.mudassir131.yt.playback.enhancement.model.EnhancementMode
import com.mudassir131.yt.playback.enhancement.model.EnhancementProfile
import com.mudassir131.yt.playback.enhancement.model.ProfileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Google Gemini AI DSP Parameter Optimizer.
 *
 * Communicates with the official Gemini REST API using structured JSON output.
 * Never uploads raw audio; transmits only compact statistical metrics.
 */
class GeminiEnhancementProvider(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build(),
) : AiEnhancementProvider {

    override suspend fun optimizeProfile(
        characteristics: AudioCharacteristics,
        trackTitle: String?,
        trackArtist: String?,
        mode: EnhancementMode,
        apiKey: String,
    ): Result<EnhancementProfile> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is blank"))
        }

        try {
            val prompt = buildAnalysisPrompt(characteristics, trackTitle, trackArtist, mode)
            val requestBodyJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", SYSTEM_INSTRUCTION)
                    }))
                })
                put("contents", JSONArray().put(JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply {
                        put("text", prompt)
                    }))
                }))
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.2)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestBodyJson.toString().toRequestBody(mediaType)

            val url = "$GEMINI_BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = response.body?.string()?.take(200) ?: "Unknown"
                    Timber.tag(TAG).w("Gemini API request failed with HTTP $code: $errorMsg")
                    return@withContext Result.failure(IllegalStateException("Gemini API error (HTTP $code)"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IllegalStateException("Empty response from Gemini"))

                val profile = parseGeminiResponse(responseBody, mode)
                Result.success(profile)
            }
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "Error optimizing DSP profile with Gemini")
            Result.failure(e)
        }
    }

    private fun buildAnalysisPrompt(
        c: AudioCharacteristics,
        title: String?,
        artist: String?,
        mode: EnhancementMode,
    ): String {
        return buildString {
            appendLine("AUDIO SPECTRAL & DYNAMICS ANALYSIS:")
            if (!title.isNullOrBlank()) appendLine("Track: $title")
            if (!artist.isNullOrBlank()) appendLine("Artist: $artist")
            appendLine("Target Mode: ${mode.name} (${mode.description})")
            appendLine("RMS Level: ${String.format(java.util.Locale.US, "%.1f", c.rmsDb)} dBFS")
            appendLine("Peak Level: ${String.format(java.util.Locale.US, "%.1f", c.peakDb)} dBFS")
            appendLine("Crest Factor: ${String.format(java.util.Locale.US, "%.1f", c.crestFactorDb)} dB")
            appendLine("Dynamic Range: ${String.format(java.util.Locale.US, "%.1f", c.dynamicRangeDb)} dB")
            appendLine("Spectral Centroid: ${c.spectralCentroidHz.toInt()} Hz")
            appendLine("Spectral Rolloff (85%): ${c.spectralRolloffHz.toInt()} Hz")
            appendLine("Sub-Bass (20-60Hz) Energy Ratio: ${String.format(java.util.Locale.US, "%.2f", c.subBassEnergy)}")
            appendLine("Bass (60-250Hz) Energy Ratio: ${String.format(java.util.Locale.US, "%.2f", c.bassEnergy)}")
            appendLine("Low-Mid (250-500Hz) Energy Ratio: ${String.format(java.util.Locale.US, "%.2f", c.lowMidEnergy)}")
            appendLine("Mid (500-2000Hz) Energy Ratio: ${String.format(java.util.Locale.US, "%.2f", c.midEnergy)}")
            appendLine("Presence (2-6kHz) Energy Ratio: ${String.format(java.util.Locale.US, "%.2f", c.presenceEnergy)}")
            appendLine("Highs (6-20kHz) Energy Ratio: ${String.format(java.util.Locale.US, "%.2f", c.highEnergy)}")
            appendLine("Air (10-20kHz) Energy Ratio: ${String.format(java.util.Locale.US, "%.2f", c.airEnergy)}")
            appendLine("Estimated Clipping: ${String.format(java.util.Locale.US, "%.1f%%", c.estimatedClipping * 100.0f)}")
            appendLine("Estimated Compression: ${String.format(java.util.Locale.US, "%.2f", c.estimatedCompression)}")
            appendLine("Transient Density: ${String.format(java.util.Locale.US, "%.1f", c.transientDensity)} onsets/sec")
            appendLine()
            appendLine("Return a strict JSON object with DSP parameter values adhering to safe ranges.")
        }
    }

    private fun parseGeminiResponse(responseJson: String, mode: EnhancementMode): EnhancementProfile {
        val root = JSONObject(responseJson)
        val candidates = root.optJSONArray("candidates")
            ?: throw IllegalArgumentException("No candidates returned in Gemini JSON")
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.getJSONObject("content")
        val parts = content.getJSONArray("parts")
        val text = parts.getJSONObject(0).getString("text")

        // Parse structured DSP JSON
        val dspJson = JSONObject(text.trim().removeSurrounding("```json", "```").trim())

        return EnhancementProfile(
            preampDb = dspJson.optDouble("preamp_db", -2.0).toFloat(),
            lowShelfDb = dspJson.optDouble("low_shelf_db", 1.0).toFloat(),
            lowMidDb = dspJson.optDouble("low_mid_db", -0.5).toFloat(),
            presenceDb = dspJson.optDouble("presence_db", 0.8).toFloat(),
            airDb = dspJson.optDouble("air_db", 1.0).toFloat(),
            bassDynamicAmount = dspJson.optDouble("bass_dynamic_amount", 0.20).toFloat(),
            compressionAmount = dspJson.optDouble("compression_amount", 0.10).toFloat(),
            harmonicAmount = dspJson.optDouble("harmonic_amount", 0.05).toFloat(),
            stereoAmount = dspJson.optDouble("stereo_amount", 0.04).toFloat(),
            limiterCeilingDb = dspJson.optDouble("limiter_ceiling_db", -1.0).toFloat(),
            confidence = dspJson.optDouble("confidence", 0.95).toFloat(),
            mode = mode,
            source = ProfileSource.AI_OPTIMIZED,
            timestampMs = System.currentTimeMillis(),
        ).sanitize()
    }

    companion object {
        private const val TAG = "GeminiEnhancement"
        private const val GEMINI_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

        const val SYSTEM_INSTRUCTION = """You are a professional audio mastering and DSP engineer.
Your task is to generate safe realtime DSP parameters for improving the perceived fidelity of an existing audio track.
Never regenerate, synthesize, remix, replace, or reconstruct the song.
Preserve the original musical performance, vocals, instruments, timing, dynamics, and stereo character.
Optimize perceived clarity, detail, bass definition, vocal presence, instrument separation, transient clarity and spatial perception.
Use subtle processing.
Never introduce audible clipping, pumping, harshness, excessive brightness, boomy bass, artificial stereo widening, excessive compression or unnatural loudness.
If the source already contains strong bass, dynamically control it instead of applying aggressive static bass boost.
Maintain sufficient headroom.
Prefer natural fidelity over exaggerated effects.
Return only the requested structured DSP parameters in JSON format:
{
  "preamp_db": float (-12.0 to 0.0),
  "low_shelf_db": float (-6.0 to 6.0),
  "low_mid_db": float (-6.0 to 3.0),
  "presence_db": float (-3.0 to 5.0),
  "air_db": float (-4.0 to 5.0),
  "bass_dynamic_amount": float (0.0 to 1.0),
  "compression_amount": float (0.0 to 1.0),
  "harmonic_amount": float (0.0 to 0.3),
  "stereo_amount": float (0.0 to 0.3),
  "limiter_ceiling_db": float (-3.0 to -0.1),
  "confidence": float (0.0 to 1.0)
}"""
    }
}
