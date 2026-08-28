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
 * OpenAI / Compatible AI DSP Parameter Optimizer.
 *
 * Supports OpenAI, OpenRouter, Groq, or custom OpenAI-compatible proxy endpoints.
 */
class OpenAiEnhancementProvider(
    private val endpointUrl: String = "https://api.openai.com/v1/chat/completions",
    private val modelName: String = "gpt-4o-mini",
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
            return@withContext Result.failure(IllegalArgumentException("OpenAI API key is blank"))
        }

        try {
            val userPrompt = buildString {
                appendLine("AUDIO SPECTRAL & DYNAMICS METRICS:")
                if (!trackTitle.isNullOrBlank()) appendLine("Title: $trackTitle")
                if (!trackArtist.isNullOrBlank()) appendLine("Artist: $trackArtist")
                appendLine("Mode: ${mode.name}")
                appendLine("RMS: ${String.format(java.util.Locale.US, "%.1f", characteristics.rmsDb)} dBFS, Peak: ${String.format(java.util.Locale.US, "%.1f", characteristics.peakDb)} dBFS, CrestFactor: ${String.format(java.util.Locale.US, "%.1f", characteristics.crestFactorDb)} dB")
                appendLine("Dynamic Range: ${String.format(java.util.Locale.US, "%.1f", characteristics.dynamicRangeDb)} dB, Centroid: ${characteristics.spectralCentroidHz.toInt()} Hz, Rolloff: ${characteristics.spectralRolloffHz.toInt()} Hz")
                appendLine("Sub-Bass: ${String.format(java.util.Locale.US, "%.2f", characteristics.subBassEnergy)}, Bass: ${String.format(java.util.Locale.US, "%.2f", characteristics.bassEnergy)}, Low-Mid: ${String.format(java.util.Locale.US, "%.2f", characteristics.lowMidEnergy)}")
                appendLine("Mid: ${String.format(java.util.Locale.US, "%.2f", characteristics.midEnergy)}, Presence: ${String.format(java.util.Locale.US, "%.2f", characteristics.presenceEnergy)}, Highs: ${String.format(java.util.Locale.US, "%.2f", characteristics.highEnergy)}, Air: ${String.format(java.util.Locale.US, "%.2f", characteristics.airEnergy)}")
                appendLine("Clipping: ${String.format(java.util.Locale.US, "%.1f%%", characteristics.estimatedClipping * 100.0f)}, Compression: ${String.format(java.util.Locale.US, "%.2f", characteristics.estimatedCompression)}, Transients: ${String.format(java.util.Locale.US, "%.1f", characteristics.transientDensity)}/s")
            }

            val requestJson = JSONObject().apply {
                put("model", modelName)
                put("response_format", JSONObject().put("type", "json_object"))
                put("temperature", 0.2)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", GeminiEnhancementProvider.SYSTEM_INSTRUCTION)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(endpointUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    Timber.tag(TAG).w("OpenAI API request failed: HTTP $code")
                    return@withContext Result.failure(IllegalStateException("OpenAI API HTTP $code"))
                }

                val responseBody = response.body?.string()
                    ?: return@withContext Result.failure(IllegalStateException("Empty OpenAI response"))

                val root = JSONObject(responseBody)
                val choices = root.getJSONArray("choices")
                val message = choices.getJSONObject(0).getJSONObject("message")
                val content = message.getString("content")

                val dspJson = JSONObject(content.trim().removeSurrounding("```json", "```").trim())

                val profile = EnhancementProfile(
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
                    confidence = dspJson.optDouble("confidence", 0.90).toFloat(),
                    mode = mode,
                    source = ProfileSource.AI_OPTIMIZED,
                    timestampMs = System.currentTimeMillis(),
                ).sanitize()

                Result.success(profile)
            }
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "Error during OpenAI enhancement optimization")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "OpenAiEnhancement"
    }
}
