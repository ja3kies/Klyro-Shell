package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroHomePatternTest {
    @Test fun cssConicLayersRepeatAtTheCorrectScale() {
        for (row in 0 until 32) {
            for (column in 0 until 32) {
                assertEquals(homePatternColoredCell(column, row),
                    homePatternColoredCell(column + 16, row + 16))
            }
        }
        for (row in 0 until 8) {
            for (column in 0 until 8) {
                assertFalse(homePatternColoredCell(column, row))
                assertTrue(homePatternColoredCell(column + 8, row + 8))
                assertEquals((column % 2) != (row % 2),
                    homePatternColoredCell(column + 8, row))
                assertEquals((column % 2) != (row % 2),
                    homePatternColoredCell(column, row + 8))
            }
        }
    }

    @Test fun hueRotationReturnsToOriginalWithoutAColorDiscontinuity() {
        val start = homePatternHue(0f)
        val end = homePatternHue(360f)
        assertEquals(.65f, start.red, .005f)
        assertEquals(.44f, start.green, .005f)
        assertEquals(.86f, start.blue, .005f)
        assertEquals(start.red, end.red, .005f)
        assertEquals(start.green, end.green, .005f)
        assertEquals(start.blue, end.blue, .005f)
        assertTrue(homePatternHue(180f) != start)
        assertEquals(20_000, HOME_PATTERN_PERIOD_MS)
    }
}
