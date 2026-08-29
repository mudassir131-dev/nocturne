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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            val appContext = context.applicationContext
            Toast.makeText(appContext, "Importing Spotify playlist in the background...", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.IO).launch {
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
                        Toast.makeText(appContext, "Import failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
}
