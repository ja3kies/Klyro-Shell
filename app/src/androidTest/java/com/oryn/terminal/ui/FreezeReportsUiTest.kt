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
    @Test fun cancelDeletionPreservesReport() {
        ui.onNode(hasText("Delete report") and hasAnyAncestor(hasTestTag("freeze-report-$id"))).performClick()
        ui.onNodeWithText("Keep report").performClick()
        assertTrue(store.list().contains(id))
    }
    @Test fun confirmDeletionRemovesOnlySelectedReport() {
        ui.onNode(hasText("Delete report") and hasAnyAncestor(hasTestTag("freeze-report-$id"))).performClick()
        ui.onNode(hasText("Delete report") and hasAnyAncestor(isDialog())).performClick()
        ui.waitUntil(5000) { !store.list().contains(id) }
        assertFalse(store.list().contains(id))
    }
}
