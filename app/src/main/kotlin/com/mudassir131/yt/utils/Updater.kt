/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.utils

import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.mudassir131.yt.App
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.constants.GitHubReleasesEtagKey
import com.mudassir131.yt.constants.GitHubReleasesFingerprintKey
import com.mudassir131.yt.constants.GitHubReleasesJsonKey
import com.mudassir131.yt.constants.GitHubReleasesLastCheckedAtKey
import com.mudassir131.yt.constants.LatestReleaseJsonKey
import com.mudassir131.yt.constants.LatestReleaseTagKey
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.json.JSONArray
import org.json.JSONObject

data class GitCommit(
    val sha: String,
    val message: String,
    val author: String,
    val date: String,
    val url: String
)

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String?,
    val publishedAt: String,
    val htmlUrl: String,
    val browserDownloadUrl: String,
    val assets: List<ReleaseAsset> = emptyList(),
)

data class ReleaseAsset(
    val name: String,
    val browserDownloadUrl: String,
    val contentType: String,
    val size: Long,
)

private data class ReleasesNetworkResult(
    val status: HttpStatusCode,
    val body: String?,
    val etag: String?,
)

object Updater {
    const val GenericReleaseNotes = "• Production-grade playback improvements and automatic failure recovery\n• Refined default Cinematic Player layout\n• Bug and crash fixes"

    private val client = HttpClient()
    private const val ReleaseCacheCheckIntervalMs: Long = 6 * 60 * 60 * 1000L
    private var hasCheckedThisSession = false
    private var cachedReleaseInfo: ReleaseInfo? = null
    var lastCheckTime = -1L
        private set

