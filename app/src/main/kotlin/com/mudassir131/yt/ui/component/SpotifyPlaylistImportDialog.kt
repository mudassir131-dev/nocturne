/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import android.widget.Toast
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import com.mudassir131.yt.LocalDatabase
import com.mudassir131.yt.R
import com.mudassir131.yt.utils.ImportSummary
import com.mudassir131.yt.utils.PlaylistImporter
import com.mudassir131.yt.utils.SpotifyAccessDeniedException
import com.mudassir131.yt.utils.SpotifyAuthException
import com.mudassir131.yt.utils.SpotifyImportInvariantException
import com.mudassir131.yt.utils.SpotifyMaxTracksExceededException
import com.mudassir131.yt.utils.SpotifyPaginationLoopException
import com.mudassir131.yt.utils.SpotifyQuotaExceededException
import com.mudassir131.yt.utils.SpotifyRateLimitException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

private val isSpotifyImportRunning = AtomicBoolean(false)

@Composable
fun SpotifyPlaylistImportDialog(
    onDismiss: () -> Unit
) {
    val database = LocalDatabase.current
    val context = LocalContext.current

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.playlist_import), contentDescription = null) },
        title = { Text(text = "Import Spotify Playlist") },
        initialTextFieldValue = TextFieldValue(""),
        placeholder = { Text(text = "Paste Spotify Playlist URL") },
        onDismiss = onDismiss,
        onDone = { url ->
            val trimmed = url.trim()
            if (!trimmed.contains("spotify.com/playlist") && !trimmed.contains("spotify.link")) {
                Toast.makeText(context, "Please enter a valid Spotify playlist link", Toast.LENGTH_SHORT).show()
                return@TextFieldDialog
            }

            if (!isSpotifyImportRunning.compareAndSet(false, true)) {
                Toast.makeText(context, "A playlist import is already in progress. Please wait.", Toast.LENGTH_SHORT).show()
                return@TextFieldDialog
            }

            val appContext = context.applicationContext
            Toast.makeText(appContext, "Importing Spotify playlist in the background...", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    var summary: ImportSummary? = null
                    val result = PlaylistImporter.importPlaylist(database, trimmed) { summary = it }
                    withContext(Dispatchers.Main) {
                        result.onSuccess { playlistName ->
                            val reported = summary
                            val message = if (reported != null) {
                                appContext.getString(
                                    R.string.import_summary,
                                    playlistName,
                                    reported.matched,
                                    reported.unmatched
                                )
                            } else {
                                "Successfully imported: $playlistName"
                            }
                            Toast.makeText(appContext, message, Toast.LENGTH_LONG).show()
                            reported?.quotaNotice?.let { notice ->
                                Toast.makeText(appContext, notice, Toast.LENGTH_LONG).show()
                            }
                        }.onFailure { error ->
                            val userMessage = when (error) {
                                is SpotifyMaxTracksExceededException -> "Spotify playlist import supports up to 100 tracks. For larger playlists, use Import Through CSV."
                                is SpotifyRateLimitException -> "Spotify rate limit reached. Please wait and try again."
                                is SpotifyQuotaExceededException -> "Spotify API quota has been exceeded. Please try again later."
                                is SpotifyAuthException -> "Spotify authentication expired. Please try again."
                                is SpotifyAccessDeniedException -> "Spotify denied access to this playlist."
                                is SpotifyImportInvariantException -> "Spotify playlist could not be fully retrieved. No songs were imported."
                                is SpotifyPaginationLoopException -> "Spotify pagination loop detected. Import cancelled."
                                is IOException -> "Unable to connect to Spotify."
                                else -> error.localizedMessage?.takeIf { it.isNotBlank() } ?: "Import failed: Unable to complete Spotify import."
                            }
                            Toast.makeText(appContext, userMessage, Toast.LENGTH_LONG).show()
                        }
                    }
                } finally {
                    isSpotifyImportRunning.set(false)
                }
            }
        }
    )
}
