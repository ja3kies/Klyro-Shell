package com.oryn.terminal.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.oryn.terminal.diagnostics.FreezeRecorder
import com.oryn.terminal.diagnostics.FreezeEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun FreezeReportsPane() {
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
                val store = FreezeRecorder.repository()
                store.list().asReversed().map { it to store.read(it) }
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
                    output.use { FreezeRecorder.repository().exportZip(it) }
                }
                status = "Diagnostic ZIP exported."
            } catch (e: Exception) { status = "Export failed (${e.javaClass.simpleName}); destination may be incomplete." }
            finally { busy = false }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Freeze reports", style = MaterialTheme.typography.titleLarge)
        Text(status)
        FreezeRecorder.lastError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row {
            TextButton(onClick = { revision++ }, enabled = !busy) { Text("Refresh") }
            TextButton(onClick = { export.launch("klyro-freeze-diagnostics.zip") }, enabled = !busy && reports.isNotEmpty()) { Text("Export ZIP") }
        }
        for ((id, body) in reports) {
            val state = body.lineSequence().lastOrNull { it.startsWith("state=") } ?: "unconfirmed"
            Text(java.util.Date(id.substringBefore('-').toLongOrNull() ?: 0).toString())
            Text(state)
            Row {
                TextButton(onClick = { selected = if (selected == id) null else id }) { Text("Details") }
                TextButton(onClick = { deleteTarget = id }, enabled = !busy) { Text("Delete report") }
            }
            if (selected == id) Text(body, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()))
        }
        if (com.oryn.terminal.BuildConfig.DEBUG) FreezeStabilityControls()
    }
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("Delete this diagnostic?") },
            text = { Text("Only this report will be removed. Terminal files and sessions are preserved.") },
            confirmButton = { TextButton(onClick = {
                deleteTarget = null
                scope.launch {
                    busy = true
                    try { withContext(Dispatchers.IO) { FreezeRecorder.repository().delete(target) }; revision++ }
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
    if (requested) {
        KlyroModalTheme {
            KlyroSheet(onDismissRequest = { FreezeRecorder.openReports.value = false }) {
                Column(Modifier.fillMaxWidth().heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .verticalScroll(rememberScrollState()).padding(20.dp)) {
                    TextButton(onClick = { FreezeRecorder.openReports.value = false }) { Text("Close diagnostics") }
                    FreezeReportsPane()
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

@Composable
private fun FreezeStabilityControls() {
    val context = LocalContext.current
    var blockSeconds by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    Text("Stability test (diagnostic build)", style = MaterialTheme.typography.titleMedium)
    Text("Creates a separate Bash tab, waits 3 seconds, then prints 1,500 lines. Go Home, open another app, then return. Keep the test tab until you inspect the END marker; close it explicitly when finished. Existing sessions receive no commands.")
    TextButton(onClick = {
        val activity = context as? com.oryn.terminal.MainActivity
        feedback = activity?.startDiagnosticStress() ?: "Activity unavailable; test not started."
    }) { Text("Start isolated 1,500-line test") }
    Text(feedback)
    val hasSessions = com.oryn.terminal.KlyroSessionRuntime.sessions.hasLiveSessions()
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
            if (!com.oryn.terminal.KlyroSessionRuntime.sessions.hasLiveSessions())
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ android.os.SystemClock.sleep(seconds * 1000L) }, 1500)
        }) { Text("Run deliberate test") } },
        dismissButton = { TextButton(onClick = { blockSeconds = 0 }) { Text("Cancel") } })
}
