package com.oryn.terminal.ui

import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import com.oryn.terminal.core.KlyroTextLimit
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.oryn.terminal.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/** Session management follows the compact dsh-Go pattern: actions first, form on demand. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroSessionSheet(
    sessionId: Long,
    sessionName: String,
    isActive: Boolean,
    activityStatus: String? = null,
    activityDetail: String? = null,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onChooseShell: () -> Unit,
    canCreateSession: Boolean = true,
    launchingBash: Boolean = false,
    onActivate: () -> Unit,
    onCloseSession: () -> Unit,
    sessionRunning: Boolean = true
) {
    var closeTargetId by rememberSaveable(sessionId) { mutableStateOf<Long?>(null) }
    val confirmClose = closeTargetId == sessionId
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss, dismissEnabled = !confirmClose) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = KlyroUiMetrics.sheetContentMax)
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
                    KlyroSheetTitle(sessionName.ifBlank { "Session" },
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss,
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)) {
                        Text("Done")
                    }
                }
                // Terminal identity and dismissal stay visible while actions scroll.
                Column(
                    modifier = Modifier.weight(1f, fill = false)
                        .fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                ) {
                if (!activityStatus.isNullOrBlank()) {
                    Surface(
                        color = colors.tile,
                        shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                activityStatus,
                                color = colors.foreground,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (!activityDetail.isNullOrBlank()) {
                                Text(activityDetail, color = colors.muted,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                    Column {
                        KlyroActionRow("Rename", R.drawable.hugeicons_pen_01, onRename)
                        KlyroActionRow("New terminal", R.drawable.hugeicons_plus_sign,
                            onChooseShell,
                            trailing = true, enabled = canCreateSession && !launchingBash,
                            subtitle = if (launchingBash) "Starting Bash…" else
                                if (canCreateSession) null else "Maximum 8 sessions")
                        if (!isActive) KlyroActionRow("Switch to session",
                            R.drawable.hugeicons_arrow_up_right_01, onActivate)
                    }
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                    Column {
                        KlyroActionRow(if (sessionRunning) "Close terminal" else "Remove finished terminal", R.drawable.hugeicons_cancel_circle,
                            onClick = { closeTargetId = sessionId }, destructive = true)
                    }
                }
                }
            }
        }
        if (confirmClose) {
            AlertDialog(
                onDismissRequest = { closeTargetId = null },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Close ${sessionName.ifBlank { "session" }}?") },
                text = {
                    Text(if (sessionRunning)
                        "This stops its Bash process and running child commands. Other sessions remain open."
                        else "This removes the retained output. Its process has already exited. Other sessions remain open.",
                        modifier = Modifier.heightIn(max = KlyroUiMetrics.paletteListMax)
                            .verticalScroll(rememberScrollState()))
                },
                confirmButton = {
                    TextButton(onClick = {
                        val expected = closeTargetId
                        closeTargetId = null
                        TerminalActionPolicy.runIfCurrent(expected, sessionId, true, onCloseSession)
                    }) { Text(if (sessionRunning) "Close session" else "Remove output", color = colors.danger, fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = {
                    TextButton(onClick = { closeTargetId = null }) { Text("Keep session") }
                }
            )
        }
    }
}

/** Separate rename modal: the keyboard opens with the current name selected. */
@Composable
internal fun KlyroRenameSessionDialog(
    name: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var value by rememberSaveable(name, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(name, selection = TextRange(0, name.length)))
    }
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var confirmDiscard by rememberSaveable(name) { mutableStateOf(false) }
    var submitted by remember(name) { mutableStateOf(false) }
    val normalized = KlyroWorkspaceCatalog.cleanName(value.text)
    val error = when {
        value.text.length > KlyroWorkspaceCatalog.MAX_NAME_CHARS -> "Name is too long. Maximum 32 characters."
        value.text.isNotEmpty() && normalized.isBlank() -> "Enter a visible session name."
        normalized != value.text.trim() -> "Remove unsupported control characters."
        else -> null
    }
    val changed = value.text.trim() != name
    val canSave = !submitted && normalized.isNotBlank() && normalized != name && error == null
    fun dismissRename() {
        keyboard?.hide()
        if (changed) confirmDiscard = true else onDismiss()
    }
    fun saveRename() {
        if (!canSave) return
        submitted = true
        keyboard?.hide()
        onSave(normalized)
    }
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        AlertDialog(
            onDismissRequest = { dismissRename() },
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = colors.sheet,
            title = { Text("Rename terminal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        val bounded = KlyroTextLimit.takeCodePoints(it.text, 128)
                        value = if (bounded == it.text) it else it.copy(
                            text = bounded,
                            selection = TextRange(it.selection.start.coerceAtMost(bounded.length),
                                it.selection.end.coerceAtMost(bounded.length)),
                            composition = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(requester),
                    isError = error != null,
                    supportingText = {
                        Text(error ?: "${value.text.length}/${KlyroWorkspaceCatalog.MAX_NAME_CHARS}",
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { saveRename() }),
                    label = { Text("Session name") },
                )
            },
            confirmButton = {
                Button(
                    enabled = canSave,
                    onClick = { saveRename() },
                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { dismissRename() }) { Text("Cancel") }
            }
        )
    }
    if (confirmDiscard) {
        KlyroModalTheme {
            AlertDialog(
                onDismissRequest = { confirmDiscard = false },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Discard name changes?") },
                text = { Text("The session keeps its current name.") },
                confirmButton = {
                    TextButton(onClick = { confirmDiscard = false; onDismiss() }) {
                        Text("Discard changes", color = colors.danger)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmDiscard = false }) {
                        Text("Keep editing")
                    }
                }
            )
        }
    }
    LaunchedEffect(requester, confirmDiscard) {
        if (!confirmDiscard) {
            withFrameNanos { }
            requester.requestFocus()
            keyboard?.show()
        }
    }
}

/** Only Bash is offered until another shell is verified in the isolated runtime. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroChooseShellSheet(
    onBack: () -> Unit,
    onNewBash: () -> Unit,
    canStartBash: Boolean = true,
    launchingBash: Boolean = false
) {
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onBack) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .verticalScroll(rememberScrollState()).padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = KlyroUiMetrics.spaceXl),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.sectionGap)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                            .semantics { contentDescription = "Back to session" }
                    ) {
                        Icon(painterResource(R.drawable.hugeicons_arrow_left_01),
                            contentDescription = null,
                            modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                    }
                    KlyroSheetTitle("Choose shell")
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .heightIn(min = KlyroUiMetrics.minTouch)
                            .clickable(enabled = canStartBash && !launchingBash, role = Role.Button,
                                onClickLabel = if (launchingBash) "Starting Bash…"
                                    else if (canStartBash) "Start a Bash terminal"
                                    else "Maximum 8 sessions reached", onClick = onNewBash)
                            .semantics {
                                role = Role.Button
                                stateDescription = if (launchingBash) "Starting Bash" else if (!canStartBash) "Session limit reached" else "Ready"
                                liveRegion = LiveRegionMode.Polite
                            }
                            .padding(KlyroUiMetrics.spaceLg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                    ) {
                        Icon(painterResource(R.drawable.hugeicons_terminal),
                            contentDescription = null,
                            tint = if (canStartBash) colors.accent else colors.muted,
                            modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
                        Column(Modifier.weight(1f)) {
                            Text("bash", fontWeight = FontWeight.SemiBold)
                            Text(if (launchingBash) "Starting Bash…" else if (canStartBash) "/bin/bash" else "Maximum 8 sessions. Close a terminal first.",
                                color = colors.muted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
