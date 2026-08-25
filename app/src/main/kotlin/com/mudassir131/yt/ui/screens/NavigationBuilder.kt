/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.DarkModeKey
import com.mudassir131.yt.constants.PureBlackKey
import com.mudassir131.yt.ui.component.BottomSheet
import com.mudassir131.yt.ui.component.BottomSheetMenu
import com.mudassir131.yt.ui.component.LocalMenuState
import com.mudassir131.yt.ui.component.rememberBottomSheetState
import com.mudassir131.yt.ui.screens.BrowseScreen
import com.mudassir131.yt.ui.screens.artist.ArtistAlbumsScreen
import com.mudassir131.yt.ui.screens.BroadcastScreen
import com.mudassir131.yt.ui.screens.artist.ArtistItemsScreen
import com.mudassir131.yt.ui.screens.artist.ArtistScreen
import com.mudassir131.yt.ui.screens.artist.ArtistSongsScreen
import com.mudassir131.yt.ui.screens.library.LibraryScreen
import com.mudassir131.yt.ui.screens.playlist.AutoPlaylistScreen
import com.mudassir131.yt.ui.screens.playlist.LocalPlaylistScreen
import com.mudassir131.yt.ui.screens.playlist.OnlinePlaylistScreen
import com.mudassir131.yt.ui.screens.playlist.CuratedDetailScreen
import com.mudassir131.yt.ui.screens.playlist.TopPlaylistScreen
import com.mudassir131.yt.ui.screens.playlist.CachePlaylistScreen
import com.mudassir131.yt.ui.screens.search.OnlineSearchResult
import com.mudassir131.yt.ui.screens.settings.AboutScreen
import com.mudassir131.yt.ui.screens.settings.AppearanceSettings
import com.mudassir131.yt.ui.screens.settings.CustomizeBackground
import com.mudassir131.yt.ui.screens.settings.BackupAndRestore
import com.mudassir131.yt.ui.screens.settings.NocturneSettingsScreen
import com.mudassir131.yt.ui.screens.settings.NocturneAccountSettingsScreen
import com.mudassir131.yt.ui.screens.settings.ChangelogScreen
import com.mudassir131.yt.ui.screens.settings.ContentSettings
import com.mudassir131.yt.ui.screens.settings.LyricsProviderPriorityScreen
import com.mudassir131.yt.ui.screens.settings.LyricsRomanizationSettingsScreen
import com.mudassir131.yt.ui.screens.settings.DarkMode
import com.mudassir131.yt.ui.screens.settings.DiscordLoginScreen
import com.mudassir131.yt.ui.screens.settings.DiscordSettings
import com.mudassir131.yt.ui.screens.settings.DebugSettings
import com.mudassir131.yt.ui.screens.settings.IntegrationScreen
import com.mudassir131.yt.ui.screens.settings.LastFMSettings
import com.mudassir131.yt.ui.screens.settings.MusicTogetherScreen
import com.mudassir131.yt.ui.screens.settings.PalettePickerScreen
import com.mudassir131.yt.ui.screens.settings.PlayerSettings
import com.mudassir131.yt.ui.screens.settings.PoTokenScreen
import com.mudassir131.yt.ui.screens.settings.PrivacySettings
import com.mudassir131.yt.ui.screens.settings.SettingsScreen
import com.mudassir131.yt.ui.screens.settings.StorageSettings
import com.mudassir131.yt.ui.screens.settings.ThemeCreatorScreen
import com.mudassir131.yt.ui.screens.settings.UpdateScreen
import com.mudassir131.yt.ui.utils.ShowMediaInfo
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.navigationBuilder(
    navController: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    searchContent: @Composable () -> Unit = { Box(modifier = Modifier.fillMaxSize()) },
) {
    composable(Screens.Home.route) {
        HomeScreen(navController)
    }
    composable(
        Screens.Library.route,
    ) {
        LibraryScreen(navController)
    }
    composable("history") {
        HistoryScreen(navController)
    }
    composable("stats") {
        StatsScreen(navController)
    }
    composable(Screens.Search.route) {
        searchContent()
    }
    composable("year_in_music") {
        YearInMusicScreen(navController)
    }
    composable("mood_and_genres") {
        MoodAndGenresScreen(navController, scrollBehavior)
    }

    composable("new_release") {
        NewReleaseScreen(navController, scrollBehavior)
    }
    composable("charts_screen") {
       ChartsScreen(navController)
    }
    composable(
        route = "browse/{browseId}",
        arguments = listOf(
            navArgument("browseId") {
                type = NavType.StringType
            }
        )
    ) {
        BrowseScreen(
            navController,
            scrollBehavior,
            it.arguments?.getString("browseId")
        )
    }
    composable(
        route = "search/{query}",
        arguments =
        listOf(
            navArgument("query") {
                type = NavType.StringType
            },
        ),
        // Search owns one authoritative header. Keeping the discovery destination composed during
        // a cross-fade leaves its tabs visible behind the submitted-query field, so search state
        // transitions now use one lightweight horizontal motion instead of an abrupt swap.
        enterTransition = { slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(160)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(140)) },
        popExitTransition = { slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(160)) },
    ) {
        OnlineSearchResult(navController)
    }
    composable(
        route = "album/{albumId}",
        arguments =
        listOf(
            navArgument("albumId") {
                type = NavType.StringType
            },
        ),
    ) {
        AlbumScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/songs",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
        ),
    ) {
        ArtistSongsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/albums",
        arguments = listOf(
            navArgument("artistId") {
                type = NavType.StringType
            }
        )
    ) {
        ArtistAlbumsScreen(navController, scrollBehavior)
    }
    composable(
        route = "artist/{artistId}/items?browseId={browseId}&params={params}",
        arguments =
        listOf(
            navArgument("artistId") {
                type = NavType.StringType
            },
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        ArtistItemsScreen(navController, scrollBehavior)
    }
    composable(
        route = "curated/{playlistId}?category={category}&title={title}&thumbnail={thumbnail}",
        arguments = listOf(
            navArgument("playlistId") { type = NavType.StringType },
            navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
            navArgument("thumbnail") { type = NavType.StringType; nullable = true; defaultValue = null },
        ),
        enterTransition = {
            fadeIn(tween(360)) + slideInHorizontally(tween(420)) { it / 4 }
        },
        exitTransition = { fadeOut(tween(220)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = {
            fadeOut(tween(260)) + slideOutHorizontally(tween(360)) { it / 4 }
        },
    ) { entry ->
        CuratedDetailScreen(
            navController = navController,
            category = entry.arguments?.getString("category"),
            fallbackTitle = entry.arguments?.getString("title"),
            fallbackThumbnail = entry.arguments?.getString("thumbnail"),
        )
    }
    composable(
        route = "online_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        OnlinePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "local_playlist/{playlistId}",
        arguments =
        listOf(
            navArgument("playlistId") {
                type = NavType.StringType
            },
        ),
    ) {
        LocalPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "auto_playlist/{playlist}",
        arguments =
        listOf(
            navArgument("playlist") {
                type = NavType.StringType
            },
        ),
    ) {
        AutoPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "cache_playlist/{playlist}",
        arguments =
            listOf(
                navArgument("playlist") {
                    type = NavType.StringType
            },
        ),
    ) {
        CachePlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "top_playlist/{top}",
        arguments =
        listOf(
            navArgument("top") {
                type = NavType.StringType
            },
        ),
    ) {
        TopPlaylistScreen(navController, scrollBehavior)
    }
    composable(
        route = "youtube_browse/{browseId}?params={params}",
        arguments =
        listOf(
            navArgument("browseId") {
                type = NavType.StringType
                nullable = true
            },
            navArgument("params") {
                type = NavType.StringType
                nullable = true
            },
        ),
    ) {
        YouTubeBrowseScreen(navController)
    }
    composable("settings") {
        NocturneSettingsScreen(navController)
    }
    composable("settings/account") {
        NocturneAccountSettingsScreen(navController)
    }
    composable("settings/appearance") {
        AppearanceSettings(navController, scrollBehavior)
    }
    composable("settings/appearance/palette_picker") {
        PalettePickerScreen(navController)
    }
    composable("settings/appearance/theme_creator") {
        ThemeCreatorScreen(navController)
    }
    composable("settings/content") {
        ContentSettings(navController, scrollBehavior)
    }
    composable("settings/content/lyrics_priority") {
        LyricsProviderPriorityScreen(navController)
    }
    composable("settings/content/lyrics_romanization") {
        LyricsRomanizationSettingsScreen(navController)
    }
    composable("settings/player") {
        PlayerSettings(navController, scrollBehavior)
    }
    composable("settings/storage") {
        StorageSettings(navController, scrollBehavior)
    }
    composable("settings/privacy") {
        PrivacySettings(navController, scrollBehavior)
    }
    composable("settings/backup_restore") {
        BackupAndRestore(navController, scrollBehavior)
    }
    composable("settings/discord") {
        DiscordSettings(navController, scrollBehavior)
    }
    composable("settings/integration") {
        IntegrationScreen(navController, scrollBehavior)
    }
    composable("settings/music_together") {
        MusicTogetherScreen(navController, scrollBehavior)
    }
    composable("settings/lastfm") {
        LastFMSettings(navController, scrollBehavior)
    }
    composable("settings/discord/experimental") {
        com.mudassir131.yt.ui.screens.settings.DiscordExperimental(navController)
    }
    composable("settings/misc") {
        DebugSettings(navController)
    }

    composable("settings/changelog") {
        ChangelogScreen(navController, scrollBehavior)
    }
    composable("settings/discord/login") {
        DiscordLoginScreen(navController)
    }
    composable("settings/update") {
        UpdateScreen(navController, scrollBehavior)
    }
    composable("settings/check_for_update") {
        UpdateScreen(navController, scrollBehavior)
    }
    composable("settings/about") {
        AboutScreen(navController, scrollBehavior)
    }
    composable("settings/po_token") {
        PoTokenScreen(navController, scrollBehavior)
    }
    composable("customize_background") {
        CustomizeBackground(navController)
    }
    composable("login") {
        LoginScreen(navController)
    }
    composable("broadcast") {
        BroadcastScreen(navController)
    }
}
