package com.oryn.terminal.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.unit.dp
import com.oryn.terminal.diagnostics.FreezeRecorder
import com.oryn.terminal.diagnostics.FreezeEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FreezeReportsPane(
    onStabilityTestStarted: () -> Unit = {},
    storeProvider: () -> com.oryn.terminal.diagnostics.FreezeReportStore = FreezeRecorder::repository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var reports by remember { mutableStateOf<List<Pair<String,String>>>(emptyList()) }
    var listStatus by remember { mutableStateOf("Loading reports…") }
    var status by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var choosingDestination by rememberSaveable { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(revision) {
        FreezeRecorder.event(FreezeEvents.Code.REPORTS)
        try {
            reports = withContext(Dispatchers.IO) {
                val store = storeProvider()
                FreezeRecorder.snapshotStabilityTimeline(store)
                store.list().asReversed().map { it to store.preview(it) }
            }
            listStatus = if (reports.isEmpty()) "No recorded incidents. This does not prove that no freeze occurred." else "Local heuristics are not official Android ANRs. An unfinished report means recovery was not observed."
        } catch (e: Exception) { listStatus = "Unable to load diagnostics (${e.javaClass.simpleName}). Retry." }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        choosingDestination = false
        if (uri == null) status = "Export cancelled."
        else if (!busy) scope.launch {
            busy = true
            status = "Exporting diagnostic ZIP…"
            try {
                withContext(Dispatchers.IO) {
                    val output = context.contentResolver.openOutputStream(uri, "w")
                        ?: throw java.io.IOException("Destination unavailable")
                    output.use {
                        val store = storeProvider()
                        FreezeRecorder.snapshotStabilityTimeline(store)
                        store.exportZip(it)
                    }
                }
                status = "Diagnostic ZIP exported."
            } catch (e: Exception) { status = "Export failed (${e.javaClass.simpleName}); destination may be incomplete." }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Freeze reports", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(listStatus, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        status?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        FreezeRecorder.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FlowRow {
            TextButton(onClick = { status = null; revision++ }, enabled = !busy) { Text("Refresh") }
            TextButton(onClick = {
                if (choosingDestination || busy) return@TextButton
                choosingDestination = true
                try {
                    export.launch("klyro-freeze-diagnostics.zip")
                } catch (e: android.content.ActivityNotFoundException) {
                    choosingDestination = false
                    status = "No document picker is available for exporting the ZIP."
                }
            }, enabled = !busy && !choosingDestination && reports.isNotEmpty()) { Text("Export ZIP") }
        }
        for ((id, body) in reports) {
            Column(Modifier.fillMaxWidth().testTag("freeze-report-$id")) {
                val summary = remember(body) {
                    body.lineSequence().firstOrNull { it.startsWith("summary_state=") }
                        ?: body.lineSequence().lastOrNull { it.startsWith("state=") }
                }
                val state = when (summary?.substringAfter('=')?.substringBefore(' ')) {
                    "UI_RECOVERED" -> "Interface recovered"
                    "UI_FREEZE_DETECTED" -> "Probable interface freeze"
                    "UI_STALL_DETECTED" -> "Possible interface stall"
                    "SYSTEM_ANR_CONFIRMED" -> "Android confirmed an ANR"
                    "PROCESS_EXIT_OTHER" -> "Process exited"
                    "STABILITY_TEST_STARTED" -> "Stability test started"
                    "STABILITY_TEST_OBSERVED" -> "Stability test observed"
                    else -> "Unconfirmed incident"
                }
                Text(state, style = MaterialTheme.typography.titleMedium)
                reportDate(id)?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                val duration = summary?.substringAfter(" duration_ms=", "")
                    ?.substringBefore(' ')?.toLongOrNull()?.takeIf { it >= 0 }
                duration?.let { Text("Observed stall: $it ms", style = MaterialTheme.typography.bodySmall) }
                FlowRow {
                    TextButton(onClick = { selected = if (selected == id) null else id }) {
                        Text(if (selected == id) "Hide details" else "Details")
                    }
                    TextButton(onClick = { deleteTarget = id }, enabled = !busy) { Text("Delete report") }
                }
                if (selected == id) Text(body, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()))
            }
        }
        if (com.oryn.terminal.BuildConfig.DEBUG) FreezeStabilityControls(onStabilityTestStarted)
    }
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete this diagnostic?") },
            text = { Text("Remove the report from ${reportDate(target) ?: target}? Terminal files and sessions are preserved.") },
            confirmButton = { TextButton(onClick = {
                deleteTarget = null
                scope.launch {
                    busy = true
                    try { withContext(Dispatchers.IO) { storeProvider().delete(target) }; status = "Diagnostic report removed."; revision++ }
                    catch (e: Exception) { status = "Delete failed (${e.javaClass.simpleName})." }
                    finally { busy = false }
                }
            }) { Text("Delete report") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep report") } })
    }
}

private fun reportDate(id: String): String? = id.substringBefore('-').toLongOrNull()?.let {
    java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))
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
    } else notice?.let { id ->
        AlertDialog(onDismissRequest = { FreezeRecorder.acknowledgeNotice(id) },
            title = { Text("Klyro stopped responding") },
            text = { Text("Klyro was unresponsive for several seconds, or Android recorded an ANR. A local diagnostic is available. This is not a root-cause determination.") },
            confirmButton = { TextButton(onClick = {
                FreezeRecorder.acknowledgeNotice(id); FreezeRecorder.openReports.value = true
            }) { Text("View / Export diagnostic") } },
            dismissButton = { TextButton(onClick = { FreezeRecorder.acknowledgeNotice(id) }) { Text("Later") } })
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

@Composable
private fun FreezeStabilityControls(onStabilityTestStarted: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var launching by remember { mutableStateOf(false) }
    var blockSeconds by remember { mutableIntStateOf(0) }
    var scheduledSeconds by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE && scheduledSeconds != 0) {
                scheduledSeconds = 0
                feedback = "Test cancelled when Klyro left the foreground."
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(scheduledSeconds) {
        val seconds = scheduledSeconds
        if (seconds == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(1500)
        if (scheduledSeconds != seconds) return@LaunchedEffect
        scheduledSeconds = 0
        if (com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions()) {
            feedback = "Test cancelled because a session is open."
        } else if (lifecycle.currentState != androidx.lifecycle.Lifecycle.State.RESUMED) {
            feedback = "Test cancelled because Klyro is not in the foreground."
        } else {
            android.os.SystemClock.sleep(seconds * 1000L)
            feedback = "Deliberate stall finished. Refresh the report list to check the recording."
        }
    }
    var strictIo by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { FreezeRecorder.setStrictIo(false) } }
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
            .toggleable(value = strictIo, role = Role.Switch) {
                strictIo = it
                FreezeRecorder.setStrictIo(it)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Check UI I/O (debug)", modifier = Modifier.weight(1f))
        Switch(checked = strictIo, onCheckedChange = null)
    }
    Text("Stability test (diagnostic build)", style = MaterialTheme.typography.titleMedium)
    Text("Creates a separate Bash tab, waits 3 seconds, then prints 1,500 lines. Go Home, open another app, then return. Keep the test tab until you inspect the END marker; close it explicitly when finished. Existing sessions receive no commands.")
    TextButton(onClick = {
        // Guard at touch dispatch too: recomposition can lag behind rapid taps.
        if (launching || scheduledSeconds != 0) return@TextButton
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
    }, enabled = !launching && scheduledSeconds == 0) {
        Text(if (launching) "Preparing…" else "Start isolated 1,500-line test")
    }
    Text(feedback, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    val hasSessions = com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions()
    Text("Artificial UI stalls can trigger Android termination. Available only with no open sessions.")
    Row {
        TextButton(onClick = { blockSeconds = 6 }, enabled = !hasSessions && !launching && scheduledSeconds == 0) { Text("Test 6s stall") }
        TextButton(onClick = { blockSeconds = 11 }, enabled = !hasSessions && !launching && scheduledSeconds == 0) { Text("Test 11s stall") }
    }
    if (scheduledSeconds != 0) {
        TextButton(onClick = { scheduledSeconds = 0; feedback = "Stall test cancelled." }) {
            Text("Cancel scheduled stall")
        }
    }
    if (blockSeconds != 0) AlertDialog(onDismissRequest = { blockSeconds = 0 },
        title = { Text("Block the interface for $blockSeconds seconds?") },
        text = { Text("This deliberate test may cause Android to terminate Klyro. No sessions may be open. The independent recorder should capture before recovery.") },
        confirmButton = { TextButton(onClick = {
            if (scheduledSeconds == 0 && blockSeconds != 0) {
                scheduledSeconds = blockSeconds
                feedback = "Preparing a ${blockSeconds}-second stall…"
            }
            blockSeconds = 0
        }) { Text("Run deliberate test") } },
        dismissButton = { TextButton(onClick = { blockSeconds = 0 }) { Text("Cancel") } })
}
