package com.oryn.terminal.ui

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oryn.terminal.R
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog

/** A workspace groups live PTYs but does not claim to restore dead processes. */
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
    val c = LocalKlyroColors.current
    var editorId by remember { mutableStateOf<String?>(null) }
    var editorText by remember { mutableStateOf("") }
    var editorError by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<String?>(null) }
    var menuId by remember { mutableStateOf<String?>(null) }
    // Treat inline editing as a navigation layer, not as a reason to close the sheet.
    BackHandler(enabled = editorId != null && deletingId == null && menuId == null) {
        editorId = null
        editorError = false
    }
    val selected = entries.firstOrNull { it.id == selectedId } ?: entries.firstOrNull()
    if (selected == null) {
        KlyroModalTheme {
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = c.sheet,
                title = { Text("Workspaces unavailable") },
                text = { Text("The workspace list is empty. Reopen this page after the workspace catalog loads.") },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            )
        }
        return
    }
    val projectTabs = tabs.filter {
        (tabWorkspaces[it.first] ?: KlyroWorkspaceCatalog.GENERAL) == selected.id
    }
    // Avoid scrolling short sheets; preserve reachability for long session lists and accessibility scaling.
    // A short combined list fits at once; count workspaces and visible session rows together.
    val needsScroll = KlyroSheetLayoutPolicy.needsScroll(
        rows = entries.size + projectTabs.size,
        screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp,
        fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale,
        editing = editorId != null
    )
    fun editProject(id: String, title: String) {
        editorId = id
        editorText = title
        editorError = false
        menuId = null
    }

    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss) {
            Column(
                Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth().heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .then(if (needsScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .imePadding()
                    .padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = KlyroUiMetrics.spaceXl),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    KlyroSheetTitle("Workspaces", modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { editProject("", "") },
                        enabled = entries.size < KlyroWorkspaceCatalog.MAX_PROJECTS
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)) {
                            val tint = if (entries.size < KlyroWorkspaceCatalog.MAX_PROJECTS)
                                c.accent else c.muted
                            Icon(painterResource(R.drawable.hugeicons_plus_sign),
                                contentDescription = null, tint = tint,
                                modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                            Text("New", color = tint)
                        }
                    }
                }
                entries.forEach { entry ->
                    val count = tabs.count {
                        (tabWorkspaces[it.first] ?: KlyroWorkspaceCatalog.GENERAL) == entry.id
                    }
                    val active = entry.id == selected.id
                    val cardColor = if (active) c.accent.copy(alpha = .11f) else c.tile
                    Surface(
                        color = cardColor,
                        border = if (active) BorderStroke(KlyroUiMetrics.selectedBorder,
                            c.accent.copy(alpha = .68f)) else null,
                        shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = KlyroUiMetrics.workspaceRow),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f)
                                    .heightIn(min = KlyroUiMetrics.workspaceRow)
                                    .clickable(role = Role.RadioButton,
                                        onClickLabel = "Select workspace " + entry.name) {
                                        onSelectProject(entry.id)
                                    }.semantics { this.selected = active },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(KlyroUiMetrics.minTouch),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painterResource(R.drawable.hugeicons_folder_01),
                                        contentDescription = null,
                                        tint = if (active) c.accent else c.muted,
                                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        entry.name, modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = c.foreground,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (count == 1) "1 session" else count.toString() + " sessions",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) c.accent else c.muted
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.size(KlyroUiMetrics.minTouch),
                                contentAlignment = Alignment.Center
                            ) {
                                if (entry.id != KlyroWorkspaceCatalog.GENERAL) {
                                    TextButton(onClick = { menuId = entry.id },
                                        modifier = Modifier.size(KlyroUiMetrics.minTouch)
                                            .semantics {
                                                contentDescription = "Options for " + entry.name
                                            },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Icon(painterResource(R.drawable.hugeicons_more_vertical),
                                            contentDescription = null, tint = c.foreground,
                                            modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
                                    }
                                    DropdownMenu(
                                        expanded = menuId == entry.id,
                                        onDismissRequest = { menuId = null },
                                        containerColor = c.sheet,
                                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                    ) {
                                        KlyroMenuItem("Rename", R.drawable.hugeicons_pen_01) {
                                            editProject(entry.id, entry.name)
                                        }
                                        KlyroMenuItem(
                                            "Delete", R.drawable.hugeicons_trash,
                                            onClick = { menuId = null; deletingId = entry.id },
                                            destructive = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (editorId != null) {
                    val editing = !editorId.isNullOrEmpty()
                    Surface(color = c.tile,
                        shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                        Column(
                            Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd),
                            verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                        ) {
                            Text(if (editing) "Rename workspace" else "New workspace",
                                fontWeight = FontWeight.SemiBold, color = c.foreground)
                            OutlinedTextField(
                                value = editorText,
                                onValueChange = {
                                    editorText = it.take(KlyroWorkspaceCatalog.MAX_NAME_CHARS)
                                    editorError = false
                                },
                                label = { Text("Workspace name") },
                                singleLine = true,
                                isError = editorError,
                                supportingText = {
                                    Text("${KlyroWorkspaceCatalog.MAX_NAME_CHARS - editorText.length} characters remaining")
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            )
                            if (editorError) Text("Choose a unique, non-empty name.",
                                color = c.danger, style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { editorId = null }) { Text("Cancel") }
                                Button(
                                    enabled = editorText.trim().isNotEmpty() &&
                                        (!editing || editorText.trim() !=
                                            entries.firstOrNull { it.id == editorId }?.name),
                                    onClick = {
                                        val saved = if (editing) onRenameProject(editorId!!, editorText)
                                            else onCreateProject(editorText)
                                        if (saved) editorId = null else editorError = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = c.accent, contentColor = c.onAccent
                                    ), shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                ) { Text("Save") }
                            }
                        }
                    }
                }

                Text(
                    "Sessions",
                    fontWeight = FontWeight.SemiBold,
                    color = c.foreground,
                    style = MaterialTheme.typography.titleSmall
                )
                if (projectTabs.isEmpty()) {
                    Text("No sessions yet.",
                        modifier = Modifier.padding(vertical = KlyroUiMetrics.spaceSm),
                        color = c.muted, style = MaterialTheme.typography.bodySmall)
                } else {
                    projectTabs.forEach { (id, name) ->
                        val current = id == activeTabId
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
                                .clickable(
                                    enabled = !current,
                                    role = Role.Button,
                                    onClickLabel = "Switch to " + name
                                ) {
                                    onSwitchTab(id)
                                    onDismiss()
                                },
                            color = c.tile,
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = KlyroUiMetrics.spaceMd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                            ) {
                                Icon(
                                    painterResource(R.drawable.hugeicons_terminal),
                                    contentDescription = null,
                                    tint = if (current) c.accent else c.muted,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall)
                                )
                                Text(name.ifBlank { "Terminal" },
                                    modifier = Modifier.weight(1f), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, color = c.foreground)
                                if (current) {
                                    Text("Current", color = c.accent,
                                        style = MaterialTheme.typography.labelMedium)
                                } else {
                                    Icon(painterResource(R.drawable.hugeicons_chevron_right),
                                        contentDescription = null, tint = c.muted,
                                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                }
                            }
                        }
                    }
                }
                if (activeTabId != null &&
                    (tabWorkspaces[activeTabId] ?: KlyroWorkspaceCatalog.GENERAL) != selected.id
                ) {
                    TextButton(
                        onClick = { onAssignCurrentTab(selected.id) },
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = KlyroUiMetrics.minTouch)
                    ) { Text("Move current session here") }
                }
            }
        }

        deletingId?.let { id ->
            val name = entries.firstOrNull { it.id == id }?.name ?: "workspace"
            AlertDialog(
                onDismissRequest = { deletingId = null },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = c.sheet,
                title = { Text("Delete " + name + "?") },
                text = { Text("Its sessions return to General. No shell or files are deleted.") },
                confirmButton = {
                    TextButton(onClick = { onDeleteProject(id); deletingId = null }) {
                        Text("Delete", color = c.danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingId = null }) { Text("Cancel") }
                }
            )
        }
    }
}
