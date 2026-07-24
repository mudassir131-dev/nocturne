/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.constants.EnableUpdateNotificationKey
import com.mudassir131.yt.constants.UpdateChannel
import com.mudassir131.yt.constants.UpdateChannelKey

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dataStore = applicationContext.dataStore

            val isEnabled = dataStore.data.map { it[EnableUpdateNotificationKey] ?: false }.first()
            if (!isEnabled) return Result.success()

            val updateChannel = dataStore.data.map {
                it[UpdateChannelKey]?.let { value ->
                    try { UpdateChannel.valueOf(value) } catch (e: Exception) { UpdateChannel.STABLE }
                } ?: UpdateChannel.STABLE
            }.first()

            if (updateChannel == UpdateChannel.NIGHTLY) return Result.success()

            Updater.getLatestReleaseInfo().onSuccess { release ->
                if (compareSemanticVersions(release.tagName, BuildConfig.VERSION_NAME) > 0) {
                    UpdateNotificationManager.notifyIfNewVersion(applicationContext, release)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
