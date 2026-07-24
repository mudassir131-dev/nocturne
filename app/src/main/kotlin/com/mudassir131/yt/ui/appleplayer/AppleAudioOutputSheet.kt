/* Audio output UI adapted from Echo Music's AudioDeviceBottomSheet (GPL-3.0). */
package com.mudassir131.yt.ui.appleplayer

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.component.SystemMediaVolumeSlider
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleAudioOutputSheet(onDismiss: () -> Unit) {
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)) {
            Text("Audio Output", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
            outputs.forEach { output ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(output.icon), null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(output.name)
                        Text(output.kind, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            SystemMediaVolumeSlider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private data class OutputDevice(val name: String, val kind: String, val icon: Int)

private fun AudioManager.currentOutputs(): List<OutputDevice> {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return listOf(OutputDevice("This device", "System audio output", R.drawable.volume_up))
    }
    return getDevices(AudioManager.GET_DEVICES_OUTPUTS).map { device ->
        val (kind, icon) = when (device.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth" to R.drawable.volume_up
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> "Headphones" to R.drawable.volume_up
            AudioDeviceInfo.TYPE_HDMI, AudioDeviceInfo.TYPE_HDMI_ARC, AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI" to R.drawable.volume_up
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Built-in speaker" to R.drawable.volume_up
            else -> "Audio output" to R.drawable.volume_up
        }
        OutputDevice(device.productName?.toString().orEmpty().ifBlank { kind }, kind, icon)
    }.distinctBy { it.name to it.kind }
}
