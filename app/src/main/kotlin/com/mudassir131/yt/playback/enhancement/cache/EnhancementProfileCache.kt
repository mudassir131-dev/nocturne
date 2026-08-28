/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.enhancement.cache

import android.content.Context
import com.mudassir131.yt.playback.enhancement.model.EnhancementMode
import com.mudassir131.yt.playback.enhancement.model.EnhancementProfile
import com.mudassir131.yt.playback.enhancement.model.ProfileSource
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance, persistent cache for track enhancement profiles.
 *
 * Keys are computed from:
 * - Stable Track ID / (Title + Artist + Duration)
 * - Enhancement Mode (Natural, Clear, Detailed, Hi-Res Feel, Studio)
 * - DSP Algorithm Version (automatically invalidates cache upon algorithm updates)
 */
class EnhancementProfileCache(private val context: Context) {

    private val memoryCache = ConcurrentHashMap<String, EnhancementProfile>()
    private val cacheDir = File(context.cacheDir, "dsp_enhancement_profiles").apply { mkdirs() }
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        checkAlgorithmVersion()
    }

    private fun checkAlgorithmVersion() {
        val storedVersion = prefs.getInt(KEY_ALGO_VERSION, 0)
        if (storedVersion != EnhancementProfile.DSP_ALGORITHM_VERSION) {
            Timber.tag(TAG).i("DSP Algorithm version changed ($storedVersion -> ${EnhancementProfile.DSP_ALGORITHM_VERSION}). Invalidating profile cache.")
            clearCache()
            prefs.edit().putInt(KEY_ALGO_VERSION, EnhancementProfile.DSP_ALGORITHM_VERSION).apply()
        }
    }

    fun generateFingerprint(
        mediaId: String?,
        title: String?,
        artist: String?,
        durationMs: Long?,
        mode: EnhancementMode,
    ): String {
        val rawKey = buildString {
            append("v${EnhancementProfile.DSP_ALGORITHM_VERSION}_")
            append("m${mode.name}_")
            if (!mediaId.isNullOrBlank()) {
                append("id:$mediaId")
            } else {
                append("t:${title?.trim()?.lowercase() ?: "unknown"}_")
                append("a:${artist?.trim()?.lowercase() ?: "unknown"}_")
                append("d:${durationMs ?: 0L}")
            }
        }

        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(rawKey.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }.take(32)
        } catch (e: Throwable) {
            rawKey.replace("[^a-zA-Z0-9_]".toRegex(), "_").take(48)
        }
    }

    fun getProfile(fingerprint: String): EnhancementProfile? {
        // 1. Memory Cache
        memoryCache[fingerprint]?.let { return it }

        // 2. Disk Cache
        val file = File(cacheDir, "$fingerprint.json")
        if (!file.exists()) return null

        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val json = JSONObject(jsonStr)
            val profile = EnhancementProfile(
                preampDb = json.optDouble("preamp_db", -2.0).toFloat(),
                lowShelfDb = json.optDouble("low_shelf_db", 1.0).toFloat(),
                lowMidDb = json.optDouble("low_mid_db", -0.5).toFloat(),
                presenceDb = json.optDouble("presence_db", 0.8).toFloat(),
                airDb = json.optDouble("air_db", 1.0).toFloat(),
                bassDynamicAmount = json.optDouble("bass_dynamic_amount", 0.20).toFloat(),
                compressionAmount = json.optDouble("compression_amount", 0.10).toFloat(),
                harmonicAmount = json.optDouble("harmonic_amount", 0.05).toFloat(),
                stereoAmount = json.optDouble("stereo_amount", 0.04).toFloat(),
                limiterCeilingDb = json.optDouble("limiter_ceiling_db", -1.0).toFloat(),
                confidence = json.optDouble("confidence", 1.0).toFloat(),
                mode = runCatching { EnhancementMode.valueOf(json.optString("mode")) }.getOrDefault(EnhancementMode.HI_RES_FEEL),
                source = ProfileSource.CACHED,
                timestampMs = json.optLong("timestamp", System.currentTimeMillis()),
            ).sanitize()

            memoryCache[fingerprint] = profile
            Timber.tag(TAG).d("Loaded cached enhancement profile for fingerprint: $fingerprint")
            profile
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "Error reading cached profile for $fingerprint")
            null
        }
    }

    fun saveProfile(fingerprint: String, profile: EnhancementProfile) {
        val sanitized = profile.sanitize().copy(source = ProfileSource.CACHED)
        memoryCache[fingerprint] = sanitized

        try {
            val json = JSONObject().apply {
                put("preamp_db", sanitized.preampDb.toDouble())
                put("low_shelf_db", sanitized.lowShelfDb.toDouble())
                put("low_mid_db", sanitized.lowMidDb.toDouble())
                put("presence_db", sanitized.presenceDb.toDouble())
                put("air_db", sanitized.airDb.toDouble())
                put("bass_dynamic_amount", sanitized.bassDynamicAmount.toDouble())
                put("compression_amount", sanitized.compressionAmount.toDouble())
                put("harmonic_amount", sanitized.harmonicAmount.toDouble())
                put("stereo_amount", sanitized.stereoAmount.toDouble())
                put("limiter_ceiling_db", sanitized.limiterCeilingDb.toDouble())
                put("confidence", sanitized.confidence.toDouble())
                put("mode", sanitized.mode.name)
                put("timestamp", sanitized.timestampMs)
            }

            val file = File(cacheDir, "$fingerprint.json")
            file.writeText(json.toString(), Charsets.UTF_8)
            Timber.tag(TAG).d("Persisted enhancement profile for fingerprint: $fingerprint")
        } catch (e: Throwable) {
            Timber.tag(TAG).e(e, "Error saving profile cache for $fingerprint")
        }
    }

    fun clearCache() {
        memoryCache.clear()
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Throwable) {
            Timber.tag(TAG).w(e, "Failed to clear disk cache")
        }
    }

    companion object {
        private const val TAG = "ProfileCache"
        private const val PREFS_NAME = "nocturne_profile_cache_prefs"
        private const val KEY_ALGO_VERSION = "dsp_algo_version"
    }
}
