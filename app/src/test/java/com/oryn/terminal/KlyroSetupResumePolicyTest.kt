package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroSetupResumePolicyTest {
    private val failure = KlyroSetupSnapshot(
        "bootstrap", "Checksum mismatch", 20, false, "Expected different digest")

    @Test fun rotatedActivityNeverAutomaticallyRetriesFailure() {
        assertEquals(KlyroSetupResumeAction.SHOW_FAILURE,
            KlyroSetupResumePolicy.decide(false, true, failure))
        assertEquals(KlyroSetupResumeAction.SHOW_FAILURE,
            KlyroSetupResumePolicy.decide(false, false, failure))
    }

    @Test fun activeOperationContinuesObservation() {
        assertEquals(KlyroSetupResumeAction.OBSERVE,
            KlyroSetupResumePolicy.decide(true, true, failure))
    }

    @Test fun aCleanBootstrapCanStillBeVerified() {
        assertEquals(KlyroSetupResumeAction.VERIFY_PROOT,
            KlyroSetupResumePolicy.decide(false, true, null))
        assertEquals(KlyroSetupResumeAction.IDLE,
            KlyroSetupResumePolicy.decide(false, false, null))
    }
}
