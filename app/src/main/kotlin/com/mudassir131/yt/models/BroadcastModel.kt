/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.models

enum class BroadcastTag(val label: String, val emoji: String) {
    UPDATE("Update", "🚀"),
    FEATURE("New Feature", "✨"),
    ANNOUNCEMENT("Announcement", "📢"),
    HOTFIX("Hotfix", "⚡"),
    GENERAL("Community", "💬")
}

data class BroadcastMessage(
    val id: String,
    val authorName: String = "Mudassir",
    val authorRole: String = "Lead Developer • Nocturne",
    val authorAvatarUrl: String? = null,
    val isVerified: Boolean = true,
    val title: String = "",
    val content: String,
    val imageUrl: String? = null,
    val gifUrl: String? = null,
    val tag: BroadcastTag = BroadcastTag.ANNOUNCEMENT,
    val actionUrl: String? = null,
    val actionText: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val reactions: Map<String, Int> = emptyMap(),
    val userReactions: Set<String> = emptySet(),
)
