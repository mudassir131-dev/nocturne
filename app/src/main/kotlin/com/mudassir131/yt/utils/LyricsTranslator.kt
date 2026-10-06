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