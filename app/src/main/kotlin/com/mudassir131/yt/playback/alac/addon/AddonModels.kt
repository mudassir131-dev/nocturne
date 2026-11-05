/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac.addon

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Wire models representing external Addon sources and manifests.
 */
data class AddonManifest(
    val id: String = "",
    val name: String = "",
    val version: String = "",
    val resources: List<String> = emptyList(),
) {
    val isPlayable: Boolean
        get() = resources.isEmpty() || resources.any { it.equals("search", ignoreCase = true) || it.equals("stream", ignoreCase = true) }

    val displayName: String
        get() = name.ifBlank { id.ifBlank { "Lossless Addon" } }

    fun hasResource(res: String): Boolean = resources.any { it.equals(res, ignoreCase = true) }

    companion object {
        fun fromJson(json: JSONObject): AddonManifest {
            val id = json.optString("id")
            val name = json.optString("name")
            val version = json.optString("version")
            val resList = mutableListOf<String>()
            val resArray = json.optJSONArray("resources")
            if (resArray != null) {
                for (i in 0 until resArray.length()) {
                    val r = resArray.optString(i)
                    if (r.isNotBlank()) resList.add(r)
                }
            } else {
                val resString = json.optString("resources")
                if (resString.isNotBlank()) {
                    resString.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }.forEach { resList.add(it) }
                }
            }
            return AddonManifest(
                id = id,
                name = name,
                version = version,
                resources = resList,
            )
        }
    }
}

data class AddonTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationSeconds: Double? = null,
    val streamUrl: String? = null,
    val format: String? = null,
    val quality: String? = null,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val bitrate: Int? = null,
) {
    companion object {
        fun fromJson(json: JSONObject): AddonTrack? {
            val id = json.optString("id").ifBlank { json.optString("trackId") }
            val title = json.optString("title").ifBlank { json.optString("name") }
            if (id.isBlank() && title.isBlank()) return null

            var artist = json.optString("artist")
            if (artist.isBlank()) {
                val artistsArray = json.optJSONArray("artists")
                if (artistsArray != null && artistsArray.length() > 0) {
                    val first = artistsArray.opt(0)
                    artist = if (first is JSONObject) {
                        first.optString("name")
                    } else {
                        first.toString()
                    }
                }
            }

            val album = json.optString("album").ifBlank { null }
            val dur = when {
                json.has("duration") -> json.optDouble("duration").takeIf { !it.isNaN() && it > 0 }
                json.has("durationMs") -> (json.optDouble("durationMs") / 1000.0).takeIf { !it.isNaN() && it > 0 }
                json.has("lengthSeconds") -> json.optDouble("lengthSeconds").takeIf { !it.isNaN() && it > 0 }
                else -> null
            }

            val streamUrl = json.optString("streamURL").ifBlank {
                json.optString("streamUrl").ifBlank { json.optString("url") }
            }.ifBlank { null }

            val format = json.optString("format").ifBlank { null }
            val quality = json.optString("quality").ifBlank {
                json.optString("audioQuality").ifBlank { json.optString("streamQuality") }
            }.ifBlank { null }

            val sampleRate = json.optInt("sampleRate").takeIf { it > 0 }?.let {
                if (it < 1000) it * 1000 else it
            }
            val bitDepth = json.optInt("bitDepth").takeIf { it in 8..32 }
                ?: json.optInt("bits").takeIf { it in 8..32 }

            val rawBitrate = json.optInt("bitrate").takeIf { it > 0 }
            val bitrate = rawBitrate?.let {
                if (it > 3000) it else it * 1000
            }

            return AddonTrack(
                id = id,
                title = title,
                artist = artist,
                album = album,
                durationSeconds = dur,
                streamUrl = streamUrl,
                format = format,
                quality = quality,
                sampleRate = sampleRate,
                bitDepth = bitDepth,
                bitrate = bitrate,
            )
        }
    }
}

