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
import com.mudassir131.yt.utils.PlaylistImporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PlaylistImportDialog(
    onDismiss: () -> Unit
) {
    val database = LocalDatabase.current
    val context = LocalContext.current

    TextFieldDialog(
        icon = { Icon(painter = painterResource(R.drawable.add), contentDescription = null) },
        title = { Text(text = "Import Playlist") },
        initialTextFieldValue = TextFieldValue(""),
        placeholder = { Text(text = "Paste Spotify, Apple Music, or YouTube link") },
        onDismiss = onDismiss,
        onDone = { url ->
            Toast.makeText(context, "Importing playlist in the background...", Toast.LENGTH_SHORT).show()
            CoroutineScope(Dispatchers.IO).launch {
                val result = PlaylistImporter.importPlaylist(database, url)
                withContext(Dispatchers.Main) {
                    result.onSuccess { playlistName ->
                        Toast.makeText(context, "Successfully imported: $playlistName", Toast.LENGTH_LONG).show()
                    }.onFailure { error ->
                        Toast.makeText(context, "Import failed: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    )
}
