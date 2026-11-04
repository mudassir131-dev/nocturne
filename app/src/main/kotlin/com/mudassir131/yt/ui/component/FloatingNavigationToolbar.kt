/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mudassir131.yt.R
import com.mudassir131.yt.ui.screens.Screens

@Composable
fun FloatingNavigationToolbar(
    items: List<Screens>,
    slim: Boolean = false,
    pureBlack: Boolean,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    isSelected: (Screens) -> Boolean,
    onItemClick: (Screens, Boolean) -> Unit,
    onIdentifyClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStatsClick: () -> Unit,
) {
    val toolbarContainerColor = floatingToolbarContainerColor(pureBlack = pureBlack)
    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .wrapContentSize(),
            shape = RoundedCornerShape(32.dp),
            color = toolbarContainerColor,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { screen ->
                    val selected = isSelected(screen)

                    FloatingNavigationToolbarItem(
                        screen = screen,
                        selected = selected,
                        pureBlack = pureBlack,
                        slim = slim,
                        onClick = { onItemClick(screen, selected) },
                    )
                }

                Box(contentAlignment = Alignment.Center) {
                    val fabContainerColor = floatingToolbarFabContainerColor(pureBlack = pureBlack)
                    val fabContentColor = floatingToolbarFabContentColor(pureBlack = pureBlack)
                    val rotation by animateFloatAsState(
                        targetValue = if (fabMenuExpanded) 90f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "fab_rotation",
                    )

                    Surface(
                        onClick = { fabMenuExpanded = !fabMenuExpanded },
                        shape = CircleShape,
                        color = fabContainerColor,
                        contentColor = fabContentColor,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                painter = painterResource(if (fabMenuExpanded) R.drawable.close else R.drawable.more_vert),
                                contentDescription = "More Options",
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(rotation),
                                tint = fabContentColor,
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = fabMenuExpanded,
                        onDismissRequest = { fabMenuExpanded = false },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 8.dp,
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.music_recognition)) },
                            onClick = {
                                fabMenuExpanded = false
                                onIdentifyClick()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.mic),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            colors = getMenuColors(pureBlack),
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.history)) },
                            onClick = {
                                fabMenuExpanded = false
                                onHistoryClick()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.history),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            colors = getMenuColors(pureBlack),
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.stats)) },
                            onClick = {
                                fabMenuExpanded = false
                                onStatsClick()
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.stats_outlined),
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                            },
                            colors = getMenuColors(pureBlack),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getMenuColors(pureBlack: Boolean): MenuItemColors {
    return MenuDefaults.itemColors(
        textColor = if (pureBlack) Color.White else MaterialTheme.colorScheme.onSurface,
        leadingIconColor = if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FloatingNavigationToolbarItem(
    screen: Screens,
    selected: Boolean,
    pureBlack: Boolean,
    slim: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val containerColor by animateColorAsState(
        targetValue = when {
            selected -> floatingToolbarSelectedItemContainerColor(pureBlack = pureBlack)
            else -> Color.Transparent
        },
        label = "item_container_color",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> floatingToolbarSelectedItemContentColor(pureBlack = pureBlack)
            else -> floatingToolbarItemContentColor(pureBlack = pureBlack)
        },
        label = "item_content_color",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "item_scale",
    )

    val showLabel = selected && !slim

    Row(
        modifier = Modifier
            .scale(scale)
            .animateContentSize()
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .widthIn(min = 48.dp)
            .padding(
                horizontal = if (showLabel) 16.dp else 12.dp,
                vertical = 10.dp,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(if (selected) screen.iconIdActive else screen.iconIdInactive),
            contentDescription = stringResource(screen.titleId),
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )

        if (showLabel) {
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(screen.titleId),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun floatingToolbarContainerColor(pureBlack: Boolean): Color =
    if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer

@Composable
private fun floatingToolbarFabContainerColor(pureBlack: Boolean): Color =
    if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.tertiaryContainer

@Composable
private fun floatingToolbarFabContentColor(pureBlack: Boolean): Color =
    if (pureBlack) Color.White else MaterialTheme.colorScheme.onTertiaryContainer

@Composable
private fun floatingToolbarSelectedItemContainerColor(pureBlack: Boolean): Color =
    if (pureBlack) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer

@Composable
private fun floatingToolbarSelectedItemContentColor(pureBlack: Boolean): Color =
    if (pureBlack) Color.White else MaterialTheme.colorScheme.onSecondaryContainer

@Composable
private fun floatingToolbarItemContentColor(pureBlack: Boolean): Color =
    if (pureBlack) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant
