package com.mudassir131.yt.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoHidingHeaderStateTest {
    @Test
    fun `header consumes only movement within its measured bounds`() {
        val state = AutoHidingHeaderState().apply { updateHeight(100f) }

        assertEquals(
            -40f,
            state.nestedScrollConnection
                .onPreScroll(Offset(0f, -40f), NestedScrollSource.UserInput)
                .y,
            0.001f,
        )
        assertEquals(
            -60f,
            state.nestedScrollConnection
                .onPreScroll(Offset(0f, -80f), NestedScrollSource.UserInput)
                .y,
            0.001f,
        )
        assertEquals(
            0f,
            state.nestedScrollConnection
                .onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput)
                .y,
            0.001f,
        )
        assertEquals(
            35f,
            state.nestedScrollConnection
                .onPreScroll(Offset(0f, 35f), NestedScrollSource.UserInput)
                .y,
            0.001f,
        )
    }
}
