/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.utils.youtube

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.getAsync
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * What a projected import would cost against the quota still available today.
 *
 * [tracksCoverable] is how many tracks the Data API can actually match before the quota runs out;
 * the rest fall through to the keyless InnerTube path.
 */
data class QuotaEstimate(
    val trackCount: Int,
    val estimatedUnits: Int,
    val remainingUnits: Int,
    val tracksCoverable: Int,
    val willExceed: Boolean,
)

/**
 * Daily ledger for YouTube Data API v3 quota, persisted in DataStore.
 *
 * The cap is per-project per-day and Google resets it at midnight **US/Pacific**, not at device-local
 * midnight — so the stored date is compared in [QUOTA_RESET_ZONE]. Without that, a user east of
 * Pacific gets their quota "reset" hours early and every call after it fails with `quotaExceeded`.
 *
 * The ledger is advisory: [YouTubeDataApi] still handles a real `quotaExceeded` response, since a
 * project may have spent units elsewhere or been granted a raised cap.
 */
object YouTubeQuotaTracker {

    private const val TAG = "YouTubeMatch"

    val QUOTA_RESET_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val QuotaDateKey = stringPreferencesKey("youtube_quota_date")
    private val QuotaUnitsUsedKey = intPreferencesKey("youtube_quota_units_used")

    /**
     * Units a single track costs: one `search.list` plus its share of a batched `videos.list`.
     *
     * The duration call is charged as a whole unit per track rather than 1/50th — rounding up keeps
     * the estimate honest, and one unit either way is noise against the 100 that search costs.
     */
    const val UNITS_PER_TRACK = YouTubeDataApi.COST_SEARCH_LIST + YouTubeDataApi.COST_VIDEOS_LIST

    /** Serializes read-then-write so two concurrent reservations can't both pass the same check. */
    private val mutex = Mutex()

    /** The quota day, as Google reckons it. */
    fun currentQuotaDate(now: Instant = Instant.now()): String =
        DATE_FORMAT.format(now.atZone(QUOTA_RESET_ZONE))

    /**
     * Projects the cost of matching [trackCount] tracks against [remainingUnits].
     *
     * Pure so the arithmetic behind the pre-flight warning is testable without a Context.
     */
    fun estimate(
        trackCount: Int,
        remainingUnits: Int,
        unitsPerTrack: Int = UNITS_PER_TRACK,
    ): QuotaEstimate {
        val safeTracks = trackCount.coerceAtLeast(0)
        val safeRemaining = remainingUnits.coerceAtLeast(0)
        val perTrack = unitsPerTrack.coerceAtLeast(1)
        val estimated = safeTracks.toLong() * perTrack
        val coverable = (safeRemaining / perTrack).coerceAtMost(safeTracks)
        return QuotaEstimate(
            trackCount = safeTracks,
            estimatedUnits = estimated.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            remainingUnits = safeRemaining,
            tracksCoverable = coverable,
            willExceed = estimated > safeRemaining,
        )
    }

    /** Units spent today, zero once the Pacific date has rolled over. */
    suspend fun usedUnits(context: Context): Int {
        val storedDate = context.dataStore.getAsync(QuotaDateKey)
        if (storedDate != currentQuotaDate()) return 0
        return context.dataStore.getAsync(QuotaUnitsUsedKey, 0)
    }

    suspend fun remainingUnits(
        context: Context,
        dailyQuota: Int = YouTubeDataApi.DEFAULT_DAILY_QUOTA_UNITS,
    ): Int = (dailyQuota - usedUnits(context)).coerceAtLeast(0)

    /**
     * Claims [units] if they fit in what is left today, spending them in the same atomic step.
     *
     * Returns false without spending anything when they don't fit — the caller's cue to stop using
     * the Data API and match through InnerTube instead.
     */
    suspend fun reserve(
        context: Context,
        units: Int,
        dailyQuota: Int = YouTubeDataApi.DEFAULT_DAILY_QUOTA_UNITS,
    ): Boolean {
        if (units <= 0) return true
        return mutex.withLock {
            var granted = false
            context.dataStore.edit { prefs ->
                val today = currentQuotaDate()
                val used = if (prefs[QuotaDateKey] == today) prefs[QuotaUnitsUsedKey] ?: 0 else 0
                if (used + units <= dailyQuota) {
                    prefs[QuotaDateKey] = today
                    prefs[QuotaUnitsUsedKey] = used + units
                    granted = true
                } else {
                    // Pin the ledger to today so a stale date can't read as fresh headroom.
                    prefs[QuotaDateKey] = today
                    prefs[QuotaUnitsUsedKey] = used
                }
            }
            if (!granted) Timber.tag(TAG).w("Quota reservation of %d units refused", units)
            granted
        }
    }

    /**
     * Records units already spent, whether or not they were reserved — used to true the ledger up
     * after an unreserved call, and it may push usage past [dailyQuota].
     */
    suspend fun record(context: Context, units: Int) {
        if (units <= 0) return
        mutex.withLock {
            context.dataStore.edit { prefs ->
                val today = currentQuotaDate()
                val used = if (prefs[QuotaDateKey] == today) prefs[QuotaUnitsUsedKey] ?: 0 else 0
                prefs[QuotaDateKey] = today
                prefs[QuotaUnitsUsedKey] = used + units
            }
        }
    }

    /** Hands the whole day's quota back. Only for a user-initiated reset in Settings. */
    suspend fun reset(context: Context) {
        mutex.withLock {
            context.dataStore.edit { prefs ->
                prefs[QuotaDateKey] = currentQuotaDate()
                prefs[QuotaUnitsUsedKey] = 0
            }
        }
    }
}
