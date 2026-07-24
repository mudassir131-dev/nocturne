package com.mudassir131.yt.utils

private val productPrefix = Regex("^(nocturne|velune)\\s+", RegexOption.IGNORE_CASE)

fun normalizeVersionLabel(raw: String): String {
    var value = raw.trim().replace(productPrefix, "").trim()
    if (value.startsWith("v", ignoreCase = true)) value = value.drop(1)
    return value.trim()
}

fun displayVersionLabel(raw: String): String = "V${normalizeVersionLabel(raw)}"

fun compareSemanticVersions(first: String, second: String): Int {
    fun segments(raw: String): List<Int> =
        normalizeVersionLabel(raw)
            .split('.', '_', '-')
            .map { segment -> segment.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    val firstParts = segments(first)
    val secondParts = segments(second)
    repeat(maxOf(firstParts.size, secondParts.size)) { index ->
        val comparison = firstParts.getOrElse(index) { 0 }
            .compareTo(secondParts.getOrElse(index) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

fun versionsEquivalent(first: String, second: String): Boolean =
    compareSemanticVersions(first, second) == 0
