package com.oryn.terminal.ui

import androidx.activity.compose.BackHandler

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
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
    var query by remember { mutableStateOf("") }
    var showingGuide by remember { mutableStateOf(false) }
    BackHandler(enabled = showingGuide) { showingGuide = false }

    var info by remember { mutableStateOf<String?>(null) }
    val matches = remember(query) { KlyroCommandCatalog.search(query) }
    val pluginMatches = remember(query, pluginCommands) {
        pluginCommands.filter { command ->
            KlyroCommandSearch.matches(query, command.label,
                command.description + " " + command.insert)
        }
    }

    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss) {
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
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Find a shell command") },
                        placeholder = { Text("pwd, pkg search, exit…") },
                        singleLine = true,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                TextButton(onClick = { query = ""; info = null }) {
                                    Text("Clear")
                                }
                            }
                        },
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                    )
                    Text(
                        "Insert only; press Enter to run.",
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (info != null) {
                        Surface(color = colors.warningContainer,
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                            Text(
                                info.orEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(12.dp)
                                    .semantics { liveRegion = LiveRegionMode.Polite },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foreground
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
                                    Text("No matching commands.", color = colors.muted)
                                    TextButton(onClick = { query = ""; info = null }) {
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
                                        contentDescription = null, tint = colors.accent,
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
                                                keyboard?.hide()
                                                onDismiss()
                                                onInsert(snippet)
                                            }
                                        } else {
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
                                        contentDescription = null, tint = colors.accent,
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
                                    }
                                    Text(
                                        if (!available) "Info" else if (entry.caution)
                                            "Review" else "Insert",
                                        color = if (entry.caution) colors.warningInk
                                            else colors.accent,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
