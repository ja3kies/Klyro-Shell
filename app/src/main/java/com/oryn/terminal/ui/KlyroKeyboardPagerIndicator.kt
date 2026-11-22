package com.oryn.terminal.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A page indicator, not a segmented control. The filled pill follows swipe
 * progress continuously; the dots are tappable with separate 48dp targets.
 * Its visible size is intentionally tiny, so it does not resemble a toolbar.
 */
@Composable
internal fun KlyroKeyboardPagerIndicator(
    pagerState: PagerState,
    accent: Color,
    muted: Color,
    motionEnabled: Boolean
) {
    val scope = rememberCoroutineScope()
    val pages = pagerState.pageCount
    if (pages <= 1) return
    val position = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
        .coerceIn(0f, (pages - 1).toFloat())

    Box(
        modifier = Modifier.width((pages * 48).dp).height(48.dp)
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
        Row(
            modifier = Modifier.width((pages * 48).dp).height(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages) { index ->
                val name = when (index) {
                    0 -> "Show terminal keys"
                    1 -> "Show pinned commands"
                    else -> "Show keyboard page ${index + 1}"
                }
                Box(
                    modifier = Modifier.size(48.dp)
                        .clickable(role = Role.Button, onClickLabel = name) {
                            scope.launch {
                                if (motionEnabled) pagerState.animateScrollToPage(index)
                                else pagerState.scrollToPage(index)
                            }
                        }
                        .semantics {
                            contentDescription = name
                            selected = pagerState.currentPage == index
                        }
                        .testTag("keyboard-page-target-$index")
                )
            }
        }
    }
}
