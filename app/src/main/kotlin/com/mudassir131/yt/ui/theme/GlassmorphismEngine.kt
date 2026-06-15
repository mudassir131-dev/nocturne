package com.mudassir131.yt.ui.theme

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.mudassir131.yt.constants.*
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.utils.rememberPreference
import kotlinx.coroutines.isActive
import com.mudassir131.yt.ui.screens.settings.DarkMode

@Stable
class GlassmorphismState(
    val isBatterySaver: Boolean,
    val batteryLevel: Int,
    val isLowEndDevice: Boolean,
    val ramGb: Float,
    val fps: Float,
    val pureBlack: Boolean
) {
    val isBatteryLow: Boolean
        get() = batteryLevel < 20 || isBatterySaver
        
    val isFpsLow: Boolean
        get() = fps < 55f
        
    val shouldReduceAnimations: Boolean
        get() = isBatteryLow || isFpsLow || isLowEndDevice
}

val LocalGlassmorphismState = staticCompositionLocalOf<GlassmorphismState?> { null }

@Composable
fun ProvideGlassmorphismState(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val isBatterySaver = rememberBatterySaverState()
    val batteryLevel = rememberBatteryLevelState()
    val isLowEndDevice = rememberLowEndDeviceState()
    val ramGb = rememberRamGb()
    val fps = rememberFpsState()
    val (pureBlackEnabled) = rememberPreference(key = PureBlackKey, defaultValue = true)
    val (darkMode) = rememberEnumPreference(key = DarkModeKey, defaultValue = DarkMode.ON)
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }
    val pureBlack = pureBlackEnabled && useDarkTheme
    
    val state = remember(isBatterySaver, batteryLevel, isLowEndDevice, ramGb, fps, pureBlack) {
        GlassmorphismState(
            isBatterySaver = isBatterySaver,
            batteryLevel = batteryLevel,
            isLowEndDevice = isLowEndDevice,
            ramGb = ramGb,
            fps = fps,
            pureBlack = pureBlack
        )
    }
    
    CompositionLocalProvider(LocalGlassmorphismState provides state) {
        content()
    }
}

@Composable
fun rememberBatterySaverState(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    var isPowerSaveMode by remember { mutableStateOf(powerManager?.isPowerSaveMode == true) }

    DisposableEffect(context, powerManager) {
        if (powerManager == null) return@DisposableEffect onDispose {}
        
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isPowerSaveMode = powerManager.isPowerSaveMode
            }
        }
        
        context.registerReceiver(receiver, IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    return isPowerSaveMode
}

@Composable
fun rememberBatteryLevelState(): Int {
    val context = LocalContext.current
    var batteryLevel by remember { mutableStateOf(100) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level != -1 && scale != -1) {
                    batteryLevel = (level * 100 / scale.toFloat()).toInt()
                }
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // ignore
            }
        }
    }
    return batteryLevel
}

@Composable
fun rememberLowEndDeviceState(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        val totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)
        
        activityManager?.isLowRamDevice == true || 
                Runtime.getRuntime().availableProcessors() < 4 ||
                totalRamGb < 4.0f
    }
}

@Composable
fun rememberRamGb(): Float {
    val context = LocalContext.current
    return remember(context) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)
        memoryInfo.totalMem / (1024f * 1024f * 1024f)
    }
}

@Composable
fun rememberFpsState(): Float {
    var fps by remember { mutableStateOf(60f) }
    LaunchedEffect(Unit) {
        var frameCount = 0
        var lastFrameTimeNanos = System.nanoTime()
        var periodStartNanos = lastFrameTimeNanos
        val updateIntervalFrames = 30 // update twice a second at 60fps
        
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                frameCount++
                if (frameCount >= updateIntervalFrames) {
                    val elapsedNanos = frameTimeNanos - periodStartNanos
                    if (elapsedNanos > 0) {
                        val averageFrameDurationNanos = elapsedNanos.toFloat() / frameCount
                        fps = 1_000_000_000f / averageFrameDurationNanos
                    }
                    frameCount = 0
                    periodStartNanos = frameTimeNanos
                }
            }
        }
    }
    return fps
}

fun filterGreenYellowHues(color: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    val hue = hsv[0]
    // Hue ranges for green (80-160) and yellow (40-80)
    if (hue in 40f..160f) {
        hsv[1] = hsv[1] * 0.4f // Desaturate by 60%
        hsv[2] = hsv[2] * 0.5f // Reduce brightness by 50%
        val targetArgb = android.graphics.Color.HSVToColor(hsv)
        return Color(targetArgb)
    }
    return color
}

