package com.oryn.terminal

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen

/** The sheet is in a separate dialog window; compare both rectangles in screen coordinates. */
internal fun LayoutCoordinates.homePatternScreenBounds(): Rect? {
    if (!isAttached || size.width <= 0 || size.height <= 0) return null
    val origin = positionOnScreen()
    if (!origin.x.isFinite() || !origin.y.isFinite()) return null
    return Rect(origin.x, origin.y, origin.x + size.width, origin.y + size.height)
}

/** Observes layout, not pointer input: the sheet retains exclusive ownership of gestures. */
internal class HomePatternVisibility {
    var coverBounds by mutableStateOf<Rect?>(null)
    var sheetBounds by mutableStateOf<Rect?>(null)
        private set
    var closing by mutableStateOf(false)
    private var revealing by mutableStateOf(false)

    val shouldAnimate by derivedStateOf {
        val cover = coverBounds
        val sheet = sheetBounds
        closing || revealing || cover == null || sheet == null ||
            sheet.left > cover.left || sheet.top > cover.top ||
            sheet.right < cover.right || sheet.bottom < cover.bottom
    }

    fun updateSheet(bounds: Rect) {
        val previous = sheetBounds
        if (previous != null) {
            if (bounds.top > previous.top) revealing = true
            else if (bounds.top < previous.top) revealing = false
        }
        sheetBounds = bounds
    }

    /** A stationary fully occluding sheet need not keep the decorative loop alive. */
    fun settleMovement() {
        revealing = false
    }

    fun clearSheet() {
        sheetBounds = null
        closing = false
        revealing = false
    }
}

internal val LocalHomePatternVisibility = compositionLocalOf<HomePatternVisibility?> { null }
