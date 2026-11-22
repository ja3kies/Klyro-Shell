package com.oryn.terminal

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionOnScreen

/** The sheet is in a separate dialog window; compare both rectangles in screen coordinates. */
internal fun LayoutCoordinates.homePatternScreenBounds(): Rect? {
    if (!isAttached || size.width <= 0 || size.height <= 0) return null
    val origin = positionOnScreen()
    if (!origin.x.isFinite() || !origin.y.isFinite()) return null
    return Rect(origin.x, origin.y, origin.x + size.width, origin.y + size.height)
}

/**
 * SheetState's offset is relative to the dialog's layout root and already
 * includes drag/expand placement. The external sheet modifier can describe
 * the unshifted layout slot, so do not use its top as an occlusion boundary.
 * Unknown geometry must keep the animation running.
 */
internal fun homeSheetPlacedBounds(
    rootX: Int,
    rootY: Int,
    rootWidth: Int,
    sheetSize: IntSize,
    offset: Float?
): Rect? {
    if (offset == null || !offset.isFinite() || rootWidth <= 0 ||
        sheetSize.width <= 0 || sheetSize.height <= 0 ||
        sheetSize.width > rootWidth) return null
    val left = rootX + (rootWidth - sheetSize.width) / 2f
    val top = rootY + offset
    return Rect(left, top, left + sheetSize.width, top + sheetSize.height)
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

    fun updateSheet(bounds: Rect?) {
        val previous = sheetBounds
        if (previous != null && bounds != null) {
            if (bounds.top > previous.top) revealing = true
            else if (bounds.top < previous.top) revealing = false
        }
        if (bounds == null) revealing = false
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
