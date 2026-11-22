package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroHomeIllustrationSettingsTest {
    @Test fun unknownStoredPatternKeepsOriginal() {
        assertEquals(HomeIllustration.CHECKERBOARD, HomeIllustration.fromStored(null))
        assertEquals(HomeIllustration.CHECKERBOARD, HomeIllustration.fromStored("unknown"))
        HomeIllustration.entries.forEach {
            assertEquals(it, HomeIllustration.fromStored(it.name))
        }
    }

    @Test fun warmLionKeepsTheSourceTimingAndSeamlessWrap() {
        assertEquals(150_000, HomeIllustration.WARM_LION.periodMs)
        assertEquals(20_000, HomeIllustration.CHECKERBOARD.periodMs)
        assertEquals(12, warmLionStreams.size)
        assertEquals(listOf(235, 252, 150, 253, 204, 134, 179, 299, 215, 281, 158, 210),
            warmLionStreams.map { it.period })
        for (stream in warmLionStreams) {
            assertEquals(stream.initial.toFloat(), stream.offset(0f), .01f)
            assertEquals(stream.offset(0f), stream.offset(1f), .01f)
            assertTrue(stream.offset(.001f) != stream.offset(0f))
            assertTrue(stream.offset(.5f) in 0f..stream.period.toFloat())
        }
    }

    @Test fun speedIsBoundedAndAdvancedSwitchRestoresDefault() {
        assertEquals(.5f, HomeIllustrationSettings.normalizeSpeed(-10f), 0f)
        assertEquals(2f, HomeIllustrationSettings.normalizeSpeed(100f), 0f)
        assertEquals(1f, HomeIllustrationSettings.normalizeSpeed(Float.NaN), 0f)
        assertEquals(1.25f, HomeIllustrationSettings.normalizeSpeed(1.2f), 0f)
        assertEquals(1f, HomeIllustrationSettings(advanced = false, speed = 2f).effectiveSpeed, 0f)
        assertEquals(2f, HomeIllustrationSettings(advanced = true, speed = 2f).effectiveSpeed, 0f)
    }
}
