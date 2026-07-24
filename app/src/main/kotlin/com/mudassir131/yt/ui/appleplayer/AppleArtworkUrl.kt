/*
 * Apple expanded-player artwork URL selection, adapted from Echo Music commit
 * 6bea1dbf25c5761707ac0d5c9f3ce32d589be7aa (GPL-3.0).
 */
package com.mudassir131.yt.ui.appleplayer

private const val APPLE_EXPANDED_ARTWORK_SIZE = 1200

internal fun String.toAppleExpandedArtworkUrl(): String {
    if (contains("i.ytimg.com")) {
        return replace(
            Regex("(default|mqdefault|hqdefault|sddefault|maxresdefault)\\.jpg"),
            "maxresdefault.jpg",
        )
    }

    if (contains("googleusercontent.com") && contains("=")) {
        return "${substringBefore("=")}=w$APPLE_EXPANDED_ARTWORK_SIZE-h$APPLE_EXPANDED_ARTWORK_SIZE"
    }

    if (contains("yt3.ggpht.com")) {
        val baseUrl = substringBefore("=").substringBefore("-s")
        return "$baseUrl=s$APPLE_EXPANDED_ARTWORK_SIZE"
    }

    if (Regex("https://lh\\d\\.googleusercontent\\.com/.*").matches(this)) {
        return "${substringBefore("=")}=w$APPLE_EXPANDED_ARTWORK_SIZE-h$APPLE_EXPANDED_ARTWORK_SIZE"
    }

    return this
}

internal fun String.appleExpandedArtworkFallback(): String? =
    takeIf { contains("i.ytimg.com") && contains("maxresdefault.jpg") }
        ?.replace("maxresdefault.jpg", "hqdefault.jpg")
