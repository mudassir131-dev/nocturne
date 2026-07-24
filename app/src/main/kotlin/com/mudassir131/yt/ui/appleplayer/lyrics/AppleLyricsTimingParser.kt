/* Strict LRC/TTML timing parser: it never distributes line duration across words. */
package com.mudassir131.yt.ui.appleplayer.lyrics

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

object AppleLyricsTimingParser {
    private val lineRegex = Regex("""^\[(\d{1,3}):(\d{1,2}(?:\.\d{1,3})?)](.*)$""")
    private val wordRegex = Regex("""<(\d{1,3}):(\d{1,2}(?:\.\d{1,3})?)>([^<]*)""")

    fun parse(raw: String): List<AppleLyricsLine> =
        if (raw.trimStart().startsWith("<tt", ignoreCase = true)) parseTtml(raw) else parseLrc(raw)

    private fun parseLrc(raw: String): List<AppleLyricsLine> {
        val parsed = mutableListOf<AppleLyricsLine>()
        raw.lineSequence().forEach { source ->
            val trimmed = source.trim()
            val match = lineRegex.matchEntire(trimmed)
            if (match == null) {
                // Paxsenix's syllable response places genuine word start/end
                // seconds on the line immediately following its LRC line.
                if (trimmed.startsWith("<") && trimmed.endsWith(">") && parsed.isNotEmpty()) {
                    val words = trimmed.removeSurrounding("<", ">").split('|').mapNotNull { token ->
                        val endSeparator = token.lastIndexOf(':')
                        val startSeparator = token.lastIndexOf(':', endSeparator - 1)
                        if (startSeparator <= 0 || endSeparator <= startSeparator) return@mapNotNull null
                        val start = token.substring(startSeparator + 1, endSeparator).toDoubleOrNull() ?: return@mapNotNull null
                        val end = token.substring(endSeparator + 1).toDoubleOrNull() ?: return@mapNotNull null
                        AppleTimedWord(token.substring(0, startSeparator), (start * 1000).toLong(), (end * 1000).toLong())
                    }
                    if (words.isNotEmpty()) {
                        parsed[parsed.lastIndex] = parsed.last().copy(
                            text = words.joinToString(" ") { it.text },
                            words = words,
                        )
                    }
                }
                return@forEach
            }
            val start = timestamp(match.groupValues[1], match.groupValues[2])
            val body = match.groupValues[3]
            val words = wordRegex.findAll(body).map { word ->
                AppleTimedWord(
                    text = word.groupValues[3],
                    startMs = timestamp(word.groupValues[1], word.groupValues[2]),
                    endMs = null,
                )
            }.toList()
            val text = if (words.isNotEmpty()) words.joinToString("") { it.text }.trim() else body.trim()
            parsed += AppleLyricsLine(text = text, startMs = start, endMs = null, words = words)
        }
        val synced = parsed.sortedBy { it.startMs }

        if (synced.isNotEmpty()) {
            return synced.mapIndexed { index, line ->
                val lineEnd = synced.getOrNull(index + 1)?.startMs
                line.copy(
                    endMs = lineEnd,
                    words = line.words.mapIndexed { wordIndex, word ->
                        word.copy(
                            endMs = word.endMs
                                ?: line.words.getOrNull(wordIndex + 1)?.startMs
                                ?: lineEnd,
                        )
                    },
                )
            }
        }
        return raw.lineSequence().map(String::trim).filter(String::isNotBlank)
            .map { AppleLyricsLine(it, null, null) }.toList()
    }

    private fun parseTtml(raw: String): List<AppleLyricsLine> = runCatching {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(raw.byteInputStream())
        val elements = document.getElementsByTagName("*")
        buildList {
            for (index in 0 until elements.length) {
                val paragraph = elements.item(index) as? Element ?: continue
                if (!paragraph.localName.orEmpty().equals("p", true) && !paragraph.tagName.endsWith(":p")) continue
                val start = parseTtmlTime(paragraph.getAttribute("begin")) ?: continue
                val end = parseTtmlTime(paragraph.getAttribute("end"))
                    ?: parseTtmlTime(paragraph.getAttribute("dur"))?.let { start + it }
                val descendants = paragraph.getElementsByTagName("*")
                val words = buildList {
                    for (spanIndex in 0 until descendants.length) {
                        val span = descendants.item(spanIndex) as? Element ?: continue
                        if (!span.localName.orEmpty().equals("span", true) && !span.tagName.endsWith(":span")) continue
                        val wordStart = parseTtmlTime(span.getAttribute("begin")) ?: continue
                        val wordEnd = parseTtmlTime(span.getAttribute("end"))
                            ?: parseTtmlTime(span.getAttribute("dur"))?.let { wordStart + it }
                        val text = span.textContent.orEmpty()
                        if (text.isNotBlank()) add(AppleTimedWord(text, wordStart, wordEnd))
                    }
                }
                val text = paragraph.textContent.orEmpty().trim()
                if (text.isNotBlank()) add(AppleLyricsLine(text, start, end, words))
            }
        }
    }.getOrDefault(emptyList())

    private fun timestamp(minutes: String, seconds: String): Long =
        (minutes.toLong() * 60_000L + seconds.toDouble() * 1_000.0).toLong()

    private fun parseTtmlTime(value: String): Long? {
        if (value.isBlank()) return null
        if (value.endsWith("ms")) return value.dropLast(2).toDoubleOrNull()?.toLong()
        if (value.endsWith("s")) return value.dropLast(1).toDoubleOrNull()?.times(1000)?.toLong()
        val parts = value.split(':')
        return when (parts.size) {
            3 -> ((parts[0].toDoubleOrNull() ?: return null) * 3_600_000 +
                (parts[1].toDoubleOrNull() ?: return null) * 60_000 +
                (parts[2].toDoubleOrNull() ?: return null) * 1_000).toLong()
            2 -> timestamp(parts[0], parts[1])
            else -> value.toDoubleOrNull()?.times(1000)?.toLong()
        }
    }
}
