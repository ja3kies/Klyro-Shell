package com.oryn.terminal.ui

/**
 * Shared reachability policy for Klyro bottom sheets.
 * Six or fewer rows remain visible without an inner scroll when geometry allows.
 * Small screens, large font settings and active editors always retain a scroll escape.
 */
object KlyroSheetLayoutPolicy {
    const val MAX_FIXED_ROWS = 6
    const val MIN_SCREEN_HEIGHT_DP = 700
    const val MAX_FIXED_FONT_SCALE = 1.2f

    fun needsScroll(
        rows: Int,
        screenHeightDp: Int,
        fontScale: Float,
        editing: Boolean = false
    ): Boolean =
        rows > MAX_FIXED_ROWS || screenHeightDp < MIN_SCREEN_HEIGHT_DP ||
            fontScale > MAX_FIXED_FONT_SCALE || editing
}
