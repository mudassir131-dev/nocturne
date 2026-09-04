/*
 * Nocturne - by Mudassir
 * Licensed Under GPL-3.0
 */

package com.mudassir131.yt.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mudassir131.yt.ui.screens.Screens

/**
 * Stable floating bottom navigation. Scroll state deliberately does not participate in this
 * composition: only a destination change animates the selected capsule.
 */
/**
 * Stable floating bottom navigation with scroll-linked horizontal collapse.
 * On scroll down, the bar compresses into the compact active pill on the left while
 * the search action remains on the right, matching reference physics.
 */
@Composable
fun NocturneBottomBar(
    modifier: Modifier = Modifier,
    items: List<Screens>,
    currentRoute: String,
    pureBlack: Boolean,
    collapseFraction: Float = 0f,
    onTabSelected: (Screens) -> Unit,
) {
    val searchItem = items.firstOrNull { it.route == Screens.Search.route }
    val pillItems = items.filterNot { it.route == Screens.Search.route }
    val selectedIndex = pillItems.indexOfFirst {
        it.route == currentRoute || (it.route == Screens.Search.route && currentRoute.startsWith("search/"))
    }
    val searchSelected = currentRoute == Screens.Search.route || currentRoute.startsWith("search/")

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
    val labels = pillItems.map { stringResource(id = it.titleId) }
    val textWidths = remember(labels, density) {
        labels.map { label ->
            with(density) { textMeasurer.measure(label, textStyle).size.width.toDp() }
        }
    }

    val iconSize = 19.dp
    val spacerWidth = 5.dp
    val paddingHorizontal = 10.dp
    val containerColor = if (pureBlack) Color.Black else MaterialTheme.colorScheme.surfaceContainer
    val inactiveContentColor = if (pureBlack) {
        Color.White.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
    }

    val scrollState = LocalBottomBarScrollState.current
    val liveFraction = scrollState?.collapseFraction ?: collapseFraction
    val clampedFraction = liveFraction.coerceIn(0f, 1f)

    BoxWithConstraints(modifier = modifier) {
        val totalAvailableWidth = maxWidth
        val searchButtonSize = if (searchItem != null) 56.dp else 0.dp
        val horizontalGap = if (searchItem != null) 10.dp else 0.dp
        val maxLeftContainerWidth = (totalAvailableWidth - searchButtonSize - horizontalGap).coerceAtLeast(0.dp)

        val activeItemWidth = if (selectedIndex >= 0 && selectedIndex < textWidths.size) {
            textWidths[selectedIndex] + iconSize + spacerWidth + (paddingHorizontal * 2)
        } else {
            56.dp
        }
        val collapsedLeftWidth = (activeItemWidth + 12.dp).coerceAtMost(maxLeftContainerWidth)
        val currentLeftWidth = androidx.compose.ui.unit.lerp(maxLeftContainerWidth, collapsedLeftWidth, clampedFraction)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Navigation Pill Container
            Box(
                modifier = Modifier
                    .width(currentLeftWidth)
                    .fillMaxHeight()
                    .shadow(12.dp, CircleShape, clip = false)
                    .background(containerColor, CircleShape)
                    .padding(horizontal = 6.dp),
            ) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val containerInnerWidth = maxWidth
                    val slotWidth = if (pillItems.isNotEmpty()) containerInnerWidth / pillItems.size else 0.dp
                    val targetCapsuleWidth = if (selectedIndex >= 0 && selectedIndex < textWidths.size) {
                        textWidths[selectedIndex] + iconSize + spacerWidth + (paddingHorizontal * 2)
                    } else {
                        0.dp
                    }

                    val naturalCenter = if (selectedIndex >= 0 && pillItems.isNotEmpty()) {
                        val halfWidth = targetCapsuleWidth / 2
                        val center = slotWidth * (selectedIndex + 0.5f)
                        center.coerceIn(halfWidth + 2.dp, (containerInnerWidth - halfWidth - 2.dp).coerceAtLeast(halfWidth + 2.dp))
                    } else {
                        0.dp
                    }
                    val collapsedCenter = containerInnerWidth / 2
                    val interpolatedCenter = androidx.compose.ui.unit.lerp(naturalCenter, collapsedCenter, clampedFraction)

                    val capsuleWidth by animateDpAsState(
                        targetValue = targetCapsuleWidth,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                        label = "capsule_width",
                    )
                    val capsuleCenter by animateDpAsState(
                        targetValue = interpolatedCenter,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                        label = "capsule_center",
                    )

                    // Active Capsule Highlight Pill
                    if (selectedIndex >= 0) {
                        val activeCapsuleRenderWidth = androidx.compose.ui.unit.lerp(
                            capsuleWidth,
                            (containerInnerWidth - 4.dp).coerceAtLeast(0.dp),
                            clampedFraction
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = (capsuleCenter - activeCapsuleRenderWidth / 2).coerceAtLeast(0.dp))
                                .width(activeCapsuleRenderWidth)
                                .fillMaxHeight()
                                .padding(vertical = 7.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        )
                    }

                    // Navigation Items
                    val unselectedAlpha = ((1f - clampedFraction * 2.2f)).coerceIn(0f, 1f)
                    val unselectedScale = 1f - (clampedFraction * 0.3f)

                    if (clampedFraction < 0.95f) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            pillItems.forEachIndexed { index, item ->
                                val isActive = selectedIndex == index
                                val label = labels[index]
                                val expandedCenter = slotWidth * (index + 0.5f)
                                val centerAdjustment = if (isActive) naturalCenter - expandedCenter else 0.dp
                                val iconOffsetTarget = if (isActive) {
                                    -(textWidths[index] + spacerWidth) / 2 + centerAdjustment
                                } else {
                                    0.dp
                                }
                                val labelOffsetTarget = if (isActive) {
                                    (iconSize + spacerWidth) / 2 + centerAdjustment
                                } else {
                                    0.dp
                                }

                                val iconOffset by animateDpAsState(iconOffsetTarget, label = "icon_offset")
                                val labelOffset by animateDpAsState(labelOffsetTarget, label = "label_offset")
                                val labelAlpha by animateFloatAsState(
                                    targetValue = if (isActive) 1f else 0f,
                                    label = "label_alpha",
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .graphicsLayer {
                                            if (!isActive) {
                                                alpha = unselectedAlpha
                                                scaleX = unselectedScale
                                                scaleY = unselectedScale
                                            }
                                        }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = { onTabSelected(item) },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painter = painterResource(if (isActive) item.iconIdActive else item.iconIdInactive),
                                        contentDescription = label,
                                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                        else inactiveContentColor,
                                        modifier = Modifier
                                            .size(iconSize)
                                            .offset(x = iconOffset),
                                    )
                                    if (labelAlpha > 0f) {
                                        Text(
                                            text = label,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            modifier = Modifier
                                                .offset(x = labelOffset)
                                                .alpha(labelAlpha),
                                        )
                                    }
                                }
                            }
                        }
                    } else if (selectedIndex >= 0 && selectedIndex < pillItems.size) {
                        // Fully collapsed: render only the single active tab centered in the compact pill
                        val activeItem = pillItems[selectedIndex]
                        val label = labels[selectedIndex]
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onTabSelected(activeItem) },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    painter = painterResource(activeItem.iconIdActive),
                                    contentDescription = label,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(iconSize),
                                )
                                Spacer(modifier = Modifier.width(spacerWidth))
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            // Right Floating Search Button
            searchItem?.let { item ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .shadow(12.dp, CircleShape, clip = false)
                        .background(
                            if (searchSelected) MaterialTheme.colorScheme.primaryContainer else containerColor,
                            CircleShape,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(item) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (searchSelected) item.iconIdActive else item.iconIdInactive),
                        contentDescription = stringResource(item.titleId),
                        tint = if (searchSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else inactiveContentColor,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}

