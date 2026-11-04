package com.oryn.terminal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = colors.sheet,
            contentColor = colors.foreground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.muted) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(sessionName.ifBlank { "Session" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Surface(color = colors.tile, shape = RoundedCornerShape(26.dp)) {
                    Column {
                        SheetAction("", "Rename", onRename, icon = R.drawable.hugeicons_pen_01)
                        SheetAction("+", "New terminal", onChooseShell, trailing = "›")
                        if (!isActive) SheetAction("↗", "Switch to session", onActivate)
                    }
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(26.dp)) {
                    Column {
                        SheetAction("×", "Close terminal", onClick = {
                            confirmClose = true
                        }, destructive = true)
                        if (confirmClose) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("Stop this session and its child processes?",
                                    color = colors.danger,
                                    style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { confirmClose = false }) {
                                        Text("Keep session")
                                    }
                                    TextButton(onClick = onCloseSession) {
                                        Text("Close session", color = colors.danger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetAction(
    symbol: String,
    label: String,
    onClick: () -> Unit,
    trailing: String? = null,
    destructive: Boolean = false,
    icon: Int? = null
) {
    val colors = LocalKlyroColors.current
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (destructive) colors.danger else colors.muted,
                modifier = Modifier.padding(2.dp)
            )
        } else {
            Text(symbol, color = if (destructive) colors.danger else colors.muted,
                style = MaterialTheme.typography.titleLarge)
        }
        Text(label, color = if (destructive) colors.danger else colors.foreground,
            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = colors.muted,
            style = MaterialTheme.typography.titleLarge)
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
            shape = RoundedCornerShape(28.dp),
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
                    shape = RoundedCornerShape(14.dp)
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
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = colors.sheet,
            contentColor = colors.foreground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.muted) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(onClick = onBack) { Text("←") }
                    Text("Choose shell", style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                Surface(color = colors.tile, shape = RoundedCornerShape(26.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(
                            onClickLabel = "Start a Bash terminal", onClick = onNewBash
                        ).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("⌘", color = colors.muted,
                            style = MaterialTheme.typography.headlineSmall)
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
