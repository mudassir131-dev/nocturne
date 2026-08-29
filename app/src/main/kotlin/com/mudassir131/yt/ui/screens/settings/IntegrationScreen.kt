/*
 * Nocturne - by Mudassir

 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.mudassir131.yt.BuildConfig
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.component.PreferenceGroupTitle
import com.mudassir131.yt.constants.ListenBrainzEnabledKey
import com.mudassir131.yt.constants.ListenBrainzTokenKey
import com.mudassir131.yt.constants.SpotifyUseDataApiMatchingKey
import com.mudassir131.yt.constants.SpotifyClientIdKey
import com.mudassir131.yt.constants.SpotifyClientSecretKey
import com.mudassir131.yt.constants.YouTubeDataApiKeyKey
import com.mudassir131.yt.ui.component.IconButton
import com.mudassir131.yt.ui.component.InfoLabel
import com.mudassir131.yt.ui.component.PreferenceEntry
import com.mudassir131.yt.ui.component.SwitchPreference
import com.mudassir131.yt.ui.component.TextFieldDialog
import com.mudassir131.yt.ui.utils.backToMain
import com.mudassir131.yt.utils.rememberPreference
import com.mudassir131.yt.utils.youtube.YouTubeDataApi
import com.mudassir131.yt.utils.youtube.YouTubeQuotaTracker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current

    val (listenBrainzEnabled, onListenBrainzEnabledChange) = rememberPreference(ListenBrainzEnabledKey, false)
    val (listenBrainzToken, onListenBrainzTokenChange) = rememberPreference(ListenBrainzTokenKey, "")

    val (youtubeDataApiKey, onYouTubeDataApiKeyChange) = rememberPreference(YouTubeDataApiKeyKey, "")
    val (spotifyUseDataApi, onSpotifyUseDataApiChange) = rememberPreference(SpotifyUseDataApiMatchingKey, true)

    val (spotifyClientId, onSpotifyClientIdChange) = rememberPreference(SpotifyClientIdKey, "")
    val (spotifyClientSecret, onSpotifyClientSecretChange) = rememberPreference(SpotifyClientSecretKey, "")

    var showListenBrainzTokenEditor = remember { mutableStateOf(false) }
    val showYouTubeDataApiKeyEditor = remember { mutableStateOf(false) }
    val showSpotifyClientIdEditor = remember { mutableStateOf(false) }
    val showSpotifyClientSecretEditor = remember { mutableStateOf(false) }

    // Read-only view of today's spend; the ledger resets on Google's Pacific-time quota boundary.
    val quotaUsed by produceState(initialValue = -1, youtubeDataApiKey, showYouTubeDataApiKeyEditor.value) {
        value = runCatching { YouTubeQuotaTracker.usedUnits(context) }.getOrDefault(0)
    }
    val hasAnyKey = youtubeDataApiKey.isNotBlank() || BuildConfig.YOUTUBE_DATA_API_KEY.isNotBlank()

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top
                )
            )
        )

        PreferenceGroupTitle(
                title = stringResource(R.string.general),
            )

        PreferenceEntry(
            title = { Text(stringResource(R.string.discord_integration)) },
            icon = { Icon(painterResource(R.drawable.discord), null) },
            onClick = {
                navController.navigate("settings/discord")
            },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.scrobbling),
        )

        PreferenceEntry(
            title = { Text(stringResource(R.string.lastfm_integration)) },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = {
                navController.navigate("settings/lastfm")
            },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.listenbrainz_scrobbling)) },
            description = stringResource(R.string.listenbrainz_scrobbling_description),
            icon = { Icon(painterResource(R.drawable.token), null) },
            checked = listenBrainzEnabled,
            onCheckedChange = onListenBrainzEnabledChange,
        )
        PreferenceEntry(
            title = { Text(if (listenBrainzToken.isBlank()) stringResource(R.string.set_listenbrainz_token) else stringResource(R.string.edit_listenbrainz_token)) },
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showListenBrainzTokenEditor.value = true },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.spotify_api_integration),
        )

        PreferenceEntry(
            title = {
                Text(
                    if (spotifyClientId.isBlank()) {
                        stringResource(R.string.set_spotify_client_id)
                    } else {
                        stringResource(R.string.edit_spotify_client_id)
                    }
                )
            },
            description = stringResource(R.string.spotify_client_id_description),
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showSpotifyClientIdEditor.value = true },
        )

        PreferenceEntry(
            title = {
                Text(
                    if (spotifyClientSecret.isBlank()) {
                        stringResource(R.string.set_spotify_client_secret)
                    } else {
                        stringResource(R.string.edit_spotify_client_secret)
                    }
                )
            },
            description = stringResource(R.string.spotify_client_secret_description),
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showSpotifyClientSecretEditor.value = true },
        )

        PreferenceGroupTitle(
            title = stringResource(R.string.youtube_data_api),
        )

        PreferenceEntry(
            title = {
                Text(
                    if (youtubeDataApiKey.isBlank()) {
                        stringResource(R.string.set_youtube_data_api_key)
                    } else {
                        stringResource(R.string.edit_youtube_data_api_key)
                    }
                )
            },
            description = stringResource(R.string.youtube_data_api_key_description),
            icon = { Icon(painterResource(R.drawable.token), null) },
            onClick = { showYouTubeDataApiKeyEditor.value = true },
        )
        SwitchPreference(
            title = { Text(stringResource(R.string.spotify_data_api_matching)) },
            description = stringResource(R.string.spotify_data_api_matching_description),
            icon = { Icon(painterResource(R.drawable.token), null) },
            checked = spotifyUseDataApi,
            onCheckedChange = onSpotifyUseDataApiChange,
            isEnabled = hasAnyKey,
        )
        if (quotaUsed >= 0) {
            InfoLabel(
                text = stringResource(
                    R.string.youtube_data_api_quota_used,
                    quotaUsed,
                    YouTubeDataApi.DEFAULT_DAILY_QUOTA_UNITS
                )
            )
        }
    }

    TopAppBar(
        title = { Text(stringResource(R.string.integration)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null
                )
            }
        }
    )

    if (showListenBrainzTokenEditor.value) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(listenBrainzToken),
            onDone = { data ->
                onListenBrainzTokenChange(data)
                showListenBrainzTokenEditor.value = false
            },
            onDismiss = { showListenBrainzTokenEditor.value = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = {
                it.isNotEmpty()
            },
            extraContent = {
                InfoLabel(text = stringResource(R.string.listenbrainz_scrobbling_description))
            }
        )
    }

    if (showSpotifyClientIdEditor.value) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(spotifyClientId),
            onDone = { data ->
                onSpotifyClientIdChange(data.trim())
                showSpotifyClientIdEditor.value = false
            },
            onDismiss = { showSpotifyClientIdEditor.value = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = { true },
            extraContent = {
                InfoLabel(text = stringResource(R.string.spotify_client_id_description))
            }
        )
    }

    if (showSpotifyClientSecretEditor.value) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(spotifyClientSecret),
            onDone = { data ->
                onSpotifyClientSecretChange(data.trim())
                showSpotifyClientSecretEditor.value = false
            },
            onDismiss = { showSpotifyClientSecretEditor.value = false },
            singleLine = true,
            maxLines = 1,
            isInputValid = { true },
            extraContent = {
                InfoLabel(text = stringResource(R.string.spotify_client_secret_description))
            }
        )
    }

    if (showYouTubeDataApiKeyEditor.value) {
        TextFieldDialog(
            initialTextFieldValue = androidx.compose.ui.text.input.TextFieldValue(youtubeDataApiKey),
            onDone = { data ->
                onYouTubeDataApiKeyChange(data.trim())
                showYouTubeDataApiKeyEditor.value = false
            },
            onDismiss = { showYouTubeDataApiKeyEditor.value = false },
            singleLine = true,
            maxLines = 1,
            // Blank is a valid value here: clearing the key falls the importer back to YouTube Music search.
            isInputValid = { true },
            extraContent = {
                InfoLabel(text = stringResource(R.string.youtube_data_api_key_description))
            }
        )
    }
}