    private fun parseReleasesJson(
        json: String,
    ): List<ReleaseInfo> {
        val jsonArray = JSONArray(json)
        val releases = ArrayList<ReleaseInfo>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            if (!item.optBoolean("draft", false) && !item.optBoolean("prerelease", false)) {
                releases.add(parseReleaseJson(item, requireCompatibleApk = false))
            }
        }
        return releases
    }

    private fun getTopReleaseFingerprint(releases: List<ReleaseInfo>): String {
        val latest = releases.firstOrNull() ?: return ""
        return listOf(
            latest.tagName,
            latest.name,
            latest.publishedAt,
            latest.body.orEmpty(),
            latest.htmlUrl,
        ).joinToString("||")
    }

    private suspend fun fetchReleasesNetwork(
        perPage: Int,
        cachedEtag: String?,
    ): ReleasesNetworkResult {
        val response: HttpResponse =
            client.get("https://api.github.com/repos/mudassir131-dev/nocturne/releases?per_page=$perPage") {
                headers {
                    append("Accept", "application/vnd.github+json")
                    append("User-Agent", "Nocturne")
                    if (!cachedEtag.isNullOrBlank()) {
                        append("If-None-Match", cachedEtag)
                    }
                }
            }
        val etag = response.headers["ETag"]
        return when (response.status) {
            HttpStatusCode.NotModified ->
                ReleasesNetworkResult(
                    status = response.status,
                    body = null,
                    etag = cachedEtag ?: etag,
                )

            else ->
                ReleasesNetworkResult(
                    status = response.status,
                    body = response.bodyAsText(),
                    etag = etag,
                )
        }
    }

    suspend fun getCachedReleases(): List<ReleaseInfo> {
        val cachedJson = App.instance.dataStore.getAsync(GitHubReleasesJsonKey)
        return cachedJson
            ?.takeIf { it.isNotBlank() }
            ?.let { runCatching { parseReleasesJson(it) }.getOrNull() }
            ?: emptyList()
    }

    private fun parseReleaseAssets(item: JSONObject): List<ReleaseAsset> {
        val jsonAssets = item.optJSONArray("assets") ?: return emptyList()
        return buildList {
            for (index in 0 until jsonAssets.length()) {
                val asset = jsonAssets.optJSONObject(index) ?: continue
                val name = asset.optString("name", "").trim()
                val url = asset.optString("browser_download_url", "").trim()
                if (name.isNotBlank() && url.startsWith("https://")) {
                    add(
                        ReleaseAsset(
                            name = name,
                            browserDownloadUrl = url,
                            contentType = asset.optString("content_type", ""),
                            size = asset.optLong("size", 0L),
                        )
                    )
                }
            }
        }
    }

    internal fun selectCompatibleApk(
        assets: List<ReleaseAsset>,
        supportedAbis: List<String> = runCatching { Build.SUPPORTED_ABIS?.toList().orEmpty() }.getOrDefault(emptyList()),
        packagedArchitecture: String = BuildConfig.ARCHITECTURE,
    ): ReleaseAsset? {
        val releaseApks = assets.filter { asset ->
            val name = asset.name.lowercase()
            name.endsWith(".apk") &&
                !name.contains("debug") &&
                !name.contains("unsigned") &&
                (name.contains("release") || name.startsWith("nocturne-"))
        }
        if (releaseApks.isEmpty()) return null

        val requestedAbis = buildList {
            addAll(supportedAbis.map(String::lowercase))
            when (packagedArchitecture.lowercase()) {
                "arm64" -> add("arm64-v8a")
                "armeabi" -> add("armeabi-v7a")
                "x86_64" -> add("x86_64")
                "x86" -> add("x86")
            }
        }.distinct()

        fun matchesAbi(name: String, abi: String): Boolean = when (abi) {
            "arm64-v8a" -> name.contains("arm64-v8a") || name.contains("arm64")
            "armeabi-v7a" -> name.contains("armeabi-v7a") || name.contains("armeabi")
            "x86_64" -> name.contains("x86_64")
            "x86" -> name.contains("x86") && !name.contains("x86_64")
            else -> false
        }

        requestedAbis.forEach { abi ->
            releaseApks.firstOrNull { matchesAbi(it.name.lowercase(), abi) }?.let { return it }
        }
        return releaseApks.firstOrNull { it.name.contains("universal", ignoreCase = true) }
    }

    private fun parseReleaseJson(
        item: JSONObject,
        requireCompatibleApk: Boolean,
    ): ReleaseInfo {
        val tagName = item.optString("tag_name", "")
        if (tagName.isBlank()) {
            throw IllegalArgumentException("Missing tag_name")
        }
        if (item.optBoolean("draft", false) || item.optBoolean("prerelease", false)) {
            throw IllegalArgumentException("Release is a draft or pre-release")
        }
        
        val name = item.optString("name", "")
        val body = if (item.has("body")) item.optString("body") else null
        val publishedAt = item.optString("published_at", "")
        val htmlUrl = item.optString("html_url", "")
        
        val assets = parseReleaseAssets(item)
        val selectedAsset = selectCompatibleApk(assets)
        if (requireCompatibleApk && selectedAsset == null) {
            throw IllegalArgumentException("No valid APK assets found in release")
        }
        
        return ReleaseInfo(
            tagName = tagName,
            name = name.ifBlank { tagName },
            body = body,
            publishedAt = publishedAt,
            htmlUrl = htmlUrl,
            browserDownloadUrl = selectedAsset?.browserDownloadUrl.orEmpty(),
            assets = assets,
        )
    }

    private fun parseSingleReleaseJson(item: JSONObject): ReleaseInfo =
        parseReleaseJson(item, requireCompatibleApk = true)

    suspend fun getLatestVersionName(): Result<String> =
        getLatestReleaseInfo().map { latest -> latest.tagName }

    suspend fun getLatestReleaseNotes(): Result<String?> =
        getLatestReleaseInfo().map { it.body }

    suspend fun getLatestReleaseInfo(): Result<ReleaseInfo> {
        val cached = cachedReleaseInfo
        if (hasCheckedThisSession && cached != null) {
            Log.d("NocturneUpdater", "Update check skipped: already checked this session. Cache hit.")
            return Result.success(cached)
        }

        Log.d("NocturneUpdater", "Update check started. Fetching latest release...")
        val networkResult = runCatching {
            val response: HttpResponse = client.get("https://api.github.com/repos/mudassir131-dev/nocturne/releases/latest") {
                headers {
                    append("Accept", "application/vnd.github+json")
                    append("User-Agent", "Nocturne")
                }
            }
            if (response.status.value !in 200..299) {
                throw IllegalStateException("Failed to fetch latest release: HTTP ${response.status.value}")
            }
            val bodyText = response.bodyAsText()
            Log.d("NocturneUpdater", "GitHub API response received successfully.")
            
            val item = JSONObject(bodyText)
            val parsedInfo = parseSingleReleaseJson(item)
            
            // Save to persistent cache
            runCatching {
                App.instance.dataStore.edit { prefs ->
                    prefs[LatestReleaseJsonKey] = bodyText
                    prefs[LatestReleaseTagKey] = parsedInfo.tagName
                }
                Log.d("NocturneUpdater", "Latest release payload cached to DataStore.")
            }.onFailure { e ->
                Log.e("NocturneUpdater", "Failed to cache latest release payload: ${e.message}")
            }
            
            cachedReleaseInfo = parsedInfo
            hasCheckedThisSession = true
            
            Log.d("NocturneUpdater", "Latest release retrieved from network. Version: ${parsedInfo.tagName}, Asset: ${parsedInfo.browserDownloadUrl}")
            parsedInfo
        }

        return networkResult.recoverCatching { networkError ->
            Log.w("NocturneUpdater", "Latest release network request failed: ${networkError.message}. Accessing local cache...")
            
            val cachedJson = App.instance.dataStore.getAsync(LatestReleaseJsonKey)
            if (!cachedJson.isNullOrBlank()) {
                val item = JSONObject(cachedJson)
                val parsedInfo = parseSingleReleaseJson(item)
                val cachedTag = App.instance.dataStore.getAsync(LatestReleaseTagKey)
                if (!cachedTag.isNullOrBlank() && cachedTag != parsedInfo.tagName) {
                    throw IllegalStateException("Cached release tag does not match cached payload")
                }

                // Migrates the legacy JSON-only cache without introducing a reset loop.
                if (cachedTag.isNullOrBlank()) {
                    App.instance.dataStore.edit { it[LatestReleaseTagKey] = parsedInfo.tagName }
                }
                
                // Cache locally in memory for this session as well
                cachedReleaseInfo = parsedInfo
                hasCheckedThisSession = true
                
                Log.d("NocturneUpdater", "Latest release retrieved from local cache. Version: ${parsedInfo.tagName}, Asset: ${parsedInfo.browserDownloadUrl}")
                parsedInfo
            } else {
                Log.w("NocturneUpdater", "Local cache miss: no cached release info available.")
                throw networkError
            }
        }
    }

    suspend fun getCommitHistory(count: Int = 20, branch: String = "dev"): Result<List<GitCommit>> =
        runCatching {
            val response =
                client.get("https://api.github.com/repos/mudassir131-dev/nocturne/commits?sha=$branch&per_page=$count")
                    .bodyAsText()
            val jsonArray = JSONArray(response)
            val commits = mutableListOf<GitCommit>()
            for (i in 0 until jsonArray.length()) {
                val commitObj = jsonArray.getJSONObject(i)
                val commit = commitObj.getJSONObject("commit")
                val authorObj = commit.optJSONObject("author")
                commits.add(
                    GitCommit(
                        sha = commitObj.optString("sha", "").take(7),
                        message = commit.optString("message", "").lines().firstOrNull() ?: "",
                        author = authorObj?.optString("name", "Unknown") ?: "Unknown",
                        date = authorObj?.optString("date", "") ?: "",
                        url = commitObj.optString("html_url", "")
                    )
                )
            }
            commits
        }

    suspend fun getAllReleases(
        perPage: Int = 30,
        forceRefresh: Boolean = false,
    ): Result<List<ReleaseInfo>> =
        runCatching {
            val now = System.currentTimeMillis()
            val cachedJson = App.instance.dataStore.getAsync(GitHubReleasesJsonKey)
            val cachedEtag = App.instance.dataStore.getAsync(GitHubReleasesEtagKey)
            val lastCheckedAt = App.instance.dataStore.getAsync(GitHubReleasesLastCheckedAtKey, 0L)
            val cachedFingerprint = App.instance.dataStore.getAsync(GitHubReleasesFingerprintKey)

            val cachedReleases =
                cachedJson
                    ?.takeIf { it.isNotBlank() }
                    ?.let { runCatching { parseReleasesJson(it) }.getOrNull() }

            val shouldCheckNetwork =
                forceRefresh || cachedJson.isNullOrBlank() || (now - lastCheckedAt) >= ReleaseCacheCheckIntervalMs

            if (!shouldCheckNetwork) {
                lastCheckTime = now
                return@runCatching cachedReleases ?: emptyList()
            }

            val networkResult = runCatching {
                fetchReleasesNetwork(
                    perPage = perPage,
                    cachedEtag = cachedEtag,
                )
            }.getOrNull()

            if (networkResult == null) {
                val fallback = cachedReleases
                if (fallback != null) {
                    lastCheckTime = now
                    return@runCatching fallback
                }
                throw IllegalStateException("Failed to fetch releases")
            }

            when {
                networkResult.status == HttpStatusCode.NotModified -> {
                    App.instance.dataStore.edit { settings ->
                        settings[GitHubReleasesLastCheckedAtKey] = now
                        networkResult.etag?.let { settings[GitHubReleasesEtagKey] = it }
                    }
                    val fallback = cachedReleases
                    if (fallback != null) {
                        lastCheckTime = now
                        return@runCatching fallback
                    }
                    throw IllegalStateException("Release cache is empty")
                }

                networkResult.status.value in 200..299 && !networkResult.body.isNullOrBlank() -> {
                    val networkBody = networkResult.body
                    val releases = parseReleasesJson(networkBody)
                    val newFingerprint = getTopReleaseFingerprint(releases)
                    val hasPayloadChanged = cachedJson != networkBody
                    val hasTopReleaseChanged = cachedFingerprint != newFingerprint

                    App.instance.dataStore.edit { settings ->
                        settings[GitHubReleasesLastCheckedAtKey] = now
                        networkResult.etag?.let { settings[GitHubReleasesEtagKey] = it }
                        if (hasPayloadChanged || hasTopReleaseChanged || cachedJson.isNullOrBlank()) {
                            settings[GitHubReleasesJsonKey] = networkBody
                            settings[GitHubReleasesFingerprintKey] = newFingerprint
                        }
                    }
                    lastCheckTime = now
                    releases
                }

                else -> {
                    val fallback = cachedReleases
                    if (fallback != null) {
                        lastCheckTime = now
                        fallback
                    } else {
                        throw IllegalStateException("Failed to fetch releases: HTTP ${networkResult.status.value}")
                    }
                }
            }
        }
}
