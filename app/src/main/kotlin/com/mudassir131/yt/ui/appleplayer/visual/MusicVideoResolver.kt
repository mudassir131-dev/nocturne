/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.innertube.models.YouTubeClient
import com.mudassir131.yt.innertube.models.response.PlayerResponse
import com.mudassir131.yt.innertube.pages.NewPipeUtils
import com.mudassir131.yt.models.MediaMetadata
import com.mudassir131.yt.utils.StreamClientUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor
import org.schabi.newpipe.extractor.stream.VideoStream
import timber.log.Timber

object MusicVideoResolver {
    private const val TAG = "MusicVideoResolver"

    private val VIDEO_CLIENTS = listOf(
        YouTubeClient.ANDROID_VR_1_61_48,
        YouTubeClient.ANDROID_VR_NO_AUTH,
        YouTubeClient.MOBILE,
        YouTubeClient.TVHTML5,
        YouTubeClient.ANDROID_TESTSUITE,
        YouTubeClient.WEB,
        YouTubeClient.WEB_REMIX,
    )

    suspend fun resolve(
        metadata: MediaMetadata,
        isMetered: Boolean = false,
        dataSaver: Boolean = false,
    ): VisualContent.MusicVideo? = withContext(Dispatchers.IO) {
        val title = metadata.title.trim()
        val artist = metadata.artists.firstOrNull()?.name.orEmpty().trim()
        if (title.isBlank()) return@withContext null

        val trackId = metadata.id
        Timber.tag(TAG).d("Resolving music video for: '$title' by '$artist' (id=$trackId)")

        // 1. Check cache first
        val cached = MusicVideoCache.getMetadata(trackId)
        if (cached != null) {
            if (cached.videoId == null) {
                Timber.tag(TAG).d("Cached negative music video result for $trackId")
                return@withContext null
            }
            Timber.tag(TAG).d("Found cached videoId: ${cached.videoId} for $trackId")
            val streamResult = resolveVideoStream(
                videoId = cached.videoId,
                title = cached.title ?: title,
                artist = cached.artist ?: artist,
                thumbnailUrl = cached.thumbnailUrl ?: metadata.thumbnailUrl,
                durationMs = cached.durationMs ?: (metadata.duration.toLong() * 1000L),
                isMetered = isMetered,
                dataSaver = dataSaver,
            )
            return@withContext streamResult
        }

        // 2. Check if track itself is an 11-char YouTube ID and is an OMV or UGC video
        var resolvedVideoId: String? = null
        var resolvedTitle = title
        var resolvedArtist = artist
        var resolvedThumbnail = metadata.thumbnailUrl
        var resolvedDurationMs = if (metadata.duration > 0) metadata.duration.toLong() * 1000L else null

        if (trackId.length == 11 && !trackId.contains("/")) {
            runCatching {
                val playerRes = YouTube.player(trackId, client = YouTubeClient.MOBILE).getOrNull()
                    ?: YouTube.player(trackId, client = YouTubeClient.WEB_REMIX).getOrNull()
                val details = playerRes?.videoDetails
                val mvType = details?.musicVideoType
                if (mvType == "MUSIC_VIDEO_TYPE_OMV" || mvType == "MUSIC_VIDEO_TYPE_UGC" || mvType == "MUSIC_VIDEO_TYPE_OFFICIAL_SOURCE_MUSIC_VIDEO") {
                    resolvedVideoId = trackId
                    resolvedTitle = details.title
                    resolvedArtist = details.author
                    resolvedThumbnail = details.thumbnail.thumbnails.lastOrNull()?.url ?: metadata.thumbnailUrl
                    resolvedDurationMs = details.lengthSeconds.toLongOrNull()?.times(1000L)
                    Timber.tag(TAG).d("Current playing track $trackId is directly an Official Music Video (type=$mvType)")
                }
            }
        }

        // 3. Multi-tier search queries on YouTube Music for the Official Music Video
        if (resolvedVideoId == null) {
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", "Feat.", "Ft.").firstOrNull()?.trim() ?: artist
            val searchQueries = listOf(
                "$cleanArtist $title",
                "$cleanArtist $title official music video",
                "$title $cleanArtist",
            ).filter { it.isNotBlank() }.distinct()

            for (query in searchQueries) {
                Timber.tag(TAG).d("Searching YouTube for music video with query: '$query'")

                val searchResult = runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_VIDEO).getOrNull()
                }.getOrNull()

                val candidates = searchResult?.items?.mapNotNull { item ->
                    when (item) {
                        is SongItem -> {
                            val mvType = item.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                            MusicVideoMatcher.Candidate(
                                videoId = item.id,
                                title = item.title,
                                artist = item.artists.joinToString { it.name },
                                durationSec = item.duration,
                                thumbnailUrl = item.thumbnail,
                                musicVideoType = mvType,
                            )
                        }
                        else -> null
                    }
                }.orEmpty()

                val bestMatch = MusicVideoMatcher.findBestMatch(
                    trackTitle = title,
                    trackArtist = artist,
                    trackAlbum = metadata.album?.title,
                    trackDurationSec = metadata.duration.takeIf { it > 0 },
                    candidates = candidates,
                )

                if (bestMatch != null) {
                    resolvedVideoId = bestMatch.videoId
                    resolvedTitle = bestMatch.title
                    resolvedArtist = bestMatch.artist
                    resolvedThumbnail = bestMatch.thumbnailUrl ?: metadata.thumbnailUrl
                    resolvedDurationMs = bestMatch.durationSec?.toLong()?.times(1000L) ?: resolvedDurationMs
                    Timber.tag(TAG).i("Matched official music video: '${bestMatch.title}' (id=${bestMatch.videoId})")
                    break
                }
            }
        }

        // 3b. SearchSummary fallback if still not resolved
        if (resolvedVideoId == null) {
            val cleanArtist = artist.split(",", "&", "feat.", "ft.", "Feat.", "Ft.").firstOrNull()?.trim() ?: artist
            runCatching {
                val summary = YouTube.searchSummary("$cleanArtist $title").getOrNull()
                val summaryCandidates = summary?.summaries?.flatMap { it.items }?.mapNotNull { item ->
                    when (item) {
                        is SongItem -> {
                            val mvType = item.endpoint?.watchEndpointMusicSupportedConfigs?.watchEndpointMusicConfig?.musicVideoType
                            MusicVideoMatcher.Candidate(
                                videoId = item.id,
                                title = item.title,
                                artist = item.artists.joinToString { it.name },
                                durationSec = item.duration,
                                thumbnailUrl = item.thumbnail,
                                musicVideoType = mvType,
                            )
                        }
                        else -> null
                    }
                }.orEmpty()

                val bestMatch = MusicVideoMatcher.findBestMatch(
                    trackTitle = title,
                    trackArtist = artist,
                    trackAlbum = metadata.album?.title,
                    trackDurationSec = metadata.duration.takeIf { it > 0 },
                    candidates = summaryCandidates,
                )

                if (bestMatch != null) {
                    resolvedVideoId = bestMatch.videoId
                    resolvedTitle = bestMatch.title
                    resolvedArtist = bestMatch.artist
                    resolvedThumbnail = bestMatch.thumbnailUrl ?: metadata.thumbnailUrl
                    resolvedDurationMs = bestMatch.durationSec?.toLong()?.times(1000L) ?: resolvedDurationMs
                    Timber.tag(TAG).i("Matched music video via summary: '${bestMatch.title}' (id=${bestMatch.videoId})")
                }
            }
        }

        // Cache the metadata lookup result (either positive or negative)
        MusicVideoCache.putMetadata(
            trackId = trackId,
            videoId = resolvedVideoId,
            title = resolvedTitle,
            artist = resolvedArtist,
            thumbnailUrl = resolvedThumbnail,
            durationMs = resolvedDurationMs,
        )

        if (resolvedVideoId == null) {
            return@withContext null
        }

        // 4. Resolve stream URL
        resolveVideoStream(
            videoId = resolvedVideoId,
            title = resolvedTitle,
            artist = resolvedArtist,
            thumbnailUrl = resolvedThumbnail,
            durationMs = resolvedDurationMs,
            isMetered = isMetered,
            dataSaver = dataSaver,
        )
    }

    suspend fun resolveVideoStream(
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        durationMs: Long?,
        isMetered: Boolean,
        dataSaver: Boolean,
        targetItag: Int? = null,
    ): VisualContent.MusicVideo? = withContext(Dispatchers.IO) {
        val qualityTag = targetItag?.toString() ?: if (dataSaver) "SAVER" else if (isMetered) "METERED" else "HIGH"

        val cachedStream = MusicVideoCache.getStream(videoId, qualityTag)
        if (cachedStream != null) {
            return@withContext VisualContent.MusicVideo(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs,
                streamUrl = cachedStream.streamUrl,
                qualityLabel = cachedStream.qualityLabel,
                itag = cachedStream.itag,
                availableQualities = cachedStream.availableQualities,
            )
        }

        // 1. Primary Resolution: Use NewPipe extractor for true unthrottled HD/4K (2160p, 1440p, 1080p, 720p) video streams
        val newPipeResult = runCatching {
            resolveWithNewPipe(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs,
                isMetered = isMetered,
                dataSaver = dataSaver,
                targetItag = targetItag,
                qualityTag = qualityTag,
            )
        }.getOrNull()

        if (newPipeResult != null) {
            Timber.tag(TAG).i("Resolved high-quality video stream via NewPipe: ${newPipeResult.qualityLabel} (itag=${newPipeResult.itag}) for $videoId")
            return@withContext newPipeResult
        }

        // 2. Fallback Resolution: Innertube player responses across multiple clients
        Timber.tag(TAG).w("NewPipe stream resolution failed or returned no streams for $videoId; falling back to Innertube clients")
        var selectedFormat: PlayerResponse.StreamingData.Format? = null
        var selectedClient: YouTubeClient? = null
        var playerResponse: PlayerResponse? = null
        var resolvedStreamUrl: String? = null
        val availableQualities = mutableListOf<VideoQualityOption>()

        val signatureTimestamp = runCatching {
            NewPipeUtils.getSignatureTimestamp(videoId).getOrNull()
        }.getOrNull()

        for (client in VIDEO_CLIENTS) {
            val response = runCatching {
                YouTube.player(videoId, null, client, signatureTimestamp).getOrNull()
            }.getOrNull() ?: continue

            if (response.playabilityStatus.status != "OK") continue
            val streamingData = response.streamingData ?: continue

            // Collect all video formats
            val videoFormats = (streamingData.adaptiveFormats + (streamingData.formats ?: emptyList()))
                .filter { it.width != null && it.height != null }
                .filter { it.url != null || it.signatureCipher != null || it.cipher != null }

            if (videoFormats.isEmpty()) continue

            // Build available qualities
            val qualities = videoFormats
                .mapNotNull { format ->
                    val h = format.height
                    if (h != null && h > 0) {
                        val label = when {
                            h >= 2160 -> "4K (${h}p)"
                            h >= 1440 -> "2K (${h}p)"
                            h >= 1080 -> "1080p (FHD)"
                            h >= 720 -> "720p (HD)"
                            else -> format.qualityLabel ?: "${h}p"
                        }
                        VideoQualityOption(
                            itag = format.itag,
                            label = label,
                            height = h,
                        )
                    } else null
                }
                .distinctBy { it.height }
                .sortedByDescending { it.height }

            if (availableQualities.isEmpty()) {
                availableQualities.addAll(qualities)
            }

            // Order candidates: if targetItag requested, prioritize that; otherwise sort by highest quality and prefer direct URLs
            val candidateFormats = if (targetItag != null) {
                videoFormats.filter { it.itag == targetItag } + videoFormats.filter { it.itag != targetItag }
            } else {
                orderVideoFormats(videoFormats, isMetered, dataSaver)
            }

            for (candidate in candidateFormats) {
                val url = runCatching {
                    NewPipeUtils.getStreamUrl(candidate, videoId, client).getOrNull()
                }.getOrNull()

                if (!url.isNullOrBlank()) {
                    selectedFormat = candidate
                    selectedClient = client
                    playerResponse = response
                    resolvedStreamUrl = url
                    break
                }
            }

            if (resolvedStreamUrl != null) break
        }

        if (selectedFormat == null || selectedClient == null || playerResponse == null || resolvedStreamUrl == null) {
            Timber.tag(TAG).w("Could not find playable video format for videoId: $videoId")
            return@withContext null
        }

        // Ensure cver matches the client used for resolution
        val streamUrl = StreamClientUtils.patchClientVersion(resolvedStreamUrl, selectedClient.clientVersion)

        val selectedLabel = when {
            (selectedFormat.height ?: 0) >= 2160 -> "4K (${selectedFormat.height}p)"
            (selectedFormat.height ?: 0) >= 1440 -> "2K (${selectedFormat.height}p)"
            (selectedFormat.height ?: 0) >= 1080 -> "1080p (FHD)"
            (selectedFormat.height ?: 0) >= 720 -> "720p (HD)"
            else -> selectedFormat.qualityLabel ?: "${selectedFormat.height}p"
        }

        val expiresIn = playerResponse.streamingData?.expiresInSeconds ?: 21600
        MusicVideoCache.putStream(
            videoId = videoId,
            qualityTag = qualityTag,
            streamUrl = streamUrl,
            qualityLabel = selectedLabel,
            itag = selectedFormat.itag,
            expiresInSeconds = expiresIn,
            availableQualities = availableQualities,
        )

        VisualContent.MusicVideo(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            durationMs = durationMs,
            streamUrl = streamUrl,
            qualityLabel = selectedLabel,
            itag = selectedFormat.itag,
            availableQualities = availableQualities,
        )
    }

    private fun resolveWithNewPipe(
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        durationMs: Long?,
        isMetered: Boolean,
        dataSaver: Boolean,
        targetItag: Int?,
        qualityTag: String,
    ): VisualContent.MusicVideo? {
        NewPipeUtils.ensureInitialized()
        val service = ServiceList.YouTube
        val extractor = service.getStreamExtractor("https://www.youtube.com/watch?v=$videoId") as YoutubeStreamExtractor
        extractor.fetchPage()

        val allStreams = (extractor.videoOnlyStreams.orEmpty() + extractor.videoStreams.orEmpty())
            .filter { !it.url.isNullOrBlank() || !it.content.isNullOrBlank() }

        if (allStreams.isEmpty()) return null

        // Collect available qualities
        val availableQualities = allStreams
            .mapNotNull { stream ->
                val h = stream.height
                if (h > 0) {
                    val itag = if (stream.itag > 0) stream.itag else stream.formatId
                    val label = when {
                        h >= 2160 -> "4K (${h}p)"
                        h >= 1440 -> "2K (${h}p)"
                        h >= 1080 -> "1080p (FHD)"
                        h >= 720 -> "720p (HD)"
                        else -> "${h}p"
                    }
                    VideoQualityOption(
                        itag = itag,
                        label = label,
                        height = h,
                    )
                } else null
            }
            .distinctBy { it.height }
            .sortedByDescending { it.height }

        val targetHeight = when {
            dataSaver -> 480
            isMetered -> 720
            else -> 2160 // Up to 4K UHD
        }

        // Select candidate stream
        val candidate = if (targetItag != null) {
            allStreams.firstOrNull { it.itag == targetItag || it.formatId == targetItag }
                ?: allStreams.firstOrNull()
        } else {
            allStreams.sortedWith(
                compareByDescending<VideoStream> { it.height <= targetHeight }
                    .thenByDescending { it.height } // 2160p -> 1440p -> 1080p -> 720p
                    .thenByDescending { it.bitrate }
                    .thenByDescending {
                        val fmt = it.format?.name.orEmpty()
                        if (fmt.contains("MP4", ignoreCase = true)) 1 else 0
                    }
            ).firstOrNull()
        } ?: return null

        val streamUrl = candidate.url?.takeIf { it.isNotBlank() }
            ?: candidate.content?.takeIf { it.isNotBlank() }
            ?: return null

        val selectedItag = if (candidate.itag > 0) candidate.itag else candidate.formatId
        val h = candidate.height
        val selectedLabel = when {
            h >= 2160 -> "4K (${h}p)"
            h >= 1440 -> "2K (${h}p)"
            h >= 1080 -> "1080p (FHD)"
            h >= 720 -> "720p (HD)"
            else -> "${h}p"
        }

        MusicVideoCache.putStream(
            videoId = videoId,
            qualityTag = qualityTag,
            streamUrl = streamUrl,
            qualityLabel = selectedLabel,
            itag = selectedItag,
            expiresInSeconds = 21600, // 6 hours
            availableQualities = availableQualities,
        )

        return VisualContent.MusicVideo(
            videoId = videoId,
            title = title,
            artist = artist,
            thumbnailUrl = thumbnailUrl,
            durationMs = durationMs,
            streamUrl = streamUrl,
            qualityLabel = selectedLabel,
            itag = selectedItag,
            availableQualities = availableQualities,
        )
    }

    private fun orderVideoFormats(
        formats: List<PlayerResponse.StreamingData.Format>,
        isMetered: Boolean,
        dataSaver: Boolean,
    ): List<PlayerResponse.StreamingData.Format> {
        val targetHeight = when {
            dataSaver -> 480
            isMetered -> 720
            else -> 2160 // Support up to 4K UHD
        }

        return formats.sortedWith(
            compareByDescending<PlayerResponse.StreamingData.Format> { (it.height ?: 0) <= targetHeight }
                .thenByDescending { it.height ?: 0 } // Prefer highest resolution (4K 2160p -> 1440p -> 1080p -> 720p)
                .thenByDescending { it.bitrate }
                .thenByDescending { if (it.mimeType.contains("mp4", ignoreCase = true)) 1 else 0 }
                .thenByDescending { it.url != null }
        )
    }
}
