/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.utils

import androidx.datastore.preferences.core.edit
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.App
import com.mudassir131.yt.constants.GitHubReleasesEtagKey
import com.mudassir131.yt.constants.GitHubReleasesFingerprintKey
import com.mudassir131.yt.constants.GitHubReleasesJsonKey
import com.mudassir131.yt.constants.GitHubReleasesLastCheckedAtKey
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
    val browserDownloadUrl: String
)

private data class ReleasesNetworkResult(
    val status: HttpStatusCode,
    val body: String?,
    val etag: String?,
)

object Updater {
    private val client = HttpClient()
    private const val ReleaseCacheCheckIntervalMs: Long = 6 * 60 * 60 * 1000L
    var lastCheckTime = -1L
        private set

    private fun parseReleasesJson(
        json: String,
    ): List<ReleaseInfo> {
        val jsonArray = JSONArray(json)
        val releases = ArrayList<ReleaseInfo>(jsonArray.length())
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val assets = item.optJSONArray("assets")
            var downloadUrl = ""
            if (assets != null && assets.length() > 0) {
                val arch = BuildConfig.ARCHITECTURE
                var foundUrl: String? = null
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val assetName = asset.optString("name", "").lowercase()
                    val assetUrl = asset.optString("browser_download_url", "")
                    if (assetName.contains(arch.lowercase())) {
                        foundUrl = assetUrl
                        break
                    }
                }
                if (foundUrl == null) {
                    foundUrl = assets.getJSONObject(0).optString("browser_download_url", "")
                }
                downloadUrl = foundUrl ?: ""
            }
            if (downloadUrl.isEmpty()) {
                downloadUrl = getLatestDownloadUrl()
            }
            releases.add(
                ReleaseInfo(
                    tagName = item.optString("tag_name", ""),
                    name = item.optString("name", ""),
                    body = if (item.has("body")) item.optString("body") else null,
                    publishedAt = item.optString("published_at", ""),
                    htmlUrl = item.optString("html_url", ""),
                    browserDownloadUrl = downloadUrl
                )
            )
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
                    append("User-Agent", "Velune")
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

    suspend fun getLatestVersionName(): Result<String> =
        getLatestReleaseInfo().map { latest ->
            latest.name.ifBlank { latest.tagName }
        }

    suspend fun getLatestReleaseNotes(): Result<String?> =
        getLatestReleaseInfo().map { it.body }

    suspend fun getLatestReleaseInfo(): Result<ReleaseInfo> =
        runCatching {
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
            val item = JSONObject(bodyText)
            
            val tagName = item.optString("tag_name", "")
            val name = item.optString("name", "")
            val body = if (item.has("body")) item.optString("body") else null
            val publishedAt = item.optString("published_at", "")
            val htmlUrl = item.optString("html_url", "")
            
            val assets = item.optJSONArray("assets")
            var downloadUrl = ""
            if (assets != null && assets.length() > 0) {
                val arch = BuildConfig.ARCHITECTURE
                var foundUrl: String? = null
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val assetName = asset.optString("name", "").lowercase()
                    val assetUrl = asset.optString("browser_download_url", "")
                    if (assetName.contains(arch.lowercase())) {
                        foundUrl = assetUrl
                        break
                    }
                }
                if (foundUrl == null) {
                    foundUrl = assets.getJSONObject(0).optString("browser_download_url", "")
                }
                downloadUrl = foundUrl ?: ""
            }
            if (downloadUrl.isEmpty()) {
                downloadUrl = getLatestDownloadUrl()
            }
            
            lastCheckTime = System.currentTimeMillis()
            ReleaseInfo(
                tagName = tagName,
                name = name.ifBlank { tagName },
                body = body,
                publishedAt = publishedAt,
                htmlUrl = htmlUrl,
                browserDownloadUrl = downloadUrl
            )
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

    fun getLatestDownloadUrl(): String {
        val baseUrl = "https://github.com/mudassir131-dev/nocturne/releases/latest/download/"
        val architecture = BuildConfig.ARCHITECTURE
        return if (architecture == "universal") {
            baseUrl + "Nocturne.apk"
        } else {
            baseUrl + "app-${architecture}-release.apk"
        }
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
