package com.oryn.terminal.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import com.oryn.terminal.diagnostics.FreezeRecorder
import com.oryn.terminal.diagnostics.FreezeEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun FreezeReportsPane(
    onStabilityTestStarted: () -> Unit = {},
    storeProvider: () -> com.oryn.terminal.diagnostics.FreezeReportStore = FreezeRecorder::repository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = LocalKlyroColors.current
    var reports by remember { mutableStateOf<List<FreezeReportItem>>(emptyList()) }
    var feedback by rememberSaveable { mutableStateOf<String?>(null) }
    var feedbackError by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var operation by remember { mutableStateOf<String?>(null) }
    var pickingDestination by rememberSaveable { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by rememberSaveable { mutableStateOf<String?>(null) }
    fun isIdle() = !loading && operation == null && !pickingDestination
    val idle = isIdle()
    val feedbackLocation = remember { BringIntoViewRequester() }
    var feedbackRevision by remember { mutableIntStateOf(0) }
    LaunchedEffect(feedbackRevision) {
        if (feedbackRevision > 0 && feedback != null) {
            withFrameNanos { }
            feedbackLocation.bringIntoView()
        }
    }
    fun announce(message: String, error: Boolean = false) {
        feedback = message
        feedbackError = error
        feedbackRevision++
    }
    LaunchedEffect(revision) {
        loading = true
        loadError = false
        FreezeRecorder.event(FreezeEvents.Code.REPORTS)
        try {
            val refreshed = withContext(Dispatchers.IO) {
                val store = storeProvider()
                FreezeRecorder.snapshotStabilityTimeline(store)
                store.list().asReversed().map { id ->
                    try { FreezeReportItem(id, store.preview(id)) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: java.io.IOException) { FreezeReportItem(id, null) }
                }
            }
            reports = refreshed
            loaded = true
            if (selected != null && refreshed.none { it.id == selected }) selected = null
            if (deleteTarget != null && refreshed.none { it.id == deleteTarget }) deleteTarget = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            loadError = true
        } finally { loading = false }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        pickingDestination = false
        if (uri == null) {
            announce("Export cancelled. Reports remain on this device.")
        } else if (operation == null) {
            operation = "Exporting ZIP…"
            announce("Exporting ZIP…")
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val store = storeProvider()
                        FreezeRecorder.snapshotStabilityTimeline(store)
                        context.contentResolver.openOutputStream(uri, "w")?.use { store.exportZip(it) }
                            ?: throw java.io.IOException("Destination unavailable")
                    }
                    announce("Diagnostic ZIP saved.")
                } catch (cancelled: CancellationException) {
                    announce("Export interrupted. The destination file may be incomplete.", true)
                    throw cancelled
                } catch (_: Exception) {
                    announce("Could not save the ZIP. Check the destination and try again. The file may be incomplete.", true)
                } finally { operation = null }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)) {
        KlyroSectionHeading("Freeze reports", "Saved on this device. Nothing is uploaded automatically.")
        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Loading reports…", modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (loadError) Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            KlyroInlineNotice(
                if (loaded) "Refresh failed. Previously loaded reports are still shown. Retry when storage is available."
                else "Could not load reports. Retry when storage is available.", isError = true)
        }
        feedback?.let { message ->
            Column(Modifier.bringIntoViewRequester(feedbackLocation).semantics { liveRegion = LiveRegionMode.Polite }) {
                KlyroInlineNotice(message, isError = feedbackError)
                if (operation == null && !pickingDestination) TextButton(onClick = { feedback = null }) { Text("Dismiss message") }
            }
        }
        FreezeRecorder.lastError?.let { KlyroInlineNotice(it, isError = true) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedButton(onClick = { if (isIdle()) { loading = true; revision++ } }, enabled = idle) {
                Text(if (loadError) "Retry loading" else "Refresh")
            }
            Button(onClick = {
                if (isIdle() && reports.isNotEmpty()) {
                    pickingDestination = true
                    announce("Choose where to save the ZIP.")
                    try { export.launch("klyro-diagnostics-${java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.ROOT).format(java.util.Date())}.zip") }
                    catch (_: Exception) {
                        pickingDestination = false
                        announce("No file picker is available. Try again after enabling a document provider.", true)
                    }
                }
            }, enabled = idle && reports.isNotEmpty()) {
                Text(if (pickingDestination) "Choosing destination…" else "Export ZIP")
            }
        }
        operation?.let { message ->
            Text(message, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (loaded && !loading && reports.isEmpty() && !loadError) {
            KlyroInlineNotice("No saved reports. A missing report does not rule out a freeze.")
        }
        if (reports.isNotEmpty()) {
            Text("${reports.size} saved ${if (reports.size == 1) "report" else "reports"}",
                style = MaterialTheme.typography.labelMedium)
            Text("Review details before sharing. Detection alone does not identify the cause.",
                color = colors.muted, style = MaterialTheme.typography.bodySmall)
        }
        reports.forEach { report ->
            key(report.id) {
                Surface(color = colors.tile, shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.cardCorner)) {
                    Column(Modifier.fillMaxWidth().testTag("freeze-report-${report.id}").padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(report.dateLabel(), style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.semantics { heading() })
                        Text(report.stateLabel(), style = MaterialTheme.typography.bodyMedium)
                        if (report.body == null) KlyroInlineNotice("Report could not be read. Refresh to retry.", isError = true)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { selected = if (selected == report.id) null else report.id },
                                enabled = report.body != null,
                                modifier = Modifier.semantics {
                                    stateDescription = if (selected == report.id) "Expanded" else "Collapsed"
                                }) { Text(if (selected == report.id) "Hide details" else "Details") }
                            TextButton(onClick = { if (isIdle()) deleteTarget = report.id }, enabled = idle,
                                colors = ButtonDefaults.textButtonColors(contentColor = colors.danger)) { Text("Delete report") }
                        }
                        if (selected == report.id && report.body != null) {
                            Text("Report ID: ${report.id}", style = MaterialTheme.typography.labelSmall)
                            SelectionContainer {
                                Text(report.body, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)
                                        .verticalScroll(rememberScrollState()))
                            }
                            if (report.body.contains("[PREVIEW TRUNCATED")) {
                                Text("Preview shortened. Export the ZIP for the complete report.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        if (com.oryn.terminal.BuildConfig.DEBUG) FreezeStabilityControls(onStabilityTestStarted, enabled = idle)
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = colors.sheet,
            title = { Text("Delete this diagnostic?") },
            text = {
                Column(Modifier.heightIn(max = KlyroUiMetrics.paletteListMax).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(reports.firstOrNull { it.id == target }?.dateLabel() ?: target)
                    Text("Only this report will be removed. Terminal files and sessions are preserved. This cannot be undone.")
                }
            },
            confirmButton = { TextButton(enabled = idle, onClick = {
                if (!isIdle()) return@TextButton
                deleteTarget = null
                operation = "Deleting report…"
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { storeProvider().delete(target) }
                        reports = reports.filterNot { it.id == target }
                        if (selected == target) selected = null
                        announce("Report deleted.")
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { announce("Could not delete the report. It remains listed; refresh and retry.", true) }
                    finally { operation = null }
                }
            }, colors = ButtonDefaults.textButtonColors(contentColor = colors.danger)) { Text("Delete report") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep report") } }
        )
    }
}

internal data class FreezeReportItem(val id: String, val body: String?) {
    fun dateLabel(): String {
        val millis = id.substringBefore('-').toLongOrNull()?.takeIf { it > 0 } ?: return "Report date unavailable"
        return java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
            .format(java.util.Date(millis))
    }

    fun stateLabel(): String {
        val lines = body?.lineSequence().orEmpty()
        val summary = lines.firstOrNull { it.startsWith("summary_state=") }
        val state = (summary ?: body?.lineSequence()?.lastOrNull { it.startsWith("state=") })
            ?.substringAfter('=')?.substringBefore(' ')
        return when (state) {
            "UI_STALL_DETECTED" -> "Slow response detected"
            "UI_FREEZE_DETECTED" -> "Possible freeze detected"
            "UI_RECOVERED" -> "Responsiveness restored"
            "SYSTEM_ANR_CONFIRMED" -> "Android reported an unresponsive app"
            "PROCESS_EXIT_OTHER" -> "Process exit recorded"
            "STABILITY_TEST_STARTED" -> "Stability test started · completion unverified"
            "STABILITY_TEST_OBSERVED" -> "Stability activity recorded · completion unverified"
            else -> "Recovery not confirmed"
        }
    }
}

@Composable
internal fun FreezeRecoveryNotice() {
    val notice by FreezeRecorder.notice.collectAsState()
    val requested by FreezeRecorder.openReports.collectAsState()
    val inSettings by FreezeRecorder.settingsVisible.collectAsState()
    if (requested && !inSettings) {
        KlyroModalTheme {
            KlyroSheet(onDismissRequest = { FreezeRecorder.openReports.value = false }) {
                Column(Modifier.fillMaxWidth().heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .verticalScroll(rememberScrollState()).padding(20.dp)) {
                    TextButton(onClick = { FreezeRecorder.openReports.value = false }) { Text("Close diagnostics") }
                    FreezeReportsPane(
                        onStabilityTestStarted = {
                            FreezeRecorder.openReports.value = false
                        }
                    )
                }
            }
        }
    } else if (!requested && !inSettings) notice?.let { id ->
        KlyroModalTheme {
            AlertDialog(onDismissRequest = { FreezeRecorder.acknowledgeNotice(id) },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = LocalKlyroColors.current.sheet,
                title = { Text("An unresponsive period was recorded") },
                text = { Text("A local report is available. It may describe a detected delay or an Android ANR; review its details before drawing conclusions.",
                    modifier = Modifier.heightIn(max = KlyroUiMetrics.paletteListMax).verticalScroll(rememberScrollState())) },
                confirmButton = { TextButton(onClick = {
                    FreezeRecorder.acknowledgeNotice(id); FreezeRecorder.openReports.value = true
                }) { Text("View report") } },
                dismissButton = { TextButton(onClick = { FreezeRecorder.acknowledgeNotice(id) }) { Text("Later") } })
        }
    }
}

/** Compose may provide a themed ContextWrapper rather than MainActivity directly. */
private fun Context.klyroActivityOrNull(): com.oryn.terminal.MainActivity? {
    var current: Context = this
    repeat(16) {
        if (current is com.oryn.terminal.MainActivity) return current
        val wrapped = current as? ContextWrapper ?: return null
        val base = wrapped.baseContext
        if (base === current) return null
        current = base
    }
    return null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FreezeStabilityControls(onStabilityTestStarted: () -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var launching by remember { mutableStateOf(false) }
    var blockSeconds by rememberSaveable { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var strictIo by remember { mutableStateOf(false) }
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var scheduledStall by remember { mutableStateOf<Runnable?>(null) }
    val activity = context.klyroActivityOrNull()
    DisposableEffect(activity) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                scheduledStall?.let(handler::removeCallbacks)
                scheduledStall = null
            }
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            activity?.lifecycle?.removeObserver(observer)
            scheduledStall?.let(handler::removeCallbacks)
            FreezeRecorder.setStrictIo(false)
        }
    }
    Row(Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
        .toggleable(value = strictIo, enabled = enabled && !launching, role = Role.Switch) {
            strictIo = it; FreezeRecorder.setStrictIo(it)
        }, verticalAlignment = Alignment.CenterVertically) {
        Text("Check UI I/O (debug)", modifier = Modifier.weight(1f))
        Switch(checked = strictIo, onCheckedChange = null, enabled = enabled && !launching)
    }
    Text("Stability test (diagnostic build)", style = MaterialTheme.typography.titleMedium)
    Text("Creates a separate Bash tab, waits 3 seconds, then prints 1,500 lines. Go Home, open another app, then return. Keep the test tab until you inspect the END marker; close it explicitly when finished. Existing sessions receive no commands.")
    OutlinedButton(onClick = {
        // Guard at touch dispatch too: recomposition can lag behind rapid taps.
        if (launching || !enabled || scheduledStall != null) return@OutlinedButton
        launching = true
        feedback = "Preparing isolated test…"
        scope.launch {
            try {
                val result = context.klyroActivityOrNull()?.startDiagnosticStress()
                feedback = result?.message ?: "Activity unavailable; test not started."
                if (result?.started == true) onStabilityTestStarted()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                feedback = "Test not started (${error.javaClass.simpleName}); try again."
            } finally { launching = false }
        }
    }, enabled = enabled && !launching && scheduledStall == null) {
        Text(if (launching) "Preparing…" else "Start isolated 1,500-line test")
    }
    Text(feedback, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    val hasSessions = com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions()
    Text("Artificial UI stalls can trigger Android termination. Available only with no open sessions.")
    if (scheduledStall != null) {
        Text("Stall test scheduled. Leaving this screen or app cancels it.",
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        TextButton(onClick = { scheduledStall?.let(handler::removeCallbacks); scheduledStall = null }) { Text("Cancel scheduled test") }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { blockSeconds = 6 }, enabled = enabled && !launching && !hasSessions && scheduledStall == null) { Text("Test 6s stall") }
        TextButton(onClick = { blockSeconds = 11 }, enabled = enabled && !launching && !hasSessions && scheduledStall == null) { Text("Test 11s stall") }
    }
    if (blockSeconds != 0) AlertDialog(onDismissRequest = { blockSeconds = 0 },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(KlyroUiMetrics.modalCorner),
        containerColor = LocalKlyroColors.current.sheet,
        title = { Text("Block the interface for $blockSeconds seconds?") },
        text = { Text("This deliberate test may cause Android to terminate Klyro. No sessions may be open. The independent recorder should capture before recovery.",
            modifier = Modifier.heightIn(max = KlyroUiMetrics.paletteListMax).verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = {
            if (blockSeconds != 6 && blockSeconds != 11 || scheduledStall != null) return@TextButton
            val seconds = blockSeconds; blockSeconds = 0
            if (enabled && !launching && !com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions()) {
                val task = Runnable {
                    scheduledStall = null
                    if (!com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions() && FreezeRecorder.lifecycle == "RESUMED") {
                        android.os.SystemClock.sleep(seconds * 1000L)
                    } else feedback = "Stall test cancelled: the app or session state changed."
                }
                scheduledStall = task
                handler.postDelayed(task, 1500)
            } else feedback = "Close all sessions before running a stall test."
        }) { Text("Run deliberate test") } },
        dismissButton = { TextButton(onClick = { blockSeconds = 0 }) { Text("Cancel") } })
}
