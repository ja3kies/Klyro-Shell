package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroRuntimeBadgePolicyTest {
    private fun badge(
        installing: Boolean = false, verifying: Boolean = false,
        verified: Boolean = false, warnings: Boolean = false,
        bashStarted: Boolean = false, installed: Boolean = false,
        bootstrap: Boolean = false, error: Boolean = false
    ) = KlyroRuntimeBadgePolicy.resolve(
        installing, verifying, verified, warnings, bashStarted, installed, bootstrap, error)

    @Test fun activeOperationsOverrideStaleFailures() {
        assertEquals("INSTALLING", badge(installing = true, error = true).label)
        assertEquals("CHECKING ENVIRONMENT", badge(verifying = true, error = true).label)
    }

    @Test fun incompleteBashNeverLooksLikeSuccessfulStartup() {
        assertEquals("BASH INCOMPLETE", badge(bashStarted = true, installed = true).label)
        assertEquals("VERIFICATION FAILED", badge(bashStarted = true, error = true).label)
    }

    @Test fun successAndPartialSuccessHaveDistinctStates() {
        assertEquals(KlyroRuntimeBadgeSeverity.READY, badge(verified = true).severity)
        assertEquals("READY WITH WARNINGS", badge(verified = true, warnings = true).label)
        assertEquals("NOT VERIFIED", badge(installed = true).label)
        assertEquals("SETUP REQUIRED", badge().label)
    }
}
