/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.constants.SelfHostedLosslessEnabledKey
import com.mudassir131.yt.constants.SelfHostedPasswordKey
import com.mudassir131.yt.constants.SelfHostedServerNameKey
import com.mudassir131.yt.constants.SelfHostedServerUrlKey
import com.mudassir131.yt.constants.SelfHostedUsernameKey
import com.mudassir131.yt.playback.alac.SelfHostedLosslessAudioProvider
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.launch

@Composable
fun SelfHostedLosslessDialog(
    onDismiss: () -> Unit,
) {
    val (enabled, onEnabledChange) = rememberPreference(SelfHostedLosslessEnabledKey, defaultValue = false)
    val (serverUrl, onServerUrlChange) = rememberPreference(SelfHostedServerUrlKey, defaultValue = "")
    val (username, onUsernameChange) = rememberPreference(SelfHostedUsernameKey, defaultValue = "")
    val (password, onPasswordChange) = rememberPreference(SelfHostedPasswordKey, defaultValue = "")
    val (serverName, onServerNameChange) = rememberPreference(SelfHostedServerNameKey, defaultValue = "Navidrome")

    var tempEnabled by remember { mutableStateOf(enabled) }
    var tempUrl by remember { mutableStateOf(serverUrl) }
    var tempUsername by remember { mutableStateOf(username) }
    var tempPassword by remember { mutableStateOf(password) }
    var tempServerName by remember { mutableStateOf(serverName) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text("Self-Hosted Lossless Server") },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onEnabledChange(tempEnabled)
                    onServerUrlChange(tempUrl.trim().trimEnd('/'))
                    onUsernameChange(tempUsername.trim())
                    onPasswordChange(tempPassword.trim())
                    onServerNameChange(tempServerName.trim().ifBlank { "Navidrome" })
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Stream genuine FLAC & ALAC audio directly from your Navidrome or Subsonic-compatible server when Hi-Res Lossless is selected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable Server",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = tempEnabled,
                    onCheckedChange = { tempEnabled = it },
                )
            }

            OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it; testStatus = null },
                label = { Text("Server URL") },
                placeholder = { Text("https://music.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = tempUsername,
                onValueChange = { tempUsername = it; testStatus = null },
                label = { Text("Username") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = tempPassword,
                onValueChange = { tempPassword = it; testStatus = null },
                label = { Text("Password or Token") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            OutlinedTextField(
                value = tempServerName,
                onValueChange = { tempServerName = it },
                label = { Text("Server Name (Optional)") },
                placeholder = { Text("Navidrome") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        if (tempUrl.isBlank() || tempUsername.isBlank() || tempPassword.isBlank()) {
                            testStatus = "Please enter Server URL, Username, and Password."
                            isSuccess = false
                            return@OutlinedButton
                        }
                        isTesting = true
                        testStatus = "Testing connection..."
                        scope.launch {
                            val result = SelfHostedLosslessAudioProvider.testConnection(
                                serverUrl = tempUrl,
                                username = tempUsername,
                                passwordOrToken = tempPassword,
                            )
                            isTesting = false
                            result.onSuccess { msg ->
                                testStatus = "✓ $msg"
                                isSuccess = true
                            }.onFailure { err ->
                                testStatus = "✗ Connection failed: ${err.message}"
                                isSuccess = false
                            }
                        }
                    },
                    enabled = !isTesting,
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Test Connection")
                    }
                }
            }

            testStatus?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
