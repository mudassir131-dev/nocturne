package com.mudassir131.yt.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.LyricsProviderPriorityKey
import com.mudassir131.yt.constants.LyricsRomanizeJapaneseKey
import com.mudassir131.yt.constants.LyricsRomanizeKoreanKey
import com.mudassir131.yt.lyrics.LyricsProviderId
import com.mudassir131.yt.lyrics.LyricsProviderRegistry
import com.mudassir131.yt.ui.component.IconButton
import com.mudassir131.yt.ui.component.PreferenceGroupTitle
import com.mudassir131.yt.ui.component.SwitchPreference
import com.mudassir131.yt.ui.utils.backToMain
import com.mudassir131.yt.utils.rememberPreference
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsBackBar(title: String, navController: NavController) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
            }
        },
    )
}

@Composable
fun LyricsProviderPriorityScreen(navController: NavController) {
    val default = remember { LyricsProviderRegistry.encodePriority(LyricsProviderRegistry.defaultPriority) }
    var persisted by rememberPreference(LyricsProviderPriorityKey, default)
    val ordered = remember(persisted) {
        mutableStateListOf<LyricsProviderId>().apply {
            addAll(LyricsProviderRegistry.decodePriority(persisted))
        }
    }
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index !in ordered.indices || to.index !in ordered.indices) return@rememberReorderableLazyListState
        val moved = ordered.removeAt(from.index)
        ordered.add(to.index, moved)
        persisted = LyricsProviderRegistry.encodePriority(ordered)
    }

    Column(Modifier.fillMaxSize()) {
        SettingsBackBar("Lyrics provider priority", navController)
        Text(
            text = "Drag to reorder which provider is tried first. Disabled providers remain in your saved order and are skipped during lookup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        )
        LazyColumn(
            state = lazyListState,
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(ordered, key = { _, item -> item.persistedId }) { index, provider ->
                ReorderableItem(reorderableState, key = provider.persistedId) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
                        ) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Text(provider.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            androidx.compose.material3.IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(),
                            ) {
                                Icon(
                                    painterResource(R.drawable.drag_handle),
                                    contentDescription = "Drag ${provider.displayName}",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LyricsRomanizationSettingsScreen(navController: NavController) {
    val (romanizeJapanese, onRomanizeJapaneseChange) = rememberPreference(LyricsRomanizeJapaneseKey, true)
    val (romanizeKorean, onRomanizeKoreanChange) = rememberPreference(LyricsRomanizeKoreanKey, true)

    Column(Modifier.fillMaxSize()) {
        SettingsBackBar("Lyrics romanization", navController)
        com.mudassir131.yt.ui.component.PreferenceGroup(
            title = "Romanization",
            items = listOf<@Composable () -> Unit>(
                {
                    SwitchPreference(
                        title = { Text("Romanize Japanese lyrics") },
                        description = "Show a Latin-script reading when supported",
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = romanizeJapanese,
                        onCheckedChange = onRomanizeJapaneseChange,
                    )
                },
                {
                    SwitchPreference(
                        title = { Text("Romanize Korean lyrics") },
                        description = "Show a Latin-script reading when supported",
                        icon = { Icon(painterResource(R.drawable.lyrics), null) },
                        checked = romanizeKorean,
                        onCheckedChange = onRomanizeKoreanChange,
                    )
                }
            )
        )
    }
}
