/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object StoryShareHelper {

    private const val INSTAGRAM_PACKAGE = "com.instagram.android"
    private const val SNAPCHAT_PACKAGE = "com.snapchat.android"
    private const val DEFAULT_FALLBACK_URL = "https://github.com/mudassir131-dev/nocturne"

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun shareToInstagram(
        context: Context,
        songTitle: String,
        artistName: String,
        thumbnailUrl: String?,
        fallbackUrl: String = DEFAULT_FALLBACK_URL,
        coroutineScope: CoroutineScope,
        onLoading: (Boolean) -> Unit = {}
    ) {
        if (!isAppInstalled(context, INSTAGRAM_PACKAGE)) {
            Toast.makeText(context, "Instagram is not installed", Toast.LENGTH_SHORT).show()
            shareTextFallback(context, songTitle, artistName, fallbackUrl)
            return
        }

        coroutineScope.launch {
            onLoading(true)
            try {
                val stickerBitmap = ComposeToImage.createShareCard(context, thumbnailUrl, songTitle, artistName)
                val backgroundBitmap = ComposeToImage.createBlurredBackground(context, thumbnailUrl)

                val stickerUri = ComposeToImage.saveBitmapToCache(context, stickerBitmap, "instagram_share_sticker")
                val backgroundUri = ComposeToImage.saveBitmapToCache(context, backgroundBitmap, "instagram_share_background")

                withContext(Dispatchers.Main) {
                    val intent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                        setDataAndType(backgroundUri, "image/*")
                        putExtra("interactive_asset_uri", stickerUri)
                        putExtra("content_url", fallbackUrl)
                        setPackage(INSTAGRAM_PACKAGE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    context.grantUriPermission(INSTAGRAM_PACKAGE, stickerUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.grantUriPermission(INSTAGRAM_PACKAGE, backgroundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to share to Instagram: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error creating share card: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    onLoading(false)
                }
            }
        }
    }

    fun shareToSnapchat(
        context: Context,
        songTitle: String,
        artistName: String,
        thumbnailUrl: String?,
        fallbackUrl: String = DEFAULT_FALLBACK_URL,
        coroutineScope: CoroutineScope,
        onLoading: (Boolean) -> Unit = {}
    ) {
        if (!isAppInstalled(context, SNAPCHAT_PACKAGE)) {
            Toast.makeText(context, "Snapchat is not installed", Toast.LENGTH_SHORT).show()
            shareTextFallback(context, songTitle, artistName, fallbackUrl)
            return
        }

        coroutineScope.launch {
            onLoading(true)
            try {
                val stickerBitmap = ComposeToImage.createShareCard(context, thumbnailUrl, songTitle, artistName)
                val backgroundBitmap = ComposeToImage.createBlurredBackground(context, thumbnailUrl)

                val stickerUri = ComposeToImage.saveBitmapToCache(context, stickerBitmap, "snapchat_share_sticker")
                val backgroundUri = ComposeToImage.saveBitmapToCache(context, backgroundBitmap, "snapchat_share_background")

                withContext(Dispatchers.Main) {
                    val intent = Intent("com.snapchat.add.TO_STORY").apply {
                        setDataAndType(backgroundUri, "image/*")
                        putExtra("sticker", stickerUri)
                        putExtra("attachmentUrl", fallbackUrl)
                        setPackage(SNAPCHAT_PACKAGE)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    context.grantUriPermission(SNAPCHAT_PACKAGE, stickerUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    context.grantUriPermission(SNAPCHAT_PACKAGE, backgroundUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to share to Snapchat: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error creating share card: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    onLoading(false)
                }
            }
        }
    }

    private fun shareTextFallback(context: Context, songTitle: String, artistName: String, fallbackUrl: String) {
        val shareText = "$songTitle - $artistName\nListen on Nocturne: $fallbackUrl"
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}
