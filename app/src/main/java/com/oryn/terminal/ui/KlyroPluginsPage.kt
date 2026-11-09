package com.oryn.terminal.ui

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryn.terminal.R
import com.oryn.terminal.plugins.KlyroPluginManager
import com.oryn.terminal.plugins.KlyroPluginRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Acode-inspired Installed / Explore manager, intentionally no remote code execution. */
@Composable
internal fun KlyroPluginsPage(onBack: () -> Unit) {
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    val manager = remember(context) { KlyroPluginManager(context) }
    val scope = rememberCoroutineScope()
    var mutationBusy by remember { mutableStateOf(false) }
    var previewBusy by remember { mutableStateOf(false) }
    var installed by remember { mutableStateOf(manager.installed()) }
    var page by rememberSaveable { mutableStateOf(if (installed.isEmpty()) "explore" else "installed") }
    var error by remember { mutableStateOf<String?>(null) }
    var removeId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<KlyroPluginRecord?>(null) }
    var pendingImportUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBundledId by rememberSaveable { mutableStateOf<String?>(null) }
    fun navigateBack() {
        // Visible Home and Android Back must unwind the same dialog stack.
        if (mutationBusy) return
        when {
            pendingImport != null || pendingImportUri != null -> {
                pendingImport = null; pendingImportUri = null; error = null
            }
            pendingBundledId != null -> { pendingBundledId = null; error = null }
            removeId != null -> { removeId = null; error = null }
            else -> onBack()
        }
    }
    BackHandler { navigateBack() }
    fun applyChange(action: () -> Unit, onSuccess: () -> Unit = {}) {
        if (mutationBusy) return
        mutationBusy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    action()
                    manager.installed()
                }
            }
            mutationBusy = false
            result.onSuccess { updated ->
                installed = updated
                error = null
                onSuccess()
            }.onFailure { failure ->
                error = failure.message?.take(160) ?: "Action failed"
            }
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !previewBusy && !mutationBusy) {
            pendingImport = null
            error = null
            pendingImportUri = uri.toString()
        }
    }
    // A rotation re-parses the same bounded local document, but never installs it.
    LaunchedEffect(pendingImportUri) {
        val selectedUri = pendingImportUri ?: return@LaunchedEffect
        previewBusy = true
        val result = withContext(Dispatchers.IO) {
            runCatching { manager.previewManifest(Uri.parse(selectedUri)) }
        }
        previewBusy = false
        if (pendingImportUri != selectedUri) return@LaunchedEffect
        result.onSuccess { pendingImport = it; error = null }
            .onFailure { failure ->
                error = failure.message?.take(160) ?: "Invalid plugin manifest"
                pendingImportUri = null
            }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = KlyroUiMetrics.sheetGutter, vertical = KlyroUiMetrics.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
            TextButton(
                onClick = ::navigateBack,
                enabled = !mutationBusy,
                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
            ) {
                Icon(painterResource(R.drawable.hugeicons_arrow_left_01),
                    contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Home")
            }
            Spacer(Modifier.weight(1f))
            Text("LOCAL EXTENSIONS", color = colors.muted, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Text("Plugins", fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = colors.foreground)
        Text("Add command packs to your terminal palette. Suggestions are inserted, never run automatically.",
            color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp)

        Row(
            modifier = Modifier.fillMaxWidth()
                .background(colors.key, RoundedCornerShape(KlyroUiMetrics.selectedCorner))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("installed" to "Installed (${installed.size})",
                "explore" to "Explore").forEach { (id, label) ->
                val selected = page == id
                Surface(
                    modifier = Modifier.weight(1f)
                        .heightIn(min = KlyroUiMetrics.minTouch)
                        .clickable(
                            enabled = !mutationBusy, role = Role.Tab,
                            onClickLabel = "Show $label"
                        ) {
                            page = id
                            error = null
                        }
                        .semantics { this.selected = selected },
                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                    color = if (selected) colors.accent else colors.key,
                    contentColor = if (selected) colors.onAccent else colors.foreground
                ) {
                    Text(label, modifier = Modifier.padding(vertical = 13.dp),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        if (mutationBusy) {
            Surface(
                color = colors.key,
                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
            ) {
                Text("Updating local plugins…",
                    modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    color = colors.muted)
            }
        }
        if (error != null) {
            Surface(color = colors.dangerContainer,
                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                Text(error.orEmpty(), color = colors.danger,
                    modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd)
                        .semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
        if (page == "installed") {
            if (installed.isEmpty()) {
                Surface(color = colors.tile,
                    shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(painterResource(R.drawable.hugeicons_grid2x2),
                            contentDescription = null, tint = colors.accent,
                            modifier = Modifier.size(28.dp))
                        Text("Nothing installed yet", fontWeight = FontWeight.SemiBold,
                            color = colors.foreground)
                        Text("Explore curated packs or import a local JSON manifest.",
                            color = colors.muted, fontSize = 13.sp)
                        TextButton(onClick = { page = "explore" }) { Text("Explore plugins") }
                    }
                }
            }
            installed.forEach { plugin ->
                Surface(
                    color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                ) {
                    Column(Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PluginHeading(plugin, "v${plugin.version} · ${plugin.commands.size} commands")
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (plugin.enabled) "Enabled" else "Disabled",
                                color = colors.muted, fontSize = 12.sp)
                            Switch(checked = plugin.enabled,
                                modifier = Modifier.semantics {
                                    contentDescription = (if (plugin.enabled) "Disable " else "Enable ") + plugin.name
                                },
                                onCheckedChange = { checked ->
                                    applyChange({ manager.setEnabled(plugin.id, checked) })
                                },
                                enabled = !mutationBusy)
                            TextButton(
                                onClick = { error = null; removeId = plugin.id },
                                enabled = !mutationBusy,
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                            ) {
                                Text("Remove", color = colors.danger)
                            }
                        }
                    }
                }
            }
        } else {
            KlyroPluginManager.bundled.forEach { plugin ->
                val isInstalled = installed.any { it.id == plugin.id }
                Surface(
                    color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                ) {
                    Column(Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PluginHeading(plugin, "${plugin.commands.size} read-only commands")
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Bundled · Offline", color = colors.muted, fontSize = 12.sp)
                            Button(
                                enabled = !isInstalled && !mutationBusy,
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                onClick = {
                                    error = null
                                    pendingBundledId = plugin.id
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent,
                                    contentColor = colors.onAccent),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            ) { Text(if (isInstalled) "Installed" else "Add") }
                        }
                    }
                }
            }
            Surface(color = colors.chrome,
                shape = RoundedCornerShape(KlyroUiMetrics.cardCorner),
                border = BorderStroke(1.dp, colors.muted.copy(alpha = .2f))) {
                Column(Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Local manifest", color = colors.foreground,
                        fontWeight = FontWeight.SemiBold)
                    Text("Review and confirm a local JSON command pack (up to 16 KiB). Extensions cannot run JavaScript or execute shell commands.",
                        color = colors.muted, fontSize = 12.sp)
                    OutlinedButton(
                        onClick = { importPicker.launch(arrayOf("*/*")) },
                        enabled = !previewBusy && !mutationBusy &&
                            pendingImportUri == null,
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                    ) {
                        Icon(painterResource(R.drawable.hugeicons_file_01),
                            contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(if (previewBusy) "Reading manifest…" else "Import JSON")
                    }
                }
            }
        }
    }

    pendingBundledId?.let { id ->
        val record = KlyroPluginManager.bundled.firstOrNull { it.id == id }
        if (record != null) {
            AlertDialog(
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                onDismissRequest = {
                    if (!mutationBusy) { pendingBundledId = null; error = null }
                },
                containerColor = colors.sheet,
                title = { Text("Add ${record.name}?") },
                text = {
                    Column(
                        Modifier.fillMaxWidth().heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                    ) {
                        Text("${record.commands.size} command suggestions · bundled offline pack",
                            color = colors.muted)
                        Text("Klyro inserts suggestions into Bash; commands are never run automatically.",
                            color = colors.foreground)
                        record.commands.forEach { command ->
                            Text(command.insert, color = colors.muted,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 12.sp)
                        }
                        if (error != null) {
                            Text(error.orEmpty(), color = colors.danger,
                                modifier = Modifier.semantics {
                                    liveRegion = LiveRegionMode.Polite
                                })
                        }
                    }
                },
                confirmButton = {
                    Button(
                        enabled = !mutationBusy,
                        onClick = {
                            applyChange({ manager.installBundled(id) }) {
                                pendingBundledId = null
                                page = "installed"
                            }
                        },
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.onAccent
                        )
                    ) { Text("Add pack") }
                },
                dismissButton = {
                    TextButton(
                        enabled = !mutationBusy,
                        onClick = { pendingBundledId = null; error = null }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    pendingImport?.let { record ->
        AlertDialog(
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            onDismissRequest = {
                if (!mutationBusy) {
                    pendingImport = null; pendingImportUri = null; error = null
                }
            },
            containerColor = colors.sheet,
            title = { Text("Install ${record.name}?") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${record.commands.size} proposed command shortcuts · v${record.version}",
                        color = colors.muted)
                    record.commands.forEach { item ->
                        Text(item.insert, color = colors.foreground,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp)
                    }
                    Text("Commands are inserted only. Review them before pressing Enter.",
                        color = colors.warningInk, fontSize = 12.sp)
                    if (error != null) {
                        Text(error.orEmpty(), color = colors.danger,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
            },
            confirmButton = {
                Button(enabled = !mutationBusy, onClick = {
                    applyChange({ manager.installPreview(record) }) {
                        pendingImport = null
                        pendingImportUri = null
                        page = "installed"
                    }
                }, shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent, contentColor = colors.onAccent
                    )) { Text("Install") }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingImport = null; pendingImportUri = null; error = null },
                    enabled = !mutationBusy,
                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                ) { Text("Cancel") }
            }
        )
    }

    removeId?.let { id ->
        val item = installed.firstOrNull { it.id == id }
        AlertDialog(
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            onDismissRequest = {
                if (!mutationBusy) { removeId = null; error = null }
            },
            containerColor = colors.sheet,
            title = { Text(if (item != null) "Remove ${item.name}?" else "Plugin unavailable") },
            text = {
                Column {
                    Text(if (item != null)
                        "This removes its command suggestions. Terminal sessions and user files are not affected."
                        else "This plugin is no longer installed. Close this dialog.")
                    if (error != null) Text(error.orEmpty(), color = colors.danger,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            },
            confirmButton = {
                TextButton(enabled = item != null && !mutationBusy, onClick = {
                    if (item == null) return@TextButton
                    applyChange({ manager.uninstall(id) }) {
                        removeId = null
                    }
                }) { Text("Remove", color = colors.danger) }
            },
            dismissButton = {
                TextButton(
                    onClick = { removeId = null; error = null },
                    enabled = !mutationBusy,
                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PluginHeading(plugin: KlyroPluginRecord, detail: String) {
    val colors = LocalKlyroColors.current
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(color = colors.key,
            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
            Icon(painterResource(when (plugin.id) {
                "app.klyro.files" -> R.drawable.hugeicons_folder_01
                "app.klyro.git" -> R.drawable.hugeicons_terminal
                "app.klyro.system" -> R.drawable.hugeicons_pulse_rectangle_01
                else -> R.drawable.ic_klyro_plugins
            }),
                contentDescription = null, tint = colors.muted,
                modifier = Modifier.padding(12.dp).size(24.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(plugin.name, fontWeight = FontWeight.SemiBold, color = colors.foreground)
            Text(plugin.description, color = colors.muted, fontSize = 12.sp)
            Text(detail, color = colors.muted, fontSize = 11.sp)
        }
    }
}
