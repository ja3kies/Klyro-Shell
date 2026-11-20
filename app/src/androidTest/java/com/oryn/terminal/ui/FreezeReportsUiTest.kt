package com.oryn.terminal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.oryn.terminal.diagnostics.FreezeRecorder
import org.junit.*
import org.junit.Assert.*

/** Only own report IDs are created/deleted. Never clears a user's report history. */
class FreezeReportsUiTest {
    @get:Rule val ui = createComposeRule()
    private lateinit var id: String
    private lateinit var store: com.oryn.terminal.diagnostics.FreezeReportStore
    private lateinit var directory: java.io.File
    @Before fun seed() {
        directory = java.nio.file.Files.createTempDirectory(
            InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.toPath(), "freeze-ui-test-").toFile()
        store = com.oryn.terminal.diagnostics.FreezeReportStore(directory)
        id = "${System.currentTimeMillis()}-uitest-${System.nanoTime()}"
        store.write(id, "state=UI_RECOVERED duration_ms=6500")
        ui.setContent { MaterialTheme { FreezeReportsPane { store } } }
        ui.waitUntil(5000) { ui.onAllNodesWithTag("freeze-report-$id").fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun removeOwnReport() { directory.walkBottomUp().forEach { it.delete() } }
    @Test fun recoveredReportShowsReadableStatusAndTogglesTechnicalDetails() {
        ui.onNodeWithText("Interface recovered").assertExists()
        ui.onNodeWithText("Observed stall: 6500 ms").assertIsDisplayed()
        ui.onNodeWithText("Details").performClick()
        ui.onNodeWithText("state=UI_RECOVERED duration_ms=6500", substring = true).assertExists()
        ui.onNodeWithText("Hide details").performClick()
        ui.onNodeWithText("state=UI_RECOVERED duration_ms=6500", substring = true).assertDoesNotExist()
    }
    @Test fun cancelPendingStallReenablesTestsWithoutRunningOne() {
        ui.onNodeWithText("Test 6s stall").performClick()
        ui.mainClock.autoAdvance = false
        try {
            ui.onNodeWithText("Run deliberate test").performClick()
            ui.mainClock.advanceTimeBy(80)
            ui.onNodeWithText("Test 11s stall").assertIsNotEnabled()
            ui.onNodeWithText("Cancel scheduled stall").performClick()
            ui.mainClock.advanceTimeBy(1600)
            ui.onNodeWithText("Stall test cancelled.").assertExists()
            ui.onNodeWithText("Test 11s stall").assertIsEnabled()
            ui.onNodeWithText("Cancel scheduled stall").assertDoesNotExist()
        } finally {
            ui.mainClock.autoAdvance = true
        }
    }

    @Test fun cancelDeletionPreservesReport() {
        ui.onNode(hasText("Delete report") and hasAnyAncestor(hasTestTag("freeze-report-$id"))).performClick()
        ui.onNodeWithText("Keep report").performClick()
        assertTrue(store.list().contains(id))
    }
    @Test fun confirmDeletionRemovesOnlySelectedReport() {
        ui.onNode(hasText("Delete report") and hasAnyAncestor(hasTestTag("freeze-report-$id"))).performClick()
        ui.onNode(hasText("Delete report") and hasAnyAncestor(isDialog())).performClick()
        ui.waitUntil(5000) { !store.list().contains(id) }
        ui.waitUntil(5000) { ui.onAllNodesWithText("Diagnostic report removed.").fetchSemanticsNodes().isNotEmpty() }
        assertFalse(store.list().contains(id))
    }
}
