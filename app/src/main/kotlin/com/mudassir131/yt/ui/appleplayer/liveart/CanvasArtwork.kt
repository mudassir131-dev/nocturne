/* SPDX-License-Identifier: GPL-3.0-or-later — adapted from Echo Music. */
package com.mudassir131.yt.ui.appleplayer.liveart

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CanvasArtwork(
    val name: String? = null,
    val artist: String? = null,
    @SerialName("albumId")
    val albumId: String? = null,
    val albumName: String? = null,
    val static: String? = null,
    val animated: String? = null,
    val videoUrl: String? = null,
) {
    val preferredAnimationUrl: String?
        get() = animated ?: videoUrl
}
