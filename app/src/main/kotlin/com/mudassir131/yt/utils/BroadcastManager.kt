/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.mudassir131.yt.App
import com.mudassir131.yt.MainActivity
import com.mudassir131.yt.R
import com.mudassir131.yt.models.BroadcastMessage
import com.mudassir131.yt.models.BroadcastTag
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object BroadcastManager {
    private const val TAG = "BroadcastManager"
    private val BroadcastJsonStorageKey = stringPreferencesKey("broadcast_messages_json")
    private val DeveloperAuthTokenKey = booleanPreferencesKey("developer_authenticated_session")
    val DeveloperGitHubTokenKey = stringPreferencesKey("developer_github_token")
    private val UserReactionsJsonKey = stringPreferencesKey("broadcast_user_reactions_json")

    // Cryptographic one-way hash for developer authentication (No plain text credentials in APK)
    private const val DEV_AUTH_HASH = "1b00ffc4c40487336aebb79993e8b32065616f10a229d6a03970bf69649c528c"

    private val _messages = MutableStateFlow<List<BroadcastMessage>>(emptyList())
    val messages: StateFlow<List<BroadcastMessage>> = _messages.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode: StateFlow<Boolean> = _isDeveloperMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val client = HttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            loadPersistedMessages()
        }
    }

    private val ANNOUNCEMENT_CHANNEL_ID = "nocturne_broadcast_channel"

    private val defaultAnnouncements = listOf(
        BroadcastMessage(
            id = "msg-welcome-01",
            authorName = "Mudassir",
            authorRole = "App developer",
            title = "Welcome to Nocturne Broadcast! 🚀",
            content = "Hey everyone! 👋 Welcome to the official Nocturne Announcements channel.\n\nHere you'll get direct updates on upcoming features, releases, hotfixes, and changelogs straight from the developer. React to this message to show your excitement!",
            tag = BroadcastTag.ANNOUNCEMENT,
            timestamp = System.currentTimeMillis() - 2 * 3600 * 1000L,
            reactions = mapOf("❤️" to 128, "🚀" to 94, "🔥" to 210, "🎉" to 77)
        ),
        BroadcastMessage(
            id = "msg-update-02",
            authorName = "Mudassir",
            authorRole = "App developer",
            title = "Check for Update & Android 16 Loading Icon! ✨",
            content = "We have added a brand new dedicated **'Check for Update'** screen in Settings with real-time GitHub Releases integration and the iconic **Android 16 / Pixel Material 3 Expressive morphing flower loader**!\n\nMake sure to check it out in Settings -> Check for update.",
            tag = BroadcastTag.UPDATE,
            actionText = "Open Check for Update",
            actionUrl = "nocturne://settings/update",
            timestamp = System.currentTimeMillis() - 45 * 60 * 1000L,
            reactions = mapOf("🔥" to 342, "❤️" to 189, "🚀" to 256, "💯" to 95)
        )
    )

    fun showAnnouncementNotification(context: Context, message: BroadcastMessage) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ANNOUNCEMENT_CHANNEL_ID,
                "Nocturne Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for developer broadcasts and announcements"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                enableVibration(true)
            }
            val nm = context.getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "broadcast")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val notificationIcon = if (isDark) R.drawable.ic_nocturne_notification_dark else R.drawable.ic_nocturne_notification_light

        val avatarBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.developer_mudassir)

        val notifBuilder = NotificationCompat.Builder(context, ANNOUNCEMENT_CHANNEL_ID)
            .setSmallIcon(notificationIcon)
            .setLargeIcon(avatarBitmap)
            .setContentTitle(if (message.title.isNotBlank()) message.title else "Announcement from Mudassir")
            .setContentText(message.content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val imageTarget = message.imageUrl ?: message.gifUrl
        if (!imageTarget.isNullOrBlank()) {
            scope.launch(Dispatchers.IO) {
                val bitmap = runCatching {
                    if (imageTarget.startsWith("content://") || imageTarget.startsWith("file://")) {
                        val uri = Uri.parse(imageTarget)
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } else {
                        val req = ImageRequest.Builder(context)
                            .data(imageTarget)
                            .allowHardware(false)
                            .build()
                        val result = context.imageLoader.execute(req)
                        result.image?.toBitmap()
                    }
                }.getOrNull()

                if (bitmap != null) {
                    notifBuilder.setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .setBigContentTitle(if (message.title.isNotBlank()) message.title else "Announcement from Mudassir")
                            .setSummaryText(message.content)
                    )
                } else {
                    notifBuilder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .setBigContentTitle(if (message.title.isNotBlank()) message.title else "Announcement from Mudassir")
                            .bigText(message.content)
                    )
                }

                try {
                    NotificationManagerCompat.from(context).notify(message.id.hashCode(), notifBuilder.build())
                } catch (_: SecurityException) {}
            }
        } else {
            notifBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(if (message.title.isNotBlank()) message.title else "Announcement from Mudassir")
                    .bigText(message.content)
            )
            try {
                NotificationManagerCompat.from(context).notify(message.id.hashCode(), notifBuilder.build())
            } catch (_: SecurityException) {}
        }
    }

    suspend fun loadPersistedMessages() {
        val cachedJson = App.instance.dataStore.getAsync(BroadcastJsonStorageKey)
        val userReactionsJson = App.instance.dataStore.getAsync(UserReactionsJsonKey)
        val devAuth = App.instance.dataStore.getAsync(DeveloperAuthTokenKey) ?: false

        _isDeveloperMode.value = devAuth

        val userReactionsMap = parseUserReactionsMap(userReactionsJson)

        val loaded = if (!cachedJson.isNullOrBlank()) {
            runCatching { parseBroadcastMessages(cachedJson, userReactionsMap) }.getOrNull()
        } else null

        if (loaded.isNullOrEmpty()) {
            val defaultsWithReactions = defaultAnnouncements.map { msg ->
                msg.copy(userReactions = userReactionsMap[msg.id] ?: emptySet())
            }.sortedBy { it.timestamp }
            _messages.value = defaultsWithReactions
            saveMessagesToDataStore(defaultsWithReactions)
        } else {
            _messages.value = loaded.sortedBy { it.timestamp }
        }
    }

    suspend fun getGitHubToken(): String? {
        return App.instance.dataStore.getAsync(DeveloperGitHubTokenKey)
    }

    suspend fun saveGitHubToken(token: String) {
        App.instance.dataStore.edit {
            if (token.isBlank()) {
                it.remove(DeveloperGitHubTokenKey)
            } else {
                it[DeveloperGitHubTokenKey] = token.trim()
            }
        }
    }

    suspend fun syncRemoteAnnouncements() {
        _isLoading.value = true
        try {
            val userReactionsJson = App.instance.dataStore.getAsync(UserReactionsJsonKey)
            val userReactionsMap = parseUserReactionsMap(userReactionsJson)
            val fetchedRemoteMessages = mutableListOf<BroadcastMessage>()
            val timestampNonce = System.currentTimeMillis()

            // 1. Try GitHub Contents API with raw header (Zero Fastly CDN cache, 100% instant)
            val apiContentsUrl = "https://api.github.com/repos/mudassir131-dev/nocturne/contents/announcements.json?ref=main&t=$timestampNonce"
            val apiRawResponse = runCatching {
                client.get(apiContentsUrl) {
                    header("Accept", "application/vnd.github.raw+json")
                    header("User-Agent", "Nocturne-Android")
                    header("Cache-Control", "no-cache")
                    header("Pragma", "no-cache")
                }.bodyAsText()
            }.getOrNull()

            if (!apiRawResponse.isNullOrBlank() && apiRawResponse.trim().startsWith("[")) {
                val list = parseBroadcastMessages(apiRawResponse, userReactionsMap)
                fetchedRemoteMessages.addAll(list)
            }

            // 2. If contents API didn't return, fetch from raw GitHub URL with cache buster
            if (fetchedRemoteMessages.isEmpty()) {
                val rawUrl = "https://raw.githubusercontent.com/mudassir131-dev/nocturne/main/announcements.json?t=$timestampNonce"
                val rawResponse = runCatching {
                    client.get(rawUrl) {
                        header("User-Agent", "Nocturne-Android")
                        header("Cache-Control", "no-cache")
                        header("Pragma", "no-cache")
                    }.bodyAsText()
                }.getOrNull()

                if (!rawResponse.isNullOrBlank() && rawResponse.trim().startsWith("[")) {
                    val list = parseBroadcastMessages(rawResponse, userReactionsMap)
                    fetchedRemoteMessages.addAll(list)
                }
            }

            // 3. Also fetch GitHub Issues labeled 'announcement' (instant web announcements)
            val issuesUrl = "https://api.github.com/repos/mudassir131-dev/nocturne/issues?labels=announcement&state=all&per_page=30&t=$timestampNonce"
            val issuesResponse = runCatching {
                client.get(issuesUrl) {
                    header("Accept", "application/vnd.github+json")
                    header("User-Agent", "Nocturne-Android")
                    header("Cache-Control", "no-cache")
                }.bodyAsText()
            }.getOrNull()

            if (!issuesResponse.isNullOrBlank() && issuesResponse.trim().startsWith("[")) {
                val issueList = parseGitHubIssuesToBroadcastMessages(issuesResponse, userReactionsMap)
                fetchedRemoteMessages.addAll(issueList)
            }

            if (fetchedRemoteMessages.isNotEmpty()) {
                val currentExistingMap = _messages.value.associateBy { it.id }
                val currentExistingIds = currentExistingMap.keys

                val combined = (fetchedRemoteMessages + _messages.value)
                    .distinctBy { it.id }
                    .map { msg ->
                        val localMsg = currentExistingMap[msg.id]
                        val userReactions = userReactionsMap[msg.id]
                            ?: localMsg?.userReactions
                            ?: emptySet()

                        val mergedReactions = msg.reactions.toMutableMap()
                        userReactions.forEach { emoji ->
                            val currentCount = mergedReactions[emoji] ?: 0
                            val localCount = localMsg?.reactions?.get(emoji) ?: 0
                            mergedReactions[emoji] = maxOf(currentCount, localCount, 1)
                        }

                        msg.copy(
                            reactions = mergedReactions,
                            userReactions = userReactions
                        )
                    }
                    .sortedBy { it.timestamp }

                _messages.value = combined
                saveMessagesToDataStore(combined)

                // Trigger heads-up notification for new broadcast messages
                val newItems = fetchedRemoteMessages.filter { it.id !in currentExistingIds }
                if (newItems.isNotEmpty() && currentExistingIds.isNotEmpty()) {
                    newItems.maxByOrNull { it.timestamp }?.let { latest ->
                        showAnnouncementNotification(App.instance, latest)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote sync skipped: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun verifyAndLoginDeveloper(email: String, password: String): Boolean {
        val cleanEmail = email.trim().lowercase(Locale.ROOT)
        val cleanPass = password.trim()
        val payload = "nocturne_dev_v2:$cleanEmail:$cleanPass"
        val computedHash = sha256Hex(payload)
        val isValid = computedHash.equals(DEV_AUTH_HASH, ignoreCase = true)
        if (isValid) {
            _isDeveloperMode.value = true
            scope.launch {
                App.instance.dataStore.edit { it[DeveloperAuthTokenKey] = true }
            }
        }
        return isValid
    }

    private fun sha256Hex(input: String): String {
        return runCatching {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        }.getOrDefault("")
    }

    fun logoutDeveloper() {
        _isDeveloperMode.value = false
        scope.launch {
            App.instance.dataStore.edit {
                it[DeveloperAuthTokenKey] = false
                it.remove(DeveloperGitHubTokenKey)
            }
        }
    }

    fun postAnnouncement(
        title: String,
        content: String,
        imageUrl: String? = null,
        gifUrl: String? = null,
        tag: BroadcastTag = BroadcastTag.ANNOUNCEMENT,
        actionText: String? = null,
        actionUrl: String? = null,
        onPublishResult: ((Result<String>) -> Unit)? = null,
    ) {
        val newMsg = BroadcastMessage(
            id = "msg-" + UUID.randomUUID().toString().take(8),
            authorName = "Mudassir",
            authorRole = "App developer",
            title = title.trim(),
            content = content.trim(),
            imageUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() },
            gifUrl = gifUrl?.trim()?.takeIf { it.isNotBlank() },
            tag = tag,
            actionText = actionText?.trim()?.takeIf { it.isNotBlank() },
            actionUrl = actionUrl?.trim()?.takeIf { it.isNotBlank() },
            timestamp = System.currentTimeMillis(),
            reactions = emptyMap(),
            userReactions = emptySet()
        )

        val updatedList = (_messages.value + newMsg).distinctBy { it.id }.sortedBy { it.timestamp }
        _messages.value = updatedList
        scope.launch {
            saveMessagesToDataStore(updatedList)

            val token = getGitHubToken()
            if (!token.isNullOrBlank()) {
                val cloudResult = publishAnnouncementToGitHub(newMsg, token)
                onPublishResult?.invoke(cloudResult)
            } else {
                onPublishResult?.invoke(Result.success("Saved locally. (Add GitHub token in developer settings to push live to all users)"))
            }
        }
        showAnnouncementNotification(App.instance, newMsg)
    }

    suspend fun publishAnnouncementToGitHub(message: BroadcastMessage, token: String? = null): Result<String> {
        val rawToken = token ?: getGitHubToken()
        if (rawToken.isNullOrBlank()) {
            return Result.failure(IllegalStateException("GitHub Token not configured. Please add your GitHub Personal Access Token in Developer console."))
        }
        val cleanToken = rawToken.trim()
        val authHeaderVal = if (cleanToken.startsWith("ghp_") || cleanToken.startsWith("github_pat_")) {
            "token $cleanToken"
        } else if (cleanToken.startsWith("token ") || cleanToken.startsWith("Bearer ")) {
            cleanToken
        } else {
            "token $cleanToken"
        }

        return runCatching {
            val repoUrl = "https://api.github.com/repos/mudassir131-dev/nocturne/contents/announcements.json"
            val getResponse = client.get(repoUrl) {
                header("Authorization", authHeaderVal)
                header("Accept", "application/vnd.github+json")
                header("User-Agent", "Nocturne-Android")
                header("X-GitHub-Api-Version", "2022-11-28")
            }

            var currentSha: String? = null
            var remoteList = emptyList<BroadcastMessage>()

            if (getResponse.status == HttpStatusCode.OK) {
                val getJson = JSONObject(getResponse.bodyAsText())
                currentSha = getJson.optString("sha").takeIf { it.isNotBlank() }
                val rawContent = getJson.optString("content", "")
                val cleanContent = rawContent.replace("\n", "").replace("\r", "")
                if (cleanContent.isNotBlank()) {
                    val decoded = runCatching { String(Base64.decode(cleanContent, Base64.DEFAULT), Charsets.UTF_8) }.getOrNull()
                    if (!decoded.isNullOrBlank() && decoded.trim().startsWith("[")) {
                        remoteList = parseBroadcastMessages(decoded, emptyMap())
                    }
                }
            }

            val allMessages = (listOf(message) + remoteList + _messages.value)
                .distinctBy { it.id }
                .sortedBy { it.timestamp }

            val jsonArray = serializeMessagesToJson(allMessages)
            val jsonString = jsonArray.toString(2)
            val base64Content = Base64.encodeToString(jsonString.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

            val putBody = JSONObject().apply {
                put("message", "Broadcast: ${message.title.ifBlank { message.content.take(30) }}")
                put("content", base64Content)
                if (!currentSha.isNullOrBlank()) {
                    put("sha", currentSha)
                }
                put("branch", "main")
            }

            val putResponse = client.put(repoUrl) {
                header("Authorization", authHeaderVal)
                header("Accept", "application/vnd.github+json")
                header("User-Agent", "Nocturne-Android")
                header("X-GitHub-Api-Version", "2022-11-28")
                contentType(ContentType.Application.Json)
                setBody(putBody.toString())
            }

            if (putResponse.status.value in 200..299) {
                _messages.value = allMessages
                saveMessagesToDataStore(allMessages)
                "Announcement published live to all users across the world! 🚀"
            } else {
                val errorBody = putResponse.bodyAsText()
                Log.e(TAG, "GitHub API publish failed with ${putResponse.status.value}: $errorBody")
                throw Exception("GitHub API Error (${putResponse.status.value}): $errorBody")
            }
        }
    }

    fun exportAnnouncementsJsonString(): String {
        return serializeMessagesToJson(_messages.value).toString(2)
    }

    fun deleteAnnouncement(messageId: String) {
        val updatedList = _messages.value.filterNot { it.id == messageId }
        _messages.value = updatedList
        scope.launch {
            saveMessagesToDataStore(updatedList)
            val token = getGitHubToken()
            if (!token.isNullOrBlank()) {
                val repoUrl = "https://api.github.com/repos/mudassir131-dev/nocturne/contents/announcements.json"
                val getResponse = runCatching {
                    client.get(repoUrl) {
                        header("Authorization", "Bearer $token")
                        header("Accept", "application/vnd.github+json")
                        header("User-Agent", "Nocturne-Android")
                    }
                }.getOrNull()

                if (getResponse?.status == HttpStatusCode.OK) {
                    val currentSha = JSONObject(getResponse.bodyAsText()).optString("sha")
                    val jsonArray = serializeMessagesToJson(updatedList)
                    val base64Content = Base64.encodeToString(jsonArray.toString(2).toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val putBody = JSONObject().apply {
                        put("message", "Broadcast: Delete announcement $messageId")
                        put("content", base64Content)
                        put("sha", currentSha)
                        put("branch", "main")
                    }
                    client.put(repoUrl) {
                        header("Authorization", "Bearer $token")
                        header("Accept", "application/vnd.github+json")
                        header("User-Agent", "Nocturne-Android")
                        contentType(ContentType.Application.Json)
                        setBody(putBody.toString())
                    }
                }
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val currentList = _messages.value
        val updatedList = currentList.map { msg ->
            if (msg.id == messageId) {
                val previousReaction = msg.userReactions.firstOrNull()
                val isSameReaction = previousReaction == emoji

                val mutReactions = msg.reactions.toMutableMap()

                if (previousReaction != null) {
                    val prevCount = mutReactions[previousReaction] ?: 0
                    if (prevCount <= 1) {
                        mutReactions.remove(previousReaction)
                    } else {
                        mutReactions[previousReaction] = prevCount - 1
                    }
                }

                val newUserReactions = if (!isSameReaction) {
                    val newCount = (mutReactions[emoji] ?: 0) + 1
                    mutReactions[emoji] = newCount
                    setOf(emoji)
                } else {
                    emptySet()
                }

                msg.copy(reactions = mutReactions, userReactions = newUserReactions)
            } else {
                msg
            }
        }

        _messages.value = updatedList
        scope.launch {
            saveMessagesToDataStore(updatedList)
            saveUserReactions(updatedList)
        }
    }

    private fun serializeMessagesToJson(messages: List<BroadcastMessage>): JSONArray {
        val jsonArray = JSONArray()
        messages.forEach { msg ->
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("authorName", msg.authorName)
            obj.put("authorRole", msg.authorRole)
            obj.put("isVerified", msg.isVerified)
            obj.put("title", msg.title)
            obj.put("content", msg.content)
            msg.imageUrl?.let { obj.put("imageUrl", it) }
            msg.gifUrl?.let { obj.put("gifUrl", it) }
            obj.put("tag", msg.tag.name)
            msg.actionText?.let { obj.put("actionText", it) }
            msg.actionUrl?.let { obj.put("actionUrl", it) }
            obj.put("timestamp", msg.timestamp)

            val reactionsObj = JSONObject()
            msg.reactions.forEach { (emoji, count) ->
                reactionsObj.put(emoji, count)
            }
            obj.put("reactions", reactionsObj)
            jsonArray.put(obj)
        }
        return jsonArray
    }

    private suspend fun saveMessagesToDataStore(messages: List<BroadcastMessage>) {
        runCatching {
            val jsonArray = serializeMessagesToJson(messages)
            App.instance.dataStore.edit { prefs ->
                prefs[BroadcastJsonStorageKey] = jsonArray.toString()
            }
        }
    }

    private suspend fun saveUserReactions(messages: List<BroadcastMessage>) {
        runCatching {
            val obj = JSONObject()
            messages.forEach { msg ->
                if (msg.userReactions.isNotEmpty()) {
                    val arr = JSONArray()
                    msg.userReactions.forEach { arr.put(it) }
                    obj.put(msg.id, arr)
                }
            }
            App.instance.dataStore.edit { prefs ->
                prefs[UserReactionsJsonKey] = obj.toString()
            }
        }
    }

    private fun parseUserReactionsMap(jsonStr: String?): Map<String, Set<String>> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(jsonStr)
            val result = mutableMapOf<String, Set<String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = obj.getJSONArray(key)
                val set = mutableSetOf<String>()
                for (i in 0 until arr.length()) {
                    set.add(arr.getString(i))
                }
                result[key] = set
            }
            result
        }.getOrDefault(emptyMap())
    }

    private fun parseGitHubIssuesToBroadcastMessages(
        jsonStr: String,
        userReactionsMap: Map<String, Set<String>>,
    ): List<BroadcastMessage> {
        val list = mutableListOf<BroadcastMessage>()
        val jsonArray = JSONArray(jsonStr)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (i in 0 until jsonArray.length()) {
            val issue = jsonArray.getJSONObject(i)
            // Skip pull requests
            if (issue.has("pull_request")) continue

            val issueId = "gh-issue-" + issue.optLong("id", i.toLong())
            val title = issue.optString("title", "")
            val body = issue.optString("body", "")
            val createdAtStr = issue.optString("created_at", "")
            val time = runCatching { isoFormat.parse(createdAtStr)?.time }.getOrNull() ?: System.currentTimeMillis()

            val userObj = issue.optJSONObject("user")
            val author = userObj?.optString("login", "Mudassir") ?: "Mudassir"

            val reactionsObj = issue.optJSONObject("reactions")
            val reactionsMap = mutableMapOf<String, Int>()
            if (reactionsObj != null) {
                val heart = reactionsObj.optInt("heart", 0)
                val rocket = reactionsObj.optInt("rocket", 0)
                val fire = reactionsObj.optInt("eyes", 0)
                val plusOne = reactionsObj.optInt("+1", 0)
                val tada = reactionsObj.optInt("hooray", 0)

                if (heart > 0) reactionsMap["❤️"] = heart
                if (rocket > 0) reactionsMap["🚀"] = rocket
                if (fire > 0) reactionsMap["🔥"] = fire
                if (plusOne > 0) reactionsMap["👍"] = plusOne
                if (tada > 0) reactionsMap["🎉"] = tada
            }

            val userReactions = userReactionsMap[issueId] ?: emptySet()

            list.add(
                BroadcastMessage(
                    id = issueId,
                    authorName = author,
                    authorRole = "App developer",
                    isVerified = true,
                    title = title,
                    content = body,
                    tag = BroadcastTag.ANNOUNCEMENT,
                    timestamp = time,
                    reactions = reactionsMap,
                    userReactions = userReactions
                )
            )
        }
        return list
    }

    private fun parseBroadcastMessages(
        jsonStr: String,
        userReactionsMap: Map<String, Set<String>>,
    ): List<BroadcastMessage> {
        val jsonArray = JSONArray(jsonStr)
        val list = mutableListOf<BroadcastMessage>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.optString("id", UUID.randomUUID().toString())
            val tagStr = obj.optString("tag", "ANNOUNCEMENT")
            val tag = runCatching { BroadcastTag.valueOf(tagStr) }.getOrDefault(BroadcastTag.ANNOUNCEMENT)

            val reactionsMap = mutableMapOf<String, Int>()
            val reactionsObj = obj.optJSONObject("reactions")
            if (reactionsObj != null) {
                val keys = reactionsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    reactionsMap[k] = reactionsObj.optInt(k, 0)
                }
            }

            val userReactions = userReactionsMap[id] ?: emptySet()

            list.add(
                BroadcastMessage(
                    id = id,
                    authorName = obj.optString("authorName", "Mudassir"),
                    authorRole = obj.optString("authorRole", "App developer"),
                    isVerified = obj.optBoolean("isVerified", true),
                    title = obj.optString("title", ""),
                    content = obj.optString("content", ""),
                    imageUrl = if (obj.has("imageUrl")) obj.optString("imageUrl") else null,
                    gifUrl = if (obj.has("gifUrl")) obj.optString("gifUrl") else null,
                    tag = tag,
                    actionText = if (obj.has("actionText")) obj.optString("actionText") else null,
                    actionUrl = if (obj.has("actionUrl")) obj.optString("actionUrl") else null,
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    reactions = reactionsMap,
                    userReactions = userReactions
                )
            )
        }
        return list
    }
}
