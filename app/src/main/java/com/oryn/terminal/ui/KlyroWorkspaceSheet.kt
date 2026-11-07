package com.oryn.terminal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog

/**
 * A project is a named grouping, not a process container. Opening another
 * project does not stop, restart, or automatically execute terminal commands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroWorkspaceSheet(
    entries: List<KlyroWorkspaceCatalog.Entry>,
    selectedId: String,
    tabs: List<Pair<Long, String>>,
    tabWorkspaces: Map<Long, String>,
    activeTabId: Long?,
    onSelectProject: (String) -> Unit,
    onCreateProject: (String) -> Boolean,
    onRenameProject: (String, String) -> Boolean,
    onDeleteProject: (String) -> Unit,
    onAssignCurrentTab: (String) -> Unit,
    onSwitchTab: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKlyroColors.current
    var editorId by remember { mutableStateOf<String?>(null) }
    var editorText by remember { mutableStateOf("") }
    var editorError by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    val selected = entries.firstOrNull { it.id == selectedId } ?: entries.first()

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
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Workspaces", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("Project grouping never stops background terminals.",
                    color = colors.muted, style = MaterialTheme.typography.bodySmall)
                entries.forEach { entry ->
                    val number = tabs.count {
                        (tabWorkspaces[it.first] ?: KlyroWorkspaceCatalog.GENERAL) == entry.id
                    }
                    Surface(
                        color = if (entry.id == selected.id) colors.accent.copy(alpha = 0.15f) else colors.tile,
                        shape = RoundedCornerShape(KlyroUiMetrics.cardCorner),
                        modifier = Modifier.fillMaxWidth().clickable { onSelectProject(entry.id) }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                entry.name,
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.foreground,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "$number tabs" +
                                if (entry.id == selected.id) " · Selected" else "",
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (entry.id == selected.id) colors.accent else colors.muted,
                                textAlign = TextAlign.Center
                            )
                            if (entry.id != KlyroWorkspaceCatalog.GENERAL) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    TextButton(onClick = {
                                        editorId = entry.id
                                        editorText = entry.name
                                        editorError = false
                                    }) { Text("Rename") }
                                    TextButton(onClick = { deletingId = entry.id }) {
                                        Text("Delete", color = colors.danger)
                                    }
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = { editorId = ""; editorText = ""; editorError = false },
                    enabled = entries.size < KlyroWorkspaceCatalog.MAX_PROJECTS,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Create project") }

                if (editorId != null) {
                    val editing = !editorId.isNullOrEmpty()
                    Surface(
                        color = colors.tile,
                        shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                if (editing) "Rename project" else "New project",
                                fontWeight = FontWeight.SemiBold,
                                color = colors.foreground
                            )
                            OutlinedTextField(
                                value = editorText,
                                onValueChange = {
                                    editorText = it.take(KlyroWorkspaceCatalog.MAX_NAME_CHARS)
                                    editorError = false
                                },
                                label = { Text("Project name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            )
                            if (editorError) {
                                Text("Choose a unique, non-empty name.",
                                    color = colors.danger,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    val saved = if (editing)
                                        onRenameProject(editorId!!, editorText)
                                    else onCreateProject(editorText)
                                    if (saved) editorId = null else editorError = true
                                }) { Text("Save") }
                                TextButton(onClick = { editorId = null }) { Text("Cancel") }
                            }
                        }
                    }
                }

                Text("Sessions in " + selected.name, fontWeight = FontWeight.SemiBold)
                val projectTabs = tabs.filter {
                    (tabWorkspaces[it.first] ?: KlyroWorkspaceCatalog.GENERAL) == selected.id
                }
                if (projectTabs.isEmpty()) {
                    Text("No live sessions in this project.", color = colors.muted,
                        style = MaterialTheme.typography.bodySmall)
                } else {
                    projectTabs.forEach { (id, name) ->
                        Surface(color = colors.tile, shape = RoundedCornerShape(14.dp)) {
                            Row(Modifier.fillMaxWidth().padding(start = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name.ifBlank { "Terminal" },
                                    modifier = Modifier.weight(1f).padding(top = 14.dp), maxLines = 1)
                                TextButton(onClick = { onSwitchTab(id); onDismiss() }) {
                                    Text(if (id == activeTabId) "Current" else "Switch")
                                }
                            }
                        }
                    }
                }
                if (activeTabId != null &&
                    (tabWorkspaces[activeTabId] ?: KlyroWorkspaceCatalog.GENERAL) != selected.id
                ) {
                    TextButton(onClick = { onAssignCurrentTab(selected.id) },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Move current tab to " + selected.name)
                    }
                }
            }
        }

        deletingId?.let { id ->
            val name = entries.firstOrNull { it.id == id }?.name ?: "project"
            AlertDialog(
                onDismissRequest = { deletingId = null },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Delete $name?") },
                text = { Text("Its tabs return to General. No shell or file will be deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        onDeleteProject(id)
                        deletingId = null
                    }) { Text("Delete project", color = colors.danger) }
                },
                dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Cancel") } }
            )
        }
    }
}
