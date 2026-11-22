package com.oryn.terminal

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePatternVisibilityTest {
    private fun tracker() = HomePatternVisibility().apply {
        coverBounds = Rect(0f, 24f, 400f, 280f)
    }

    @Test fun compactSheetUsesPlacedTopNotTheUnshiftedLayoutSlot() {
        val state = tracker()
        val compact = homeSheetPlacedBounds(0, 0, 400, IntSize(400, 360), 440f)
        assertEquals(Rect(0f, 440f, 400f, 800f), compact)
        state.updateSheet(compact)
        assertTrue(state.shouldAnimate)
        state.settleMovement()
        assertTrue(state.shouldAnimate)
    }

    @Test fun actualFullCoverAndDownwardDragFollowOffset() {
        val state = tracker()
        val size = IntSize(400, 800)
        state.updateSheet(homeSheetPlacedBounds(0, 0, 400, size, 0f))
        assertFalse(state.shouldAnimate)
        state.updateSheet(homeSheetPlacedBounds(0, 0, 400, size, 8f))
        assertTrue(state.shouldAnimate)
        state.updateSheet(homeSheetPlacedBounds(0, 0, 400, size, 0f))
        assertFalse(state.shouldAnimate)
    }

    @Test fun unknownPlacementDoesNotStopAnimation() {
        val state = tracker()
        state.updateSheet(Rect(0f, 0f, 400f, 800f))
        assertFalse(state.shouldAnimate)
        assertNull(homeSheetPlacedBounds(0, 0, 400, IntSize.Zero, 0f))
        assertNull(homeSheetPlacedBounds(0, 0, 400, IntSize(400, 800), null))
        state.updateSheet(null)
        assertTrue(state.shouldAnimate)
    }

    @Test fun noSheetAndPartiallyCoveredBannerAnimate() {
        val state = tracker()
        assertTrue(state.shouldAnimate)
        state.updateSheet(Rect(0f, 275f, 400f, 800f))
        assertTrue(state.shouldAnimate)
        state.updateSheet(Rect(0f, 25f, 400f, 800f))
        assertTrue(state.shouldAnimate)
    }

    @Test fun aFullyCoveringStationarySheetStopsTheLoop() {
        val state = tracker()
        state.updateSheet(Rect(0f, 0f, 400f, 800f))
        assertFalse(state.shouldAnimate)
    }

    @Test fun smallDownwardDragWakesLoopBeforeBannerIsExposedAndReversalStopsIt() {
        val state = tracker()
        state.updateSheet(Rect(0f, 0f, 400f, 800f))
        state.updateSheet(Rect(0f, 8f, 400f, 808f))
        assertTrue(state.shouldAnimate)
        state.updateSheet(Rect(0f, 0f, 400f, 800f))
        assertFalse(state.shouldAnimate)
    }

    @Test fun aStalledDragStopsIfStillFullyCovered() {
        val state = tracker()
        state.updateSheet(Rect(0f, 0f, 400f, 800f))
        state.updateSheet(Rect(0f, 8f, 400f, 808f))
        state.settleMovement()
        assertFalse(state.shouldAnimate)
    }

    @Test fun dismissalTargetWakesTheLoopBeforeMovementAndDisposalClearsOcclusion() {
        val state = tracker()
        state.updateSheet(Rect(0f, 0f, 400f, 800f))
        state.closing = true
        assertTrue(state.shouldAnimate)
        state.closing = false
        assertFalse(state.shouldAnimate)
        state.clearSheet()
        assertTrue(state.shouldAnimate)
    }

    @Test fun aNarrowSheetDoesNotHideTheEntireBanner() {
        val state = tracker()
        state.updateSheet(Rect(20f, 0f, 380f, 800f))
        assertTrue(state.shouldAnimate)
    }
}
