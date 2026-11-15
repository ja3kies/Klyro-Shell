package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroHomeActionPolicyTest {
    @Test fun sessionResumesEvenIfRuntimeVerificationFails() {
        assertEquals(KlyroHomeAction.RESUME_SESSION,
            KlyroHomeActionPolicy.decide(runtimeVerified = false, hasSessions = true))
        assertEquals(KlyroHomeAction.RESUME_SESSION,
            KlyroHomeActionPolicy.decide(runtimeVerified = true, hasSessions = true))
    }

    @Test fun newShellLaunchNeedsVerifiedRuntimeAndNoExistingSessions() {
        assertEquals(KlyroHomeAction.START_BASH,
            KlyroHomeActionPolicy.decide(runtimeVerified = true, hasSessions = false))
        assertEquals(KlyroHomeAction.NONE,
            KlyroHomeActionPolicy.decide(runtimeVerified = false, hasSessions = false))
    }
}
