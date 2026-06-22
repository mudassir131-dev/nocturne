package com.mudassir131.yt.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

enum class AppIconStyle(val key: String, val activityName: String) {
    ECLIPSE("eclipse", ".MainActivityEclipse"),
    MIDNIGHT("midnight", ".MainActivityMidnight"),
    AURA("aura", ".MainActivityAura"),
    PULSE("pulse", ".MainActivityPulse");

    companion object {
        fun fromKey(key: String?): AppIconStyle {
            return values().firstOrNull { it.key == key } ?: ECLIPSE
        }
    }
}

object AppIconManager {
    fun setAppIcon(context: Context, newStyle: AppIconStyle) {
        val pm = context.packageManager
        
        AppIconStyle.values().forEach { style ->
            val compName = ComponentName(context, "${context.packageName}${style.activityName}")
            val state = if (style == newStyle) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            try {
                pm.setComponentEnabledSetting(
                    compName,
                    state,
                    PackageManager.DONT_KILL_APP
                )
                Timber.d("Component ${compName.className} set to state $state")
            } catch (e: Exception) {
                Timber.e(e, "Failed to set component state for ${compName.className} to $state")
            }
        }
    }
}
