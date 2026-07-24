package com.mudassir131.yt.ui.component

import android.content.Context
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mudassir131.yt.R
import kotlin.math.roundToInt

/** Lifecycle-aware view of Android's actual STREAM_MUSIC volume. */
@Stable
class SystemMediaVolumeState internal constructor(
    private val audioManager: AudioManager,
) {
    var currentVolume by mutableIntStateOf(0)
        private set
    var maxVolume by mutableIntStateOf(1)
        private set

    val fraction: Float
        get() = currentVolume.toFloat() / maxVolume.coerceAtLeast(1)

    val isMuted: Boolean
        get() = currentVolume == 0

    init {
        refresh()
    }

    fun refresh() {
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        currentVolume = audioManager
            .getStreamVolume(AudioManager.STREAM_MUSIC)
            .coerceIn(0, maxVolume)
    }

    fun setFraction(value: Float) {
        val volume = (value.coerceIn(0f, 1f) * maxVolume).roundToInt()
        currentVolume = volume
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
    }
}

@Composable
fun rememberSystemMediaVolumeState(): SystemMediaVolumeState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val state = remember(audioManager) { SystemMediaVolumeState(audioManager) }

    DisposableEffect(context, lifecycleOwner, state) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                state.refresh()
            }
        }
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.refresh()
        }
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            observer,
        )
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        state.refresh()
        onDispose {
            context.contentResolver.unregisterContentObserver(observer)
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
    return state
}

/** Compact system-style control used only by the Cinematic player surfaces. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemMediaVolumeSlider(
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    activeColor: Color = MaterialTheme.colorScheme.primary,
) {
    val state = rememberSystemMediaVolumeState()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(R.drawable.volume_off),
            contentDescription = "Mute",
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
        Slider(
            value = state.fraction,
            onValueChange = state::setFraction,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                activeTrackColor = activeColor,
                inactiveTrackColor = contentColor.copy(alpha = 0.24f),
            ),
            thumb = { Spacer(Modifier.size(0.dp)) },
            track = { sliderState ->
                PlayerSliderTrack(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = activeColor,
                        inactiveTrackColor = contentColor.copy(alpha = 0.24f),
                    ),
                    trackHeight = 8.dp,
                )
            },
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .padding(horizontal = 12.dp),
        )
        Icon(
            painter = painterResource(R.drawable.volume_up),
            contentDescription = "Maximum volume",
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(18.dp),
        )
    }
}
