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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
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

private val SheetBackground = Color(0xFFF1F7FA)
private val CardBackground = Color(0xFFFBFEFC)
private val SheetInk = Color(0xFF18282B)
private val SecondaryInk = Color(0xFF516C66)
private val DestructiveInk = Color(0xFF943F43)
private val SheetScheme = lightColorScheme(
    primary = Color(0xFF266B7A),
    onPrimary = Color.White,
    background = SheetBackground,
    surface = SheetBackground,
    onSurface = SheetInk,
    outline = Color(0xFF91B5BB)
)

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
    MaterialTheme(colorScheme = SheetScheme) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = SheetBackground,
            contentColor = SheetInk,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFB5C3C9)) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(sessionName.ifBlank { "Session" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Surface(color = CardBackground, shape = RoundedCornerShape(26.dp)) {
                    Column {
                        SheetAction("✎", "Rename", onRename)
                        SheetAction("+", "New terminal", onChooseShell, trailing = "›")
                        if (!isActive) SheetAction("↗", "Switch to session", onActivate)
                    }
                }
                Surface(color = CardBackground, shape = RoundedCornerShape(26.dp)) {
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
                                    color = DestructiveInk,
                                    style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { confirmClose = false }) {
                                        Text("Keep session")
                                    }
                                    TextButton(onClick = onCloseSession) {
                                        Text("Close session", color = DestructiveInk)
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
    destructive: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .clickable(onClickLabel = label, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(symbol, color = if (destructive) DestructiveInk else SecondaryInk,
            style = MaterialTheme.typography.titleLarge)
        Text(label, color = if (destructive) DestructiveInk else SheetInk,
            style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = SecondaryInk,
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
    MaterialTheme(colorScheme = SheetScheme) {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(28.dp),
            containerColor = SheetBackground,
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
    MaterialTheme(colorScheme = SheetScheme) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = SheetBackground,
            contentColor = SheetInk,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFFB5C3C9)) }
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
                Surface(color = CardBackground, shape = RoundedCornerShape(26.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(
                            onClickLabel = "Start a Bash terminal", onClick = onNewBash
                        ).padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("⌘", color = SecondaryInk,
                            style = MaterialTheme.typography.headlineSmall)
                        Column {
                            Text("bash", fontWeight = FontWeight.SemiBold)
                            Text("/bin/bash", color = SecondaryInk,
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
