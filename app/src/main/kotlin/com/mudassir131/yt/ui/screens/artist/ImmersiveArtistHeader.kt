package com.mudassir131.yt.ui.screens.artist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.utils.resize
import com.mudassir131.yt.ui.appleplayer.liveart.CanvasArtwork
import com.mudassir131.yt.ui.appleplayer.liveart.CanvasArtworkPlayer

@Composable
internal fun ImmersiveArtistHeader(
    artistName: String,
    thumbnail: String?,
    description: String?,
    subscriberCount: String?,
    monthlyListeners: String?,
    inlineCanvasArtwork: CanvasArtwork?,
    backgroundCanvasArtwork: CanvasArtwork?,
    palette: List<Color>,
    scrollOffsetPx: Int,
    isSubscribed: Boolean,
    songCount: Int,
    albumCount: Int,
    canShuffle: Boolean,
    canRadio: Boolean,
    onSubscribe: () -> Unit,
    onShuffle: () -> Unit,
    onRadio: () -> Unit,
) {
    val surface = MaterialTheme.colorScheme.surface
    var descriptionExpanded by rememberSaveable(artistName) { mutableStateOf(false) }
    val accent = palette.firstOrNull() ?: MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(390.dp)
                .graphicsLayer {
                    val progress = (scrollOffsetPx / 900f).coerceIn(0f, 0.10f)
                    scaleX = 1f + progress
                    scaleY = 1f + progress
                    translationY = scrollOffsetPx * 0.16f
                },
        ) {
            if (backgroundCanvasArtwork?.preferredAnimationUrl != null) {
                CanvasArtworkPlayer(
                    primaryUrl = backgroundCanvasArtwork.preferredAnimationUrl,
                    fallbackUrl = backgroundCanvasArtwork.videoUrl,
                    isPlaying = true,
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (!thumbnail.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnail.resize(1400, 900),
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.72f), MaterialTheme.colorScheme.secondaryContainer),
                        ),
                    ),
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.52f to accent.copy(alpha = 0.20f),
                        1f to surface,
                    ),
                ),
            )
        }

        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (inlineCanvasArtwork?.preferredAnimationUrl != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(58.dp),
                    ) {
                        CanvasArtworkPlayer(
                            primaryUrl = inlineCanvasArtwork.preferredAnimationUrl,
                            fallbackUrl = inlineCanvasArtwork.videoUrl,
                            isPlaying = true,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            if (!subscriberCount.isNullOrBlank() || !monthlyListeners.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    if (!subscriberCount.isNullOrBlank()) {
                        ArtistMetricPill(R.drawable.person, subscriberCount)
                    }
                    if (!monthlyListeners.isNullOrBlank()) {
                        ArtistMetricPill(R.drawable.graphic_eq, monthlyListeners)
                    }
                }
            }

            if (songCount > 0 || albumCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    if (songCount > 0) ArtistMetricPill(R.drawable.music_note, "$songCount Songs")
                    if (albumCount > 0) ArtistMetricPill(R.drawable.album, "$albumCount Albums")
                }
            }

            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clickable { descriptionExpanded = !descriptionExpanded },
                )
                AnimatedVisibility(
                    visible = !descriptionExpanded && description.length > 100,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Text(
                        "more",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            ) {
                ArtistAction(
                    icon = if (isSubscribed) R.drawable.done else R.drawable.add,
                    label = if (isSubscribed) "Subscribed" else "Subscribe",
                    onClick = onSubscribe,
                    modifier = Modifier.weight(1f),
                )
                if (canRadio) {
                    ArtistAction(
                        icon = R.drawable.radio,
                        label = "Radio",
                        onClick = onRadio,
                        modifier = Modifier.weight(1f),
                    )
                }
                ArtistAction(
                    icon = R.drawable.shuffle,
                    label = "Shuffle",
                    onClick = onShuffle,
                    enabled = canShuffle,
                    primary = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ArtistMetricPill(icon: Int, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        ) {
            Icon(painterResource(icon), null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.size(6.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
private fun ArtistAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = if (primary) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 7.dp),
        modifier = modifier.height(48.dp),
    ) {
        Icon(painterResource(icon), null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(5.dp))
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