fun Modifier.glassmorphic(
    shape: Shape,
    tintColor: Color? = null,
    borderColor: Color = Color.White.copy(alpha = 0.08f),
    borderWidth: Dp = 1.dp,
    fallbackColor: Color? = null,
    forceEnabledMode: GlassEffectsMode? = null,
    alpha: Float = 1f
): Modifier = composed {
    if (alpha <= 0.001f) {
        return@composed this
    }

    val glassEffectsMode by rememberEnumPreference(
        key = GlassEffectsKey,
        defaultValue = GlassEffectsMode.ADAPTIVE
    )
    
    val activeMode = forceEnabledMode ?: glassEffectsMode
    if (activeMode == GlassEffectsMode.DISABLED) {
        return@composed fallbackColor?.let { this.background(it, shape) } ?: this
    }

    val blurIntensityPref by rememberPreference(key = GlassBlurIntensityKey, defaultValue = 20f)
    val transparencyPref by rememberPreference(key = GlassTransparencyKey, defaultValue = 0.3f)
    val dynamicTintPref by rememberPreference(key = GlassDynamicTintKey, defaultValue = true)
    val performanceModePref by rememberPreference(key = GlassPerformanceModeKey, defaultValue = true)
    val glassQualityPref by rememberEnumPreference(key = GlassQualityModeKey, defaultValue = GlassQualityMode.AUTO)

    // Read from single shared state if available to avoid multiple loops/receivers
    val sharedState = LocalGlassmorphismState.current
    val isBatterySaver = sharedState?.isBatterySaver ?: rememberBatterySaverState()
    val batteryLevel = sharedState?.batteryLevel ?: rememberBatteryLevelState()
    val isLowEndDevice = sharedState?.isLowEndDevice ?: rememberLowEndDeviceState()
    val ramGb = sharedState?.ramGb ?: rememberRamGb()
    val fps = sharedState?.fps ?: rememberFpsState()
    val (pureBlackPref) = rememberPreference(key = PureBlackKey, defaultValue = true)
    val (darkMode) = rememberEnumPreference(key = DarkModeKey, defaultValue = DarkMode.ON)
    val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val useDarkTheme = remember(darkMode, isSystemInDarkTheme) {
        if (darkMode == DarkMode.AUTO) isSystemInDarkTheme else darkMode == DarkMode.ON
    }
    val pureBlackFallback = pureBlackPref && useDarkTheme
    val pureBlack = sharedState?.pureBlack ?: pureBlackFallback
    
    val isDark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Calculate glass quality. Use threshold adjustments to match physical memory tags.
    val quality = when (glassQualityPref) {
        GlassQualityMode.LOW -> GlassQualityMode.LOW
        GlassQualityMode.MEDIUM -> GlassQualityMode.MEDIUM
        GlassQualityMode.HIGH -> GlassQualityMode.HIGH
        GlassQualityMode.AUTO -> {
            when {
                ramGb < 5.5f -> GlassQualityMode.LOW      // Physical RAM < 6GB (4GB or below)
                ramGb <= 8.5f -> GlassQualityMode.MEDIUM  // Physical RAM 6-8GB
                else -> GlassQualityMode.HIGH             // Physical RAM > 8GB (12GB or above)
            }
        }
    }

    // FPS Protection
    val isFpsLow = performanceModePref && fps < 55f

    // Smart Battery Mode
    val isBatteryLow = performanceModePref && (batteryLevel < 20 || isBatterySaver)

    // Calculate settings
    val shouldDisableBlur = isLowEndDevice || (quality == GlassQualityMode.LOW && isBatteryLow)
    
    var actualBlurRadius = if (isBatteryLow) {
        (blurIntensityPref * 0.4f).dp // Battery < 20% -> Reduce blur by 60%
    } else {
        blurIntensityPref.dp
    }
    
    if (isFpsLow) {
        actualBlurRadius = (actualBlurRadius.value * 0.5f).dp // FPS < 55 -> Reduce blur by 50%
    }

    actualBlurRadius = when (quality) {
        GlassQualityMode.LOW -> (actualBlurRadius.value * 0.5f).dp
        GlassQualityMode.MEDIUM -> actualBlurRadius
        GlassQualityMode.HIGH -> (actualBlurRadius.value * 1.3f).dp
        else -> actualBlurRadius
    }

    // Scale blur radius by alpha
    actualBlurRadius = (actualBlurRadius.value * alpha).dp

    var baseTransparency = transparencyPref
    if (!isDark) {
        baseTransparency = (transparencyPref + 0.45f).coerceAtMost(0.85f)
    }
    var baseBorderAlpha = borderColor.alpha

    // AMOLED Protection
    if (pureBlack) {
        baseTransparency *= 0.70f // Reduce glass opacity by 30%
        baseBorderAlpha = (baseBorderAlpha + 0.07f).coerceAtMost(1.0f) // Increase border visibility slightly
    }

    val actualTransparency = if (isBatteryLow) {
        // Battery optimization: increase transparency to compensate for reduced blur
        (baseTransparency + 0.15f).coerceAtMost(0.9f)
    } else {
        baseTransparency
    }

    // Scale border and tint by alpha
    baseBorderAlpha = baseBorderAlpha * alpha
    val finalTransparency = actualTransparency * alpha

    val finalBorderColor = if (borderColor.red == 1f && borderColor.green == 1f && borderColor.blue == 1f && !isDark) {
        Color.Black.copy(alpha = baseBorderAlpha * 0.7f)
    } else {
        borderColor.copy(alpha = baseBorderAlpha)
    }

    val adjustedTintColor = if (!isDark && tintColor != null) {
        tintColor.copy(
            red = (tintColor.red + 4f) / 5f,
            green = (tintColor.green + 4f) / 5f,
            blue = (tintColor.blue + 4f) / 5f
        )
    } else {
        tintColor
    }

    val baseTintColor = if (dynamicTintPref && adjustedTintColor != null) {
        filterGreenYellowHues(adjustedTintColor)
    } else {
        if (isDark) {
            if (pureBlack) Color.Black else Color(0xFF121212)
        } else {
            Color.White
        }
    }

    // Avoid gray haze on pure black background
    val finalTintColor = if (pureBlack) {
        if (dynamicTintPref && adjustedTintColor != null) {
            // Apply desaturated and darkened tint mixed down with black to prevent gray overlays
            val filtered = filterGreenYellowHues(adjustedTintColor)
            Color(
                red = filtered.red * 0.25f,
                green = filtered.green * 0.25f,
                blue = filtered.blue * 0.25f,
                alpha = finalTransparency
            )
        } else {
            Color.Black.copy(alpha = finalTransparency)
        }
    } else {
        baseTintColor.copy(alpha = finalTransparency)
    }

    val supportsBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !shouldDisableBlur
    val supportsLens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !isBatteryLow && !isFpsLow

    val backdrop = rememberCanvasBackdrop {
        drawRect(
            color = if (pureBlack) Color.Black.copy(alpha = 0.15f * alpha) else if (isDark) Color.Black.copy(alpha = 0.25f * alpha) else Color.White.copy(alpha = 0.15f * alpha),
            size = size
        )
    }

    this
        .clip(shape)
        .then(
            if (supportsBackdrop) {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        if (actualBlurRadius.value > 0f) {
                            blur(with(density) { actualBlurRadius.toPx() })
                        }
                        if (supportsLens) {
                            lens(20f, 40f) // Reflections/lens disabled under Low Battery or Low FPS
                        }
                    },
                    onDrawSurface = {
                        drawRect(finalTintColor)
                        if (!pureBlack) {
                            drawRect(Color.White.copy(alpha = 0.03f * alpha)) // Avoid gray haze on pure black by dropping white overlay
                        }
                    }
                )
            } else {
                Modifier.background(
                    color = finalTintColor,
                    shape = shape
                )
            }
        )
        .border(
            width = borderWidth,
            color = finalBorderColor,
            shape = shape
        )
}

fun Modifier.glassmorphicButton(
    isGlassActive: Boolean,
    shape: Shape,
    baseColor: Color
): Modifier = composed {
    if (isGlassActive) {
        val sharedState = LocalGlassmorphismState.current
        val (pureBlackPref) = rememberPreference(key = PureBlackKey, defaultValue = true)
        val pureBlack = sharedState?.pureBlack ?: pureBlackPref
        
        val tintColor = if (pureBlack) Color.Black.copy(alpha = 0.25f) else baseColor.copy(alpha = 0.2f)
        val borderAlpha = if (pureBlack) 0.15f else 0.10f
        
        this.glassmorphic(
            shape = shape,
            tintColor = tintColor,
            fallbackColor = baseColor.copy(alpha = 0.5f),
            borderColor = Color.White.copy(alpha = borderAlpha),
            borderWidth = 0.5.dp
        )
    } else {
        this.background(baseColor, shape)
    }
}
