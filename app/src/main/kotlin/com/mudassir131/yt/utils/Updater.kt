/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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
    const val GenericReleaseNotes = """### 🎵 What's New in Nocturne v2.22.34

* **In-App Updater & Smooth Progress** — Direct in-app streaming downloads with a player-style thick smooth progress slider and one-tap package installation.
* **Storage & Clean Up** — Easily delete previously downloaded APK files directly within the app.
* **Compact Update Dialog & Fixes** — Minimalist centered update card with direct check-for-updates navigation and beautifully rendered changelogs."""

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

    suspend fun getLatestVersionName(forceRefresh: Boolean = false): Result<String> =
        getLatestReleaseInfo(forceRefresh).map { latest -> latest.tagName }

    suspend fun getLatestReleaseNotes(forceRefresh: Boolean = false): Result<String?> =
        getLatestReleaseInfo(forceRefresh).map { it.body }

    suspend fun getLatestReleaseInfo(forceRefresh: Boolean = false): Result<ReleaseInfo> {
        val cached = cachedReleaseInfo
        if (!forceRefresh && hasCheckedThisSession && cached != null) {
            Log.d("NocturneUpdater", "Update check skipped: already checked this session. Cache hit.")
            return Result.success(cached)
        }

        Log.d("NocturneUpdater", "Update check started (forceRefresh=$forceRefresh). Fetching latest release...")
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

    fun getDownloadedApks(context: Context): List<DownloadedApkInfo> {
        val results = mutableListOf<DownloadedApkInfo>()
        val dirsToScan = listOfNotNull(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.cacheDir,
            context.externalCacheDir,
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }.getOrNull()
        )

        for (dir in dirsToScan) {
            if (!dir.exists() || !dir.isDirectory) continue
            val files = dir.listFiles() ?: continue
            for (file in files) {
                val name = file.name.lowercase()
                if (file.isFile && name.endsWith(".apk") && (name.contains("nocturne") || name.startsWith("app-"))) {
                    results.add(
                        DownloadedApkInfo(
                            file = file,
                            name = file.name,
                            sizeBytes = file.length(),
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
        return results.distinctBy { it.file.absolutePath }
    }

    fun deleteDownloadedApks(context: Context): Pair<Int, Long> {
        val apks = getDownloadedApks(context)
        var deletedCount = 0
        var freedBytes = 0L
        for (apk in apks) {
            val len = apk.sizeBytes
            if (apk.file.delete()) {
                deletedCount++
                freedBytes += len
            }
        }
        return Pair(deletedCount, freedBytes)
    }

    suspend fun downloadApkWithProgress(
        downloadUrl: String,
        destinationFile: File,
        onProgress: (bytesRead: Long, totalBytes: Long, progressFraction: Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirectCount = 0
            while (true) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("User-Agent", "Nocturne")
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!newUrl.isNullOrBlank() && redirectCount < 10) {
                        currentUrl = newUrl
                        redirectCount++
                        continue
                    } else {
                        throw IOException("Too many redirects: $responseCode")
                    }
                } else if (responseCode !in 200..299) {
                    connection.disconnect()
                    throw IOException("HTTP Error: $responseCode")
                }
                break
            }

            val totalLength = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            destinationFile.parentFile?.mkdirs()
            if (destinationFile.exists()) destinationFile.delete()

            var bytesCopied = 0L
            val buffer = ByteArray(16384)
            connection.inputStream.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    var bytes = input.read(buffer)
                    while (bytes >= 0) {
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        val fraction = if (totalLength > 0) (bytesCopied.toFloat() / totalLength.toFloat()).coerceIn(0f, 1f) else 0f
                        onProgress(bytesCopied, totalLength, fraction)
                        bytes = input.read(buffer)
                    }
                }
            }
            destinationFile
        }
    }

    fun installApk(context: Context, apkFile: File): Boolean {
        if (!apkFile.exists()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return false
        }

        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.FileProvider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("NocturneUpdater", "Failed to launch package installer", e)
            false
        }
    }
}

data class DownloadedApkInfo(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val lastModified: Long
)
