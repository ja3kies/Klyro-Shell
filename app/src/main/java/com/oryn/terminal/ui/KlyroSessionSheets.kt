package com.oryn.terminal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
    onActivate: () -> Unit,
    onCloseSession: () -> Unit
) {
    var closeTargetId by rememberSaveable { mutableStateOf<Long?>(null) }
    val confirmClose = closeTargetId == sessionId
    val colors = LocalKlyroColors.current
    val needsScroll = KlyroSheetLayoutPolicy.needsScroll(
        rows = if (activityStatus.isNullOrBlank()) 4 else 5,
        screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp,
        fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale
    )
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss, dismissEnabled = !confirmClose) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .then(if (needsScroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = KlyroUiMetrics.spaceXl),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
            ) {
                KlyroSheetTitle(sessionName.ifBlank { "Session" })
                if (!activityStatus.isNullOrBlank()) {
                    Surface(
                        color = colors.tile,
                        shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                            trailing = true, enabled = canCreateSession,
                            subtitle = if (canCreateSession) null else "Maximum 8 sessions")
                        if (!isActive) KlyroActionRow("Switch to session",
                            R.drawable.hugeicons_arrow_up_right_01, onActivate)
                    }
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                    Column {
                        KlyroActionRow("Close terminal", R.drawable.hugeicons_cancel_circle,
                            onClick = { closeTargetId = sessionId }, destructive = true)
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
                    Text("This stops its Bash process and running child commands. Other sessions remain open.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val expected = closeTargetId
                        closeTargetId = null
                        TerminalActionPolicy.runIfCurrent(expected, sessionId, true, onCloseSession)
                    }) { Text("Close session", color = colors.danger, fontWeight = FontWeight.SemiBold) }
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
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        AlertDialog(
            onDismissRequest = { if (value.text == name) onDismiss() },
            properties = androidx.compose.ui.window.DialogProperties(
                dismissOnBackPress = value.text == name, dismissOnClickOutside = value.text == name),
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = colors.sheet,
            title = { Text("Rename terminal", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        val cleaned = it.text.replace("\n", "").replace("\r", "")
                        if (cleaned.length <= 32) {
                            value = it.copy(text = cleaned)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(requester),
                    isError = value.text.trim().isBlank() && value.text.isNotEmpty(),
                    supportingText = {
                        Text("Maximum 32 characters. Save or Cancel to close a changed name.")
                    },
                    singleLine = true,
                    label = { Text("Session name") },
                )
            },
            confirmButton = {
                Button(
                    enabled = value.text.trim().isNotBlank() && value.text.trim() != name,
                    onClick = { onSave(value.text.trim()) },
                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
    LaunchedEffect(requester) {
        requester.requestFocus()
        keyboard?.show()
    }
}

/** Only Bash is offered until another shell is verified in the isolated runtime. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroChooseShellSheet(
    onBack: () -> Unit,
    onNewBash: () -> Unit,
    canStartBash: Boolean = true
) {
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onBack) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = KlyroUiMetrics.sheetGutter)
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
                            .clickable(enabled = canStartBash, role = Role.Button,
                                onClickLabel = if (canStartBash) "Start a Bash terminal"
                                    else "Maximum 8 sessions reached", onClick = onNewBash)
                            .semantics {
                                role = Role.Button
                                if (!canStartBash) stateDescription = "Unavailable"
                            }
                            .padding(KlyroUiMetrics.spaceLg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                    ) {
                        Icon(painterResource(R.drawable.hugeicons_terminal),
                            contentDescription = null,
                            tint = if (canStartBash) colors.accent else colors.muted,
                            modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
                        Column {
                            Text("bash", fontWeight = FontWeight.SemiBold)
                            Text(if (canStartBash) "/bin/bash" else
                                "Maximum 8 sessions. Close a terminal first.",
                                color = colors.muted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