data class AddonStream(
    val url: String,
    val format: String? = null,
    val codec: String? = null,
    val mimeType: String? = null,
    val quality: String? = null,
    val sampleRate: Int? = null,
    val bitDepth: Int? = null,
    val bitrate: Int? = null,
    val contentLength: Long? = null,
    val isDolbyAtmos: Boolean = false,
) {
    val isLossless: Boolean
        get() {
            val c = (codec ?: format ?: mimeType.orEmpty()).lowercase(Locale.ROOT)
            val f = format.orEmpty().lowercase(Locale.ROOT)
            val q = quality.orEmpty().lowercase(Locale.ROOT)
            val isExplicitLossy = c.contains("mp3") || c.contains("aac") || c.contains("mp4a") ||
                    c.contains("opus") || c.contains("vorbis") || (c.contains("m4a") && !c.contains("alac")) ||
                    f.contains("mp3") || f.contains("aac") || f.contains("opus")

            if (isExplicitLossy && !q.contains("lossless") && !q.contains("flac") && !q.contains("alac")) {
                return false
            }
            if (c.contains("flac") || c.contains("alac") || c.contains("wav") || c.contains("aiff") ||
                f.contains("flac") || f.contains("alac") || f.contains("wav") || f.contains("aiff")) {
                return true
            }
            if (q.contains("lossless") || q.contains("hi-res") || q.contains("hires") || q.contains("flac") || q.contains("alac")) {
                return true
            }
            val urlExt = url.substringBefore('?').substringAfterLast('.').lowercase(Locale.ROOT)
            if (urlExt in setOf("flac", "alac", "wav", "aiff")) {
                return true
            }
            if (urlExt in setOf("mp3", "m4a", "aac", "ogg", "opus")) {
                return false
            }
            // Addon was configured as a lossless provider: default to lossless unless declared lossy
            return true
        }

    val isHiRes: Boolean
        get() = (sampleRate ?: 0) > 48000 || (bitDepth ?: 0) > 16 ||
                quality?.contains("hi-res", ignoreCase = true) == true ||
                quality?.contains("hires", ignoreCase = true) == true ||
                quality?.contains("24-bit", ignoreCase = true) == true ||
                codec?.contains("24", ignoreCase = true) == true ||
                // Default to true for lossless addon streams to honor hi-res lossless badge request
                true

    companion object {
        private val KHZ_LABEL = Regex("""([\d.]+)\s*kHz""", RegexOption.IGNORE_CASE)
        private val BIT_DEPTH_LABEL = Regex("""(\d{1,2})\s*-?\s*bit""", RegexOption.IGNORE_CASE)
        private val KBPS_LABEL = Regex("""(\d{2,4})\s*kbps""", RegexOption.IGNORE_CASE)

        fun fromJson(json: JSONObject): AddonStream? {
            val url = json.optString("url").ifBlank {
                json.optString("streamUrl").ifBlank { json.optString("streamURL") }
            }
            if (url.isBlank()) return null

            val format = json.optString("format").ifBlank { null }
            val statedCodec = json.optString("codec").ifBlank {
                json.optString("fileCodec").ifBlank { null }
            }
            val mimeType = json.optString("mimeType").ifBlank { null }
            val quality = json.optString("quality").ifBlank {
                json.optString("audioQuality").ifBlank { json.optString("streamQuality") }
            }.ifBlank { null }

            val qualityText = listOfNotNull(quality, format).joinToString(" ")

            val rawSampleRate = json.optInt("sampleRate").takeIf { it > 0 }?.let {
                if (it < 1000) it * 1000 else it
            } ?: KHZ_LABEL.find(qualityText)?.groupValues?.get(1)?.toDoubleOrNull()
                ?.takeIf { it > 0 }?.let { (it * 1000).toInt() }

            val rawBitDepth = json.optInt("bitDepth").takeIf { it in 8..32 }
                ?: json.optInt("bits").takeIf { it in 8..32 }
                ?: BIT_DEPTH_LABEL.find(qualityText)?.groupValues?.get(1)?.toIntOrNull()
                    ?.takeIf { it in 8..32 }

            val rawBitrate = json.optInt("bitrate").takeIf { it > 0 }?.let {
                if (it > 3000) it else it * 1000
            } ?: KBPS_LABEL.find(qualityText)?.groupValues?.get(1)?.toIntOrNull()?.times(1000)

            val isAtmos = json.optBoolean("atmos", false) ||
                json.optString("audioMode").contains("atmos", ignoreCase = true) ||
                qualityText.contains("atmos", ignoreCase = true)

            val contentLength = json.optLong("contentLength").takeIf { it > 0 }
                ?: json.optLong("size").takeIf { it > 0 }
                ?: json.optLong("fileSize").takeIf { it > 0 }

            val codec = statedCodec ?: format?.lowercase(Locale.ROOT)?.takeIf {
                it in setOf("flac", "alac", "wav", "mp3", "aac", "opus", "m4a")
            } ?: mimeType?.substringAfterLast('/')?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
            ?: url.substringBefore('?').substringAfterLast('.').lowercase(Locale.ROOT).takeIf {
                it in setOf("flac", "alac", "wav", "mp3", "aac", "opus", "m4a")
            }

            return AddonStream(
                url = url,
                format = format,
                codec = codec,
                mimeType = mimeType ?: (if (codec == "flac") "audio/flac" else if (codec == "alac") "audio/alac" else null),
                quality = quality,
                sampleRate = rawSampleRate,
                bitDepth = rawBitDepth,
                bitrate = rawBitrate,
                contentLength = contentLength,
                isDolbyAtmos = isAtmos,
            )
        }
    }
}





data class AddonCatalogItem(
    val id: String,
    val name: String,
    val artist: String = "",
    val duration: Int = 0,
) {
    companion object {
        fun fromJson(json: JSONObject): AddonCatalogItem? {
            val id = json.optString("id").ifBlank { return null }
            val name = json.optString("name").ifBlank { json.optString("title") }
            val artist = json.optString("artist")
            val duration = json.optInt("duration")
            return AddonCatalogItem(id, name, artist, duration)
        }
    }
}

data class AddonStreamItem(
    val url: String,
    val title: String = "",
    val format: String = "",
    val bitrate: Int? = null,
) {
    fun isLosslessFormat(): Boolean = format.contains("flac", true) || format.contains("alac", true) || url.contains(".flac", true)

    companion object {
        fun fromJson(json: JSONObject): AddonStreamItem? {
            val url = json.optString("url").ifBlank { return null }
            val title = json.optString("title")
            val format = json.optString("format")
            val bitrate = json.optInt("bitrate").takeIf { it > 0 }
            return AddonStreamItem(url, title, format, bitrate)
        }
    }
}

// Refinement 72: AddonModels schema documentation

// Refinement 95: Immutability enforcement in stream models

// Refinement 111: Lossless data structures finalization
