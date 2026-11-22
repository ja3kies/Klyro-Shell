package com.oryn.terminal

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePatternVisibilityTest {
    private fun tracker() = HomePatternVisibility().apply {
        coverBounds = Rect(0f, 24f, 400f, 280f)
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
