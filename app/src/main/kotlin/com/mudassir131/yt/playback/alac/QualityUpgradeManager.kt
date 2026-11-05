/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.playback.alac

import androidx.media3.common.Player
import com.mudassir131.yt.db.entities.FormatEntity
import com.mudassir131.yt.extensions.currentMetadata
import com.mudassir131.yt.playback.MusicService
import com.mudassir131.yt.playback.alac.addon.AddonLosslessAudioProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Manages background audio quality upgrades for playing tracks.
 * Real-time lossless quality upgrade engine:
 * 1. Playback begins instantly on YouTube Opus/AAC without startup latency (<500ms).
 * 2. Background worker queries user-configured Lossless Addon.
 * 3. Shows "Upgrading quality" in the player scrubber badge while searching.
 * 4. When a lossless candidate is resolved, seamlessly hot-swaps playback to Hi-Res Lossless.
 */
object QualityUpgradeManager {
    private const val TAG = "QualityUpgrade"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _upgradingMediaId = MutableStateFlow<String?>(null)
    val upgradingMediaId = _upgradingMediaId.asStateFlow()

    private var activeUpgradeJob: Job? = null
    @Volatile
    private var lastUpgradedMediaId: String? = null

    fun isUpgrading(mediaId: String?): Boolean {
        if (mediaId == null) return false
        return _upgradingMediaId.value == mediaId
    }

