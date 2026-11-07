package com.oryn.terminal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroSessionRecoveryTest {
    @Test fun reportsLostPtysOnlyAfterProcessDeath() {
        assertTrue(KlyroSessionRecovery.shouldNotify(true, 0))
        assertFalse(KlyroSessionRecovery.shouldNotify(true, 1))
        assertFalse(KlyroSessionRecovery.shouldNotify(true, 8))
        assertFalse(KlyroSessionRecovery.shouldNotify(false, 0))
    }
}
