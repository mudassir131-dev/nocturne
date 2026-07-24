/*
 * Nocturne - by Mudassir
 * Licensed under GPL-3.0.
 */
package com.mudassir131.yt.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.mudassir131.yt.LocalPlayerAwareWindowInsets
import com.mudassir131.yt.LocalPlayerConnection
import com.mudassir131.yt.R
import com.mudassir131.yt.constants.SearchSource
import com.mudassir131.yt.innertube.models.AlbumItem
import com.mudassir131.yt.innertube.models.SongItem
import com.mudassir131.yt.innertube.pages.MoodAndGenres
import com.mudassir131.yt.models.toMediaMetadata
import com.mudassir131.yt.playback.queues.YouTubeQueue
import com.mudassir131.yt.ui.component.ChipsRow
import com.mudassir131.yt.ui.component.RootScreenHeader
import com.mudassir131.yt.viewmodels.HomeViewModel

enum class SearchPrimaryTab {
    EXPLORE,
    SUGGESTIONS,
    ALBUMS,
}

/** Shared Search identity used by discovery and active input states. */
@Composable
fun SearchShellHeader(
    query: TextFieldValue,
    active: Boolean,
    searchSource: SearchSource,
    selectedTab: SearchPrimaryTab,
    focusRequester: FocusRequester,
    onQueryChange: (TextFieldValue) -> Unit,
    onSearch: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onToggleSource: () -> Unit,
    onSelectedTab: (SearchPrimaryTab) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        RootScreenHeader(
            title = "Search",
        )
        TextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            placeholder = { Text("Search songs, artists, playlists…") },
            leadingIcon = {
                IconButton(onClick = { onActiveChange(!active) }) {
                    Icon(
                        painter = painterResource(if (active) R.drawable.arrow_back else R.drawable.search),
                        contentDescription = if (active) "Back to Search discovery" else "Search",
                    )
                }
            },
            trailingIcon = {
                Row {
                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange(TextFieldValue()) }) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Clear search query",
                            )
                        }
                    }
                    IconButton(onClick = onToggleSource) {
                        Icon(
                            painter = painterResource(
                                if (searchSource == SearchSource.ONLINE) R.drawable.language
                                else R.drawable.library_music,
                            ),
                            contentDescription = "Toggle search source",
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query.text) }),
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused && !active) onActiveChange(true) },
        )
        if (query.text.isBlank()) {
            SearchPrimaryTabs(selectedTab = selectedTab, onSelected = onSelectedTab)
        }
    }
}

@Composable
fun SearchPrimaryTabs(
    selectedTab: SearchPrimaryTab,
    onSelected: (SearchPrimaryTab) -> Unit,
) {
    ChipsRow(
        chips = listOf(
            SearchPrimaryTab.EXPLORE to "Explore",
            SearchPrimaryTab.SUGGESTIONS to "Suggestions",
            SearchPrimaryTab.ALBUMS to "Albums",
        ),
        currentValue = selectedTab,
        onValueUpdate = onSelected,
    )
}

/** Submitted-query identity: one back affordance in the field; result filters live in content. */
@Composable
fun SearchResultShellHeader(
    query: String,
    onEditQuery: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        RootScreenHeader(
            title = "Search",
        )
        Surface(
            onClick = onEditQuery,
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                Text(
                    text = query,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                )
                Icon(painterResource(R.drawable.language), contentDescription = null)
            }
        }
    }
}

