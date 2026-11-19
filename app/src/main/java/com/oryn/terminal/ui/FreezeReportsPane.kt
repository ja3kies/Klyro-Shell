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
    var status by remember { mutableStateOf("Loading reports…") }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(revision) {
        FreezeRecorder.event(FreezeEvents.Code.REPORTS)
        try {
            reports = withContext(Dispatchers.IO) {
                val store = storeProvider()
                FreezeRecorder.snapshotStabilityTimeline(store)
                store.list().asReversed().map { it to store.preview(it) }
            }
            status = if (reports.isEmpty()) "No recorded incidents. This does not prove that no freeze occurred." else "Local heuristics are not official Android ANRs. An unfinished report means recovery was not observed."
        } catch (e: Exception) { status = "Unable to load diagnostics (${e.javaClass.simpleName}). Retry." }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null && !busy) scope.launch {
            busy = true
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
        Text(status, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        FreezeRecorder.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        FlowRow {
            TextButton(onClick = { revision++ }, enabled = !busy) { Text("Refresh") }
            TextButton(onClick = { export.launch("klyro-freeze-diagnostics.zip") }, enabled = !busy && reports.isNotEmpty()) { Text("Export ZIP") }
        }
        for ((id, body) in reports) {
            Column(Modifier.fillMaxWidth().testTag("freeze-report-$id")) {
                val state = remember(body) {
                    val line = body.lineSequence().firstOrNull { it.startsWith("summary_state=") }
                        ?: body.lineSequence().lastOrNull { it.startsWith("state=") }
                    when (line?.substringAfter('=')?.substringBefore(' ')) {
                        "UI_RECOVERED" -> "Interface recovered"
                        "UI_FREEZE_DETECTED" -> "Probable interface freeze"
                        "UI_STALL_DETECTED" -> "Possible interface stall"
                        "SYSTEM_ANR_CONFIRMED" -> "Android confirmed an ANR"
                        "PROCESS_EXIT_OTHER" -> "Process exited"
                        "STABILITY_TEST_STARTED" -> "Stability test started"
                        "STABILITY_TEST_OBSERVED" -> "Stability test observed"
                        else -> "Unconfirmed incident"
                    }
                }
                Text(state, style = MaterialTheme.typography.titleMedium)
                id.substringBefore('-').toLongOrNull()?.let { timestamp ->
                    Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(timestamp)),
                        style = MaterialTheme.typography.bodySmall)
                }
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
            text = { Text("Only this report will be removed. Terminal files and sessions are preserved.") },
            confirmButton = { TextButton(onClick = {
                deleteTarget = null
                scope.launch {
                    busy = true
                    try { withContext(Dispatchers.IO) { storeProvider().delete(target) }; revision++ }
                    catch (e: Exception) { status = "Delete failed (${e.javaClass.simpleName})." }
                    finally { busy = false }
                }
            }) { Text("Delete report") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Keep report") } })
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
    var feedback by remember { mutableStateOf("") }
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
        if (launching) return@TextButton
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
    }, enabled = !launching) {
        Text(if (launching) "Preparing…" else "Start isolated 1,500-line test")
    }
    Text(feedback, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    val hasSessions = com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions()
    Text("Artificial UI stalls can trigger Android termination. Available only with no open sessions.")
    Row {
        TextButton(onClick = { blockSeconds = 6 }, enabled = !hasSessions) { Text("Test 6s stall") }
        TextButton(onClick = { blockSeconds = 11 }, enabled = !hasSessions) { Text("Test 11s stall") }
    }
    if (blockSeconds != 0) AlertDialog(onDismissRequest = { blockSeconds = 0 },
        title = { Text("Block the interface for $blockSeconds seconds?") },
        text = { Text("This deliberate test may cause Android to terminate Klyro. No sessions may be open. The independent recorder should capture before recovery.") },
        confirmButton = { TextButton(onClick = {
            val seconds = blockSeconds; blockSeconds = 0
            if (!com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions())
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (!com.oryn.terminal.KlyroSessionRuntime.sessions.hasOpenSessions() && FreezeRecorder.lifecycle == "RESUMED")
                        android.os.SystemClock.sleep(seconds * 1000L)
                }, 1500)
        }) { Text("Run deliberate test") } },
        dismissButton = { TextButton(onClick = { blockSeconds = 0 }) { Text("Cancel") } })
}
