/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mudassir131.yt.App
import com.mudassir131.yt.models.BroadcastMessage
import com.mudassir131.yt.models.BroadcastTag
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BroadcastManager {
    private const val TAG = "BroadcastManager"
    private val BroadcastJsonStorageKey = stringPreferencesKey("broadcast_messages_json")
    private val DeveloperAuthTokenKey = booleanPreferencesKey("developer_authenticated_session")
    private val UserReactionsJsonKey = stringPreferencesKey("broadcast_user_reactions_json")

    // Default Developer Master Passkey (255242)
    private val ValidDevPasskeys = setOf("255242", "nocturne2026")

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

    private val defaultAnnouncements = listOf(
        BroadcastMessage(
            id = "msg-welcome-01",
            authorName = "Mudassir",
            authorRole = "Lead Developer • Nocturne",
            title = "Welcome to Nocturne Broadcast! 🚀",
            content = "Hey everyone! 👋 Welcome to the official Nocturne Announcements channel.\n\nHere you'll get direct updates on upcoming features, releases, hotfixes, and changelogs straight from the developer. React to this message to show your excitement!",
            tag = BroadcastTag.ANNOUNCEMENT,
            timestamp = System.currentTimeMillis() - 2 * 3600 * 1000L,
            reactions = mapOf("❤️" to 128, "🚀" to 94, "🔥" to 210, "🎉" to 77)
        ),
        BroadcastMessage(
            id = "msg-update-02",
            authorName = "Mudassir",
            authorRole = "Lead Developer • Nocturne",
            title = "Check for Update & Android 16 Loading Icon! ✨",
            content = "We have added a brand new dedicated **'Check for Update'** screen in Settings with real-time GitHub Releases integration and the iconic **Android 16 / Pixel Material 3 Expressive morphing flower loader**!\n\nMake sure to check it out in Settings -> Check for update.",
            tag = BroadcastTag.UPDATE,
            actionText = "Open Check for Update",
            actionUrl = "nocturne://settings/update",
            timestamp = System.currentTimeMillis() - 45 * 60 * 1000L,
            reactions = mapOf("🔥" to 342, "❤️" to 189, "🚀" to 256, "💯" to 95)
        )
    )

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
            }
            _messages.value = defaultsWithReactions
            saveMessagesToDataStore(defaultsWithReactions)
        } else {
            _messages.value = loaded
        }
    }

    suspend fun syncRemoteAnnouncements() {
        _isLoading.value = true
        try {
            // Optional remote endpoint from GitHub raw or Gist if online
            val rawUrl = "https://raw.githubusercontent.com/mudassir131-dev/nocturne/main/announcements.json"
            val response = runCatching {
                client.get(rawUrl).bodyAsText()
            }.getOrNull()

            if (!response.isNullOrBlank()) {
                val userReactionsJson = App.instance.dataStore.getAsync(UserReactionsJsonKey)
                val userReactionsMap = parseUserReactionsMap(userReactionsJson)
                val remoteList = parseBroadcastMessages(response, userReactionsMap)
                if (remoteList.isNotEmpty()) {
                    // Merge remote with local posts
                    val combined = (remoteList + _messages.value).distinctBy { it.id }
                        .sortedBy { it.timestamp }
                    _messages.value = combined
                    saveMessagesToDataStore(combined)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Remote sync skipped: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    fun verifyAndLoginDeveloper(passkey: String): Boolean {
        val trimmed = passkey.trim().lowercase()
        val isValid = trimmed in ValidDevPasskeys || trimmed == "nocturne2026"
        if (isValid) {
            _isDeveloperMode.value = true
            scope.launch {
                App.instance.dataStore.edit { it[DeveloperAuthTokenKey] = true }
            }
        }
        return isValid
    }

    fun logoutDeveloper() {
        _isDeveloperMode.value = false
        scope.launch {
            App.instance.dataStore.edit { it[DeveloperAuthTokenKey] = false }
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
    ) {
        val newMsg = BroadcastMessage(
            id = "msg-" + UUID.randomUUID().toString().take(8),
            authorName = "Mudassir",
            authorRole = "Developer • Nocturne",
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

        val updatedList = _messages.value + newMsg
        _messages.value = updatedList
        scope.launch {
            saveMessagesToDataStore(updatedList)
        }
    }

    fun deleteAnnouncement(messageId: String) {
        val updatedList = _messages.value.filterNot { it.id == messageId }
        _messages.value = updatedList
        scope.launch {
            saveMessagesToDataStore(updatedList)
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        val currentList = _messages.value
        val updatedList = currentList.map { msg ->
            if (msg.id == messageId) {
                val previousReaction = msg.userReactions.firstOrNull()
                val isSameReaction = previousReaction == emoji

                val mutReactions = msg.reactions.toMutableMap()

                // Remove previous reaction count if any
                if (previousReaction != null) {
                    val prevCount = mutReactions[previousReaction] ?: 0
                    if (prevCount <= 1) {
                        mutReactions.remove(previousReaction)
                    } else {
                        mutReactions[previousReaction] = prevCount - 1
                    }
                }

                // If user selected a new reaction, add it
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

    private suspend fun saveMessagesToDataStore(messages: List<BroadcastMessage>) {
        runCatching {
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
                    authorRole = obj.optString("authorRole", "Developer • Nocturne"),
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
