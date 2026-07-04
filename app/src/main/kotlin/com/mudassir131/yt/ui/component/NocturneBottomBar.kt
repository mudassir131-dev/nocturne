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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
 * Premium custom floating bottom navigation bar for Nocturne.
 * Implements a pill-shaped layout with a dynamic Material You capsule highlight that smoothly
 * travels between destinations.
 */
@Composable
fun NocturneBottomBar(
    modifier: Modifier = Modifier,
    items: List<Screens>,
    currentRoute: String,
    pureBlack: Boolean,
    onTabSelected: (Screens) -> Unit
) {
    val selectedIndex = items.indexOfFirst { 
        it.route == currentRoute || (it.route == "search" && currentRoute.startsWith("search/"))
    }.coerceAtLeast(0)

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )

    val labels = items.map { stringResource(id = it.titleId) }
    val textWidths = remember(labels, density) {
        labels.map { label ->
            val result = textMeasurer.measure(label, textStyle)
            with(density) { result.size.width.toDp() }
        }
    }

    val iconSize = 20.dp
    val spacerWidth = 6.dp
    val paddingHorizontal = 12.dp

    val containerColor = if (pureBlack) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(containerColor)
            .padding(horizontal = 8.dp) // padding inset from bar ends
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
        ) {
            val totalWidth = maxWidth
            val slotWidth = totalWidth / items.size

            // Capsule dimensions and offset
            val targetWidth = textWidths[selectedIndex] + iconSize + spacerWidth + (paddingHorizontal * 2)
            val capsuleWidth by animateDpAsState(
                targetValue = targetWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "capsule_width"
            )

            val targetCenter = slotWidth * (selectedIndex + 0.5f)
            
            // Constrain capsule center within the bar boundaries (with 6.dp margin)
            val halfWidth = targetWidth / 2
            val minCenter = halfWidth + 6.dp
            val maxCenter = totalWidth - halfWidth - 6.dp
            val constrainedTargetCenter = targetCenter.coerceIn(minCenter, maxCenter)

            val capsuleCenter by animateDpAsState(
                targetValue = constrainedTargetCenter,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "capsule_center"
            )

            // Animated Material You capsule background
            Box(
                modifier = Modifier
                    .offset(x = capsuleCenter - (capsuleWidth / 2))
                    .width(capsuleWidth)
                    .height(44.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    )
            )

            // Tab slots
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isActive = selectedIndex == index
                    val label = labels[index]
                    val textWidthDp = textWidths[index]

                    val centerAdjustment = if (isActive) constrainedTargetCenter - targetCenter else 0.dp
                    val targetIconOffset = if (isActive) -(textWidthDp + spacerWidth) / 2 + centerAdjustment else 0.dp
                    val targetLabelOffset = if (isActive) (iconSize + spacerWidth) / 2 + centerAdjustment else 0.dp
                    val targetAlpha = if (isActive) 1f else 0f

                    val iconOffset by animateDpAsState(
                        targetValue = targetIconOffset,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "icon_offset"
                    )

                    val labelOffset by animateDpAsState(
                        targetValue = targetLabelOffset,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "label_offset"
                    )

                    val labelAlpha by animateFloatAsState(
                        targetValue = targetAlpha,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "label_alpha"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(item) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (isActive) item.iconIdActive else item.iconIdInactive),
                            contentDescription = label,
                            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier
                                .size(iconSize)
                                .offset(x = iconOffset)
                        )

                        if (labelAlpha > 0f) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                modifier = Modifier
                                    .offset(x = labelOffset)
                                    .alpha(labelAlpha)
                            )
                        }
                    }
                }
            }
        }
    }
}