/** Query-empty Search discovery content; focus and IME state remain owned by the root shell. */
@Composable
fun SearchDiscoveryHub(
    navController: NavController,
    homeViewModel: HomeViewModel,
    pureBlack: Boolean,
    selectedTab: SearchPrimaryTab,
    modifier: Modifier = Modifier,
) {
    val explore by homeViewModel.explorePage.collectAsState()
    val suggestions by homeViewModel.forYouSuggestions.collectAsState()
    val playerConnection = LocalPlayerConnection.current
    val bottomPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        when (selectedTab) {
            SearchPrimaryTab.EXPLORE -> ExploreDiscoveryTab(
                items = explore?.moodAndGenres.orEmpty(),
                bottomPadding = bottomPadding,
                onClick = { item ->
                    navController.navigate(
                        "youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}",
                    )
                },
            )

            SearchPrimaryTab.SUGGESTIONS -> SuggestionsDiscoveryTab(
                songs = suggestions.orEmpty(),
                bottomPadding = bottomPadding,
                onSongClick = { song ->
                    playerConnection?.playQueue(YouTubeQueue.radio(song.toMediaMetadata()))
                },
            )

            SearchPrimaryTab.ALBUMS -> AlbumDiscoveryTab(
                albums = explore?.newReleaseAlbums.orEmpty(),
                bottomPadding = bottomPadding,
                onAlbumClick = { album -> navController.navigate("album/${album.id}") },
            )
        }
    }
}

private val moodNames = setOf(
    "chill", "commute", "energize", "feel good", "focus", "gaming",
    "party", "romance", "sad", "sleep", "workout",
)

@Composable
private fun ExploreDiscoveryTab(
    items: List<MoodAndGenres.Item>,
    bottomPadding: Dp,
    onClick: (MoodAndGenres.Item) -> Unit,
) {
    val matchedMoods = remember(items) { items.filter { it.title.trim().lowercase() in moodNames } }
    val moods = remember(items, matchedMoods) {
        if (matchedMoods.isNotEmpty()) matchedMoods else items.take(11)
    }
    val genres = remember(items, moods) { items.filterNot { it in moods } }

    LazyColumn(
        contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, bottomPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { DiscoverySectionTitle("Moods & moments") }
        itemsIndexed(moods.chunked(2), key = { index, _ -> "moods_$index" }) { _, row ->
            DiscoveryCardRow(row, onClick)
        }
        item { DiscoverySectionTitle("Genres", topPadding = 10.dp) }
        itemsIndexed(genres.chunked(2), key = { index, _ -> "genres_$index" }) { _, row ->
            DiscoveryCardRow(row, onClick)
        }
    }
}

@Composable
private fun DiscoverySectionTitle(text: String, topPadding: Dp = 0.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp),
    )
}

@Composable
private fun DiscoveryCardRow(
    row: List<MoodAndGenres.Item>,
    onClick: (MoodAndGenres.Item) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        row.forEach { item ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f))
                    .clickable { onClick(item) }
                    .padding(horizontal = 14.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (row.size == 1) Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SuggestionsDiscoveryTab(
    songs: List<SongItem>,
    bottomPadding: Dp,
    onSongClick: (SongItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp, 14.dp, 12.dp, bottomPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Column(Modifier.padding(bottom = 6.dp)) {
                Text("Suggested for you", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Based on your listening",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(songs.take(20), key = { _, song -> song.id }) { _, song ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
                ),
                modifier = Modifier.fillMaxWidth().clickable { onSongClick(song) },
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                ) {
                    AsyncImage(
                        model = song.thumbnail.highResolutionArtworkUrl(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)),
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            song.artists.joinToString { it.name },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = "Play ${song.title}",
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumDiscoveryTab(
    albums: List<AlbumItem>,
    bottomPadding: Dp,
    onAlbumClick: (AlbumItem) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(12.dp, 14.dp, 12.dp, bottomPadding),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { DiscoverySectionTitle("New release albums") }
        itemsIndexed(albums.chunked(2), key = { index, _ -> "album_row_$index" }) { _, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { album ->
                    AlbumGridCard(album, onClick = { onAlbumClick(album) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AlbumGridCard(album: AlbumItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = album.thumbnail.highResolutionArtworkUrl(),
            contentDescription = album.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)),
        )
        Text(
            album.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 7.dp),
        )
        Text(
            album.artists.orEmpty().joinToString { it.name },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun String.highResolutionArtworkUrl(): String =
    replace(Regex("=w\\d+-h\\d+"), "=w720-h720")
