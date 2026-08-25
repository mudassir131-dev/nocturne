/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils

import android.app.Activity
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Window
import android.view.WindowManager
import java.util.Locale

object DisplayRefreshRateManager {
    private const val TAG = "DisplayRefreshRate"

    /**
     * Identifies whether the current device is a Vivo or iQOO device where forcing
     * raw display modes can trigger vendor display HAL / SurfaceFlinger instability.
     */
    fun isVivoOrIqoo(): Boolean {
        return runCatching {
            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase(Locale.ROOT)
            val brand = Build.BRAND.orEmpty().lowercase(Locale.ROOT)
            val fingerprint = Build.FINGERPRINT.orEmpty().lowercase(Locale.ROOT)
            val model = Build.MODEL.orEmpty().lowercase(Locale.ROOT)

            manufacturer.contains("vivo") || manufacturer.contains("iqoo") ||
                brand.contains("vivo") || brand.contains("iqoo") ||
                fingerprint.contains("vivo") || fingerprint.contains("iqoo") ||
                model.contains("vivo") || model.contains("iqoo")
        }.getOrDefault(false)
    }

    /**
     * Default preference setting. Defaults to false on Vivo & iQOO devices for safety,
     * and true on other modern high refresh rate devices.
     */
    fun getDefaultPeakRefreshRate(): Boolean {
        return !isVivoOrIqoo()
    }

    /**
     * Defensively applies or resets the peak refresh rate on the provided Window.
     * Guaranteed to never throw an uncaught exception or crash the host activity.
     */
    fun applyPeakRefreshRate(activity: Activity?, forceHighRate: Boolean) {
        val window = activity?.window ?: return
        applyPeakRefreshRate(window, forceHighRate)
    }

    /**
     * Defensively applies or resets the peak refresh rate on the provided Window.
     */
    fun applyPeakRefreshRate(window: Window, forceHighRate: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                applyForAndroidRAndAbove(window, forceHighRate)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                applyForLegacyAndroid(window, forceHighRate)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to apply refresh rate safely: ${t.message}")
            fallbackToDefault(window)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
    private fun applyForAndroidRAndAbove(window: Window, forceHighRate: Boolean) {
        val display: Display? = runCatching { window.context.display }.getOrNull()

        if (!forceHighRate || display == null) {
            resetWindowRefreshRate(window)
            return
        }

        val supportedModes = runCatching { display.supportedModes }.getOrNull()
        if (supportedModes.isNullOrEmpty()) {
            resetWindowRefreshRate(window)
            return
        }

        val currentMode = runCatching { display.mode }.getOrNull()
        val currentWidth = currentMode?.physicalWidth ?: 0
        val currentHeight = currentMode?.physicalHeight ?: 0

        // Strict resolution matching to prevent resolution-switching HAL crashes
        val compatibleHighRateModes = supportedModes.filter { mode ->
            val matchesResolution = (currentWidth == 0 || mode.physicalWidth == currentWidth) &&
                (currentHeight == 0 || mode.physicalHeight == currentHeight)
            val isValidRefreshRate = mode.refreshRate in 85f..241f
            matchesResolution && isValidRefreshRate
        }

        val targetMode = compatibleHighRateModes.maxByOrNull { it.refreshRate }

        if (targetMode != null && targetMode.modeId != 0) {
            val params = window.attributes
            params.preferredDisplayModeId = targetMode.modeId
            window.attributes = params
            Log.d(TAG, "Applied high refresh rate mode: ${targetMode.refreshRate}Hz (modeId: ${targetMode.modeId})")
        } else {
            // Fallback: If no dedicated matching mode ID found or already at optimal rate, reset override safely
            resetWindowRefreshRate(window)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.M)
    private fun applyForLegacyAndroid(window: Window, forceHighRate: Boolean) {
        val params = window.attributes
        params.preferredRefreshRate = if (forceHighRate) 120f else 0f
        window.attributes = params
    }

    private fun resetWindowRefreshRate(window: Window) {
        try {
            val params = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                params.preferredDisplayModeId = 0
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                params.preferredRefreshRate = 0f
            }
            window.attributes = params
        } catch (t: Throwable) {
            Log.w(TAG, "Error resetting window refresh rate: ${t.message}")
        }
    }

    private fun fallbackToDefault(window: Window) {
        try {
            resetWindowRefreshRate(window)
        } catch (_: Throwable) {}
    }
}
