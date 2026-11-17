package com.oryn.terminal.ui

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryn.terminal.core.KlyroTextLimit
import com.oryn.terminal.R
import com.oryn.terminal.plugins.KlyroPluginManager
import com.oryn.terminal.plugins.KlyroPluginRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.NonCancellable
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
    var installed by remember { mutableStateOf<List<KlyroPluginRecord>>(emptyList()) }
    var page by rememberSaveable { mutableStateOf("explore") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var removeId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<KlyroPluginRecord?>(null) }
    var pendingImportUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBundledId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmedBundledId by rememberSaveable { mutableStateOf<String?>(null) }
    // These IDs identify a confirmed operation after Activity recreation.
    var confirmedImportId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmedRemovalId by rememberSaveable { mutableStateOf<String?>(null) }

    val confirmedMutationPending = confirmedBundledId != null ||
        confirmedImportId != null || confirmedRemovalId != null

    // A revision cancels stale IO refreshes: an older read cannot overwrite a
    // newer registry snapshot when two rapid writes finish out of order.
    var registryLoading by remember { mutableStateOf(true) }
    var registryReadFailed by remember { mutableStateOf(false) }
    var registryRevision by remember { mutableStateOf(0) }
    DisposableEffect(manager) {
        // Listen first, then refresh to close the registration/read race.
        val stopObserving = manager.observeRegistry {
            scope.launch { registryRevision += 1 }
        }
        scope.launch { registryRevision += 1 }
        onDispose { stopObserving() }
    }
    LaunchedEffect(manager, registryRevision) {
        registryLoading = true
        val result = withContext(Dispatchers.IO) { runCatching { manager.installed() } }
        result.onSuccess {
            installed = it
            if (registryReadFailed) error = null
            registryReadFailed = false
        }.onFailure {
            registryReadFailed = true
            error = it.message?.let { message ->
                KlyroTextLimit.takeCodePoints(message, 160)
            } ?: "Unable to load local plugins"
        }
        registryLoading = false
    }
    // Recover after rotation if the old Activity's confirmed commit never
    // reports completion (e.g. its coroutine failed after destruction).
    // Do not retain an unresponsive disabled dialog indefinitely.
    LaunchedEffect(confirmedBundledId, confirmedImportId, confirmedRemovalId, mutationBusy, registryRevision) {
        if (confirmedMutationPending.not()) return@LaunchedEffect
        if (mutationBusy) return@LaunchedEffect
        delay(10_000L)
        val refreshed = withContext(Dispatchers.IO) { runCatching { manager.installed() } }
            .getOrElse { failure ->
                confirmedBundledId = null
                confirmedImportId = null
                confirmedRemovalId = null
                registryReadFailed = true
                error = failure.message?.let { KlyroTextLimit.takeCodePoints(it, 160) }
                    ?: "Unable to confirm the previous operation. Reload the list before retrying."
                return@LaunchedEffect
            }
        installed = refreshed
        registryReadFailed = false
        val ids = refreshed.map { it.id }.toSet()
        val stuck = (confirmedBundledId?.let { it !in ids } == true) ||
            (confirmedImportId?.let { it !in ids } == true) ||
            (confirmedRemovalId?.let { it in ids } == true)
        if (stuck) {
            confirmedBundledId = null
            confirmedImportId = null
            confirmedRemovalId = null
            error = "The previous operation could not be confirmed. Review the list before retrying."
        }
    }
    LaunchedEffect(installed, registryLoading, registryReadFailed,
        pendingBundledId, confirmedBundledId, confirmedImportId, confirmedRemovalId) {
        if (registryLoading || registryReadFailed) return@LaunchedEffect
        val installedIds = installed.map { it.id }.toSet()
        val bundledId = pendingBundledId
        val confirmedBundle = confirmedBundledId
        val importedId = confirmedImportId
        val removedId = confirmedRemovalId
        if (bundledId != null && bundledId in installedIds) {
            pendingBundledId = null
            confirmedBundledId = null
            page = "installed"
            error = null
        }
        if (confirmedBundle != null && confirmedBundle in installedIds) {
            confirmedBundledId = null
            page = "installed"
        }
        if (importedId != null && importedId in installedIds) {
            pendingImport = null
            pendingImportUri = null
            confirmedImportId = null
            page = "installed"
            error = null
        }
        if (removedId != null && removedId !in installedIds) {
            removeId = null
            confirmedRemovalId = null
            error = null
        }
    }
    fun navigateBack() {
        // Visible Home and Android Back must unwind the same dialog stack.
        if (mutationBusy || confirmedMutationPending) return
        when {
            pendingImport != null || pendingImportUri != null -> {
                pendingImport = null; pendingImportUri = null
                confirmedImportId = null; error = null
            }
            pendingBundledId != null -> {
                pendingBundledId = null; confirmedBundledId = null; error = null
            }
            removeId != null -> { removeId = null; confirmedRemovalId = null; error = null }
            else -> onBack()
        }
    }
    BackHandler { navigateBack() }
    fun applyChange(action: () -> Unit, onSuccess: () -> Unit = {}) {
        if (mutationBusy) return
        mutationBusy = true
        scope.launch {
            // A confirmed registry transaction must finish even if this
            // Composable is destroyed by rotation. Cross-instance writes are locked.
            val result = withContext(Dispatchers.IO + NonCancellable) {
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
                confirmedBundledId = null
                confirmedImportId = null
                confirmedRemovalId = null
                error = failure.message?.let { KlyroTextLimit.takeCodePoints(it, 160) } ?: "Action failed"
            }
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && !registryLoading && !previewBusy && !mutationBusy && !confirmedMutationPending) {
            pendingImport = null
            error = null
            pendingImportUri = uri.toString()
        }
    }
    // A rotation re-parses the same bounded local document, but never installs it.
    LaunchedEffect(pendingImportUri) {
        val selectedUri = pendingImportUri ?: run {
            previewBusy = false
            return@LaunchedEffect
        }
        previewBusy = true
        val result = withContext(Dispatchers.IO) {
            runCatching { manager.previewManifest(Uri.parse(selectedUri)) }
        }
        previewBusy = false
        if (pendingImportUri != selectedUri) return@LaunchedEffect
        result.onSuccess { pendingImport = it; error = null }
            .onFailure { failure ->
                error = failure.message?.let { KlyroTextLimit.takeCodePoints(it, 160) } ?: "Invalid plugin manifest"
                pendingImportUri = null
            }
    }

    val exploreScroll = rememberScrollState()
    val installedScroll = rememberScrollState()
    val pageScroll = if (page == "installed") installedScroll else exploreScroll
    // Keep navigation in view normally, but retain a full-page scroll escape
    // when a short viewport or large text would leave no room for the list.
    val scrollWholePage = LocalConfiguration.current.screenHeightDp < 600 ||
        LocalDensity.current.fontScale >= 1.3f
    Column(
        modifier = Modifier.fillMaxSize()
            .then(if (scrollWholePage) Modifier.verticalScroll(pageScroll) else Modifier)
            .padding(horizontal = KlyroUiMetrics.sheetGutter, vertical = KlyroUiMetrics.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
        ) {
            TextButton(
                onClick = ::navigateBack,
                enabled = !mutationBusy && !confirmedMutationPending,
                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
            ) {
                Icon(painterResource(R.drawable.hugeicons_arrow_left_01),
                    contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Home")
            }
            Text("Plugins", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = colors.foreground,
                modifier = Modifier.weight(1f).semantics { heading() })
        }
        Text("Offline packs · commands run only when you choose.",
            color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp)

        Row(
            modifier = Modifier.fillMaxWidth()
                .selectableGroup()
                .background(colors.key, RoundedCornerShape(KlyroUiMetrics.selectedCorner))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("explore" to "Explore", "installed" to
                if (registryLoading || registryReadFailed) "Installed"
                else "Installed (${installed.size})").forEach { (id, label) ->
                val selected = page == id
                Surface(
                    modifier = Modifier.weight(1f)
                        .heightIn(min = KlyroUiMetrics.minTouch)
                        .clickable(
                            enabled = !mutationBusy && !confirmedMutationPending, role = Role.Tab,
                            onClickLabel = "Show $label"
                        ) {
                            page = id
                            error = null
                        }
                        .semantics { this.selected = selected },
                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                    color = if (selected) colors.sheet else colors.key,
                    border = if (selected) BorderStroke(1.dp, colors.muted.copy(alpha = .45f)) else null,
                    contentColor = colors.foreground
                ) {
                    Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 11.dp),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().then(
                if (scrollWholePage) Modifier else Modifier.weight(1f).verticalScroll(pageScroll)
            ),
            verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
        ) {
            if (mutationBusy || confirmedMutationPending) {
                KlyroInlineNotice("Updating local plugins…")
            }
            if (registryLoading) KlyroInlineNotice("Loading local plugins…")
            if (registryReadFailed) {
                KlyroInlineNotice(error ?: "Unable to load local plugins. Retry to refresh the list.", isError = true)
            } else {
                error?.let { KlyroInlineNotice(it, isError = true) }
            }
            if (registryReadFailed) {
                TextButton(
                    onClick = { registryRevision += 1 },
                    enabled = !registryLoading && !mutationBusy && !confirmedMutationPending,
                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                ) { Text("Retry") }
            }
            if (page == "installed") {
                if (installed.isEmpty() && !registryLoading && !registryReadFailed) {
                    KlyroEmptyState(
                        title = "Nothing installed yet",
                        detail = "Explore curated packs or import a local JSON manifest.",
                        actionLabel = "Explore plugins",
                        onAction = { page = "explore" }
                    )
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
                                modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
                                    .toggleable(
                                        value = plugin.enabled,
                                        enabled = !registryLoading && !mutationBusy && !confirmedMutationPending && !registryReadFailed,
                                        role = Role.Switch,
                                        onValueChange = { checked ->
                                            applyChange({ manager.setEnabled(plugin.id, checked) })
                                        }
                                    ).semantics { contentDescription = plugin.name }) {
                                Text(if (plugin.enabled) "Enabled" else "Disabled",
                                    modifier = Modifier.weight(1f),
                                    color = colors.muted, fontSize = 12.sp)
                                Switch(checked = plugin.enabled,
                                    onCheckedChange = null,
                                    enabled = !registryLoading && !mutationBusy && !confirmedMutationPending && !registryReadFailed)
                            }
                            TextButton(
                                onClick = { error = null; removeId = plugin.id },
                                enabled = !registryLoading && !mutationBusy && !confirmedMutationPending && !registryReadFailed,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colors.danger, disabledContentColor = colors.muted
                                ),
                                modifier = Modifier.align(Alignment.End).heightIn(min = KlyroUiMetrics.minTouch)
                                    .semantics { contentDescription = "Remove ${plugin.name}" }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            } else {
                KlyroPluginManager.bundled.forEach { plugin ->
                    val isInstalled = installed.any { it.id == plugin.id }
                    KlyroExplorePluginCard(
                        plugin = plugin,
                        isInstalled = isInstalled,
                        enabled = !registryLoading && !mutationBusy && !confirmedMutationPending && !registryReadFailed,
                        onAdd = { error = null; pendingBundledId = plugin.id }
                    )
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
                            onClick = {
                                // Common SAF providers expose JSON as application/json,
                                // text/plain or octet-stream. Avoid showing every media file.
                                importPicker.launch(arrayOf(
                                    "application/json", "application/x-json",
                                    "text/plain", "application/octet-stream"
                                ))
                            },
                            enabled = !registryLoading && !previewBusy && !mutationBusy && !confirmedMutationPending && !registryReadFailed &&
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
    }

    pendingBundledId?.let { id ->
        val record = KlyroPluginManager.bundled.firstOrNull { it.id == id }
        if (record != null) {
            AlertDialog(
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                onDismissRequest = {
                    if (!mutationBusy && !confirmedMutationPending) {
                        pendingBundledId = null; error = null
                    }
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
                        enabled = !registryLoading && !mutationBusy && !confirmedMutationPending && !registryReadFailed,
                        onClick = {
                            confirmedBundledId = id
                            applyChange({ manager.installBundled(id) }) {
                                pendingBundledId = null
                                confirmedBundledId = null
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
                        enabled = !registryLoading && !mutationBusy && !confirmedMutationPending,
                        onClick = {
                            pendingBundledId = null; confirmedBundledId = null; error = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { pendingBundledId = null; error = null },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Pack unavailable") },
                text = { Text("This bundled pack is no longer available. Close this dialog.") },
                confirmButton = {
                    TextButton(onClick = { pendingBundledId = null; error = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }

    pendingImport?.let { record ->
        AlertDialog(
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            onDismissRequest = {
                if (!mutationBusy && !confirmedMutationPending) {
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
                    error?.let { KlyroInlineNotice(it, isError = true) }
                }
            },
            confirmButton = {
                Button(enabled = !registryLoading && !mutationBusy && !confirmedMutationPending && !registryReadFailed, onClick = {
                    confirmedImportId = record.id
                    applyChange({ manager.installPreview(record) }) {
                        pendingImport = null
                        pendingImportUri = null
                        confirmedImportId = null
                        page = "installed"
                    }
                }, shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent, contentColor = colors.onAccent
                    )) { Text("Install") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingImport = null; pendingImportUri = null
                        confirmedImportId = null; error = null
                    },
                    enabled = !registryLoading && !mutationBusy && !confirmedMutationPending,
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
                if (!mutationBusy && !confirmedMutationPending) {
                    removeId = null; error = null
                }
            },
            containerColor = colors.sheet,
            title = { Text(if (item != null) "Remove ${item.name}?" else "Plugin unavailable") },
            text = {
                Column {
                    Text(if (item != null)
                        "This removes its command suggestions. Terminal sessions and user files are not affected."
                        else "This plugin is no longer installed. Close this dialog.")
                    error?.let { KlyroInlineNotice(it, isError = true) }
                }
            },
            confirmButton = {
                TextButton(enabled = item != null && !mutationBusy && !confirmedMutationPending && !registryReadFailed,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colors.danger, disabledContentColor = colors.muted
                    ), onClick = {
                    if (item == null) return@TextButton
                    confirmedRemovalId = id
                    applyChange({ manager.uninstall(id) }) {
                        removeId = null
                        confirmedRemovalId = null
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        removeId = null; confirmedRemovalId = null; error = null
                    },
                    enabled = !registryLoading && !mutationBusy && !confirmedMutationPending,
                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                ) { Text("Cancel") }
            }
        )
    }
}

@Composable
internal fun KlyroExplorePluginCard(
    plugin: KlyroPluginRecord,
    isInstalled: Boolean,
    enabled: Boolean,
    onAdd: () -> Unit
) {
    val colors = LocalKlyroColors.current
    Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(12.dp)) {
            val stacked = maxWidth < 320.dp || LocalDensity.current.fontScale >= 1.3f
            val addAction: @Composable () -> Unit = {
                OutlinedButton(
                    enabled = enabled && !isInstalled,
                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch).semantics {
                        contentDescription = if (isInstalled) "${plugin.name} installed"
                        else "Review and add ${plugin.name}"
                    },
                    onClick = onAdd,
                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = colors.foreground, disabledContentColor = colors.muted
                    ),
                    border = BorderStroke(1.dp, colors.muted.copy(
                        alpha = if (enabled && !isInstalled) .55f else .2f
                    ))
                ) {
                    if (isInstalled) {
                        Icon(painterResource(R.drawable.hugeicons_checkmark_circle_01),
                            contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(if (isInstalled) "Installed" else "Add")
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                    PluginHeading(plugin, "${plugin.commands.size} commands")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { addAction() }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PluginHeading(plugin, "${plugin.commands.size} commands", Modifier.weight(1f))
                    addAction()
                }
            }
        }
    }
}

@Composable
private fun PluginHeading(plugin: KlyroPluginRecord, detail: String, modifier: Modifier = Modifier) {
    val colors = LocalKlyroColors.current
    Row(modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(color = colors.key,
            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
            Icon(painterResource(when (plugin.id) {
                "app.klyro.files" -> R.drawable.hugeicons_folder_01
                "app.klyro.git" -> R.drawable.hugeicons_terminal
                "app.klyro.system" -> R.drawable.hugeicons_pulse_rectangle_01
                "app.klyro.git-review" -> R.drawable.hugeicons_clipboard_list
                "app.klyro.storage" -> R.drawable.hugeicons_information_square
                "app.klyro.project" -> R.drawable.hugeicons_grid2x2
                else -> R.drawable.ic_klyro_plugins
            }),
                contentDescription = null, tint = colors.muted,
                modifier = Modifier.padding(10.dp).size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(plugin.name, fontWeight = FontWeight.SemiBold, color = colors.foreground,
                fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.semantics { heading() })
            Text(plugin.description, color = colors.muted, fontSize = 12.sp, lineHeight = 17.sp)
            if (detail.isNotBlank()) {
                Text(detail, color = colors.muted, fontSize = 11.sp)
            }
        }
    }
}
