/*
 * Nocturne - by Mudassir
 * Nikhil
 * Licensed Under GPL-3.0
 */



package com.mudassir131.yt.ui.utils

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun CornerBasedShape.top(): CornerBasedShape =
    copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))

fun CornerBasedShape.bottom(): CornerBasedShape =
    copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))

enum class PreferencePosition {
    SINGLE, FIRST, MIDDLE, LAST
}

fun getPreferenceShape(position: PreferencePosition, cornerRadius: Dp = 24.dp): Shape {
    return when (position) {
        PreferencePosition.SINGLE -> RoundedCornerShape(cornerRadius)
        PreferencePosition.FIRST -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        PreferencePosition.LAST -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
        PreferencePosition.MIDDLE -> androidx.compose.ui.graphics.RectangleShape
    }
}

fun getGroupShape(index: Int, count: Int, cornerRadius: Dp = 18.dp): Shape {
    if (count <= 1) return RoundedCornerShape(cornerRadius)
    return when (index) {
        0 -> RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
        count - 1 -> RoundedCornerShape(bottomStart = cornerRadius, bottomEnd = cornerRadius)
        else -> androidx.compose.ui.graphics.RectangleShape
    }
}

fun isCornerZero(cornerSize: CornerSize): Boolean {
    return cornerSize == CornerSize(0.dp) || 
           cornerSize.toString().contains("0px") || 
           cornerSize.toString().contains("0.0.dp") || 
           cornerSize.toString().contains("0%")
}

val LocalListItemShape = androidx.compose.runtime.staticCompositionLocalOf<Shape?> { null }
val LocalPreferenceShape = androidx.compose.runtime.staticCompositionLocalOf<Shape?> { null }
