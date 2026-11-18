package com.oryn.terminal.ui


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.key
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
    onAssignCurrentTab: (String) -> Boolean,
    finishedTabIds: Set<Long>,
    onRenameTab: (Long, String) -> Unit,
    onCloseTab: (Long) -> Unit,
    onSwitchTab: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val c = LocalKlyroColors.current
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorText by rememberSaveable { mutableStateOf("") }
    var editingSessions by rememberSaveable(selectedId) { mutableStateOf(false) }
    var renamingSessionId by rememberSaveable(selectedId) { mutableStateOf<Long?>(null) }
    var closingSessionId by rememberSaveable(selectedId) { mutableStateOf<Long?>(null) }
    var feedback by rememberSaveable(selectedId) { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var menuId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastAssignedWorkspaceId by rememberSaveable { mutableStateOf<String?>(null) }
    // Menus can outlive their backing workspace after a deletion.
    LaunchedEffect(entries, menuId) {
        if (menuId != null && entries.none { it.id == menuId }) menuId = null
    }
    // A form owns dismissal while a draft is open; changed names require discard confirmation.
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
    // Measured overflow, not row count, controls scrolling.
    val workspaceScroll = key(selectedId) { rememberScrollState() }
    LaunchedEffect(tabs, tabWorkspaces, selectedId, renamingSessionId, closingSessionId) {
        if (renamingSessionId != null && tabs.none { it.first == renamingSessionId }) {
            renamingSessionId = null
            feedback = "Session no longer available."
        }
        if (closingSessionId != null && tabs.none { it.first == closingSessionId }) {
            closingSessionId = null
            feedback = "Session already closed."
        }
        if (projectTabs.isEmpty()) editingSessions = false
    }
    val childOpen = editorId != null || deletingId != null || menuId != null ||
        renamingSessionId != null || closingSessionId != null
    fun editProject(id: String, title: String) {
        if (editorId != null) return
        editorId = id
        editorText = title
        menuId = null
    }

    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss,
            dismissEnabled = !childOpen) {
            Column(
                Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth().heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .imePadding()
                    .navigationBarsPadding()
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
                    TextButton(onClick = {
                        if (editingSessions) editingSessions = false else onDismiss()
                    }, enabled = !childOpen) { Text(if (editingSessions) "Done" else "Close") }
                    OutlinedButton(
                        onClick = { editProject("", "") },
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                            .semantics { contentDescription = "New workspace" },
                        enabled = !childOpen && !editingSessions && entries.size < KlyroWorkspaceCatalog.MAX_PROJECTS
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)) {
                            val tint = if (!childOpen && !editingSessions &&
                                entries.size < KlyroWorkspaceCatalog.MAX_PROJECTS)
                                c.accent else c.muted
                            Icon(painterResource(R.drawable.hugeicons_plus_sign),
                                contentDescription = null, tint = tint,
                                modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                            Text("New", color = tint)
                        }
                    }
                }
                // Fixed heading/New action; all varying content scrolls independently.
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(workspaceScroll),
                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                ) {
                if (entries.size >= KlyroWorkspaceCatalog.MAX_PROJECTS) {
                    Text(
                        "Workspace limit reached. Remove an unused workspace to add another.",
                        color = c.muted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                ) {
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
                                    .clip(RoundedCornerShape(KlyroUiMetrics.selectedCorner))
                                    .clickable(enabled = !childOpen && !editingSessions, role = Role.RadioButton,
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
                                        painterResource(if (active) R.drawable.hugeicons_checkmark_circle_01
                                            else R.drawable.hugeicons_folder_01),
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
                                        maxLines = 2, overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (count == 1) "1 session" else count.toString() + " sessions",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = c.muted
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier.size(KlyroUiMetrics.minTouch),
                                contentAlignment = Alignment.Center
                            ) {
                                if (entry.id != KlyroWorkspaceCatalog.GENERAL) {
                                    IconButton(onClick = { menuId = entry.id },
                                        enabled = !childOpen && !editingSessions,
                                        modifier = Modifier.size(KlyroUiMetrics.minTouch)
                                            .semantics {
                                                contentDescription = "Options for " + entry.name
                                            }
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

                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sessions", modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold, color = c.foreground,
                        style = MaterialTheme.typography.titleSmall)
                    if (projectTabs.isNotEmpty() && !editingSessions) {
                        IconButton(onClick = { editingSessions = true }, enabled = !childOpen) {
                            Icon(painterResource(R.drawable.hugeicons_pen_01),
                                contentDescription = "Edit sessions in ${selected.name}", tint = c.accent,
                                modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                        }
                    }
                }
                feedback?.let {
                    Text(it, color = c.muted, style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                if (projectTabs.isEmpty()) {
                    Text("No sessions in ${selected.name}.",
                        modifier = Modifier.padding(vertical = KlyroUiMetrics.spaceSm),
                        color = c.muted, style = MaterialTheme.typography.bodySmall)
                } else {
                    projectTabs.forEach { (id, name) ->
                        key(id) {
                            KlyroWorkspaceSessionRow(
                                name = name,
                                current = id == activeTabId,
                                finished = id in finishedTabIds,
                                editing = editingSessions,
                                enabled = !childOpen,
                                animateEdit = !childOpen,
                                onSelect = {
                                    if (tabs.any { it.first == id }) {
                                        onSwitchTab(id)
                                        onDismiss()
                                    }
                                },
                                onEdit = { editingSessions = true },
                                onRename = { renamingSessionId = id },
                                onClose = { closingSessionId = id }
                            )
                        }
                    }
                }
                if (lastAssignedWorkspaceId == selected.id && activeTabId != null &&
                    tabWorkspaces[activeTabId] == selected.id) {
                    KlyroInlineNotice("Current terminal moved to ${selected.name}.")
                }
                if (activeTabId != null &&
                    (tabWorkspaces[activeTabId] ?: KlyroWorkspaceCatalog.GENERAL) != selected.id
                ) {
                    OutlinedButton(
                        enabled = !childOpen && !editingSessions,
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = c.foreground),
                        onClick = {
                            if (onAssignCurrentTab(selected.id)) {
                                lastAssignedWorkspaceId = selected.id
                            } else feedback = "Session changed. Try moving the current session again."
                        },
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = KlyroUiMetrics.minTouch)
                    ) { Text("Move current session here") }
                }
                }
            }
        }

        editorId?.let { id ->
            val creating = id.isEmpty()
            val target = entries.firstOrNull { it.id == id }
            key("workspace", id) {
                KlyroNameDialog(
                    name = editorText,
                    title = if (creating) "New workspace" else "Rename workspace",
                    label = "Workspace name",
                    saveLabel = if (creating) "Create workspace" else "Save name",
                    discardMessage = if (creating) "This workspace will not be created."
                        else "The workspace keeps its current name.",
                    available = creating || target != null,
                    validateName = { value ->
                        when {
                            creating && entries.size >= KlyroWorkspaceCatalog.MAX_PROJECTS ->
                                "Workspace limit reached. Remove an unused workspace first."
                            entries.any { it.id != id &&
                                it.name.lowercase(Locale.ROOT) == value.lowercase(Locale.ROOT) } ->
                                "A workspace already uses this name."
                            else -> null
                        }
                    },
                    onSave = { value ->
                        val saved = if (creating) onCreateProject(value)
                            else target != null && onRenameProject(id, value)
                        if (saved) { editorId = null; feedback = "Workspace name saved." }
                        saved
                    },
                    onDismiss = { editorId = null }
                )
            }
        }
        renamingSessionId?.let { id ->
            tabs.firstOrNull { it.first == id }?.let { (_, name) ->
                key("session", id) {
                    KlyroNameDialog(name = name,
                        onSave = { value ->
                            if (tabs.any { it.first == id }) {
                                onRenameTab(id, value)
                                renamingSessionId = null
                                feedback = "Session renamed to $value."
                                true
                            } else false
                        },
                        onDismiss = { renamingSessionId = null })
                }
            }
        }
        closingSessionId?.let { id ->
            tabs.firstOrNull { it.first == id }?.let { (_, name) ->
                key(id) {
                    KlyroCloseSessionDialog(sessionId = id, name = name,
                        running = id !in finishedTabIds,
                        onDismiss = { closingSessionId = null },
                        onConfirm = {
                            closingSessionId = null
                            if (tabs.any { it.first == id }) {
                                onCloseTab(id)
                                feedback = "$name closed."
                            }
                        })
                }
            }
        }

        deletingId?.let { id ->
            val target = entries.firstOrNull {
                it.id == id && it.id != KlyroWorkspaceCatalog.GENERAL
            }
            val name = target?.name ?: "workspace"
            AlertDialog(
                onDismissRequest = { deletingId = null },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = c.sheet,
                title = { Text("Delete " + name + "?") },
                text = {
                    Text(if (target == null) "Workspace no longer available. Cancel this action."
                        else "Its sessions return to General. No shell or files are deleted.",
                        modifier = Modifier.heightIn(max = KlyroUiMetrics.paletteListMax)
                            .verticalScroll(rememberScrollState()))
                },
                confirmButton = {
                    TextButton(
                        enabled = target != null,
                        onClick = { if (target != null) { onDeleteProject(id); deletingId = null } }
                    ) {
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
