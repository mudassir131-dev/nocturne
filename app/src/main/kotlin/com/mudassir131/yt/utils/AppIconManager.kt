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
        
        // 1. Enable the new launcher component first
        val newCompName = ComponentName(context, "${context.packageName}${newStyle.activityName}")
        try {
            pm.setComponentEnabledSetting(
                newCompName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            Timber.d("Enabled new app icon component: ${newCompName.className}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable component: ${newCompName.className}")
        }

        // 2. Disable all other launcher components
        AppIconStyle.values().filter { it != newStyle }.forEach { style ->
            val compName = ComponentName(context, "${context.packageName}${style.activityName}")
            try {
                pm.setComponentEnabledSetting(
                    compName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Timber.d("Disabled old app icon component: ${compName.className}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to disable component: ${compName.className}")
            }
        }
    }
}
