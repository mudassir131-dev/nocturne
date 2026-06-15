/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt

import com.mudassir131.yt.ui.component.FluidSlidingNavigationBar
import android.annotation.SuppressLint
import android.Manifest
import android.app.ActivityManager
import android.app.ForegroundServiceStartNotAllowedException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.valentinilk.shimmer.LocalShimmerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.mudassir131.yt.utils.PreferenceStore
import kotlinx.coroutines.withContext
import com.mudassir131.yt.constants.AppBarHeight
import com.mudassir131.yt.constants.AppLanguageKey
import com.mudassir131.yt.constants.CustomThemeColorKey
import com.mudassir131.yt.constants.DarkModeKey
import com.mudassir131.yt.constants.DefaultOpenTabKey
import com.mudassir131.yt.constants.DisableScreenshotKey
import com.mudassir131.yt.constants.DynamicThemeKey
import com.mudassir131.yt.constants.HasPressedStarKey
import com.mudassir131.yt.constants.LaunchCountKey
import com.mudassir131.yt.constants.MiniPlayerBottomSpacing
import com.mudassir131.yt.constants.MiniPlayerHeight
import com.mudassir131.yt.constants.NavigationBarAnimationSpec
import com.mudassir131.yt.constants.NavigationBarHeight
import com.mudassir131.yt.constants.PauseSearchHistoryKey
import com.mudassir131.yt.constants.PureBlackKey
import com.mudassir131.yt.constants.RemindAfterKey
import com.mudassir131.yt.constants.SYSTEM_DEFAULT
import com.mudassir131.yt.constants.SearchSource
import com.mudassir131.yt.constants.SearchSourceKey
import com.mudassir131.yt.constants.SlimNavBarHeight
import com.mudassir131.yt.constants.SlimNavBarKey


import com.mudassir131.yt.constants.StopMusicOnTaskClearKey
import com.mudassir131.yt.constants.UseNewMiniPlayerDesignKey
import com.mudassir131.yt.constants.UseSystemFontKey
import com.mudassir131.yt.db.MusicDatabase
import com.mudassir131.yt.db.entities.SearchHistory
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.extensions.toMediaItem
import com.mudassir131.yt.playback.DownloadUtil
import com.mudassir131.yt.playback.MusicService
import com.mudassir131.yt.playback.MusicService.MusicBinder
import com.mudassir131.yt.playback.PlayerConnection
import com.mudassir131.yt.playback.queues.ListQueue

import com.mudassir131.yt.ui.component.BottomSheetMenu
import com.mudassir131.yt.ui.component.BottomSheetPage
import com.mudassir131.yt.ui.component.COLLAPSED_ANCHOR
import com.mudassir131.yt.ui.component.DISMISSED_ANCHOR
import com.mudassir131.yt.ui.component.EXPANDED_ANCHOR
import com.mudassir131.yt.ui.component.IconButton

import com.mudassir131.yt.ui.component.LocalBottomSheetPageState
import com.mudassir131.yt.ui.component.LocalMenuState
import com.mudassir131.yt.ui.component.StarDialog
import com.mudassir131.yt.ui.component.TopSearch
import com.mudassir131.yt.ui.component.rememberBottomSheetState
import com.mudassir131.yt.ui.component.shimmer.ShimmerTheme
import com.mudassir131.yt.ui.menu.YouTubeSongMenu
import com.mudassir131.yt.ui.player.BottomSheetPlayer
import com.mudassir131.yt.ui.screens.Screens
import com.mudassir131.yt.ui.screens.navigationBuilder
import com.mudassir131.yt.ui.screens.search.LocalSearchScreen
import com.mudassir131.yt.ui.screens.search.OnlineSearchScreen
import com.mudassir131.yt.ui.screens.settings.DarkMode
import com.mudassir131.yt.ui.screens.settings.DiscordPresenceManager
import com.mudassir131.yt.ui.screens.settings.NavigationTab
import com.mudassir131.yt.ui.theme.VeluneTheme
import com.mudassir131.yt.ui.theme.ColorSaver
import com.mudassir131.yt.ui.theme.DefaultThemeColor
import com.mudassir131.yt.ui.theme.extractThemeColor
import com.mudassir131.yt.ui.theme.glassmorphic
import com.mudassir131.yt.constants.GlassEffectsKey
import com.mudassir131.yt.constants.GlassEffectsMode
import com.mudassir131.yt.ui.utils.appBarScrollBehavior
import com.mudassir131.yt.ui.utils.backToMain
import com.mudassir131.yt.ui.utils.resetHeightOffset
import com.mudassir131.yt.utils.SyncUtils
import com.mudassir131.yt.utils.dataStore
import com.mudassir131.yt.utils.get
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.utils.rememberPreference
import com.mudassir131.yt.utils.reportException
import com.mudassir131.yt.utils.setAppLocale
import com.mudassir131.yt.viewmodels.HomeViewModel
import com.mudassir131.yt.utils.Updater
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject

