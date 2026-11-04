/* Audio output UI adapted from Echo Music's AudioDeviceBottomSheet (GPL-3.0). */
package com.mudassir131.yt.ui.appleplayer

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.DynamicThemeKey
import com.mudassir131.yt.ui.component.rememberSystemMediaVolumeState
import com.mudassir131.yt.ui.theme.extractThemeColor
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

private val AudioSheetAccent = Color(0xFF4E85B4)
private val AudioSheetAccentContainer = Color(0xFF405E83)
private val AudioSheetOnAccent = Color(0xFF10171B)

/**
 * The sheet is painted from the app's Material scheme (which already follows the
 * custom seed, or the current song's artwork when the Dynamic Theme toggle is on)
 * and one artwork-derived accent pair so the pill cards morph with the music.
 */
private data class AppleAudioSheetPalette(
    val accent: Color = AudioSheetAccent,
    val accentContainer: Color = AudioSheetAccentContainer,
    val onAccent: Color = AudioSheetOnAccent,
)

private fun Color.toAppleAudioSheetPalette(): AppleAudioSheetPalette {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    val sourceSaturation = max(hsv[1], 0.38f)

    fun tone(saturation: Float, value: Float): Color {
        val toned = hsv.copyOf()
        toned[1] = saturation.coerceIn(0f, 1f)
        toned[2] = value.coerceIn(0f, 1f)
        return Color(android.graphics.Color.HSVToColor(toned))
    }

    val accent = tone(max(sourceSaturation, 0.58f), 0.55f)
    return AppleAudioSheetPalette(
        accent = accent,
        accentContainer = tone(sourceSaturation * 0.84f, 0.40f),
        onAccent = if (accent.luminance() > 0.12f) AudioSheetOnAccent else Color.White,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppleAudioOutputSheet(
    artworkId: String?,
    artworkUrl: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var palette by remember { mutableStateOf(AppleAudioSheetPalette()) }
    val dynamicThemeEnabled by rememberPreference(DynamicThemeKey, defaultValue = false)
    val themePrimary = MaterialTheme.colorScheme.primary

    LaunchedEffect(dynamicThemeEnabled, themePrimary, artworkId, artworkUrl) {
        if (!dynamicThemeEnabled) {
            // Theme toggle off -> follow the app's fixed/custom seed color.
            palette = themePrimary.toAppleAudioSheetPalette()
            return@LaunchedEffect
        }
        if (artworkUrl.isNullOrBlank()) {
            palette = AppleAudioSheetPalette()
            return@LaunchedEffect
        }
        val bitmap = runCatching {
            withContext(Dispatchers.IO) {
                context.imageLoader.execute(
                    ImageRequest.Builder(context)
                        .data(artworkUrl)
                        .size(200, 200)
                        .allowHardware(false)
                        .build(),
                ).image?.toBitmap()
            }
        }.getOrNull()
        palette = bitmap?.extractThemeColor()?.toAppleAudioSheetPalette()
            ?: AppleAudioSheetPalette()
    }

    val themedBackground = MaterialTheme.colorScheme.surface
    val themedTrack = MaterialTheme.colorScheme.surfaceContainerHighest
    val themedContent = MaterialTheme.colorScheme.onSurface
    val backgroundColor by animateColorAsState(themedBackground, tween(280), label = "audioSheetBackground")
    val trackColor by animateColorAsState(themedTrack, tween(280), label = "audioSheetTrack")
    val accentColor by animateColorAsState(palette.accent, tween(280), label = "audioSheetAccent")
    val accentContainerColor by animateColorAsState(
        palette.accentContainer,
        tween(280),
        label = "audioSheetAccentContainer",
    )
    val onAccentColor by animateColorAsState(palette.onAccent, tween(280), label = "audioSheetOnAccent")

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var outputs by remember { mutableStateOf(audioManager.currentOutputs()) }
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
        containerColor = backgroundColor,
        contentColor = themedContent,
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 14.dp, bottom = 18.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(themedContent.copy(alpha = .52f)),
            )
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
        ) {
            AppleDeviceOutputCard(
                device = activeOutput,
                accentContainerColor = accentContainerColor,
            )

            AppleOutputVolumePill(
                fillColor = accentContainerColor,
                trackColor = trackColor,
                contentColor = Color.White,
                modifier = Modifier,
            )

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = onAccentColor,
                    ),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 30.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AppleDeviceOutputCard(
    device: OutputDevice,
    accentContainerColor: Color,
) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = accentContainerColor,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 24.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = .12f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(device.icon),
                        contentDescription = device.name,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = .16f),
                ) {
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = .94f),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** Stadium-shaped media volume control: the fill and thumb-style dot live inside the pill. */
@Composable
private fun AppleOutputVolumePill(
    fillColor: Color,
    trackColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val volumeState = rememberSystemMediaVolumeState()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                contentDescription = "Output volume"
            }
            .clip(CircleShape)
            .background(trackColor)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    volumeState.setFraction(down.position.x / width)
                    down.consume()
                    horizontalDrag(down.id) { change ->
                        volumeState.setFraction(change.position.x / width)
                        change.consume()
                    }
                }
            },
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(maxWidth * volumeState.fraction)
                .background(fillColor),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 22.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.volume_up),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Volume",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(contentColor.copy(alpha = .6f)),
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
            icon = if (kind == "Bluetooth" || kind == "Headphones") R.drawable.ic_ios_headphones else R.drawable.volume_up,
            isExternal = external,
        )
    }.distinctBy { it.name to it.kind }
}
