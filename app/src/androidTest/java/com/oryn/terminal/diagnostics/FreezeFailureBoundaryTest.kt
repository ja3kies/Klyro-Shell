package com.oryn.terminal.diagnostics

import org.junit.Test
import org.junit.Assert.*

class FreezeFailureBoundaryTest {
    @Test fun timelinePersistenceRejectsTheMainThreadBeforeAnyIo() {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val directory = java.nio.file.Files.createTempDirectory(context.cacheDir.toPath(), "timeline-worker-test-").toFile()
        val store = FreezeReportStore(directory)
        try {
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
                try { FreezeRecorder.snapshotStabilityTimeline(store); fail("UI I/O must be rejected") }
                catch (_: IllegalStateException) { }
            }
            assertTrue(store.list().isEmpty())
        } finally { directory.walkBottomUp().forEach { it.delete() } }
    }
    @Test fun injectedCollectorMemoryFailureDoesNotEscapeAndNextTaskRuns() {
        // Inject the error, do not consume device memory or touch user sessions.
        FreezeRecorder.guard { throw OutOfMemoryError("injected collector failure") }
        assertTrue(FreezeRecorder.lastError!!.contains("memory pressure"))
        var ran = false
        FreezeRecorder.guard { ran = true }
        assertTrue(ran)
    }
}
