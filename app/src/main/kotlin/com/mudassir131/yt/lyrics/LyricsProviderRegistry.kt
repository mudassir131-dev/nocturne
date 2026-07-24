package com.mudassir131.yt.lyrics

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.mudassir131.yt.constants.EnablePaxSenixLyricsKey
import com.mudassir131.yt.constants.EnableYouLyPlusLyricsKey
import com.mudassir131.yt.constants.LyricsProviderPriorityKey
import com.mudassir131.yt.constants.PreferredLyricsProvider
import com.mudassir131.yt.constants.PreferredLyricsProviderKey
import com.mudassir131.yt.extensions.toEnum
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.get
import com.music.paxsenix.Paxsenix
import com.music.youlyplus.YouLyPlus
import kotlinx.coroutines.flow.first

enum class LyricsTimingCapability {
    PLAIN,
    LINE_SYNCED,
    WORD_SYNCED,
    SYLLABLE_SYNCED,
}

enum class LyricsProviderId(val persistedId: String, val displayName: String) {
    LRCLIB("lrclib", "LrcLib"),
    KUGOU("kugou", "KuGou"),
    BETTER_LYRICS("betterlyrics", "BetterLyrics"),
    SIMPMUSIC("simpmusic", "SimpMusic Lyrics"),
    YOULY_PLUS("youlyplus", "YouLyPlus"),
    PAX_SENIX("paxsenix", "PaxSenix"),
    ;

    companion object {
        fun fromPersistedId(value: String): LyricsProviderId? =
            entries.firstOrNull { it.persistedId.equals(value.trim(), ignoreCase = true) }
    }
}

object YouLyPlusLyricsProvider : LyricsProvider {
    override val id = LyricsProviderId.YOULY_PLUS
    override val name = id.displayName
    override val timingCapabilities = setOf(
        LyricsTimingCapability.PLAIN,
        LyricsTimingCapability.LINE_SYNCED,
        LyricsTimingCapability.WORD_SYNCED,
    )

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnableYouLyPlusLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = YouLyPlus.getLyrics(title, artist, duration, album, id)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) = YouLyPlus.getAllLyrics(title, artist, duration, album, id, callback = callback)
}

object PaxSenixLyricsProvider : LyricsProvider {
    override val id = LyricsProviderId.PAX_SENIX
    override val name = id.displayName
    override val timingCapabilities = setOf(
        LyricsTimingCapability.PLAIN,
        LyricsTimingCapability.LINE_SYNCED,
        LyricsTimingCapability.WORD_SYNCED,
        LyricsTimingCapability.SYLLABLE_SYNCED,
    )

    override fun isEnabled(context: Context): Boolean =
        context.dataStore[EnablePaxSenixLyricsKey] ?: true

    override suspend fun getLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
    ): Result<String> = Paxsenix.getLyrics(title, artist, duration, album)

    override suspend fun getAllLyrics(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Int,
        callback: (String) -> Unit,
    ) = Paxsenix.getAllLyrics(title, artist, duration, album, callback)
}

object LyricsProviderRegistry {
    val configurableProviders: List<LyricsProvider> = listOf(
        LrcLibLyricsProvider,
        KuGouLyricsProvider,
        BetterLyricsProvider,
        SimpMusicLyricsProvider,
        YouLyPlusLyricsProvider,
        PaxSenixLyricsProvider,
    )

    val defaultPriority: List<LyricsProviderId> = configurableProviders.map { it.id }

    fun encodePriority(priority: List<LyricsProviderId>): String =
        priority.distinct().joinToString(",") { it.persistedId }

    fun decodePriority(value: String?): List<LyricsProviderId> {
        val decoded = value.orEmpty().split(',').mapNotNull(LyricsProviderId::fromPersistedId).distinct()
        return decoded + defaultPriority.filterNot(decoded::contains)
    }

    suspend fun orderedProviders(context: Context): List<LyricsProvider> {
        val preferences = context.dataStore.data.first()
        val storedPriority = preferences[LyricsProviderPriorityKey]
        val priority = if (storedPriority.isNullOrBlank()) {
            val legacy = preferences[PreferredLyricsProviderKey].toEnum(PreferredLyricsProvider.LRCLIB)
            val migratedFirst = when (legacy) {
                PreferredLyricsProvider.LRCLIB -> LyricsProviderId.LRCLIB
                PreferredLyricsProvider.KUGOU -> LyricsProviderId.KUGOU
                PreferredLyricsProvider.BETTER_LYRICS -> LyricsProviderId.BETTER_LYRICS
                PreferredLyricsProvider.SIMPMUSIC -> LyricsProviderId.SIMPMUSIC
            }
            (listOf(migratedFirst) + defaultPriority.filterNot { it == migratedFirst }).also { migrated ->
                context.dataStore.edit { current ->
                    if (current[LyricsProviderPriorityKey].isNullOrBlank()) {
                        current[LyricsProviderPriorityKey] = encodePriority(migrated)
                    }
                }
            }
        } else {
            decodePriority(storedPriority)
        }
        val byId = configurableProviders.associateBy { it.id }
        return priority.mapNotNull(byId::get)
    }
}
