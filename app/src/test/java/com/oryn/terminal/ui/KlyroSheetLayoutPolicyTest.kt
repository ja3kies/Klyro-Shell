package com.oryn.terminal.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroSheetLayoutPolicyTest {
    @Test fun sixOrFewerRowsRemainFixedOnNormalScreens() {
        for (rows in 0..6) {
            assertFalse(KlyroSheetLayoutPolicy.needsScroll(rows, 700, 1.0f))
            assertFalse(KlyroSheetLayoutPolicy.needsScroll(rows, 880, 1.2f))
        }
    }

    @Test fun overflowAndAccessibilityAlwaysRetainScrolling() {
        assertTrue(KlyroSheetLayoutPolicy.needsScroll(7, 850, 1.0f))
        assertTrue(KlyroSheetLayoutPolicy.needsScroll(4, 699, 1.0f))
        assertTrue(KlyroSheetLayoutPolicy.needsScroll(4, 850, 1.21f))
        assertTrue(KlyroSheetLayoutPolicy.needsScroll(4, 850, 1.0f, editing = true))
    }

    @Test fun boundaryValuesMatchSharedDesignContract() {
        assertTrue(KlyroSheetLayoutPolicy.MAX_FIXED_ROWS == 6)
        assertTrue(KlyroSheetLayoutPolicy.MIN_SCREEN_HEIGHT_DP == 700)
        assertTrue(KlyroSheetLayoutPolicy.MAX_FIXED_FONT_SCALE == 1.2f)
    }
}