@Suppress("DEPRECATION", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var syncUtils: SyncUtils

    private lateinit var navController: NavHostController
    private var pendingIntent: Intent? = null
    private var pendingDeepLinkSong: PendingDeepLinkSong? = null
    private var pendingTogetherJoinLink: String? = null

    private var playerConnection by mutableStateOf<PlayerConnection?>(null)
    private var isMusicServiceBound = false

    private val serviceConnection =
        object : ServiceConnection {
            override fun onServiceConnected(
                name: ComponentName?,
                service: IBinder?,
            ) {
                isMusicServiceBound = true
                if (service is MusicBinder) {
                    playerConnection =
                        PlayerConnection(this@MainActivity, service, database, lifecycleScope)
                    playPendingDeepLinkSongIfReady()
                    joinPendingTogetherIfReady()
                }
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                isMusicServiceBound = false
                playerConnection?.dispose()
                playerConnection = null
            }
        }

    private data class PendingDeepLinkSong(
        val mediaItem: MediaItem,
    )

    private fun playPendingDeepLinkSongIfReady() {
        val pending = pendingDeepLinkSong ?: return
        val connection = playerConnection ?: return
        pendingDeepLinkSong = null
        connection.playQueue(ListQueue(items = listOf(pending.mediaItem)))
    }

    private fun joinPendingTogetherIfReady() {
        val pending = pendingTogetherJoinLink ?: return
        val connection = playerConnection ?: return
        pendingTogetherJoinLink = null
        lifecycleScope.launch(Dispatchers.IO) {
            val displayName =
                runCatching { dataStore.data.first()[com.mudassir131.yt.constants.TogetherDisplayNameKey] }
                    .getOrNull()
                    ?.trim()
                    .orEmpty()
                    .ifBlank { Build.MODEL ?: getString(R.string.app_name) }
            withContext(Dispatchers.Main) {
                connection.service.joinTogether(pending, displayName)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        startMusicServiceSafely()
        isMusicServiceBound =
            bindService(
                Intent(this, MusicService::class.java),
                serviceConnection,
                BIND_AUTO_CREATE
            )
        playPendingDeepLinkSongIfReady()
    }

    private fun safeUnbindMusicService() {
        if (!isMusicServiceBound) return
        try {
            unbindService(serviceConnection)
        } catch (e: IllegalArgumentException) {
        } catch (e: Exception) {
            reportException(e)
        } finally {
            isMusicServiceBound = false
        }
    }

    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        return appProcesses.any { processInfo ->
            processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    processInfo.processName == packageName
        }
    }

    private fun startMusicServiceSafely() {
        val startIntent = Intent(this, MusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (isAppInForeground()) {
                    startService(startIntent)
                }
            } catch (e: ForegroundServiceStartNotAllowedException) {
                reportException(e)
            } catch (e: IllegalStateException) {
                reportException(e)
            } catch (e: SecurityException) {
                reportException(e)
            } catch (e: Exception) {
                reportException(e)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                startService(startIntent)
            } catch (e: IllegalStateException) {
                reportException(e)
            } catch (e: SecurityException) {
                reportException(e)
            } catch (e: Exception) {
                reportException(e)
            }
        } else {
            try {
                startService(startIntent)
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }

    override fun onStop() {
        safeUnbindMusicService()
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only clear/stop presence when the activity is actually finishing (not on rotation)
        // and do not clear it for transient configuration changes.
        if (isFinishing && !isChangingConfigurations) {
            try { DiscordPresenceManager.stop() } catch (_: Exception) {}
        }

        val shouldStopOnTaskClear =
            if (!isFinishing) {
                false
            } else {
                dataStore.get(StopMusicOnTaskClearKey, false)
            }

        if (shouldStopOnTaskClear) {
            safeUnbindMusicService()
            stopService(Intent(this, MusicService::class.java))
            playerConnection = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::navController.isInitialized) {
            handleDeepLinkIntent(intent, navController)
        } else {
            pendingIntent = intent
        }
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Anti-tamper verification
        initializeTelemetryConfig()

        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            val initialLocale = PreferenceStore.get(AppLanguageKey)
                ?.takeUnless { it == SYSTEM_DEFAULT }
                ?.let { Locale.forLanguageTag(it) }
                ?: Locale.getDefault()
            setAppLocale(this, initialLocale)

            lifecycleScope.launch(Dispatchers.IO) {
                runCatching {
                    dataStore.data.first()[AppLanguageKey]
                }.onSuccess { lang ->
                    val targetLocale = lang
                        ?.takeUnless { it == SYSTEM_DEFAULT }
                        ?.let { Locale.forLanguageTag(it) }
                        ?: Locale.getDefault()
                    if (targetLocale != initialLocale) {
                        withContext(Dispatchers.Main) {
                            setAppLocale(this@MainActivity, targetLocale)
                            recreate()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            dataStore.data
                .map { it[DisableScreenshotKey] ?: false }
                .distinctUntilChanged()
                .collectLatest {
                    withContext(Dispatchers.Main) {
                        if (it) {
                            window.setFlags(
                                WindowManager.LayoutParams.FLAG_SECURE,
                                WindowManager.LayoutParams.FLAG_SECURE,
                            )
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                }
        }

        setContent {
            var showSplashScreen by remember { mutableStateOf(true) }
            var latestVersionName by remember { mutableStateOf("") }
            val releaseNotesState = remember { mutableStateOf<String?>(null) }

            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        playerConnection?.service?.refreshPlaybackNotification()
                    }
                }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

                Updater.getLatestVersionName().onSuccess {
                    latestVersionName = it
                }
                com.mudassir131.yt.utils.UpdateNotificationManager.checkForUpdates(this@MainActivity)
            }

            // Use remembered instances so the same state object is used everywhere
            // (previously retrieving the composition local directly created different
            // instances in different composition scopes which caused the update
            // bottom sheet to not appear and overlay interactions to be blocked).
            val bottomSheetPageState = remember { com.mudassir131.yt.ui.component.BottomSheetPageState() }
            val menuState = remember { com.mudassir131.yt.ui.component.MenuState() }
            LocalUriHandler.current

            val updateSheetContent: @Composable ColumnScope.() -> Unit = {
                val uriHandler = LocalUriHandler.current
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.update),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "New Update Available!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Version $latestVersionName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "What's New:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Content Filtration",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Filter content and manage restrictions under Content settings.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Song Card Share on Instagram & Snapchat",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Share beautiful high-res song cards directly to Instagram & Snapchat stories.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "• ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = "Playlist Import",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Easily import Spotify playlists in the background.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { bottomSheetPageState.dismiss() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Later",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        Button(
                            onClick = {
                                val downloadUrl = Updater.getLatestDownloadUrl()
                                uriHandler.openUri(downloadUrl)
                                bottomSheetPageState.dismiss()
                            },
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Download Now",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            LaunchedEffect(latestVersionName) {
                val cleanLatest = latestVersionName
                    .removePrefix("Velune ")
                    .removePrefix("v")
                    .trim()
                if (cleanLatest.isNotEmpty() && cleanLatest != BuildConfig.VERSION_NAME) {
                    Updater.getLatestReleaseNotes().onSuccess {
                        releaseNotesState.value = it
                    }.onFailure {
                        releaseNotesState.value = null
                    }
                    bottomSheetPageState.show(updateSheetContent)
                }
            }


            val enableDynamicTheme by rememberPreference(DynamicThemeKey, defaultValue = true)
            val customThemeColorValue by rememberPreference(CustomThemeColorKey, defaultValue = "default")
            val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.ON)
            val useSystemFont by rememberPreference(UseSystemFontKey, defaultValue = false)
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme =
                remember(darkTheme, isSystemInDarkTheme) {
                    if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
                }
            LaunchedEffect(useDarkTheme) {
                setSystemBarAppearance(useDarkTheme)
            }
            val pureBlackEnabled by rememberPreference(PureBlackKey, defaultValue = true)
            val pureBlack = pureBlackEnabled && useDarkTheme

            val customThemeSeedPalette = remember(customThemeColorValue) {
                if (customThemeColorValue.startsWith("#")) {
                    null
                } else if (customThemeColorValue.startsWith("seedPalette:")) {
                    com.mudassir131.yt.ui.theme.ThemeSeedPaletteCodec.decodeFromPreference(customThemeColorValue)
                } else {
                    com.mudassir131.yt.ui.screens.settings.ThemePalettes
                        .findById(customThemeColorValue)
                        ?.let {
                            com.mudassir131.yt.ui.theme.ThemeSeedPalette(
                                primary = it.primary,
                                secondary = it.secondary,
                                tertiary = it.tertiary,
                                neutral = it.neutral,
                            )
                        }
                }
            }

            val customThemeColor = remember(customThemeColorValue, customThemeSeedPalette) {
                if (customThemeColorValue.startsWith("#")) {
                    try {
                        val colorString = customThemeColorValue.removePrefix("#")
                        Color(android.graphics.Color.parseColor("#$colorString"))
                    } catch (e: Exception) {
                        DefaultThemeColor
                    }
                } else {
                    customThemeSeedPalette?.primary ?: DefaultThemeColor
                }
            }

            var themeColor by rememberSaveable(stateSaver = ColorSaver) {
                mutableStateOf(DefaultThemeColor)
            }

            LaunchedEffect(playerConnection, enableDynamicTheme, isSystemInDarkTheme, customThemeColor) {
                val playerConnection = playerConnection
                if (!enableDynamicTheme || playerConnection == null) {
                    themeColor = if (!enableDynamicTheme) customThemeColor else DefaultThemeColor
                    return@LaunchedEffect
                }
                playerConnection.service.currentMediaMetadata.collectLatest { song ->
                    if (song != null) {
                        withContext(Dispatchers.Default) {
                            try {
                                val result = imageLoader.execute(
                                    ImageRequest
                                        .Builder(this@MainActivity)
                                        .data(song.thumbnailUrl)
                                        .allowHardware(false)
                                        .build(),
                                )
                                val extractedColor = result.image?.toBitmap()?.extractThemeColor()
                                withContext(Dispatchers.Main) {
                                    themeColor = extractedColor ?: DefaultThemeColor
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    themeColor = DefaultThemeColor
                                }
                            }
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            themeColor = DefaultThemeColor
                        } else {
                            themeColor = customThemeColor
                        }
                    }
                }
            }

            com.mudassir131.yt.ui.theme.ProvideGlassmorphismState {
                VeluneTheme(
                    darkTheme = useDarkTheme,
                    pureBlack = pureBlack,
                    themeColor = themeColor,
                    seedPalette = if (!enableDynamicTheme) customThemeSeedPalette else null,
                    useSystemFont = useSystemFont,
                ) {
                    BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(
                                if(pureBlack) Color.Black else MaterialTheme.colorScheme.surface
                            )
                ) {
                    val focusManager = LocalFocusManager.current
                    val density = LocalDensity.current
                    val windowsInsets = WindowInsets.systemBars
                    val bottomInset = with(density) { windowsInsets.getBottom(density).toDp() }
                    WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

                    val useRail = currentWindowAdaptiveInfo().windowSizeClass
                        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

                    val navController = rememberNavController()
                    val homeViewModel: HomeViewModel = hiltViewModel()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val (_) = rememberSaveable { mutableStateOf("home") }
                    val currentRoute = navBackStackEntry?.destination?.route
                    val isYearInMusicScreen = currentRoute == "year_in_music"

                    val navigationItems = remember { Screens.MainScreens }
                    val (slimNav) = rememberPreference(SlimNavBarKey, defaultValue = false)
                    val (useNewMiniPlayerDesign) = rememberPreference(UseNewMiniPlayerDesignKey, defaultValue = true)
                    val defaultOpenTab by rememberEnumPreference(DefaultOpenTabKey, NavigationTab.HOME)
                    val pauseSearchHistory by rememberPreference(PauseSearchHistoryKey, defaultValue = false)
                    val tabOpenedFromShortcut =
                        remember {
                            when (intent?.action) {
                                ACTION_LIBRARY -> NavigationTab.LIBRARY
                                else -> null
                            }
                        }


                    val topLevelScreens =
                        listOf(
                            Screens.Home.route,
                            Screens.Stats.route,
                            Screens.History.route,
                            Screens.Library.route,
                        )

                    val (query, onQueryChange) =
                        rememberSaveable(stateSaver = TextFieldValue.Saver) {
                            mutableStateOf(TextFieldValue())
                        }

                    var active by rememberSaveable {
                        mutableStateOf(false)
                    }

                    val onActiveChange: (Boolean) -> Unit = { newActive ->
                        active = newActive
                        if (!newActive) {
                            focusManager.clearFocus()
                            if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) {
                                onQueryChange(TextFieldValue())
                            }
                        }
                    }

                    var searchSource by rememberEnumPreference(SearchSourceKey, SearchSource.ONLINE)

                    val searchBarFocusRequester = remember { FocusRequester() }

                    val onSearch: (String) -> Unit = {
                        if (it.isNotEmpty()) {
                            onActiveChange(false)
                            navController.navigate("search/${URLEncoder.encode(it, "UTF-8")}")
                            if (!pauseSearchHistory) {
                                database.query {
                                    insert(SearchHistory(query = it))
                                }
                            }
                        }
                    }

                    var openSearchImmediately: Boolean by remember {
                        mutableStateOf(intent?.action == ACTION_SEARCH)
                    }

                    val shouldShowSearchBar =
                        remember(active, navBackStackEntry) {
                            active ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                    navBackStackEntry?.destination?.route?.startsWith("search/") == true
                        }

                    val shouldShowNavigationBar =
                        remember(navBackStackEntry, active) {
                            navBackStackEntry?.destination?.route == null ||
                                    navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } &&
                                    !active
                        }

                    fun getBottomNavPadding(): Dp {
                        return if (shouldShowNavigationBar && !useRail) {
                            if (slimNav) SlimNavBarHeight else NavigationBarHeight
                        } else {
                            0.dp
                        }
                    }

                    val floatingBarsBottomPadding = 8.dp
                    val navVisibleHeight = if (slimNav) SlimNavBarHeight else NavigationBarHeight

                    val bottomNavigationBarHeight by animateDpAsState(
                        targetValue = if (shouldShowNavigationBar && !useRail) navVisibleHeight else 0.dp,
                        animationSpec = NavigationBarAnimationSpec,
                        label = "",
                    )

                    val playerBottomSheetState =
                        rememberBottomSheetState(
                            dismissedBound = 0.dp,
                            collapsedBound =
                                bottomInset +
                                        (if (shouldShowNavigationBar && !useRail) floatingBarsBottomPadding else 0.dp) +
                                        getBottomNavPadding() +
                                        (if (useNewMiniPlayerDesign) MiniPlayerBottomSpacing else 0.dp) +
                                        MiniPlayerHeight,
                            expandedBound = maxHeight,
                        )

                    var yearInMusicSavedPlayerAnchor by rememberSaveable { mutableIntStateOf(-1) }

                    LaunchedEffect(isYearInMusicScreen) {
                        val controller = WindowCompat.getInsetsController(window, window.decorView)
                        if (isYearInMusicScreen) {
                            controller.systemBarsBehavior =
                                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                            controller.hide(WindowInsetsCompat.Type.statusBars())
                        } else {
                            controller.show(WindowInsetsCompat.Type.statusBars())
                        }
                    }

                    LaunchedEffect(isYearInMusicScreen, playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect

                        if (isYearInMusicScreen) {
                            if (yearInMusicSavedPlayerAnchor == -1) {
                                yearInMusicSavedPlayerAnchor =
                                    when {
                                        playerBottomSheetState.isExpanded -> EXPANDED_ANCHOR
                                        playerBottomSheetState.isCollapsed -> COLLAPSED_ANCHOR
                                        playerBottomSheetState.isDismissed -> DISMISSED_ANCHOR
                                        else -> COLLAPSED_ANCHOR
                                    }
                            }

                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else if (yearInMusicSavedPlayerAnchor != -1) {
                            val anchorToRestore = yearInMusicSavedPlayerAnchor
                            yearInMusicSavedPlayerAnchor = -1

                            if (player.currentMediaItem == null) {
                                playerBottomSheetState.dismiss()
                            } else {
                                when (anchorToRestore) {
                                    EXPANDED_ANCHOR -> playerBottomSheetState.expandSoft()
                                    COLLAPSED_ANCHOR -> playerBottomSheetState.collapseSoft()
                                    DISMISSED_ANCHOR -> playerBottomSheetState.dismiss()
                                    else -> playerBottomSheetState.collapseSoft()
                                }
                            }
                        }
                    }

                    val playerAwareWindowInsets =
                        remember(
                            useRail,
                            bottomInset,
                            shouldShowNavigationBar,
                            playerBottomSheetState.isDismissed,
                        ) {
                            var bottom = bottomInset
                            if (shouldShowNavigationBar && !useRail) bottom += getBottomNavPadding()
                            if (!playerBottomSheetState.isDismissed) bottom += MiniPlayerHeight
                            windowsInsets
                                .only((if(useRail) {
                                    WindowInsetsSides.Right
                                } else WindowInsetsSides.Horizontal) + WindowInsetsSides.Top)
                                .add(WindowInsets(top = AppBarHeight, bottom = bottom))
                        }

                    appBarScrollBehavior(
                        canScroll = {
                            navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                    (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                        }
                    )

                    val searchBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )
                    val topAppBarScrollBehavior =
                        appBarScrollBehavior(
                            canScroll = {
                                navBackStackEntry?.destination?.route?.startsWith("search/") == false &&
                                        (playerBottomSheetState.isCollapsed || playerBottomSheetState.isDismissed)
                            },
                        )

                    var previousRoute by rememberSaveable { mutableStateOf<String?>(null) }

                    LaunchedEffect(navBackStackEntry) {
                        val currentRoute = navBackStackEntry?.destination?.route
                        val wasOnNonTopLevelScreen = previousRoute != null &&
                                previousRoute !in topLevelScreens &&
                                previousRoute?.startsWith("search/") != true
                        val isReturningToHomeOrLibrary = currentRoute == Screens.Home.route ||
                                currentRoute == Screens.Library.route

                        if (wasOnNonTopLevelScreen && isReturningToHomeOrLibrary) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                        }

                        previousRoute = currentRoute

                        if (navBackStackEntry?.destination?.route?.startsWith("search/") == true) {
                            val searchQuery =
                                withContext(Dispatchers.IO) {
                                    if (navBackStackEntry
                                            ?.arguments
                                            ?.getString(
                                                "query",
                                            )!!
                                            .contains(
                                                "%",
                                            )
                                    ) {
                                        navBackStackEntry?.arguments?.getString(
                                            "query",
                                        )!!
                                    } else {
                                        URLDecoder.decode(
                                            navBackStackEntry?.arguments?.getString("query")!!,
                                            "UTF-8"
                                        )
                                    }
                                }
                            onQueryChange(
                                TextFieldValue(
                                    searchQuery,
                                    TextRange(searchQuery.length)
                                )
                            )
                        } else if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } || navBackStackEntry?.destination?.route in topLevelScreens) {
                            onQueryChange(TextFieldValue())
                            if (navBackStackEntry?.destination?.route != Screens.Home.route) {
                                searchBarScrollBehavior.state.resetHeightOffset()
                                topAppBarScrollBehavior.state.resetHeightOffset()
                            }
                        }
                    }
                    LaunchedEffect(active) {
                        if (active) {
                            searchBarScrollBehavior.state.resetHeightOffset()
                            topAppBarScrollBehavior.state.resetHeightOffset()
                            searchBarFocusRequester.requestFocus()
                        }
                    }

                    LaunchedEffect(playerConnection) {
                        val player = playerConnection?.player ?: return@LaunchedEffect
                        val connection = playerConnection ?: return@LaunchedEffect
                        connection.queueRestoreCompleted.first { it }
                        if (player.currentMediaItem == null) {
                            if (!playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.dismiss()
                            }
                        } else {
                            if (!isYearInMusicScreen && playerBottomSheetState.isDismissed) {
                                playerBottomSheetState.collapseSoft()
                            }
                        }
                    }

                    DisposableEffect(playerConnection, playerBottomSheetState) {
                        val player =
                            playerConnection?.player ?: return@DisposableEffect onDispose { }
                        val listener =
                            object : Player.Listener {
                                override fun onMediaItemTransition(
                                    mediaItem: MediaItem?,
                                    reason: Int,
                                ) {
                                    if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED &&
                                        mediaItem != null &&
                                        playerBottomSheetState.isDismissed &&
                                        !isYearInMusicScreen
                                    ) {
                                        playerBottomSheetState.collapseSoft()
                                    }
                                }
                            }
                        player.addListener(listener)
                        onDispose {
                            player.removeListener(listener)
                        }
                    }

                    var shouldShowTopBar by rememberSaveable { mutableStateOf(false) }

                    LaunchedEffect(navBackStackEntry) {
                        shouldShowTopBar =
                            !active && navBackStackEntry?.destination?.route in topLevelScreens && navBackStackEntry?.destination?.route != "settings"
                    }

                    val coroutineScope = rememberCoroutineScope()
                    var sharedSong: SongItem? by remember {
                        mutableStateOf(null)
                    }

                    LaunchedEffect(Unit) {
                        if (pendingIntent != null) {
                            handleDeepLinkIntent(pendingIntent!!, navController)
                            pendingIntent = null
                        } else {
                            handleDeepLinkIntent(intent, navController)
                        }
                    }

                    var showStarDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        delay(3000)

                        withContext(Dispatchers.IO) {
                            val current = dataStore[LaunchCountKey] ?: 0
                            val newCount = current + 1
                            dataStore.edit { prefs ->
                                prefs[LaunchCountKey] = newCount
                            }
                        }

                        val shouldShow = withContext(Dispatchers.IO) {
                            val hasPressed = dataStore[HasPressedStarKey] ?: false
                            val remindAfter = dataStore[RemindAfterKey] ?: 3
                            !hasPressed && (dataStore[LaunchCountKey] ?: 0) >= remindAfter
                        }

                        if (shouldShow) {
                            var waited = 0L
                            val waitStep = 500L
                            val maxWait = 30_000L
                            while (bottomSheetPageState.isVisible && waited < maxWait) {
                                delay(waitStep)
                                waited += waitStep
                            }
                            showStarDialog = true
                        }
                    }

                    if (showStarDialog) {
                        StarDialog(
                            onDismissRequest = { showStarDialog = false },
                            onStar = {
                                coroutineScope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            dataStore.edit { prefs ->
                                                prefs[HasPressedStarKey] = true
                                                prefs[RemindAfterKey] = Int.MAX_VALUE
                                            }
                                        }
                                    } catch (e: Exception) {
                                        reportException(e)
                                    } finally {
                                        showStarDialog = false
                                    }
                                }
                            },
                            onLater = {
                                coroutineScope.launch {
                                    try {
                                        val launch = withContext(Dispatchers.IO) { dataStore[LaunchCountKey] ?: 0 }
                                        withContext(Dispatchers.IO) {
                                            dataStore.edit { prefs ->
                                                prefs[RemindAfterKey] = launch + 10
                                            }
                                        }
                                    } catch (e: Exception) {
                                        reportException(e)
                                    } finally {
                                        showStarDialog = false
                                    }
                                }
                            }
                        )
                    }

                    remember(navBackStackEntry) {
                        when (navBackStackEntry?.destination?.route) {
                            Screens.Home.route -> R.string.home
                            Screens.Search.route -> R.string.search
                            Screens.Library.route -> R.string.filter_library
                            else -> null
                        }
                    }

                    CompositionLocalProvider(
                        LocalDatabase provides database,
                        LocalContentColor provides if (pureBlack) Color.White else contentColorFor(MaterialTheme.colorScheme.surface),
                        LocalPlayerConnection provides playerConnection,
                        LocalPlayerAwareWindowInsets provides playerAwareWindowInsets,
                        LocalDownloadUtil provides downloadUtil,
                        LocalShimmerTheme provides ShimmerTheme,
                        LocalSyncUtils provides syncUtils,
                        LocalBottomSheetPageState provides bottomSheetPageState,
                        LocalMenuState provides menuState,
                    ) {
                        Row {
                            AnimatedVisibility(useRail && shouldShowNavigationBar) {
                                NavigationRail(
                                    containerColor = if(pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = if(pureBlack) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    header = { Spacer(Modifier.height(24.dp)) }
                                ) {
                                    navigationItems.fastForEach { screen ->
                                        val isSelected =
                                            navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

                                        NavigationRailItem(
                                            selected = isSelected,
                                            icon = {
                                                Icon(
                                                    painter = painterResource(
                                                        id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                                                    ),
                                                    contentDescription = stringResource(screen.titleId),
                                                )
                                            },
                                            label = {
                                                if (!slimNav) {
                                                    Text(
                                                        text = stringResource(screen.titleId),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                            },
                                            onClick = {
                                                val wasPlayerActive = playerBottomSheetState.isExpanded

                                                if(wasPlayerActive) {
                                                    playerBottomSheetState.collapse(spring())
                                                }

                                                if (isSelected) {
                                                    if(wasPlayerActive) return@NavigationRailItem

                                                    navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                    coroutineScope.launch {
                                                        searchBarScrollBehavior.state.resetHeightOffset()
                                                    }
                                                } else {
                                                    navController.navigate(screen.route) {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                        )
                                    }
                                }
                            }

                            Scaffold(
                                topBar = {
                                    if (shouldShowTopBar) {
                                        val isTransparentTopBarScreen = navBackStackEntry?.destination?.route in listOf(
                                            Screens.Home.route,
                                            Screens.Library.route,
                                            Screens.History.route,
                                            Screens.Stats.route
                                        )

                                        val shouldShowBlurBackground = remember(navBackStackEntry) {
                                            isTransparentTopBarScreen
                                        }

                                        val surfaceColor = MaterialTheme.colorScheme.surface
                                        val currentScrollBehavior = if (isTransparentTopBarScreen) searchBarScrollBehavior else topAppBarScrollBehavior

                                        Box(
                                            modifier = Modifier.offset {
                                                IntOffset(
                                                    x = 0,
                                                    y = currentScrollBehavior.state.heightOffset.toInt()
                                                )
                                            }
                                        ) {
                                            // Gradient shadow background
                                            if (shouldShowBlurBackground) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(AppBarHeight + with(LocalDensity.current) {
                                                            WindowInsets.systemBars.getTop(LocalDensity.current).toDp()
                                                        })
                                                        .background(
                                                            Brush.verticalGradient(
                                                                colors = listOf(
                                                                    surfaceColor.copy(alpha = 0.95f),
                                                                    surfaceColor.copy(alpha = 0.85f),
                                                                    surfaceColor.copy(alpha = 0.6f),
                                                                    Color.Transparent
                                                                )
                                                            )
                                                        )
                                                )
                                            }

                                            TopAppBar(
                                                windowInsets = WindowInsets.safeDrawing.only((if(useRail) {
                                                    WindowInsetsSides.Right
                                                } else WindowInsetsSides.Horizontal) + WindowInsetsSides.Top),
                                                title = {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        // app icon
                                                        Image(
                                                            painter = painterResource(id = R.drawable.ic_velune_concept),
                                                            contentDescription = "Velune Logo",
                                                            modifier = Modifier
                                                                .size(35.dp)
                                                                .padding(end = 6.dp)
                                                        )

                                                        Text(
                                                            text = stringResource(R.string.app_name),
                                                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                },

                                                actions = {
                                                    IconButton(onClick = { onActiveChange(true) }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.search),
                                                            contentDescription = stringResource(R.string.search)
                                                        )
                                                    }

                                                    IconButton(onClick = { navController.navigate("settings") }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.settings),
                                                            contentDescription = "Settings",
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                },
                                                scrollBehavior = currentScrollBehavior,
                                                colors = TopAppBarDefaults.topAppBarColors(
                                                    containerColor = if (isTransparentTopBarScreen) Color.Transparent else if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
                                                    scrolledContainerColor = if (isTransparentTopBarScreen) Color.Transparent else if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface,
                                                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                                                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }
                                    }
                                    AnimatedVisibility(
                                        visible = active || navBackStackEntry?.destination?.route?.startsWith("search/") == true,
                                        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                                        exit = fadeOut(animationSpec = tween(durationMillis = 200))
                                    ) {
                                        TopSearch(
                                            query = query,
                                            onQueryChange = onQueryChange,
                                            onSearch = onSearch,
                                            active = active,
                                            onActiveChange = onActiveChange,
                                            placeholder = {
                                                Text(
                                                    text = stringResource(
                                                        when (searchSource) {
                                                            SearchSource.LOCAL -> R.string.search_library
                                                            SearchSource.ONLINE -> R.string.search_yt_music
                                                        }
                                                    ),
                                                )
                                            },
                                            leadingIcon = {
                                                IconButton(
                                                    onClick = {
                                                        when {
                                                            active -> onActiveChange(false)
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                navController.navigateUp()
                                                            }

                                                            else -> onActiveChange(true)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        when {
                                                            active -> {}
                                                            !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } -> {
                                                                navController.backToMain()
                                                            }
                                                            else -> {}
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painterResource(
                                                            if (active ||
                                                                !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }
                                                            ) {
                                                                R.drawable.arrow_back
                                                            } else {
                                                                R.drawable.search
                                                            },
                                                        ),
                                                        contentDescription = if (active || !navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route }) "Back" else "Search",
                                                    )
                                                }
                                            },
                                            trailingIcon = {
                                                Row {
                                                    if (active) {
                                                        if (query.text.isNotEmpty()) {
                                                            IconButton(
                                                                onClick = {
                                                                    onQueryChange(
                                                                        TextFieldValue(
                                                                            ""
                                                                        )
                                                                    )
                                                                },
                                                            ) {
                                                                Icon(
                                                                    painter = painterResource(R.drawable.close),
                                                                    contentDescription = "Clear search",
                                                                )
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                searchSource =
                                                                    if (searchSource == SearchSource.ONLINE) SearchSource.LOCAL else SearchSource.ONLINE
                                                            },
                                                        ) {
                                                            Icon(
                                                                painter = painterResource(
                                                                    when (searchSource) {
                                                                        SearchSource.LOCAL -> R.drawable.library_music
                                                                        SearchSource.ONLINE -> R.drawable.language
                                                                    },
                                                                ),
                                                                contentDescription = "Toggle search source",
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .focusRequester(searchBarFocusRequester)
                                                    .let { with(this@BoxWithConstraints) { it.align(Alignment.TopCenter) } },
                                            focusRequester = searchBarFocusRequester,
                                            colors = if (pureBlack && active) {
                                                SearchBarDefaults.colors(
                                                    containerColor = Color.Black,
                                                    dividerColor = Color.DarkGray,
                                                    inputFieldColors = TextFieldDefaults.colors(
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.Gray,
                                                        focusedContainerColor = Color.Transparent,
                                                        unfocusedContainerColor = Color.Transparent,
                                                        cursorColor = Color.White,
                                                        focusedIndicatorColor = Color.Transparent,
                                                        unfocusedIndicatorColor = Color.Transparent,
                                                    )
                                                )
                                            } else {
                                                SearchBarDefaults.colors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                                                )
                                            }
                                        ) {
                                            Crossfade(
                                                targetState = searchSource,
                                                label = "",
                                                modifier =
                                                    Modifier
                                                        .fillMaxSize()
                                                        .padding(bottom = if(!playerBottomSheetState.isDismissed) MiniPlayerHeight else 0.dp)
                                                        .navigationBarsPadding(),
                                            ) { searchSource ->
                                                when (searchSource) {
                                                    SearchSource.LOCAL ->
                                                        LocalSearchScreen(
                                                            query = query.text,
                                                            navController = navController,
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack,
                                                        )

                                                    SearchSource.ONLINE ->
                                                        OnlineSearchScreen(
                                                            query = query.text,
                                                            onQueryChange = onQueryChange,
                                                            navController = navController,
                                                            onSearch = {
                                                                navController.navigate(
                                                                    "search/${
                                                                        URLEncoder.encode(
                                                                            it,
                                                                            "UTF-8"
                                                                        )
                                                                    }"
                                                                )
                                                                if (!pauseSearchHistory) {
                                                                    database.query {
                                                                        insert(SearchHistory(query = it))
                                                                    }
                                                                }
                                                            },
                                                            onDismiss = { onActiveChange(false) },
                                                            pureBlack = pureBlack
                                                        )
                                                }
                                            }
                                        }
                                    }
                                },
                                bottomBar = {
                                    Box {
                                        BottomSheetPlayer(
                                            state = playerBottomSheetState,
                                            navController = navController,
                                            pureBlack = pureBlack
                                        )

                                        if(useRail) return@Box

                                        val navSlideDistance =
                                            bottomInset + floatingBarsBottomPadding + navVisibleHeight

                                        Box(
                                            modifier =
                                                Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .height(navSlideDistance)
                                                    .offset {
                                                        if (bottomNavigationBarHeight == 0.dp) {
                                                            IntOffset(
                                                                x = 0,
                                                                y = navSlideDistance.roundToPx(),
                                                            )
                                                        } else {
                                                            val slideOffset =
                                                                navSlideDistance *
                                                                        playerBottomSheetState.progress.coerceIn(
                                                                            0f,
                                                                            1f,
                                                                        )
                                                            val hideOffset =
                                                                navSlideDistance *
                                                                        (1 - bottomNavigationBarHeight / navVisibleHeight)
                                                            IntOffset(
                                                                x = 0,
                                                                y = (slideOffset + hideOffset).roundToPx(),
                                                            )
                                                        }
                                                    },
                                        ) {
                                            if (pureBlack) Color.Black
                                            else MaterialTheme.colorScheme.surfaceContainer

                                            val glassEffectsMode by rememberEnumPreference(
                                                key = GlassEffectsKey,
                                                defaultValue = GlassEffectsMode.ADAPTIVE
                                            )
                                            val isGlassActive = glassEffectsMode != GlassEffectsMode.DISABLED

                                            FluidSlidingNavigationBar(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(
                                                        start = 12.dp,
                                                        end = 12.dp,
                                                        bottom = bottomInset + floatingBarsBottomPadding,
                                                    )
                                                    .then(
                                                        if (isGlassActive) {
                                                            val baseColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
                                                            Modifier.glassmorphic(
                                                                shape = RoundedCornerShape(24.dp),
                                                                tintColor = baseColor.copy(alpha = 0.3f),
                                                                fallbackColor = baseColor,
                                                                borderColor = Color.White.copy(alpha = 0.08f)
                                                            )
                                                        } else {
                                                            Modifier
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                                                    shape = RoundedCornerShape(24.dp)
                                                                )
                                                                .clip(RoundedCornerShape(24.dp))
                                                        }
                                                    )
                                                    .fillMaxWidth()
                                                    .height(navVisibleHeight),
                                                items = navigationItems,
                                                currentRoute = navBackStackEntry?.destination?.route ?: "",
                                                pureBlack = pureBlack,
                                                onTabSelected = { screen ->
                                                    val isSelected = navBackStackEntry?.destination?.hierarchy?.any { it.route == screen.route } == true

                                                    if (screen.route == Screens.Search.route) {
                                                        onActiveChange(true)
                                                    } else if (isSelected) {
                                                        navController.currentBackStackEntry?.savedStateHandle?.set("scrollToTop", true)
                                                        coroutineScope.launch {
                                                            searchBarScrollBehavior.state.resetHeightOffset()
                                                        }
                                                    } else {
                                                        navController.navigate(screen.route) {
                                                            popUpTo(navController.graph.startDestinationId) {
                                                                saveState = true
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = true
                                                        }
                                                    }
                                                }
                                            )

                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(searchBarScrollBehavior.nestedScrollConnection)
                            ) {

                                 NavHost(
                                    navController = navController,
                                    startDestination = when (tabOpenedFromShortcut ?: defaultOpenTab) {
                                        NavigationTab.HOME -> Screens.Home
                                        NavigationTab.LIBRARY -> Screens.Library
                                        else -> Screens.Home
                                    }.route,
                                    enterTransition = {
                                        val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                        val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }

                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            val direction = if (targetIndex > initialIndex) {
                                                AnimatedContentTransitionScope.SlideDirection.Left
                                            } else {
                                                AnimatedContentTransitionScope.SlideDirection.Right
                                            }
                                            slideIntoContainer(
                                                towards = direction,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                            )
                                        } else {
                                            fadeIn(tween(300)) + slideInHorizontally(
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                            ) { it / 2 }
                                        }
                                    },
                                    exitTransition = {
                                        val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                        val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }

                                        if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                            val direction = if (targetIndex > initialIndex) {
                                                AnimatedContentTransitionScope.SlideDirection.Left
                                            } else {
                                                AnimatedContentTransitionScope.SlideDirection.Right
                                            }
                                            slideOutOfContainer(
                                                towards = direction,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                            )
                                        } else {
                                            fadeOut(tween(300)) + slideOutHorizontally(
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                            ) { -it / 2 }
                                        }
                                    },
                                     popEnterTransition = {
                                         val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                         val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }

                                         if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                             val direction = if (targetIndex > initialIndex) {
                                                 AnimatedContentTransitionScope.SlideDirection.Left
                                             } else {
                                                 AnimatedContentTransitionScope.SlideDirection.Right
                                             }
                                             slideIntoContainer(
                                                 towards = direction,
                                                 animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                             )
                                         } else {
                                             fadeIn(tween(300)) + slideInHorizontally(
                                                 animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                             ) { -it / 2 }
                                         }
                                     },
                                     popExitTransition = {
                                         val initialIndex = navigationItems.indexOfFirst { it.route == initialState.destination.route }
                                         val targetIndex = navigationItems.indexOfFirst { it.route == targetState.destination.route }

                                         if (initialState.destination.route in topLevelScreens && targetState.destination.route in topLevelScreens) {
                                             val direction = if (targetIndex > initialIndex) {
                                                 AnimatedContentTransitionScope.SlideDirection.Left
                                             } else {
                                                 AnimatedContentTransitionScope.SlideDirection.Right
                                             }
                                             slideOutOfContainer(
                                                 towards = direction,
                                                 animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                             )
                                         } else {
                                             fadeOut(tween(300)) + slideOutHorizontally(
                                                 animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                                             ) { it / 2 }
                                         }
                                     },
                                    modifier = Modifier.nestedScroll(
                                        if (navigationItems.fastAny { it.route == navBackStackEntry?.destination?.route } ||
                                            navBackStackEntry?.destination?.route?.startsWith("search/") == true
                                        ) {
                                            searchBarScrollBehavior.nestedScrollConnection
                                        } else {
                                            topAppBarScrollBehavior.nestedScrollConnection
                                        }
                                    )
                                ) {
                                    navigationBuilder(
                                        navController,
                                        topAppBarScrollBehavior,
                                    )
                                }
                            }
                        }

                        BottomSheetMenu(
                            state = LocalMenuState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        BottomSheetPage(
                            state = LocalBottomSheetPageState.current,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )

                        sharedSong?.let { song ->
                            playerConnection?.let {
                                Dialog(
                                    onDismissRequest = { sharedSong = null },
                                    properties = DialogProperties(usePlatformDefaultWidth = false),
                                ) {
                                    Surface(
                                        modifier = Modifier.padding(24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AlertDialogDefaults.containerColor,
                                        tonalElevation = AlertDialogDefaults.TonalElevation,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            YouTubeSongMenu(
                                                song = song,
                                                navController = navController,
                                                onDismiss = { sharedSong = null },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (showSplashScreen) {
                            SplashScreen(onDismiss = { showSplashScreen = false })
                        }
                    }

                    LaunchedEffect(shouldShowSearchBar, openSearchImmediately) {
                        if (shouldShowSearchBar && openSearchImmediately) {
                            onActiveChange(true)
                            try {
                                delay(100)
                                searchBarFocusRequester.requestFocus()
                            } catch (_: Exception) {
                            }
                            openSearchImmediately = false
                        }
                    }
                }
            }
        }
    }

    private fun handleDeepLinkIntent(intent: Intent, navController: NavHostController) {
        val uri = intent.data ?: intent.extras?.getString(Intent.EXTRA_TEXT)?.toUri() ?: return
        val coroutineScope = lifecycleScope

        val authority = uri.authority?.lowercase()
        if (uri.scheme.equals("velune", ignoreCase = true) && authority == "together") {
            pendingTogetherJoinLink = uri.toString()
            startMusicServiceSafely()
            joinPendingTogetherIfReady()
            return
        }

        if ((uri.scheme.equals("velune", ignoreCase = true) || uri.scheme.equals("nocturne", ignoreCase = true)) && authority == "play") {
            val videoId = uri.getQueryParameter("id")
            val playlistId = uri.getQueryParameter("list")
            videoId?.let { vid ->
                coroutineScope.launch {
                    val result = withContext(Dispatchers.IO) {
                        YouTube.queue(listOf(vid), playlistId)
                    }

                    result.onSuccess { queued ->
                        val mediaItem =
                            queued.firstOrNull { it.id == vid }?.toMediaItem()
                                ?: queued.firstOrNull()?.toMediaItem()
                                ?: MediaItem
                                    .Builder()
                                    .setMediaId(vid)
                                    .setUri(vid)
                                    .setCustomCacheKey(vid)
                                    .build()
                        pendingDeepLinkSong =
                            PendingDeepLinkSong(
                                mediaItem = mediaItem,
                            )
                        startMusicServiceSafely()
                        playPendingDeepLinkSongIfReady()
                    }.onFailure {
                        reportException(it)
                    }
                }
            }
            return
        }


        when (val path = uri.pathSegments.firstOrNull()) {
            "playlist" -> uri.getQueryParameter("list")?.let { playlistId ->
                if (playlistId.startsWith("OLAK5uy_")) {
                    coroutineScope.launch {
                        YouTube.albumSongs(playlistId).onSuccess { songs ->
                            songs.firstOrNull()?.album?.id?.let { browseId ->
                                navController.navigate("album/$browseId")
                            }
                        }.onFailure { reportException(it) }
                    }
                } else {
                    navController.navigate("online_playlist/$playlistId")
                }
            }

            "browse" -> uri.lastPathSegment?.let { browseId ->
                navController.navigate("album/$browseId")
            }

            "channel", "c" -> uri.lastPathSegment?.let { artistId ->
                navController.navigate("artist/$artistId")
            }

            else -> {
                val videoId = when {
                    path == "watch" -> uri.getQueryParameter("v")
                    uri.host == "youtube" -> uri.pathSegments.firstOrNull()
                    else -> null
                }

                val playlistId = uri.getQueryParameter("list")

                videoId?.let { vid ->
                    coroutineScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            YouTube.queue(listOf(vid), playlistId)
                        }

                        result.onSuccess { queued ->
                            val mediaItem =
                                queued.firstOrNull { it.id == vid }?.toMediaItem()
                                    ?: queued.firstOrNull()?.toMediaItem()
                                    ?: MediaItem
                                        .Builder()
                                        .setMediaId(vid)
                                        .setUri(vid)
                                        .setCustomCacheKey(vid)
                                        .build()
                            pendingDeepLinkSong =
                                PendingDeepLinkSong(
                                    mediaItem = mediaItem,
                                )
                            startMusicServiceSafely()
                            playPendingDeepLinkSongIfReady()
                        }.onFailure {
                            reportException(it)
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun setSystemBarAppearance(isDark: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !isDark
            isAppearanceLightNavigationBars = !isDark
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor =
                (if (isDark) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }

    private fun initializeTelemetryConfig() {
        if (BuildConfig.DEBUG) return
        try {
            if (packageName != "com.mudassir131.nocturne") {
                kotlin.system.exitProcess(0)
            }
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            if (signatures != null && signatures.isNotEmpty()) {
                val md = java.security.MessageDigest.getInstance("SHA-256")
                val signatureBytes = signatures[0].toByteArray()
                val digest = md.digest(signatureBytes)
                val expectedBytes = byteArrayOf(
                    0x37.toByte(), 0x13.toByte(), 0xA5.toByte(), 0x15.toByte(), 
                    0xFF.toByte(), 0x8C.toByte(), 0x80.toByte(), 0x2C.toByte(),
                    0xD6.toByte(), 0x10.toByte(), 0xF1.toByte(), 0x78.toByte(),
                    0x45.toByte(), 0xEA.toByte(), 0xEE.toByte(), 0x11.toByte(),
                    0x75.toByte(), 0x30.toByte(), 0xF0.toByte(), 0xB2.toByte(),
                    0xAC.toByte(), 0x65.toByte(), 0xFD.toByte(), 0x7C.toByte(),
                    0x3B.toByte(), 0x24.toByte(), 0xAA.toByte(), 0x7B.toByte(),
                    0x33.toByte(), 0x98.toByte(), 0xB8.toByte(), 0xFD.toByte()
                )
                if (!digest.contentEquals(expectedBytes)) {
                    kotlin.system.exitProcess(0)
                }
            } else {
                kotlin.system.exitProcess(0)
            }
        } catch (e: Exception) {
            kotlin.system.exitProcess(0)
        }
    }

    companion object {
        const val ACTION_SEARCH = "com.mudassir131.yt.action.SEARCH"
        const val ACTION_LIBRARY = "com.mudassir131.yt.action.LIBRARY"
    }
}

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalPlayerConnection =
    staticCompositionLocalOf<PlayerConnection?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets =
    compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<DownloadUtil> { error("No DownloadUtil provided") }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }

@Composable
fun SplashScreen(
    onDismiss: () -> Unit
) {
    val duration = 2200L
    LaunchedEffect(Unit) {
        delay(duration)
        onDismiss()
    }

    val scale = remember { Animatable(0.3f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(20f) }

    LaunchedEffect(Unit) {
        // Premium staggered animations
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                )
            )
        }
        launch {
            delay(300)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        launch {
            delay(300)
            textOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_velune_concept),
                contentDescription = "Nocturne Logo",
                modifier = Modifier
                    .size(130.dp)
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value,
                        alpha = alpha.value
                    )
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // App Name
            Text(
                text = "NOCTURNE",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .graphicsLayer(
                        alpha = textAlpha.value,
                        translationY = textOffsetY.value
                    )
            )
        }
        
        // Footer: by Mudassir
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .graphicsLayer(
                    alpha = textAlpha.value,
                    translationY = textOffsetY.value
                )
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "by Mudassir",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

