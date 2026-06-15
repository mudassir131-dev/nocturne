/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import com.mudassir131.yt.constants.GlassEffectsKey
import com.mudassir131.yt.constants.GlassEffectsMode
import com.mudassir131.yt.utils.rememberEnumPreference
import com.mudassir131.yt.ui.theme.glassmorphic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSliderTrack(
    sliderState: SliderState,
    modifier: Modifier = Modifier,
    colors: SliderColors = SliderDefaults.colors(),
    trackHeight: Dp = 10.dp
) {
    val glassEffectsMode by rememberEnumPreference(
        key = GlassEffectsKey,
        defaultValue = GlassEffectsMode.ADAPTIVE
    )
    val isGlassActive = glassEffectsMode != GlassEffectsMode.DISABLED

    if (isGlassActive) {
        val activeColor = colors.activeTrackColor
        val valueRange = sliderState.valueRange
        val fraction = calcFraction(
            valueRange.start,
            valueRange.endInclusive,
            sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(trackHeight)
                .glassmorphic(
                    shape = RoundedCornerShape(50),
                    borderColor = Color.White.copy(alpha = 0.15f),
                    borderWidth = 0.5.dp,
                    fallbackColor = Color.White.copy(alpha = 0.08f)
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(activeColor, RoundedCornerShape(50))
            )
        }
    } else {
        val inactiveTrackColor = colors.inactiveTrackColor
        val activeTrackColor = colors.activeTrackColor
        val inactiveTickColor = colors.inactiveTickColor
        val activeTickColor = colors.activeTickColor
        val valueRange = sliderState.valueRange
        Canvas(
            modifier
                .fillMaxWidth()
                .height(trackHeight)
        ) {
            drawTrack(
                stepsToTickFractions(sliderState.steps),
                0f,
                calcFraction(
                    valueRange.start,
                    valueRange.endInclusive,
                    sliderState.value.coerceIn(valueRange.start, valueRange.endInclusive)
                ),
                inactiveTrackColor,
                activeTrackColor,
                inactiveTickColor,
                activeTickColor,
                trackHeight
            )
        }
    }
}

private fun DrawScope.drawTrack(
    tickFractions: FloatArray,
    activeRangeStart: Float,
    activeRangeEnd: Float,
    inactiveTrackColor: Color,
    activeTrackColor: Color,
    inactiveTickColor: Color,
    activeTickColor: Color,
    trackHeight: Dp = 2.dp
) {
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val sliderLeft = Offset(0f, center.y)
    val sliderRight = Offset(size.width, center.y)
    val sliderStart = if (isRtl) sliderRight else sliderLeft
    val sliderEnd = if (isRtl) sliderLeft else sliderRight
    val tickSize = 2.0.dp.toPx()
    val trackStrokeWidth = trackHeight.toPx()
    drawLine(
        inactiveTrackColor,
        sliderStart,
        sliderEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    val sliderValueEnd = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeEnd,
        center.y
    )
    val sliderValueStart = Offset(
        sliderStart.x +
                (sliderEnd.x - sliderStart.x) * activeRangeStart,
        center.y
    )
    drawLine(
        activeTrackColor,
        sliderValueStart,
        sliderValueEnd,
        trackStrokeWidth,
        StrokeCap.Round
    )
    for (tick in tickFractions) {
        val outsideFraction = tick > activeRangeEnd || tick < activeRangeStart
        drawCircle(
            color = if (outsideFraction) inactiveTickColor else activeTickColor,
            center = Offset(lerp(sliderStart, sliderEnd, tick).x, center.y),
            radius = tickSize / 2f
        )
    }
}

private fun stepsToTickFractions(steps: Int): FloatArray {
    return if (steps == 0) floatArrayOf() else FloatArray(steps + 2) { it.toFloat() / (steps + 1) }
}

private fun calcFraction(a: Float, b: Float, pos: Float) =
    (if (b - a == 0f) 0f else (pos - a) / (b - a)).coerceIn(0f, 1f)
