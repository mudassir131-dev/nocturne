/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.R

@Composable
fun PlaylistActionChoiceDialog(
    onDismiss: () -> Unit,
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit
) {
    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text(text = "Playlist Options") },
        buttons = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCreateClick()
                        onDismiss()
                    }
                    .padding(vertical = 16.dp, horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.add),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Create New Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onImportClick()
                        onDismiss()
                    }
                    .padding(vertical = 16.dp, horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.playlist_import),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Import Playlist",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
