package com.oryn.terminal.ui

import androidx.activity.compose.BackHandler
import android.content.Context

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.oryn.terminal.R
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oryn.terminal.termux.KlyroWelcomeGuide
import com.oryn.terminal.plugins.KlyroPluginManager
import com.oryn.terminal.plugins.KlyroPluginCommand
import java.io.File

/**
 * Command discovery, not a second Settings/tools menu. Selecting a command
 * inserts literal text into Bash without Enter; Klyro never auto-executes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroCommandPalette(
    installedPrefix: File?,
    pluginCommands: List<KlyroPluginCommand>,
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("klyro_command_quick_access", Context.MODE_PRIVATE)
    }
    var pinnedIds by remember(context) {
        mutableStateOf(KlyroCommandQuickAccess.decode(
            prefs.getString("pinned", null), KlyroCommandQuickAccess.MAX_PINNED
        ))
    }
    var recentIds by remember(context) {
        mutableStateOf(KlyroCommandQuickAccess.decode(
            prefs.getString("recent_inserts", null), KlyroCommandQuickAccess.MAX_RECENT
        ))
    }
    fun recordInsertion(id: String) {
        recentIds = KlyroCommandQuickAccess.recordInsertion(recentIds, id)
        // Store curated identifiers only, never raw Bash or plugin input.
        prefs.edit().putString("recent_inserts",
            KlyroCommandQuickAccess.encode(recentIds, KlyroCommandQuickAccess.MAX_RECENT)).apply()
    }
    var query by rememberSaveable { mutableStateOf("") }
    var showingGuide by rememberSaveable { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    var unsafeFeedback by remember { mutableStateOf(false) }
    var pendingCautionCommand by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCautionId by rememberSaveable { mutableStateOf<String?>(null) }
    var pinnedOnly by rememberSaveable { mutableStateOf(false) }
    var availableOnly by rememberSaveable { mutableStateOf(false) }
    var confirmClearRecent by rememberSaveable { mutableStateOf(false) }
    fun togglePin(id: String) {
        val next = KlyroCommandQuickAccess.togglePinned(pinnedIds, id)
        if (next == pinnedIds && id !in pinnedIds) {
            unsafeFeedback = false
            info = "Up to 8 pinned commands. Unpin one to make room."
            return
        }
        pinnedIds = next
        prefs.edit().putString("pinned",
            KlyroCommandQuickAccess.encode(next, KlyroCommandQuickAccess.MAX_PINNED)).apply()
        info = null
    }
    BackHandler(enabled = showingGuide) { showingGuide = false }
    BackHandler(enabled = pendingCautionCommand != null) {
        pendingCautionCommand = null; pendingCautionId = null
    }
    BackHandler(enabled = confirmClearRecent) { confirmClearRecent = false }
    val matches = remember(query, pinnedIds, recentIds, pinnedOnly, availableOnly, installedPrefix) {
        val ranked = KlyroCommandQuickAccess.rank(
            KlyroCommandCatalog.search(query), pinnedIds, recentIds
        )
        val selected = if (pinnedOnly) ranked.filter { it.command in pinnedIds } else ranked
        if (availableOnly) KlyroCommandQuickAccess.available(selected, installedPrefix) else selected
    }
    val pluginMatches = remember(query, pluginCommands, pinnedOnly) {
        if (pinnedOnly) emptyList() else pluginCommands.filter { command ->
            KlyroCommandSearch.matches(query, command.label,
                command.description + " " + command.insert)
        }
    }

    KlyroModalTheme {
        KlyroSheet(onDismissRequest = {
            when {
                pendingCautionCommand != null -> {
                    pendingCautionCommand = null; pendingCautionId = null
                }
                confirmClearRecent -> confirmClearRecent = false
                showingGuide -> showingGuide = false
                else -> onDismiss()
            }
        }) {
            Column(
                modifier = Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth().imePadding()
                    .padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = KlyroUiMetrics.spaceXl),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KlyroSheetTitle(if (showingGuide) "Welcome to Klyro" else "Command Palette",
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = {
                        keyboard?.hide()
                        showingGuide = !showingGuide
                        info = null
                        unsafeFeedback = false
                    }) {
                        Text(if (showingGuide) "Commands" else "Welcome guide")
                    }
                }

                if (showingGuide) {
                    // One in-sheet help page; scroll independently on small phones.
                    Column(Modifier.fillMaxWidth().heightIn(max = KlyroUiMetrics.paletteListMax)
                        .verticalScroll(rememberScrollState())) {
                        Text(KlyroWelcomeGuide.MESSAGE,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foreground)
                    }
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = it.take(KlyroCommandSearch.MAX_QUERY_LENGTH)
                            info = null
                            unsafeFeedback = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Find a shell command") },
                        placeholder = { Text("pwd, pkg search, exit…") },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                TextButton(onClick = { query = ""; info = null; unsafeFeedback = false }) {
                                    Text("Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                    )
                    Text(
                        "Insert only; press Enter to run. Recent items are not Bash history.",
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { pinnedOnly = !pinnedOnly },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (pinnedOnly) colors.accent else colors.foreground
                            ),
                            modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                                .semantics { contentDescription = if (pinnedOnly)
                                    "Show all commands" else "Show pinned commands only" }
                        ) {
                            Text(if (pinnedOnly) "Pinned only ✓" else "Pinned ${pinnedIds.size}/8")
                        }
                        TextButton(
                            onClick = { availableOnly = !availableOnly },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (availableOnly) colors.accent else colors.foreground
                            ),
                            modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                                .semantics { contentDescription = if (availableOnly)
                                    "Include unavailable commands" else "Show available commands only" }
                        ) {
                            Text(if (availableOnly) "Available ✓" else "All commands")
                        }
                    }
                    if (recentIds.isNotEmpty() && !pinnedOnly) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Recently inserted · ${recentIds.size}",
                                color = colors.muted, style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = { confirmClearRecent = true },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = colors.foreground
                                )) { Text("Clear recent") }
                        }
                    }
                    if (info != null) {
                        Surface(color = if (unsafeFeedback) colors.warningContainer else colors.key,
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                            Text(
                                info.orEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(12.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (unsafeFeedback) colors.warningInk else colors.foreground
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = KlyroUiMetrics.paletteListMax),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (matches.isEmpty() && pluginMatches.isEmpty()) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                    Text(
                                        if (pinnedOnly && pinnedIds.isEmpty()) "No pinned commands yet."
                                        else if (availableOnly) "No available commands match."
                                        else "No matching commands.",
                                        color = colors.muted
                                    )
                                    TextButton(onClick = { query = ""; info = null; unsafeFeedback = false }) {
                                        Text("Show all commands")
                                    }
                                }
                            }
                        }
                        if (pluginMatches.isNotEmpty()) {
                            item {
                                Text("Installed plugins", color = colors.muted,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp))
                            }
                        }
                        items(pluginMatches, key = { "plugin:" + it.label + ":" + it.insert }) { command ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(
                                    role = Role.Button,
                                    onClickLabel = "Insert " + command.label,
                                    onClick = {
                                        if (KlyroPluginManager.safeInsertion(command.insert)) {
                                            keyboard?.hide()
                                            onDismiss()
                                            onInsert(command.insert)
                                        } else {
                                            unsafeFeedback = true
                                            info = "This plugin command cannot be inserted safely."
                                        }
                                    }
                                ),
                                color = colors.tile,
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().heightIn(
                                        min = KlyroUiMetrics.minTouch
                                    ).padding(KlyroUiMetrics.spaceMd),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                                ) {
                                    Icon(painterResource(R.drawable.hugeicons_grid2x2),
                                        contentDescription = null, tint = colors.muted,
                                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                    Column(Modifier.weight(1f)) {
                                        Text(command.label, fontWeight = FontWeight.SemiBold,
                                            color = colors.foreground)
                                        Text(command.insert, color = colors.muted,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Insert", color = colors.accent,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        items(matches, key = { it.command }) { entry ->
                            val available = KlyroCommandCatalog.isInstalled(entry, installedPrefix)
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(
                                    role = Role.Button,
                                    onClickLabel = if (available) "Insert " + entry.command
                                        else "Show command requirements",
                                    onClick = {
                                        if (available) {
                                            val snippet = KlyroCommandCatalog.safeInsertion(entry)
                                            if (snippet.isNotEmpty()) {
                                                if (entry.caution) {
                                                    keyboard?.hide()
                                                    pendingCautionId = entry.command
                                                    pendingCautionCommand = snippet
                                                } else {
                                                    keyboard?.hide()
                                                    recordInsertion(entry.command)
                                                    onDismiss()
                                                    onInsert(snippet)
                                                }
                                            }
                                        } else {
                                            unsafeFeedback = false
                                            info = if (!entry.insertable) entry.note else
                                                "Command not installed in Klyro's private prefix. " + entry.note
                                        }
                                    }
                                ),
                                color = colors.tile,
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .heightIn(min = KlyroUiMetrics.minTouch)
                                        .padding(horizontal = KlyroUiMetrics.spaceMd,
                                            vertical = KlyroUiMetrics.spaceSm),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(
                                        KlyroUiMetrics.spaceMd)
                                ) {
                                    Icon(painterResource(R.drawable.hugeicons_terminal),
                                        contentDescription = null, tint = colors.muted,
                                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                    Column(Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(
                                            KlyroUiMetrics.spaceXs)) {
                                        Text(entry.command, color = colors.foreground,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            entry.purpose,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.muted,
                                            maxLines = 2
                                        )
                                        if (entry.command in pinnedIds || entry.command in recentIds) {
                                            Text(
                                                if (entry.command in pinnedIds) "Pinned"
                                                else "Recently inserted",
                                                color = colors.muted,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            if (!available) "Info" else if (entry.caution)
                                                "Review" else "Insert",
                                            color = if (entry.caution) colors.warningInk
                                                else colors.accent,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        TextButton(
                                            onClick = { togglePin(entry.command) },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = colors.foreground
                                            ),
                                            modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                                                .semantics { contentDescription = if (entry.command in pinnedIds)
                                                    "Unpin ${entry.command}" else "Pin ${entry.command}" }
                                        ) {
                                            Text(if (entry.command in pinnedIds) "Unpin" else "Pin")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        pendingCautionCommand?.let { command ->
            AlertDialog(
                onDismissRequest = {
                    pendingCautionCommand = null
                    pendingCautionId = null
                },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Review command") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("This command may change installed packages or files. " +
                            "It will be inserted into the terminal, not run automatically.",
                            color = colors.foreground)
                        Surface(color = colors.key,
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                            Text(command, fontFamily = FontFamily.Monospace,
                                color = colors.foreground,
                                modifier = Modifier.fillMaxWidth().padding(12.dp))
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = pendingCautionId
                            pendingCautionCommand = null
                            pendingCautionId = null
                            if (id != null) recordInsertion(id)
                            onDismiss()
                            onInsert(command)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent, contentColor = colors.onAccent
                        )
                    ) { Text("Insert command") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        pendingCautionCommand = null
                        pendingCautionId = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
        if (confirmClearRecent) {
            AlertDialog(
                onDismissRequest = { confirmClearRecent = false },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Clear recent insertions?") },
                text = {
                    Text("Only this quick-access list is cleared. Running sessions, " +
                        "Bash history and pinned commands stay unchanged.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        recentIds = emptyList()
                        prefs.edit().remove("recent_inserts").apply()
                        confirmClearRecent = false
                    }) { Text("Clear recent") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClearRecent = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
