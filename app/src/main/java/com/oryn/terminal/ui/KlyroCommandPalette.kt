package com.oryn.terminal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oryn.terminal.termux.KlyroWelcomeGuide

/**
 * Command discovery, not a second Settings/tools menu. Selecting a command
 * inserts literal text into Bash without Enter; Klyro never auto-executes it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroCommandPalette(
    onInsert: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var showingGuide by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<String?>(null) }
    val matches = remember(query) { KlyroCommandCatalog.search(query) }

    KlyroModalTheme {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = colors.sheet,
            contentColor = colors.foreground,
            shape = RoundedCornerShape(
                topStart = KlyroUiMetrics.sheetCorner,
                topEnd = KlyroUiMetrics.sheetCorner
            ),
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.muted) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (showingGuide) "Welcome to Klyro" else "Command Palette",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.foreground
                    )
                    TextButton(onClick = {
                        keyboard?.hide()
                        showingGuide = !showingGuide
                        info = null
                    }) {
                        Text(if (showingGuide) "Commands" else "Welcome guide")
                    }
                }

                if (showingGuide) {
                    // Reopen the exact onboarding text, without creating a
                    // second full-screen preview or sending shell input.
                    Surface(
                        color = colors.tile,
                        shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                    ) {
                        Text(
                            KlyroWelcomeGuide.MESSAGE,
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.foreground
                        )
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
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                    )
                    Text(
                        "Tap a command to insert it without running it.",
                        color = colors.muted,
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (info != null) {
                        Surface(color = colors.warningContainer,
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                            Text(
                                info.orEmpty(),
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.foreground
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (matches.isEmpty()) {
                            item {
                                Text("No matching commands.", color = colors.muted,
                                    modifier = Modifier.padding(12.dp))
                            }
                        }
                        items(matches, key = { it.command }) { entry ->
                            val available = entry.insertable
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(
                                    role = Role.Button,
                                    onClickLabel = if (available) "Insert " + entry.command
                                        else "Read storage instructions",
                                    onClick = {
                                        if (available) {
                                            val snippet = KlyroCommandCatalog.safeInsertion(entry)
                                            if (snippet.isNotEmpty()) {
                                                keyboard?.hide()
                                                onDismiss()
                                                onInsert(snippet)
                                            }
                                        } else {
                                            info = entry.note
                                        }
                                    }
                                ),
                                color = colors.tile,
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(entry.command,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accent,
                                            modifier = Modifier.weight(1f))
                                        Text(
                                            if (!available) "Info" else if (entry.caution) "Review"
                                            else "Insert",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (entry.caution) colors.warningInk else colors.muted
                                        )
                                    }
                                    Text(entry.purpose,
                                        color = colors.foreground,
                                        style = MaterialTheme.typography.bodySmall)
                                    Text(entry.category + " · " + entry.note,
                                        color = colors.muted,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
