package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroHomeIllustrationSettingsTest {
    @Test fun unknownStoredPatternKeepsOriginal() {
        assertEquals(HomeIllustration.CHECKERBOARD, HomeIllustration.fromStored(null))
        assertEquals(HomeIllustration.CHECKERBOARD, HomeIllustration.fromStored("unknown"))
        HomeIllustration.entries.forEach {
            assertEquals(it, HomeIllustration.fromStored(it.name))
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
