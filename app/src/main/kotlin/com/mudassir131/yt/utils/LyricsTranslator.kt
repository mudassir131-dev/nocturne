/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import timber.log.Timber
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

object LyricsTranslator {
    private const val TAG = "LyricsTranslator"
    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val TIMESTAMP_REGEX = Regex("^((?:\\[[0-9]{2}:[0-9]{2}(?:\\.[0-9]+)?\\])+)")

    private val LANGUAGE_CODE_MAP = mapOf(
        "AFRIKAANS" to "af",
        "ALBANIAN" to "sq",
        "AMHARIC" to "am",
        "ARABIC" to "ar",
        "ARMENIAN" to "hy",
        "AZERBAIJANI" to "az",
        "BASQUE" to "eu",
        "BELARUSIAN" to "be",
        "BENGALI" to "bn",
        "BOSNIAN" to "bs",
        "BULGARIAN" to "bg",
        "BURMESE" to "my",
        "CATALAN" to "ca",
        "CEBUANO" to "ceb",
        "CHICHEWA" to "ny",
        "CHINESE" to "zh-CN",
        "CHINESE_SIMPLIFIED" to "zh-CN",
        "CHINESE_TRADITIONAL" to "zh-TW",
        "CORSICAN" to "co",
        "CROATIAN" to "hr",
        "CZECH" to "cs",
        "DANISH" to "da",
        "DUTCH" to "nl",
        "ENGLISH" to "en",
        "ESPERANTO" to "eo",
        "ESTONIAN" to "et",
        "FILIPINO" to "tl",
        "FINNISH" to "fi",
        "FRENCH" to "fr",
        "FRISIAN" to "fy",
        "GALICIAN" to "gl",
        "GEORGIAN" to "ka",
        "GERMAN" to "de",
        "GREEK" to "el",
        "GUJARATI" to "gu",
        "HAITIAN_CREOLE" to "ht",
        "HATIAN_CREOLE" to "ht",
        "HAUSA" to "ha",
        "HAWAIIAN" to "haw",
        "HEBREW" to "he",
        "HEBREW_HE" to "he",
        "HEBREW_IW" to "iw",
        "HINDI" to "hi",
        "HMONG" to "hmn",
        "HUNGARIAN" to "hu",
        "ICELANDIC" to "is",
        "IGBO" to "ig",
        "INDONESIAN" to "id",
        "IRISH" to "ga",
        "ITALIAN" to "it",
        "JAPANESE" to "ja",
        "JAVANESE" to "jv",
        "KANNADA" to "kn",
        "KAZAKH" to "kk",
        "KHMER" to "km",
        "KOREAN" to "ko",
        "KURDISH_KURMANJI" to "ku",
        "KYRGYZ" to "ky",
        "LAO" to "lo",
        "LATIN" to "la",
        "LATVIAN" to "lv",
        "LITHUANIAN" to "lt",
        "LUXEMBOURGISH" to "lb",
        "MACEDONIAN" to "mk",
        "MALAGASY" to "mg",
        "MALAY" to "ms",
        "MALAYALAM" to "ml",
        "MALTESE" to "mt",
        "MAORI" to "mi",
        "MARATHI" to "mr",
        "MONGOLIAN" to "mn",
        "MYANMAR_BURMESE" to "my",
        "NEPALI" to "ne",
        "NORWEGIAN" to "no",
        "ODIA" to "or",
        "PASHTO" to "ps",
        "PERSIAN" to "fa",
        "POLISH" to "pl",
        "PORTUGUESE" to "pt",
        "PUNJABI" to "pa",
        "ROMANIAN" to "ro",
        "RUSSIAN" to "ru",
        "SAMOAN" to "sm",
        "SCOTS_GAELIC" to "gd",
        "SERBIAN" to "sr",
        "SESOTHO" to "st",
        "SHONA" to "sn",
        "SINDHI" to "sd",
        "SINHALA" to "si",
        "SLOVAK" to "sk",
        "SLOVENIAN" to "sl",
        "SOMALI" to "so",
        "SPANISH" to "es",
        "SUDANESE" to "su",
        "SWAHILI" to "sw",
        "SWEDISH" to "sv",
        "TAJIK" to "tg",
        "TAMIL" to "ta",
        "TELUGU" to "te",
        "THAI" to "th",
        "TURKISH" to "tr",
        "UKRAINIAN" to "uk",
        "URDU" to "ur",
        "UYGHUR" to "ug",
        "UZBEK" to "uz",
        "VIETNAMESE" to "vi",
        "WELSH" to "cy",
        "XHOSA" to "xh",
        "YIDDISH" to "yi",
        "YORUBA" to "yo",
        "ZULU" to "zu"
    )

