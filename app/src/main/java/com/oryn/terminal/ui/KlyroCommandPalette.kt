package com.oryn.terminal.ui

import android.content.Context

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.stateDescription
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.oryn.terminal.R
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oryn.terminal.termux.KlyroWelcomeGuide
import com.oryn.terminal.core.KlyroTextLimit
import com.oryn.terminal.plugins.KlyroPluginManager
import com.oryn.terminal.plugins.KlyroPluginCommand
import java.io.File

/**
 * Command discovery, not a second Settings/tools menu. Selecting a command
 * inserts literal text into Bash without Enter; Klyro never auto-executes it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun KlyroCommandPalette(
    installedPrefix: File?,
    pluginCommands: List<KlyroPluginCommand>,
    onInsert: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    val searchFocus = remember { FocusRequester() }
    val resultListState = rememberLazyListState()
    val guideScrollState = rememberScrollState()
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("klyro_command_quick_access", Context.MODE_PRIVATE)
    }
    var pinnedIds: List<String> by remember(context) {
        mutableStateOf(KlyroCommandQuickAccess.decode(
            prefs.getString("pinned", null), KlyroCommandQuickAccess.MAX_PINNED
        ))
    }
    var recentIds: List<String> by remember(context) {
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
    var searchFocused by rememberSaveable { mutableStateOf(false) }
    var info by rememberSaveable { mutableStateOf<String?>(null) }
    var feedbackRevision by rememberSaveable { mutableStateOf(0) }
    var unsafeFeedback by rememberSaveable { mutableStateOf(false) }
    var pendingCautionCommand by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingCautionId by rememberSaveable { mutableStateOf<String?>(null) }
    var pinnedOnly by rememberSaveable { mutableStateOf(false) }
    var availableOnly by rememberSaveable { mutableStateOf(false) }
    var confirmClearRecent by rememberSaveable { mutableStateOf(false) }
    var insertionAccepted by remember { mutableStateOf(false) }
    fun insertOnce(snippet: String): Boolean {
        if (insertionAccepted) return false
        val accepted = onInsert(snippet)
        if (accepted) insertionAccepted = true
        return accepted
    }
    fun insertCurated(id: String, snippet: String) {
        if (insertOnce(snippet)) {
            recordInsertion(id)
            keyboard?.hide()
            onDismiss()
        } else {
            unsafeFeedback = true
            feedbackRevision++
            info = "Not inserted: terminal unavailable or input buffer full. Return to the Bash prompt and retry."
        }
    }
    fun togglePin(id: String) {
        val next = KlyroCommandQuickAccess.togglePinned(pinnedIds, id)
        if (next == pinnedIds && id !in pinnedIds) {
            unsafeFeedback = false
            feedbackRevision++
            info = "Up to ${KlyroCommandQuickAccess.MAX_PINNED} pinned commands. Unpin one to make room."
            return
        }
        pinnedIds = next
        prefs.edit().putString("pinned",
            KlyroCommandQuickAccess.encode(next, KlyroCommandQuickAccess.MAX_PINNED)).apply()
        unsafeFeedback = false
        feedbackRevision++
        info = if (id in next) "Command pinned." else "Command unpinned."
    }
    val matches = remember(query, pinnedIds, recentIds, pinnedOnly, availableOnly, installedPrefix) {
        val ranked = KlyroCommandQuickAccess.rank(
            KlyroCommandCatalog.search(query), pinnedIds, recentIds
        )
        val selected = if (pinnedOnly) ranked.filter { it.command in pinnedIds } else ranked
        if (availableOnly) KlyroCommandQuickAccess.available(selected, installedPrefix) else selected
    }
    // Plugin templates have no verified executable/dependency status.
    // Do not include them when the user explicitly requests Available.
    val pluginMatches = remember(query, pluginCommands, pinnedOnly, availableOnly) {
        if (pinnedOnly || availableOnly) emptyList() else pluginCommands.filter { command ->
            KlyroCommandSearch.matches(query, command.label,
                command.description + " " + command.insert)
        }.distinctBy { it.label to it.insert }
    }

    var lastShownRevision by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(feedbackRevision) {
        if (info != null && feedbackRevision != lastShownRevision) resultListState.animateScrollToItem(0)
        lastShownRevision = feedbackRevision
    }
    val resultCount = matches.size + pluginMatches.size

    KlyroModalTheme {
        KlyroSheet(onDismissRequest = { keyboard?.hide(); onDismiss() },
            dismissEnabled = pendingCautionCommand == null && !confirmClearRecent) {
            Column(
                modifier = Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth()
                    .heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .imePadding()
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
                    TextButton(onClick = { keyboard?.hide(); onDismiss() }) { Text("Close") }
                }
                TextButton(onClick = {
                        keyboard?.hide()
                        showingGuide = !showingGuide
                        if (!showingGuide) searchFocused = false
                        info = null
                        unsafeFeedback = false
                    }) {
                        Text(if (showingGuide) "Back to commands" else "Welcome guide")
                    }

                if (showingGuide) {
                    // One in-sheet help page; scroll independently on small phones.
                    Column(Modifier.fillMaxWidth().heightIn(max = KlyroUiMetrics.paletteListMax)
                        .weight(1f, fill = false)
                        .verticalScroll(guideScrollState)) {
                        SelectionContainer { Text(KlyroWelcomeGuide.MESSAGE,
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foreground) }
                    }
                } else {
                    LazyColumn(
                        state = resultListState,
                        modifier = Modifier.fillMaxWidth()
                            .weight(1f, fill = false)
                            .heightIn(max = KlyroUiMetrics.paletteListMax),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (info != null) {
                            item(key = "feedback") {
                                Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                                    KlyroInlineNotice(info.orEmpty(), isError = unsafeFeedback)
                                }
                            }
                        }
                        item(key = "search-controls") {
                            LaunchedEffect(Unit) {
                                if (!searchFocused) {
                                    searchFocus.requestFocus()
                                    searchFocused = true
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = {
                                        query = KlyroTextLimit.takeCodePoints(it, KlyroCommandSearch.MAX_QUERY_LENGTH)
                                        info = null
                                        unsafeFeedback = false
                                    },
                                    modifier = Modifier.fillMaxWidth().focusRequester(searchFocus),
                                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                                    label = { Text("Find a shell command") },
                                    placeholder = { Text("pwd, pkg search, exit…") },
                                    singleLine = true,
                                    leadingIcon = { Icon(painterResource(R.drawable.hugeicons_search_01),
                                        contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        if (query.isNotEmpty()) {
                                            IconButton(onClick = {
                                                query = ""; info = null; unsafeFeedback = false
                                                searchFocus.requestFocus()
                                                keyboard?.show()
                                            }) {
                                                Icon(painterResource(R.drawable.hugeicons_cancel_circle),
                                                    contentDescription = "Clear search", modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                )
                                Text(
                                    "Inserts into the terminal. Press Enter there to run.",
                                    color = colors.muted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilterChip(
                                        selected = pinnedOnly,
                                        onClick = { pinnedOnly = !pinnedOnly; info = null; unsafeFeedback = false },
                                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                        label = {
                                            Text("Pinned (${pinnedIds.size})")
                                        },
                                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = colors.tile,
                                            labelColor = colors.foreground,
                                            selectedContainerColor = colors.accent.copy(alpha = .17f),
                                            selectedLabelColor = colors.foreground
                                        )
                                    )
                                    FilterChip(
                                        selected = availableOnly,
                                        onClick = { availableOnly = !availableOnly; info = null; unsafeFeedback = false },
                                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch),
                                        label = {
                                            Text("Available only")
                                        },
                                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = colors.tile,
                                            labelColor = colors.foreground,
                                            selectedContainerColor = colors.accent.copy(alpha = .17f),
                                            selectedLabelColor = colors.foreground
                                        )
                                    )
                                }

                            }
                        }
                        item(key = "result-summary") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (availableOnly) {
                                    KlyroInlineNotice(
                                        "Plugins are hidden: their dependencies are not verified."
                                    )
                                }
                                Text(
                                    if (resultCount == 1) "1 command" else "$resultCount commands",
                                    color = colors.muted,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                                )
                                if (recentIds.isNotEmpty() && !pinnedOnly) {
                                    FlowRow(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Recently inserted · ${recentIds.size}",
                                            color = colors.muted, style = MaterialTheme.typography.labelSmall)
                                        TextButton(onClick = { confirmClearRecent = true },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = colors.foreground
                                            )) { Text("Clear recent") }
                                    }
                                }

                            }
                        }
                        if (matches.isEmpty() && pluginMatches.isEmpty()) {
                            item {
                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                    Text(
                                        if (pinnedOnly && pinnedIds.isEmpty()) "No pinned commands yet."
                                        else if (pinnedOnly) "No pinned commands match."
                                        else if (availableOnly) "No available commands match."
                                        else "No matching commands.",
                                        color = colors.muted
                                    )
                                    TextButton(onClick = {
                                        query = ""; pinnedOnly = false; availableOnly = false
                                        feedbackRevision++
                                        info = null; unsafeFeedback = false
                                    }) {
                                        Text("Show all commands")
                                    }
                                }
                            }
                        }
                        if (pluginMatches.isNotEmpty()) {
                            item {
                                Text("Plugin commands", color = colors.muted,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() })
                            }
                        }
                        items(pluginMatches, key = { "plugin:${it.label.length}:${it.label}:${it.insert}" }) { command ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(
                                    role = Role.Button,
                                    onClickLabel = "Insert " + command.label,
                                    onClick = {
                                        if (KlyroPluginManager.safeInsertion(command.insert)) {
                                            if (insertOnce(command.insert)) {
                                                keyboard?.hide()
                                                onDismiss()
                                            } else {
                                                unsafeFeedback = true
                                                feedbackRevision++
                                                info = "Not inserted: terminal unavailable or input buffer full. Return to the Bash prompt and retry."
                                            }
                                        } else {
                                            unsafeFeedback = true
                                            feedbackRevision++
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
                                        if (command.description.isNotBlank()) Text(command.description,
                                            color = colors.muted, style = MaterialTheme.typography.bodySmall)
                                        Text(command.insert, color = colors.muted,
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Insert", color = colors.accent,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        if (matches.isNotEmpty()) {
                            item(key = "catalog-heading") {
                                Text("Shell commands", color = colors.muted,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp).semantics { heading() })
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
                                                    insertCurated(entry.command, snippet)
                                                }
                                            } else {
                                                unsafeFeedback = true
                                                feedbackRevision++
                                                info = "This command cannot be inserted safely."
                                            }
                                        } else {
                                            unsafeFeedback = false
                                            feedbackRevision++
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
                                            overflow = TextOverflow.Ellipsis,
                                            maxLines = 3
                                        )
                                        if (entry.insertion.trim() != entry.command) {
                                            Text(entry.insertion, fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.bodySmall, color = colors.muted)
                                        }
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
                                            if (!available) "Details" else if (entry.caution)
                                                "Review" else "Insert",
                                            color = if (!available) colors.muted else if (entry.caution) colors.warningInk
                                                else colors.accent,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        TextButton(
                                            onClick = { togglePin(entry.command) },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = colors.foreground
                                            ),
                                            modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                                                .semantics {
                                                    stateDescription = if (entry.command in pinnedIds) "Pinned" else "Not pinned"
                                                    contentDescription = if (entry.command in pinnedIds)
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
                    Column(Modifier.heightIn(max = KlyroUiMetrics.paletteListMax)
                        .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(KlyroCommandCatalog.search("").firstOrNull { it.command == pendingCautionId }?.purpose
                            ?: "Review the command before inserting it.")
                        Text("This command can change your environment. " +
                            "It will be inserted into the terminal, not run automatically.",
                            color = colors.foreground)
                        Surface(color = colors.key,
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                            SelectionContainer { Text(command, fontFamily = FontFamily.Monospace,
                                color = colors.foreground,
                                modifier = Modifier.fillMaxWidth().padding(12.dp)) }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = pendingCautionId
                            pendingCautionCommand = null
                            pendingCautionId = null
                            val entry = KlyroCommandCatalog.search("").firstOrNull { it.command == id }
                            if (entry != null && KlyroCommandCatalog.isInstalled(entry, installedPrefix)) {
                                insertCurated(entry.command, KlyroCommandCatalog.safeInsertion(entry))
                            } else {
                                unsafeFeedback = true
                                feedbackRevision++
                                info = "Command no longer available. Check its requirements and retry."
                            }
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
                        "Bash history and pinned commands stay unchanged.",
                        modifier = Modifier.heightIn(max = KlyroUiMetrics.paletteListMax)
                            .verticalScroll(rememberScrollState()))
                },
                confirmButton = {
                    TextButton(onClick = {
                        recentIds = emptyList()
                        prefs.edit().remove("recent_inserts").apply()
                        confirmClearRecent = false
                        unsafeFeedback = false
                        feedbackRevision++
                        info = "Recent insertions cleared."
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
