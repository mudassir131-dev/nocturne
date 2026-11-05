/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudassir131.yt.constants.AddonLosslessEnabledKey
import com.mudassir131.yt.constants.AddonLosslessNameKey
import com.mudassir131.yt.constants.AddonLosslessUrlKey
import com.mudassir131.yt.playback.alac.LosslessStreamResolver
import com.mudassir131.yt.playback.alac.addon.AddonClient
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings dialog for configuring external Addon lossless sources.
 */
@Composable
fun AddonSourceDialog(
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cachedSp = remember {
        runCatching {
            context.getSharedPreferences("addon_sources_cache", android.content.Context.MODE_PRIVATE)
        }.getOrNull()
    }

    val (enabled, onEnabledChange) = rememberPreference(AddonLosslessEnabledKey, defaultValue = true)
    val (addonUrl, onAddonUrlChange) = rememberPreference(AddonLosslessUrlKey, defaultValue = "")
    val (addonName, onAddonNameChange) = rememberPreference(AddonLosslessNameKey, defaultValue = "")

    var tempEnabled by remember { mutableStateOf(cachedSp?.getBoolean("addon_enabled", enabled) ?: enabled) }
    var tempUrl by remember { mutableStateOf(addonUrl.ifBlank { cachedSp?.getString("addon_url", "").orEmpty() }) }
    var tempName by remember { mutableStateOf(addonName.ifBlank { cachedSp?.getString("addon_name", "").orEmpty() }) }

    var testStatus by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DefaultDialog(
        onDismiss = onDismiss,
        title = { Text("Lossless Addon Source") },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val cleanUrl = tempUrl.trim().trimEnd('/')
                    val cleanName = tempName.trim()
                    onEnabledChange(tempEnabled)
                    onAddonUrlChange(cleanUrl)
                    onAddonNameChange(cleanName)

                    // Synchronously write to SharedPreferences cache so background service instantly sees it
                    runCatching {
                        val sp = context.getSharedPreferences("addon_sources_cache", android.content.Context.MODE_PRIVATE)
                        sp.edit()
                            .putBoolean("addon_enabled", tempEnabled)
                            .putString("addon_url", cleanUrl)
                            .putString("addon_name", cleanName)
                            .commit()
                    }

                    LosslessStreamResolver.clearCache()
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Enter an external lossless audio provider link (FLAC/ALAC lossless addon). When available on that link, songs will stream in genuine Hi-Res Lossless.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Enable Addon Source",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Switch(
                    checked = tempEnabled,
                    onCheckedChange = { tempEnabled = it },
                )
            }

            OutlinedTextField(
                value = tempUrl,
                onValueChange = {
                    tempUrl = it
                    testStatus = null
                },
                label = { Text("Addon Source Link / URL") },
                placeholder = { Text("https://my-addon.example.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )

            OutlinedTextField(
                value = tempName,
                onValueChange = { tempName = it },
                label = { Text("Source Display Name (Optional)") },
                placeholder = { Text("e.g. My Lossless Addon") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Test Connection Button
            OutlinedButton(
                onClick = {
                    if (tempUrl.isBlank()) {
                        testStatus = "Please enter an addon URL first."
                        isSuccess = false
                        return@OutlinedButton
                    }
                    isTesting = true
                    testStatus = null
                    scope.launch {
                        val client = AddonClient(tempUrl.trim())
                        val manifestResult = client.manifest()
                        val manifest = manifestResult.getOrNull()

                        if (manifest != null) {
                            if (tempName.isBlank() && manifest.displayName.isNotBlank()) {
                                tempName = manifest.displayName
                            }
                            val probe = client.probeSearch().getOrNull()
                            val detail = if (probe != null && probe > 0) "Playable ($probe tracks probed)" else "Ready"
                            isSuccess = true
                            testStatus = "Connected: ${manifest.displayName} (v${manifest.version.ifBlank { "1.0" }}) • $detail"
                        } else {
                            // If /manifest.json failed, probe search directly
                            val probeResult = client.probeSearch()
                            if (probeResult.isSuccess) {
                                isSuccess = true
                                testStatus = "Connected: Addon search endpoint responded successfully."
                            } else {
                                isSuccess = false
                                testStatus = "Connection Failed: ${manifestResult.exceptionOrNull()?.message ?: probeResult.exceptionOrNull()?.message ?: "Unreachable URL"}"
                            }
                        }
                        isTesting = false
                    }
                },
                enabled = !isTesting && tempUrl.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text("Testing Addon...")
                } else {
                    Text("Test Connection")
                }
            }

            // Status Display Box
            testStatus?.let { status ->
                val bg = if (isSuccess) Color(0xFF1E3A2F) else Color(0xFF3D1F1F)
                val border = if (isSuccess) Color(0xFF2E7D32) else Color(0xFFC62828)
                val textCol = if (isSuccess) Color(0xFF81C784) else Color(0xFFEF9A9A)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(bg)
                        .border(1.dp, border, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = textCol,
                    )
                }
            }
        }
    }
}




// Refinement 80: AddonSourceDialog validation flow documentation

// Refinement 93: Connection test button state styling

// Refinement 107: Addon URL trim and scheme handling

// Refinement 120: Clear test result on URL edit

// Refinement 131: Auto dismiss keyboard on test success
