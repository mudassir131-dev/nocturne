/*
 * Nocturne - Production-Ready Music Video & Visual Architecture
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.appleplayer.visual

import android.view.TextureView
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.utils.StreamClientUtils
import okhttp3.OkHttpClient
import timber.log.Timber
import kotlin.math.abs

@Composable
fun MusicVideoPlayer(
    musicVideo: VisualContent.MusicVideo,
    isPlaying: Boolean,
    position: Long,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    muted: Boolean = true,
    onVideoReady: () -> Unit = {},
    onPlaybackFailed: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lifecycleStarted by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            lifecycleStarted = when (event) {
                Lifecycle.Event.ON_START, Lifecycle.Event.ON_RESUME -> true
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> false
                else -> lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val shouldPlay = isPlaying && lifecycleStarted
    val streamUrl = musicVideo.streamUrl?.takeIf { it.isNotBlank() } ?: run {
        LaunchedEffect(Unit) { onPlaybackFailed() }
        return
    }

    var isVideoReady by remember(streamUrl) { mutableStateOf(false) }
    var videoAspectRatio by remember(streamUrl) { mutableFloatStateOf(16f / 9f) }
    var retryCount by remember(streamUrl) { mutableIntStateOf(0) }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .proxy(YouTube.proxy)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("youtube.com") ||
                        host.endsWith("youtube-nocookie.com") ||
                        host.endsWith("ytimg.com")

                if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

                val clientParam = request.url.queryParameter("c")?.trim().orEmpty()
                val userAgent = StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

                val builder = request.newBuilder().header("User-Agent", userAgent)
                originReferer.origin?.let { builder.header("Origin", it) }
                originReferer.referer?.let { builder.header("Referer", it) }

                chain.proceed(builder.build())
            }
            .build()
    }

    val mediaSourceFactory = remember(okHttpClient) {
        DefaultMediaSourceFactory(
            DefaultDataSource.Factory(
                context,
                OkHttpDataSource.Factory(okHttpClient),
            ),
        )
    }

    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                30_000,
                1_500,
                2_500,
            )
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build()
            .apply {
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(1280, 720)
                    .setMaxVideoBitrate(2_500_000)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                    .build()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    false,
                )
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                volume = 0f
                repeatMode = Player.REPEAT_MODE_OFF
                playWhenReady = shouldPlay
            }
    }

    // Keep volume muted for background video
    LaunchedEffect(muted) {
        exoPlayer.volume = 0f
    }

    // Sync play/pause with audio player
    LaunchedEffect(shouldPlay) {
        if (exoPlayer.playWhenReady != shouldPlay) {
            exoPlayer.playWhenReady = shouldPlay
        }
    }

    var lastSyncedPosition by remember(streamUrl) { mutableStateOf(-1L) }

    // Sync playback position with audio player smoothly:
    // Only seek when user explicitly jumps/seeks (> 3500ms jump) or on initial sync.
    // For small drift (400ms - 3500ms), adjust playback rate to catch up seamlessly without clearing the buffer.
    LaunchedEffect(position) {
        val currentVideoPos = exoPlayer.currentPosition
        val driftMs = abs(currentVideoPos - position)

        if (lastSyncedPosition < 0L || driftMs > 3500L) {
            lastSyncedPosition = position
            exoPlayer.seekTo(position)
            exoPlayer.setPlaybackSpeed(1.0f)
        } else if (driftMs > 400L) {
            if (currentVideoPos < position) {
                exoPlayer.setPlaybackSpeed(1.05f)
            } else {
                exoPlayer.setPlaybackSpeed(0.95f)
            }
        } else {
            if (exoPlayer.playbackParameters.speed != 1.0f) {
                exoPlayer.setPlaybackSpeed(1.0f)
            }
        }
    }

    DisposableEffect(exoPlayer, streamUrl) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Timber.w(error, "Music video playback error for ${musicVideo.videoId}, retryCount=$retryCount")
                if (retryCount < 1) {
                    retryCount++
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = shouldPlay
                } else {
                    onPlaybackFailed()
                }
            }

            override fun onRenderedFirstFrame() {
                isVideoReady = true
                onVideoReady()
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = videoSize.width.toFloat() / videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(streamUrl, exoPlayer) {
        val normalized = streamUrl.trim()
        val mediaItem = if (normalized.contains(".m3u8", ignoreCase = true)) {
            MediaItem.Builder()
                .setUri(normalized)
                .setMimeType(MimeTypes.APPLICATION_M3U8)
                .build()
        } else {
            val detectedMime = when {
                normalized.contains("video%2Fwebm", ignoreCase = true) ||
                    normalized.contains("video/webm", ignoreCase = true) ||
                    normalized.contains(".webm", ignoreCase = true) -> MimeTypes.VIDEO_WEBM
                normalized.contains("video%2Fmp4", ignoreCase = true) ||
                    normalized.contains("video/mp4", ignoreCase = true) ||
                    normalized.contains(".mp4", ignoreCase = true) -> MimeTypes.VIDEO_MP4
                else -> null
            }
            MediaItem.Builder()
                .setUri(normalized)
                .apply { if (detectedMime != null) setMimeType(detectedMime) }
                .build()
        }

        exoPlayer.stop()
        isVideoReady = false
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        if (position > 0) {
            exoPlayer.seekTo(position)
        }
        exoPlayer.playWhenReady = shouldPlay
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVideoReady) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "videoAlpha",
    )

    Box(modifier = modifier) {
        AndroidView(
            factory = { viewContext ->
                AspectRatioFrameLayout(viewContext).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    this.resizeMode = resizeMode

                    val textureView = TextureView(viewContext).apply {
                        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    }
                    addView(textureView)
                    exoPlayer.setVideoTextureView(textureView)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                }
            },
            update = { frameLayout ->
                frameLayout.setAspectRatio(videoAspectRatio)
                frameLayout.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize().alpha(alpha),
        )
    }
}
