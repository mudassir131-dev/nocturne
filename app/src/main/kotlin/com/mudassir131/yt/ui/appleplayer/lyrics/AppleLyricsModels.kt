package com.mudassir131.yt.ui.appleplayer.lyrics

import androidx.compose.runtime.Immutable

@Immutable
data class AppleTimedWord(
    val text: String,
    val startMs: Long,
    val endMs: Long?,
)

@Immutable
data class AppleLyricsLine(
    val text: String,
    val startMs: Long?,
    val endMs: Long?,
    val words: List<AppleTimedWord> = emptyList(),
)

@Immutable
data class AppleLyricsResult(
    val provider: String,
    val raw: String,
    val lines: List<AppleLyricsLine>,
) {
    val hasRealWordTiming: Boolean get() = lines.any { it.words.isNotEmpty() }
    val isLineSynced: Boolean get() = lines.any { it.startMs != null }
}
