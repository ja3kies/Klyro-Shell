package com.oryn.terminal.ui

import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.oryn.terminal.R
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

/** Session management follows the compact dsh-Go pattern: actions first, form on demand. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroSessionSheet(
    sessionName: String,
    isActive: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onChooseShell: () -> Unit,
    onActivate: () -> Unit,
    onCloseSession: () -> Unit
) {
    var confirmClose by remember(sessionName) { mutableStateOf(false) }
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = { if (!confirmClose) onDismiss() }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = KlyroUiMetrics.spaceXl),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
            ) {
                KlyroSheetTitle(sessionName.ifBlank { "Session" })
                Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                    Column {
                        KlyroActionRow("Rename", R.drawable.hugeicons_pen_01, onRename)
                        KlyroActionRow("New terminal", R.drawable.hugeicons_plus_sign, onChooseShell, trailing = true)
                        if (!isActive) KlyroActionRow("Switch to session",
                            R.drawable.hugeicons_arrow_up_right_01, onActivate)
                    }
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                    Column {
                        KlyroActionRow("Close terminal", R.drawable.hugeicons_cancel_circle,
                            onClick = { confirmClose = true }, destructive = true)
                    }
                }
            }
        }
        if (confirmClose) {
            AlertDialog(
                onDismissRequest = { confirmClose = false },
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                containerColor = colors.sheet,
                title = { Text("Close ${sessionName.ifBlank { "session" }}?") },
                text = {
                    Text("This stops its Bash process and running child commands. Other sessions remain open.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmClose = false
                        onCloseSession()
                    }) { Text("Close session", color = colors.danger, fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClose = false }) { Text("Keep session") }
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
    var value by remember(name) {
        mutableStateOf(TextFieldValue(name, selection = TextRange(0, name.length)))
    }
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
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
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onNewBash: () -> Unit
) {
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = KlyroUiMetrics.spaceXl),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.hugeicons_arrow_left_01),
                            contentDescription = "Back",
                            modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                    }
                    KlyroSheetTitle("Choose shell")
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(
                            onClickLabel = "Start a Bash terminal", onClick = onNewBash
                        ).padding(KlyroUiMetrics.spaceLg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(painterResource(R.drawable.hugeicons_terminal),
                            contentDescription = null, tint = colors.accent,
                            modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
                        Column {
                            Text("bash", fontWeight = FontWeight.SemiBold)
                            Text("/bin/bash", color = colors.muted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
