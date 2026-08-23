/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mudassir131.yt.R
import com.mudassir131.yt.viewmodels.HomeViewModel
import com.mudassir131.yt.App.Companion.forgetAccount
import com.mudassir131.yt.utils.rememberPreference
import com.mudassir131.yt.constants.AccountChannelHandleKey
import com.mudassir131.yt.constants.AccountEmailKey
import com.mudassir131.yt.constants.AccountNameKey
import com.mudassir131.yt.constants.DataSyncIdKey
import com.mudassir131.yt.constants.InnerTubeCookieKey
import com.mudassir131.yt.constants.PoTokenKey
import com.mudassir131.yt.constants.SelectedYtmPlaylistsKey
import com.mudassir131.yt.constants.VisitorDataKey
import com.mudassir131.yt.innertube.YouTube
import com.mudassir131.yt.innertube.utils.completed
import com.mudassir131.yt.innertube.utils.parseCookieString
import com.mudassir131.yt.ui.component.InfoLabel
import com.mudassir131.yt.ui.component.TextFieldDialog
import com.mudassir131.yt.ui.component.NocturneLoader
import com.mudassir131.yt.utils.dataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NocturneAccountSettingsScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val viewModel: HomeViewModel = hiltViewModel(context as androidx.activity.ComponentActivity)
    val accountName by viewModel.accountName.collectAsState()
    val isLoggedIn = accountName?.let { it != "Guest" && it.isNotEmpty() }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showTokenEditor by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val (innerTubeCookie, onInnerTubeCookieChange) = rememberPreference(InnerTubeCookieKey, "")
    val (poToken, onPoTokenChange) = rememberPreference(PoTokenKey, "")
    val (visitorData, onVisitorDataChange) = rememberPreference(VisitorDataKey, "")
    val (dataSyncId, onDataSyncIdChange) = rememberPreference(DataSyncIdKey, "")
    val (accountNamePref, onAccountNameChange) = rememberPreference(AccountNameKey, "")
    val (accountEmail, onAccountEmailChange) = rememberPreference(AccountEmailKey, "")
    val (accountChannelHandle, onAccountChannelHandleChange) = rememberPreference(AccountChannelHandleKey, "")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {

            // Settings Section
            item {
                AccountSectionHeader("Settings")
            }


            item {
                AnimatedContent(
                    targetState = isLoggedIn,
                    label = "LoginLogoutAnimation"
                ) { targetIsLoggedIn ->
                    when (targetIsLoggedIn) {
                        null -> {
                            Box(modifier = Modifier.fillMaxWidth().height(64.dp))
                        }
                        true -> {
                            AccountSettingsItem(
                                icon = painterResource(R.drawable.logout),
                                title = "Logout",
                                subtitle = "Sign out of the current account",
                                onClick = { showLogoutDialog = true }
                            )
                        }
                        false -> {
                            AccountSettingsItem(
                                icon = painterResource(R.drawable.login),
                                title = "Login",
                                subtitle = "Sign in to YouTube Music",
                                onClick = { navController.navigate("login") }
                            )
                        }
                    }
                }
            }



            // Login with token Section
            item {
                AccountSectionHeader("Login with token")
            }
            item {
                AccountSettingsItem(
                    icon = painterResource(R.drawable.token),
                    title = "Login with token",
                    subtitle = "Use an advanced login token",
                    onClick = { showTokenEditor = true }
                )
            }
            item {
                AccountSettingsItem(
                    icon = painterResource(R.drawable.security),
                    title = "PO Token Generation",
                    subtitle = "Generate playback tokens",
                    onClick = { navController.navigate("settings/po_token") }
                )
            }

            // Integrations Section
            item {
                AccountSectionHeader("Integrations")
            }
            item {
                AccountSettingsItem(
                    icon = painterResource(R.drawable.integration),
                    title = "Integrations",
                    subtitle = "Manage connected services",
                    onClick = { navController.navigate("settings/integration") }
                )
            }
            item {
                AccountSettingsItem(
                    icon = painterResource(R.drawable.sync),
                    title = "Select playlist to sync",
                    subtitle = "Choose playlists for library sync",
                    onClick = { showPlaylistDialog = true }
                )
            }

            item { Spacer(Modifier.height(32.dp)) }
        }

        // Token Editor Dialog
        if (showTokenEditor) {
            TokenEditorDialog(
                innerTubeCookie = innerTubeCookie,
                visitorData = visitorData,
                dataSyncId = dataSyncId,
                accountNamePref = accountNamePref,
                accountEmail = accountEmail,
                accountChannelHandle = accountChannelHandle,
                onInnerTubeCookieChange = onInnerTubeCookieChange,
                onPoTokenChange = onPoTokenChange,
                onVisitorDataChange = onVisitorDataChange,
                onDataSyncIdChange = onDataSyncIdChange,
                onAccountNameChange = onAccountNameChange,
                onAccountEmailChange = onAccountEmailChange,
                onAccountChannelHandleChange = onAccountChannelHandleChange,
                onDismiss = { showTokenEditor = false }
            )
        }

        // Playlist Selection Dialog
        if (showPlaylistDialog) {
            PlaylistSelectionDialog(
                onDismiss = { showPlaylistDialog = false }
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Sign Out") },
                text = { Text("Are you sure you want to sign out of your YouTube Music account?") },
                confirmButton = {
                    TextButton(onClick = {
                        showLogoutDialog = false
                        onInnerTubeCookieChange("")
                        forgetAccount(context)
                    }) {
                        Text("Sign Out")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun AccountSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp, top = 22.dp),
    )
}

@Composable
private fun AccountSettingsItem(
    icon: Painter,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
@Composable
private fun TokenEditorDialog(
    innerTubeCookie: String,
    visitorData: String,
    dataSyncId: String,
    accountNamePref: String,
    accountEmail: String,
    accountChannelHandle: String,
    onInnerTubeCookieChange: (String) -> Unit,
    onPoTokenChange: (String) -> Unit,
    onVisitorDataChange: (String) -> Unit,
    onDataSyncIdChange: (String) -> Unit,
    onAccountNameChange: (String) -> Unit,
    onAccountEmailChange: (String) -> Unit,
    onAccountChannelHandleChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val text = """
        ***INNERTUBE COOKIE*** =$innerTubeCookie
        ***VISITOR DATA*** =$visitorData
        ***DATASYNC ID*** =$dataSyncId
        ***PO TOKEN*** =${YouTube.poToken.orEmpty()}
        ***ACCOUNT NAME*** =$accountNamePref
        ***ACCOUNT EMAIL*** =$accountEmail
        ***ACCOUNT CHANNEL HANDLE*** =$accountChannelHandle
    """.trimIndent()

    TextFieldDialog(
        initialTextFieldValue = TextFieldValue(text),
        onDone = { data ->
            data.split("\n").forEach {
                when {
                    it.startsWith("***INNERTUBE COOKIE*** =") -> onInnerTubeCookieChange(it.substringAfter("="))
                    it.startsWith("***VISITOR DATA*** =") -> onVisitorDataChange(it.substringAfter("="))
                    it.startsWith("***DATASYNC ID*** =") -> onDataSyncIdChange(it.substringAfter("="))
                    it.startsWith("***PO TOKEN*** =") -> onPoTokenChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT NAME*** =") -> onAccountNameChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT EMAIL*** =") -> onAccountEmailChange(it.substringAfter("="))
                    it.startsWith("***ACCOUNT CHANNEL HANDLE*** =") -> onAccountChannelHandleChange(it.substringAfter("="))
                }
            }
        },
        onDismiss = onDismiss,
        singleLine = false,
        maxLines = 20,
        isInputValid = {
            it.isNotEmpty() && "SAPISID" in parseCookieString(it)
        },
        extraContent = {
            InfoLabel(text = stringResource(R.string.token_adv_login_description))
        }
    )
}

@Composable
private fun PlaylistSelectionDialog(onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val (initialSelected, _) = rememberPreference(SelectedYtmPlaylistsKey, "")
    val selectedList = remember { mutableStateListOf<String>() }

    LaunchedEffect(initialSelected) {
        selectedList.clear()
        if (initialSelected.isNotEmpty()) {
            selectedList.addAll(
                initialSelected.split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            )
        }
    }

    var loading by remember { mutableStateOf(true) }
    val playlists = remember { mutableStateListOf<com.mudassir131.yt.innertube.models.PlaylistItem>() }

    LaunchedEffect(Unit) {
        loading = true
        com.mudassir131.yt.innertube.YouTube
            .library("FEmusic_liked_playlists")
            .completed()
            .onSuccess { page ->
                playlists.clear()
                playlists.addAll(
                    page.items
                        .filterIsInstance<com.mudassir131.yt.innertube.models.PlaylistItem>()
                        .filterNot { it.id == "LM" || it.id == "SE" }
                        .reversed()
                )
            }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        confirmButton = {
            TextButton(
                onClick = {
                    com.mudassir131.yt.utils.PreferenceStore.launchEdit(context.dataStore) {
                        this[SelectedYtmPlaylistsKey] = selectedList.joinToString(",")
                    }
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.save),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel_button))
            }
        },
        title = {
            Text(
                text = stringResource(R.string.select_playlist_to_sync),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NocturneLoader(size = 48.dp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(playlists) { pl ->
                        val isSelected = selectedList.contains(pl.id)
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            else
                                Color.Transparent,
                            label = "playlistItemColor"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(backgroundColor)
                                .clickable {
                                    if (isSelected) selectedList.remove(pl.id)
                                    else selectedList.add(pl.id)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    if (checked) selectedList.add(pl.id)
                                    else selectedList.remove(pl.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary
                                )
                            )

                            Spacer(Modifier.width(8.dp))

                            AsyncImage(
                                model = pl.thumbnail,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(Modifier.width(12.dp))

                            Text(
                                text = pl.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    )
}


@Composable
fun VeluneAccountSettingsScreen(navController: androidx.navigation.NavController) {
    NocturneAccountSettingsScreen(navController)
}
