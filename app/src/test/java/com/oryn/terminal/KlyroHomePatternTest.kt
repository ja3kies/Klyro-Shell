package com.oryn.terminal

import androidx.compose.ui.graphics.Color
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

    @Test fun motionRepeatsWithoutJumpingAndMovesWithinOneSecond() {
        assertEquals(0f, homePatternOffsetCells(0f), .001f)
        assertEquals(3.2f, homePatternOffsetCells(18f), .001f)
        assertEquals(8f, homePatternOffsetCells(45f), .001f)
        assertEquals(0f, homePatternOffsetCells(90f), .001f)
        assertEquals(0f, homePatternOffsetCells(360f), .001f)
        for (angle in 0..360) {
            assertTrue(homePatternOffsetCells(angle.toFloat()) in 0f..<16f)
        }
    }

    @Test fun themePulsePreservesHueAndLoopsWithoutDiscontinuity() {
        val accents = listOf(Color(0xFFBDCC64), Color(0xFF00F0FF),
            Color(0xFFF57C41), Color(0xFFCA5DEC), Color.White)
        for (accent in accents) {
            val start = homePatternTint(accent, 0f)
            val middle = homePatternTint(accent, 180f)
            val end = homePatternTint(accent, 360f)
            assertEquals(accent.red * .84f, start.red, .005f)
            assertEquals(accent.green * .84f, start.green, .005f)
            assertEquals(accent.blue * .84f, start.blue, .005f)
            assertEquals(start.red, end.red, .005f)
            assertEquals(start.green, end.green, .005f)
            assertEquals(start.blue, end.blue, .005f)
            assertEquals(start.red * .5f, middle.red, .005f)
            assertEquals(start.green * .5f, middle.green, .005f)
            assertEquals(start.blue * .5f, middle.blue, .005f)
            assertTrue(middle != start)
        }
        assertEquals(20_000, HOME_PATTERN_PERIOD_MS)
    }
}
