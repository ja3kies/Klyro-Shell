package com.oryn.terminal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun KlyroKeyboardPagerIndicator(
    pagerState: PagerState,
    accent: Color,
    muted: Color,
    motionEnabled: Boolean
) {
    val pages = pagerState.pageCount
    if (pages <= 1) return
    val position = (pagerState.currentPage + (if (motionEnabled) pagerState.currentPageOffsetFraction else 0f))
        .coerceIn(0f, (pages - 1).toFloat())

    Box(
        modifier = Modifier.height(12.dp)
            .testTag("terminal-keyboard-pager-indicator"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .width((18 + (pages - 1) * 24).dp)
                .height(8.dp)
        ) {
            val unit = 1.dp.toPx()
            val dotRadius = 3f * unit
            val pillWidth = 18f * unit
            val pillHeight = 6f * unit
            val mutedInk = muted.copy(alpha = .55f)

            for (index in 0 until pages) {
                drawCircle(
                    color = mutedInk,
                    radius = dotRadius,
                    center = Offset((9f + index * 24f) * unit, size.height / 2f)
                )
            }
            drawRoundRect(
                color = accent,
                topLeft = Offset(position * 24f * unit, (size.height - pillHeight) / 2f),
                size = Size(pillWidth, pillHeight),
                cornerRadius = CornerRadius(pillHeight / 2f)
            )
        }
    }
}