    fun resolveLanguageCode(input: String): String {
        val trimmed = input.trim()
        if (trimmed.length in 2..5 && (trimmed.contains('-') || trimmed.all { it.isLowerCase() })) {
            return trimmed
        }
        val upper = trimmed.uppercase(Locale.ROOT).replace(' ', '_').replace("-", "_")
        return LANGUAGE_CODE_MAP[upper]
            ?: LANGUAGE_CODE_MAP.entries.firstOrNull { it.key.contains(upper) || upper.contains(it.key) }?.value
            ?: trimmed.lowercase(Locale.ROOT)
    }

    suspend fun translateSingleText(text: String, targetLangCode: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text
        val encodedText = URLEncoder.encode(text, "UTF-8")
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLangCode&dt=t&q=$encodedText"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IllegalStateException("Translation request failed with HTTP ${response.code}")
        }
        val responseBody = response.body?.string().orEmpty()
        parseGoogleTranslateResponse(responseBody).ifBlank { text }
    }

    private fun parseGoogleTranslateResponse(jsonString: String): String {
        if (jsonString.isBlank()) return ""
        val root = JSONArray(jsonString)
        val sentences = root.optJSONArray(0) ?: return ""
        val sb = StringBuilder()
        for (i in 0 until sentences.length()) {
            val sentence = sentences.optJSONArray(i) ?: continue
            val part = sentence.optString(0, "")
            sb.append(part)
        }
        return sb.toString()
    }

    suspend fun translateLyrics(lyricsText: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        if (lyricsText.isBlank()) return@withContext lyricsText
        val targetCode = resolveLanguageCode(targetLanguage)

        val rawLines = lyricsText.split("\n")
        val timestamps = mutableListOf<String?>()
        val textContents = mutableListOf<String?>()

        for (line in rawLines) {
            val trimmed = line.trimEnd()
            val match = TIMESTAMP_REGEX.find(trimmed)
            if (match != null) {
                val stamps = match.groupValues[1]
                val content = trimmed.substring(match.range.last + 1).trimStart()
                timestamps.add(stamps)
                textContents.add(if (content.isBlank()) null else content)
            } else {
                timestamps.add(null)
                textContents.add(if (trimmed.isBlank()) null else trimmed)
            }
        }

        val translatableIndices = textContents.indices.filter { textContents[it] != null }
        if (translatableIndices.isEmpty()) return@withContext lyricsText

        val translatedMap = mutableMapOf<Int, String>()
        val maxBatchSize = 35
        val maxBatchChars = 1600

        var cursor = 0
        while (cursor < translatableIndices.size) {
            val batchIndices = mutableListOf<Int>()
            var batchChars = 0

            while (cursor < translatableIndices.size && batchIndices.size < maxBatchSize) {
                val idx = translatableIndices[cursor]
                val len = textContents[idx]!!.length
                if (batchIndices.isEmpty() || batchChars + len <= maxBatchChars) {
                    batchIndices.add(idx)
                    batchChars += len
                    cursor++
                } else break
            }

            val batchTexts = batchIndices.map { textContents[it]!! }
            val joinedText = batchTexts.joinToString("\n")

            val batchTranslated = runCatching {
                translateSingleText(joinedText, targetCode)
            }.getOrNull()

            val translatedLines = batchTranslated?.split("\n")
            if (translatedLines != null && translatedLines.size == batchTexts.size) {
                for (i in batchIndices.indices) {
                    translatedMap[batchIndices[i]] = translatedLines[i]
                }
            } else {