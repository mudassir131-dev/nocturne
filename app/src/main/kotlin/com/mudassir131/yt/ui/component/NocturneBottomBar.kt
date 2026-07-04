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
 * Implements a pill-shaped layout with a pink capsule highlight that smoothly
 * travels between destinations.
 */
@Composable
fun NocturneBottomBar(
    modifier: Modifier = Modifier,
    items: List<Screens>,
    currentRoute: String,
    onTabSelected: (Screens) -> Unit
) {
    val selectedIndex = items.indexOfFirst { 
        it.route == currentRoute || (it.route == "search" && currentRoute.startsWith("search/"))
    }.coerceAtLeast(0)

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold
    )

    val labels = items.map { stringResource(id = it.titleId) }
    val textWidths = remember(labels, density) {
        labels.map { label ->
            val result = textMeasurer.measure(label, textStyle)
            with(density) { result.size.width.toDp() }
        }
    }

    val iconSize = 22.dp
    val spacerWidth = 8.dp
    val paddingHorizontal = 16.dp

    Box(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(Color.Black)
            .padding(horizontal = 12.dp) // extra horizontal inset for internal elements
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
            val capsuleCenter by animateDpAsState(
                targetValue = targetCenter,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "capsule_center"
            )

            // Animated pink capsule background
            Box(
                modifier = Modifier
                    .offset(x = capsuleCenter - (capsuleWidth / 2))
                    .width(capsuleWidth)
                    .height(48.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        color = Color(0xFFFFB2BC), // Elegant soft pink
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

                    val targetIconOffset = if (isActive) -(textWidthDp + spacerWidth) / 2 else 0.dp
                    val targetLabelOffset = if (isActive) (iconSize + spacerWidth) / 2 else 0.dp
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
                            tint = if (isActive) Color.Black else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(iconSize)
                                .offset(x = iconOffset)
                        )

                        if (labelAlpha > 0f) {
                            Text(
                                text = label,
                                color = Color.Black,
                                fontSize = 13.sp,
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
