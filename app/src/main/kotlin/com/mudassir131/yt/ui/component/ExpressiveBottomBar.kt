/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.screens.Screens

/**
 * Expressive compact floating bottom navigation bar.
 * Matches the reference design: a sleek pill holding Home, Search, Library with the active
 * tab expanded into an icon+label pill, plus a detached circular button on the right that
 * flips to a close icon and opens an anchored popup menu (Identify Music, Mood & Genres, Shuffle).
 */
@Composable
fun ExpressiveBottomBar(
    modifier: Modifier = Modifier,
    items: List<Screens>,
    currentRoute: String,
    pureBlack: Boolean,
    onTabSelected: (Screens) -> Unit,
    onIdentifyMusic: () -> Unit,
    onMoodAndGenres: () -> Unit,
    onShuffle: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    val barHeight = 56.dp
    val density = LocalDensity.current
    val barHeightPx = with(density) { barHeight.roundToPx() }
    val popupOffsetPx = with(density) { 10.dp.roundToPx() }

    val barContainerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val activePillColor = if (pureBlack) Color(0xFF262626) else MaterialTheme.colorScheme.secondaryContainer
    val activeContentColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
    val inactiveIconColor = if (pureBlack) Color.White.copy(alpha = 0.70f) else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (pureBlack) BorderStroke(1.dp, Color(0xFF222222)) else null

    Row(
        modifier = modifier.height(barHeight),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Main floating pill for tabs (Home, Search, Library)
        Surface(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = CircleShape,
            color = barContainerColor,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
            border = borderColor,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val isSelected = item.route == currentRoute ||
                            (item.route == Screens.Search.route && currentRoute.startsWith("search/"))
                    val label = stringResource(id = item.titleId)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(item) },
                            ),
                    ) {
                        if (isSelected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .background(activePillColor, CircleShape)
                                    .padding(horizontal = 16.dp),
                            ) {
                                Icon(
                                    painter = painterResource(item.iconIdActive),
                                    contentDescription = label,
                                    tint = activeContentColor,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = activeContentColor,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            ) {
                                Icon(
                                    painter = painterResource(item.iconIdInactive),
                                    contentDescription = label,
                                    tint = inactiveIconColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Circular overflow button + anchored popup menu above it
        Box(
            modifier = Modifier.size(barHeight),
            contentAlignment = Alignment.Center,
        ) {
            val buttonColor by animateColorAsState(
                targetValue = if (menuOpen) activePillColor else barContainerColor,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "buttonColor",
            )
            val iconTint by animateColorAsState(
                targetValue = if (menuOpen) activeContentColor else (if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface),
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "iconTint",
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { menuOpen = !menuOpen },
                    ),
                shape = CircleShape,
                color = buttonColor,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                border = borderColor,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (menuOpen) R.drawable.close else R.drawable.more_horiz),
                        contentDescription = "More actions",
                        tint = iconTint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            // Popup menu anchored directly above the button
            if (menuOpen) {
                Popup(
                    alignment = Alignment.BottomEnd,
                    offset = IntOffset(x = 0, y = -(barHeightPx + popupOffsetPx)),
                    onDismissRequest = { menuOpen = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (pureBlack) Color(0xFF1C1C1C) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        tonalElevation = 6.dp,
                        shadowElevation = 12.dp,
                        border = if (pureBlack) BorderStroke(1.dp, Color(0xFF303030)) else null,
                        modifier = Modifier
                            .width(190.dp)
                            .padding(bottom = 2.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                        ) {
                            OverflowActionRow(
                                icon = R.drawable.search,
                                label = stringResource(R.string.identify_music),
                                pureBlack = pureBlack,
                                onClick = {
                                    menuOpen = false
                                    onIdentifyMusic()
                                },
                            )
                            OverflowActionRow(
                                icon = R.drawable.list,
                                label = stringResource(R.string.mood_and_genres),
                                pureBlack = pureBlack,
                                onClick = {
                                    menuOpen = false
                                    onMoodAndGenres()
                                },
                            )
                            OverflowActionRow(
                                icon = R.drawable.play,
                                label = stringResource(R.string.shuffle),
                                pureBlack = pureBlack,
                                onClick = {
                                    menuOpen = false
                                    onShuffle()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverflowActionRow(
    icon: Int,
    label: String,
    pureBlack: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}
