package com.oryn.terminal.diagnostics

import org.junit.Test
import org.junit.Assert.*

class FreezeFailureBoundaryTest {
    @Test fun injectedCollectorMemoryFailureDoesNotEscapeAndNextTaskRuns() {
        // Inject the error, do not consume device memory or touch user sessions.
        FreezeRecorder.guard { throw OutOfMemoryError("injected collector failure") }
        assertTrue(FreezeRecorder.lastError!!.contains("memory pressure"))
        var ran = false
        FreezeRecorder.guard { ran = true }
        assertTrue(ran)
    }
}
