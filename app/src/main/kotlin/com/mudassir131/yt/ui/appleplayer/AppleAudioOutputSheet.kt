/* Audio output UI adapted from Echo Music's AudioDeviceBottomSheet (GPL-3.0). */
package com.mudassir131.yt.ui.appleplayer

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.AudioQuality
import com.mudassir131.yt.constants.AudioQualityKey
import com.mudassir131.yt.ui.component.SystemMediaVolumeSlider
import com.mudassir131.yt.utils.rememberEnumPreference

private val AudioSheetBackground = Color(0xFF101318)
private val AudioSheetSurface = Color(0xFF1A1E25)
private val AudioSheetAccent = Color(0xFFAAC7F5)
private val AudioSheetAccentContainer = Color(0xFF405E83)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleAudioOutputSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var outputs by remember { mutableStateOf(audioManager.currentOutputs()) }
    var audioQuality by rememberEnumPreference(AudioQualityKey, AudioQuality.OPUS)
    val activeOutput = remember(outputs) {
        outputs.firstOrNull { it.isExternal }
            ?: outputs.firstOrNull()
            ?: OutputDevice("This device", "System audio output", R.drawable.volume_up)
    }

    DisposableEffect(audioManager) {
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    outputs = audioManager.currentOutputs()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    outputs = audioManager.currentOutputs()
                }
            }.also { audioManager.registerAudioDeviceCallback(it, Handler(Looper.getMainLooper())) }
        } else {
            null
        }

        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AudioSheetBackground,
        contentColor = Color.White,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 14.dp, bottom = 20.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = .52f)),
            )
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
        ) {
            Text(
                text = "Audio Output",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Surface(
                shape = RoundedCornerShape(30.dp),
                color = AudioSheetAccentContainer,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = .08f),
                        modifier = Modifier.size(56.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(activeOutput.icon),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = activeOutput.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                        )
                        Text(
                            text = activeOutput.kind,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = .8f),
                        )
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.labelMedium,
                            color = AudioSheetAccent,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(30.dp),
                color = AudioSheetAccentContainer,
                contentColor = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.volume_up),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = "Volume",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    SystemMediaVolumeSlider(
                        contentColor = Color.White,
                        activeColor = AudioSheetAccent,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            Text(
                text = "Music Quality",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            AppleQualitySelector(
                selected = audioQuality,
                onSelected = { audioQuality = it },
            )

            Text(
                text = "Download Quality",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            AppleQualitySelector(
                selected = audioQuality,
                onSelected = { audioQuality = it },
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (audioQuality == AudioQuality.OPUS) "Opus enabled" else "Saavn enabled",
                    style = MaterialTheme.typography.labelLarge,
                    color = AudioSheetAccent,
                )
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AudioSheetAccent,
                        contentColor = Color(0xFF1B2F4A),
                    ),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleQualitySelector(
    selected: AudioQuality,
    onSelected: (AudioQuality) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(AudioSheetSurface),
    ) {
        QualityOption(
            text = "Opus",
            selected = selected == AudioQuality.OPUS,
            onClick = { onSelected(AudioQuality.OPUS) },
            modifier = Modifier.weight(1f),
        )
        QualityOption(
            text = "Saavn",
            selected = selected == AudioQuality.SAAVN,
            onClick = { onSelected(AudioQuality.SAAVN) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun QualityOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) AudioSheetAccent else Color.Transparent)
            .clickable(onClick = onClick),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            color = if (selected) Color(0xFF1B2F4A) else Color.White.copy(alpha = .68f),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

internal data class OutputDevice(
    val name: String,
    val kind: String,
    val icon: Int,
    val isExternal: Boolean = false,
)

@Composable
internal fun rememberAppleAudioOutputs(): List<OutputDevice> {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var outputs by remember { mutableStateOf(audioManager.currentOutputs()) }

    DisposableEffect(audioManager) {
        val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
                    outputs = audioManager.currentOutputs()
                }

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
                    outputs = audioManager.currentOutputs()
                }
            }.also { audioManager.registerAudioDeviceCallback(it, Handler(Looper.getMainLooper())) }
        } else null
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && callback != null) {
                audioManager.unregisterAudioDeviceCallback(callback)
            }
        }
    }
    return outputs
}

private fun AudioManager.currentOutputs(): List<OutputDevice> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return listOf(OutputDevice("This device", "System audio output", R.drawable.volume_up))
    }

    return getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { device ->
        val (kind, external) = when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth" to true

            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_USB_HEADSET -> "Headphones" to true

            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI" to true

            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker" to false
            else -> "Audio output" to false
        }
        OutputDevice(
            name = device.productName?.toString().orEmpty().ifBlank { kind },
            kind = kind,
            icon = if (kind == "Bluetooth") R.drawable.bluetooth else R.drawable.volume_up,
            isExternal = external,
        )
    }.distinctBy { it.name to it.kind }
}
