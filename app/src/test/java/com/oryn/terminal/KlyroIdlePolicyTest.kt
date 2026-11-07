package com.oryn.terminal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroIdlePolicyTest {
    private val sixHours = KlyroIdlePolicy.IDLE_TIMEOUT_MS

    @Test fun doesNotExpireBeforeSixHours() {
        assertFalse(KlyroIdlePolicy.isExpired(sixHours - 1L, 0L))
        assertFalse(KlyroIdlePolicy.isExpired(sixHours, sixHours + 1L))
    }

    @Test fun expiresAtSixHoursNotAfterSeven() {
        assertTrue(KlyroIdlePolicy.isExpired(sixHours, 0L))
        assertTrue(KlyroIdlePolicy.isExpired(sixHours + 1234L, 1234L))
        assertTrue(KlyroIdlePolicy.isExpired(sixHours * 2L, 0L))
    }
}
