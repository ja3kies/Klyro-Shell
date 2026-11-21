package com.oryn.terminal.ui

import androidx.compose.animation.animateColorAsState
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import com.oryn.terminal.rememberHomeMotionEnabled
import kotlin.math.sin
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.IconButton

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
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
    onRenameTab: (Long, String) -> Unit = { _, _ -> },
    onCloseTab: (Long) -> Unit = {},
    onDismiss: () -> Unit
) {
    val c = LocalKlyroColors.current
    var editingSessions by rememberSaveable { mutableStateOf(false) }
    var renameSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var closeSessionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorText by rememberSaveable { mutableStateOf("") }
    var editorError by rememberSaveable { mutableStateOf(false) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    var menuId by rememberSaveable { mutableStateOf<String?>(null) }
    var lastAssignedWorkspaceId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(tabs) {
        if (renameSessionId != null && tabs.none { it.first == renameSessionId }) renameSessionId = null
        if (closeSessionId != null && tabs.none { it.first == closeSessionId }) closeSessionId = null
    }
    // Menus can outlive their backing workspace after a deletion.
    LaunchedEffect(entries, menuId) {
        if (menuId != null && entries.none { it.id == menuId }) menuId = null
    }
    // An explicit Save/Cancel ends editing; external dismissal cannot discard the draft.
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
    val motionEnabled = rememberHomeMotionEnabled()
    val editPhase = if (projectTabs.isNotEmpty() && editingSessions && motionEnabled && renameSessionId == null &&
        closeSessionId == null && editorId == null && deletingId == null && menuId == null
    ) {
        rememberInfiniteTransition(label = "Session editing").animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
            label = "Edit phase"
        )
    } else null
    // Measured overflow, not row count, controls scrolling.
    val workspaceScroll = rememberScrollState()
    val editorFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(selectedId) { workspaceScroll.scrollTo(0) }
    LaunchedEffect(editorId) {
        if (editorId != null) {
            withFrameNanos { }
            editorFocus.requestFocus()
            keyboard?.show()
        }
    }
    fun editProject(id: String, title: String) {
        editorId = id
        editorText = title
        editorError = false
        menuId = null
    }

    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss,
            dismissEnabled = editorId == null && deletingId == null && menuId == null && renameSessionId == null && closeSessionId == null) {
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
                    TextButton(
                        onClick = { editProject("", "") },
                        enabled = editorId == null && entries.size < KlyroWorkspaceCatalog.MAX_PROJECTS
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)) {
                            val tint = if (editorId == null &&
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
                entries.forEach { entry ->
                    val count = tabs.count {
                        (tabWorkspaces[it.first] ?: KlyroWorkspaceCatalog.GENERAL) == entry.id
                    }
                    val active = entry.id == selected.id
                    val cardColor by animateColorAsState(
                        if (active) c.accent.copy(alpha = .11f) else c.tile,
                        label = "workspace-selection"
                    )
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
                                        color = c.muted
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
                    val editTarget = entries.firstOrNull { it.id == editorId }
                    val canSave = editorText.trim().isNotEmpty() &&
                        (!editing || (editTarget != null &&
                            editorText.trim() != editTarget.name))
                    fun saveWorkspaceEdit() {
                        if (!canSave || editorId == null) return
                        val saved = if (editing) {
                            editTarget != null && onRenameProject(editTarget.id, editorText)
                        } else onCreateProject(editorText)
                        if (saved) {
                            keyboard?.hide()
                            editorId = null
                            editorError = false
                        } else editorError = true
                    }
                    Surface(color = c.tile,
                        shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                        Column(
                            Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd),
                            verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                        ) {
                            KlyroInlineNotice("Save or Cancel this edit before closing Workspaces.")
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
                                modifier = Modifier.fillMaxWidth().focusRequester(editorFocus),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { if (canSave) saveWorkspaceEdit() }),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            )
                            if (editing && editTarget == null) {
                                Text("Workspace no longer exists. Cancel this edit.",
                                    color = c.danger,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            if (editorError) Text("Choose a unique, non-empty name.",
                                color = c.danger, style = MaterialTheme.typography.bodySmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = {
                                    keyboard?.hide()
                                    editorId = null
                                    editorError = false
                                }) { Text("Cancel") }
                                Button(
                                    enabled = canSave,
                                    onClick = { saveWorkspaceEdit() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = c.accent, contentColor = c.onAccent
                                    ), shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                ) { Text("Save") }
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sessions", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                        color = c.foreground, style = MaterialTheme.typography.titleSmall)
                    if (projectTabs.isNotEmpty()) TextButton(onClick = { editingSessions = !editingSessions }) {
                        Text(if (editingSessions) "Done" else "Edit")
                    }
                }
                if (projectTabs.isEmpty()) {
                    Text("No sessions yet.",
                        modifier = Modifier.padding(vertical = KlyroUiMetrics.spaceSm),
                        color = c.muted, style = MaterialTheme.typography.bodySmall)
                } else {
                    projectTabs.forEach { (id, name) ->
                        val current = id == activeTabId
                        Surface(
                            modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
                                .graphicsLayer {
                                    val phase = editPhase?.value
                                    if (phase == null) {
                                        rotationZ = 0f
                                        translationY = 0f
                                    } else {
                                        val offset = (id % 17) / 17.0
                                        rotationZ = (0.65 * sin(2 * Math.PI * (phase * 5 + offset))).toFloat()
                                        translationY = (sin(2 * Math.PI * (phase * 7 + offset * 2))).toFloat().dp.toPx()
                                    }
                                }
                                .clickable(
                                    enabled = !current && !editingSessions,
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
                                if (editingSessions) {
                                    IconButton(onClick = { renameSessionId = id }) {
                                        Icon(painterResource(R.drawable.hugeicons_pen_01),
                                            contentDescription = "Rename $name", tint = c.foreground)
                                    }
                                    IconButton(onClick = { closeSessionId = id }) {
                                        Icon(painterResource(R.drawable.hugeicons_cancel_circle),
                                            contentDescription = "Close $name", tint = c.danger)
                                    }
                                } else if (current) {
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
                if (lastAssignedWorkspaceId == selected.id && activeTabId != null &&
                    tabWorkspaces[activeTabId] == selected.id) {
                    KlyroInlineNotice("Current terminal moved to ${selected.name}.")
                }
                if (activeTabId != null &&
                    (tabWorkspaces[activeTabId] ?: KlyroWorkspaceCatalog.GENERAL) != selected.id
                ) {
                    TextButton(
                        onClick = {
                            onAssignCurrentTab(selected.id)
                            lastAssignedWorkspaceId = selected.id
                        },
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = KlyroUiMetrics.minTouch)
                    ) { Text("Move current session here") }
                }
                }
            }
        }

        renameSessionId?.let { id ->
            tabs.firstOrNull { it.first == id }?.let { (_, name) ->
                KlyroRenameSessionDialog(name = name, onSave = { onRenameTab(id, it); renameSessionId = null },
                    onDismiss = { renameSessionId = null })
            }
        }
        closeSessionId?.let { id ->
            val target = tabs.firstOrNull { it.first == id }
            if (target != null) AlertDialog(
                onDismissRequest = { closeSessionId = null },
                title = { Text("Close ${target.second.ifBlank { "session" }}?") },
                text = { Text("Closing a session stops its shell and any running commands.") },
                confirmButton = { TextButton(onClick = { onCloseTab(id); closeSessionId = null }) {
                    Text("Close session", color = c.danger)
                } },
                dismissButton = { TextButton(onClick = { closeSessionId = null }) { Text("Keep session") } },
                containerColor = c.sheet,
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner)
            )
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
                        else "Its sessions return to General. No shell or files are deleted.")
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
