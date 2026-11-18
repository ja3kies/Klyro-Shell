package com.oryn.terminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
internal fun KlyroStorageTransferDialog(
    busy: Boolean,
    cancelling: Boolean,
    message: String,
    isError: Boolean,
    onCancel: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        AlertDialog(
            onDismissRequest = { if (busy && !cancelling) onCancel() else onClose() },
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = colors.sheet,
            title = { Text(if (cancelling) "Cancelling transfer…" else if (busy) "File transfer" else "Transfer result") },
            text = {
                Column(Modifier.heightIn(max = KlyroUiMetrics.paletteListMax).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    SelectionContainer {
                        Text(message, color = if (isError) colors.danger else colors.foreground,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    if (cancelling) Text("Waiting for the document provider. You can close this dialog and continue using Klyro.")
                    else if (busy) Text("Cancelling keeps files already imported. An unfinished export may leave an incomplete destination file.")
                }
            },
            confirmButton = {
                TextButton(onClick = { if (busy && !cancelling) onCancel() else onClose() }) {
                    Text(if (busy && !cancelling) "Cancel transfer" else "Close")
                }
            }
        )
    }
}
