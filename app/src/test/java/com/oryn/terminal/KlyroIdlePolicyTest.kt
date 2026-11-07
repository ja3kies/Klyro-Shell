package com.oryn.terminal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroIdlePolicyTest {
    private val sixHours = KlyroSessionRuntime.IDLE_TIMEOUT_MS

    @Test fun doesNotExpireBeforeSixHours() {
        assertFalse(KlyroSessionRuntime.isIdleExpired(sixHours - 1L, 0L))
        assertFalse(KlyroSessionRuntime.isIdleExpired(sixHours, sixHours + 1L))
    }

    @Test fun expiresAtSixHoursNotAfterSeven() {
        assertTrue(KlyroSessionRuntime.isIdleExpired(sixHours, 0L))
        assertTrue(KlyroSessionRuntime.isIdleExpired(sixHours + 1234L, 1234L))
        assertTrue(KlyroSessionRuntime.isIdleExpired(sixHours * 2L, 0L))
    }
}
