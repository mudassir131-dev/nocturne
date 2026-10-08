/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */
package com.mudassir131.yt.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R

private data class SettingsDestination(
    val iconRes: Int,
    val title: String,
    val subtitle: String,
    val route: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NocturneSettingsScreen(navController: NavController) {
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val settingGroups = remember {
        listOf(
            listOf(
                SettingsDestination(R.drawable.account, "Account", "Manage login and integrations", "settings/account"),
            ),
            listOf(
                SettingsDestination(R.drawable.palette, "Appearance", "Themes, colors, and UI layout", "settings/appearance"),
                SettingsDestination(R.drawable.play, "Player and audio", "Playback, quality, and equalizer", "settings/player"),
                SettingsDestination(R.drawable.multi_user, "Listen Together", "Sync playback with friends", "settings/music_together"),
            ),
            listOf(
                SettingsDestination(R.drawable.language, "Content", "Language, region, and providers", "settings/content"),
                SettingsDestination(R.drawable.security, "Privacy", "History and tracking", "settings/privacy"),
                SettingsDestination(R.drawable.discord, "Discord", "Presence and Discord settings", "settings/discord"),
                SettingsDestination(R.drawable.integration, "Integration", "Connected services and scrobbling", "settings/integration"),
            ),
            listOf(
                SettingsDestination(R.drawable.storage, "Storage", "Cache and downloads", "settings/storage"),
                SettingsDestination(R.drawable.backup, "Backup and restore", "Export or restore your library", "settings/backup_restore"),
            ),
            listOf(
                SettingsDestination(R.drawable.update, "Check for update", "Check GitHub releases for latest versions", "settings/update"),
                SettingsDestination(R.drawable.info, "About", "Project, contributors, and app info", "settings/about"),
            ),
        )
    }

    val allDestinations = remember(settingGroups) { settingGroups.flatten() }

    val filteredDestinations = remember(searchQuery, allDestinations) {
        val query = searchQuery.trim()
        if (query.isEmpty()) emptyList()
        else allDestinations.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navController::popBackStack) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    placeholder = { Text("Search") },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    painter = painterResource(R.drawable.close),
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }

            if (searchQuery.isNotBlank()) {
                if (filteredDestinations.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "No settings found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp),
                            )
                        }
                    }
                } else {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                filteredDestinations.forEachIndexed { index, destination ->
                                    val isFirst = index == 0
                                    val isLast = index == filteredDestinations.size - 1
                                    val itemShape = when {
                                        isFirst && isLast -> RoundedCornerShape(24.dp)
                                        isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                                        isLast -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                        else -> androidx.compose.ui.graphics.RectangleShape
                                    }

                                    SettingsDestinationRow(
                                        destination = destination,
                                        shape = itemShape,
                                        onClick = { navController.navigate(destination.route) },
                                    )

                                    if (index < filteredDestinations.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                settingGroups.forEach { group ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                group.forEachIndexed { index, destination ->
                                    val isFirst = index == 0
                                    val isLast = index == group.size - 1
                                    val itemShape = when {
                                        isFirst && isLast -> RoundedCornerShape(24.dp)
                                        isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
                                        isLast -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                        else -> androidx.compose.ui.graphics.RectangleShape
                                    }

                                    SettingsDestinationRow(
                                        destination = destination,
                                        shape = itemShape,
                                        onClick = { navController.navigate(destination.route) },
                                    )

                                    if (index < group.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                                            thickness = 1.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SettingsDestinationRow(
    destination: SettingsDestination,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = shape,
        color = androidx.compose.ui.graphics.Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(destination.iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(18.dp))
            androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                Text(
                    text = destination.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = destination.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