    fun startUpgradeIfEligible(
        service: MusicService,
        mediaId: String,
        title: String?,
        artist: String?,
        durationSeconds: Int?,
    ) {
        val config = AddonLosslessAudioProvider.getConfig()
        if (config == null || !config.enabled || config.url.isBlank()) {
            return
        }

        if (mediaId.isBlank()) return

        // If player is already playing the upgraded stream for this mediaId, skip
        val activeItem = service.player.currentMediaItem
        val isAlreadyPlayingUpgraded = activeItem != null &&
            activeItem.mediaId == mediaId &&
            activeItem.localConfiguration?.uri?.toString()?.contains("?q=hifi") == true

        if (isAlreadyPlayingUpgraded && lastUpgradedMediaId == mediaId) {
            Timber.tag(TAG).d("Track $mediaId is already actively playing upgraded lossless.")
            return
        }

        activeUpgradeJob?.cancel()
        activeUpgradeJob = scope.launch {
            try {
                _upgradingMediaId.value = mediaId

                // Resolve title and artist robustly
                var effectiveTitle = title?.trim().orEmpty()
                var effectiveArtist = artist?.trim().orEmpty()
                var effectiveDuration = durationSeconds ?: -1

                if (effectiveTitle.isBlank()) {
                    val meta = service.player.currentMetadata ?: service.currentMediaMetadata.value
                    if (meta != null) {
                        effectiveTitle = meta.title.orEmpty()
                        effectiveArtist = meta.artists.joinToString { it.name }
                        effectiveDuration = meta.duration ?: effectiveDuration
                    }
                }

                if (effectiveTitle.isBlank()) {
                    val dbSong = runCatching { service.database.song(mediaId).first() }.getOrNull()
                    if (dbSong != null) {
                        effectiveTitle = dbSong.song.title
                        effectiveArtist = dbSong.artists.joinToString { it.name }
                        effectiveDuration = dbSong.song.duration ?: effectiveDuration
                    }
                }

                if (effectiveTitle.isBlank()) {
                    delay(400)
                    val dbSong = runCatching { service.database.song(mediaId).first() }.getOrNull()
                    if (dbSong != null) {
                        effectiveTitle = dbSong.song.title
                        effectiveArtist = dbSong.artists.joinToString { it.name }
                        effectiveDuration = dbSong.song.duration ?: effectiveDuration
                    }
                    if (effectiveTitle.isBlank()) {
                        val meta = service.player.currentMetadata ?: service.currentMediaMetadata.value
                        effectiveTitle = meta?.title.orEmpty()
                        effectiveArtist = meta?.artists?.joinToString { it.name }.orEmpty()
                        effectiveDuration = meta?.duration ?: effectiveDuration
                    }
                }

                if (effectiveTitle.isBlank()) {
                    Timber.tag(TAG).w("Cannot upgrade quality: title is still blank for $mediaId")
                    return@launch
                }

                Timber.tag(TAG).i("[UPGRADE_START] Upgrading quality for '$effectiveTitle' by '$effectiveArtist' ($mediaId)...")

                val cached = LosslessStreamResolver.getCachedResult(mediaId)
                val targetStreamUrl: String
                val contentLength: Long?
                val isAlac: Boolean
                val isHiRes: Boolean
                val sampleRate: Int
                val bitDepth: Int
                val channels: Int
                val bitrate: Int?

                if (cached != null && (cached.format.mimeType.contains("alac", ignoreCase = true) || cached.format.mimeType.contains("flac", ignoreCase = true))) {
                    Timber.tag(TAG).i("Using cached lossless stream for $mediaId: ${cached.streamUrl}")
                    targetStreamUrl = cached.streamUrl
                    isAlac = cached.format.mimeType.contains("alac", ignoreCase = true)
                    sampleRate = cached.format.audioSampleRate ?: 48000
                    bitDepth = if (sampleRate > 48000) 24 else 16
                    channels = cached.format.audioChannels ?: 2
                    bitrate = cached.format.bitrate
                    contentLength = cached.format.contentLength ?: AddonLosslessAudioProvider.probeContentLength(targetStreamUrl)
                    val mb = (contentLength ?: 0L) / (1024.0 * 1024.0)
                    isHiRes = if (mb > 0.0) mb > 50.0 else (sampleRate > 48000 || bitDepth > 16)
                } else {
                    val resolved = AddonLosslessAudioProvider.resolve(
                        videoId = mediaId,
                        title = effectiveTitle,
                        artist = effectiveArtist,
                        durationSeconds = effectiveDuration,
                    ) ?: run {
                        Timber.tag(TAG).d("[UPGRADE_UNAVAILABLE] No lossless stream returned by addon for '$effectiveTitle'")
                        return@launch
                    }

                    Timber.tag(TAG).i("[UPGRADE_SUCCESS] Addon provided lossless stream: ${resolved.url}")
                    LosslessStreamResolver.cacheExplicitStream(mediaId, resolved)
                    targetStreamUrl = resolved.url
                    isAlac = resolved.codec.contains("alac", ignoreCase = true) || resolved.mimeType.contains("alac", ignoreCase = true)
                    sampleRate = resolved.sampleRate
                    bitDepth = resolved.bitDepth
                    channels = resolved.channels
                    bitrate = resolved.bitrate
                    contentLength = resolved.contentLength ?: AddonLosslessAudioProvider.probeContentLength(targetStreamUrl)
                    val mb = (contentLength ?: 0L) / (1024.0 * 1024.0)
                    isHiRes = if (mb > 0.0) mb > 50.0 else (resolved.sampleRate > 48000 || resolved.bitDepth > 16 || resolved.codec.contains("24"))
                }

                lastUpgradedMediaId = mediaId
                service.updatePlaybackUrl(mediaId, targetStreamUrl)
                val finalCodec = if (isAlac) (if (isHiRes) "alac.24" else "alac") else (if (isHiRes) "flac.24" else "flac")
                val finalMime = if (isAlac) "audio/alac" else "audio/flac"

                service.database.query {
                    upsert(
                        FormatEntity(
                            id = mediaId,
                            itag = 999,
                            mimeType = finalMime,
                            codecs = finalCodec,
                            bitrate = bitrate ?: (sampleRate * bitDepth * 2 * 6 / 10),
                            sampleRate = sampleRate,
                            contentLength = contentLength ?: -1L,
                            loudnessDb = null,
                            perceptualLoudnessDb = null,
                            playbackUrl = targetStreamUrl,
                        )
                    )
                }

                val formatInfo = AudioFormatInfo(
                    codec = if (isAlac) "ALAC" else "FLAC",
                    mimeType = finalMime,
                    isLossless = true,
                    isHiRes = isHiRes,
                    sampleRate = sampleRate,
                    bitDepth = bitDepth,
                    channelCount = channels,
                    bitrate = bitrate,
                    contentLength = contentLength,
                    itag = 999,
                    source = "addon_lossless",
                    decoderName = if (isAlac) "AlacDecoder" else "FlacDecoder",
                    outputInfo = service.nativeAudioEngine.audioOutputInfo.value,
                )
                service.liveAudioFormat.value = formatInfo

                withContext(Dispatchers.Main) {
                    service.swapToUpgradedLossless(mediaId, targetStreamUrl)
                }
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Error during background quality upgrade for $mediaId")
            } finally {
                if (_upgradingMediaId.value == mediaId) {
                    _upgradingMediaId.value = null
                }
            }
        }
    }

    fun cancelForTrack(mediaId: String?) {
        if (mediaId != null && _upgradingMediaId.value == mediaId) {
            activeUpgradeJob?.cancel()
            _upgradingMediaId.value = null
        }
    }

    fun markUpgrading(mediaId: String?) {
        _upgradingMediaId.value = mediaId
    }

    fun clearUpgrade(mediaId: String? = null) {
        if (mediaId == null || _upgradingMediaId.value == mediaId) {
            _upgradingMediaId.value = null
        }
    }

}
