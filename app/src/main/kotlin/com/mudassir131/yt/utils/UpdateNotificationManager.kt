/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.MainActivity
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.EnableUpdateNotificationKey
import com.mudassir131.yt.constants.LastNotifiedVersionKey
import com.mudassir131.yt.constants.LastUpdateCheckKey
import com.mudassir131.yt.constants.UpdateChannel
import com.mudassir131.yt.constants.UpdateChannelKey
import java.util.concurrent.TimeUnit

object UpdateNotificationManager {
    private const val CHANNEL_ID = "update_notification_channel"
    private const val NOTIFICATION_ID = 9999
    private const val WORK_NAME = "update_check_work"
    private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.update_notification_channel_name)
            val descriptionText = context.getString(R.string.update_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun schedulePeriodicUpdateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val updateCheckRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            6, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            updateCheckRequest
        )
    }

    fun cancelPeriodicUpdateCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun checkForUpdates(context: Context) {
        scope.launch {
            try {
                val dataStore = context.dataStore

                // Always query the latest version first to see if they are running an older version
                val latestResult = Updater.getLatestVersionName()
                val latestVersion = latestResult.getOrNull()

                if (latestVersion != null && latestVersion != BuildConfig.VERSION_NAME) {
                    // Persistent update notification bypasses user settings & intervals
                    showUpdateNotification(context, latestVersion)
                    return@launch
                } else {
                    cancelUpdateNotification(context)
                }

                val isEnabled = dataStore.data.map { it[EnableUpdateNotificationKey] ?: true }.first()
                if (!isEnabled) {
                    cancelPeriodicUpdateCheck(context)
                    return@launch
                }

                schedulePeriodicUpdateCheck(context)

                val updateChannel = dataStore.data.map { 
                    it[UpdateChannelKey]?.let { value -> 
                        try { UpdateChannel.valueOf(value) } catch (e: Exception) { UpdateChannel.STABLE }
                    } ?: UpdateChannel.STABLE
                }.first()

                if (updateChannel == UpdateChannel.NIGHTLY) return@launch

                val lastCheck = dataStore.data.map { it[LastUpdateCheckKey] ?: 0L }.first()
                val now = System.currentTimeMillis()

                if (now - lastCheck < CHECK_INTERVAL_MS) return@launch

                dataStore.edit { it[LastUpdateCheckKey] = now }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    suspend fun notifyIfNewVersion(context: Context, latestVersion: String) {
        try {
            if (latestVersion != BuildConfig.VERSION_NAME) {
                showUpdateNotification(context, latestVersion)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }

    private fun showUpdateNotification(context: Context, newVersion: String) {
        createNotificationChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "settings/update")
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(Updater.getLatestDownloadUrl()))
        val downloadPendingIntent = PendingIntent.getActivity(
            context,
            1,
            downloadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val notificationIcon = if (isDark) R.drawable.ic_nocturne_notification_dark else R.drawable.ic_nocturne_notification_light

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(notificationIcon)
            .setContentTitle("Update Available: $newVersion")
            .setContentText("Features: Content Filtration, Song Card Share, Playlist Import")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.download,
                "Download Now",
                downloadPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }

    fun cancelUpdateNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
