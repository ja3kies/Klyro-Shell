package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KlyroSetupLedgerTest {
    @Test fun errorSurvivesActivityCallbackReplacement() {
        KlyroSetupLedger.begin("bootstrap", "Preparing")
        KlyroSetupLedger.progress("bootstrap", "Downloading", 60)
        KlyroSetupLedger.finish("bootstrap", false, "Digest mismatch", "SHA-256 mismatch")
        assertEquals(false, KlyroSetupLedger.snapshot()?.success)
        assertEquals("SHA-256 mismatch", KlyroSetupLedger.snapshot()?.details)
        assertEquals(60, KlyroSetupLedger.snapshot()?.progress)
    }

    @Test fun staleCallbacksCannotOverwriteNewOperation() {
        KlyroSetupLedger.begin("bootstrap", "Old")
        KlyroSetupLedger.begin("proot", "New")
        KlyroSetupLedger.finish("bootstrap", false, "Stale", "obsolete")
        KlyroSetupLedger.progress("bootstrap", "Stale", 99)
        assertNull(KlyroSetupLedger.snapshot()?.success)
        assertEquals("New", KlyroSetupLedger.snapshot()?.message)
    }

    @Test fun successKeepsCompletionState() {
        KlyroSetupLedger.begin("proot", "Verifying")
        KlyroSetupLedger.finish("proot", true, "Verified", null)
        assertEquals(100, KlyroSetupLedger.snapshot()?.progress)
        assertEquals(true, KlyroSetupLedger.snapshot()?.success)
    }
}
