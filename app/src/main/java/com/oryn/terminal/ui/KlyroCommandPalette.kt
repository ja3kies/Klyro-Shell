package com.oryn.terminal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal data class KlyroPaletteAction(
    val title: String,
    val details: String,
    val enabled: Boolean = true,
    val execute: () -> Unit
)

/** Application-only commands. Never interpolates input into a running shell. */
@Composable
internal fun KlyroCommandPalette(
    actions: List<KlyroPaletteAction>,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val colors = LocalKlyroColors.current
    val filtered = actions.filter {
        KlyroCommandSearch.matches(query, it.title, it.details)
    }
    KlyroModalTheme {
        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
            containerColor = colors.sheet,
            title = { Text("Command Palette", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it.take(KlyroCommandSearch.MAX_QUERY_LENGTH) },
                        label = { Text("Search actions") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Column(
                        Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (filtered.isEmpty()) {
                            Text("No matching actions.", color = colors.muted,
                                style = MaterialTheme.typography.bodySmall)
                        }
                        filtered.forEach { action ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable(enabled = action.enabled) {
                                    keyboard?.hide()
                                    onDismiss()
                                    action.execute()
                                },
                                color = if (action.enabled) colors.tile else colors.tile.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(action.title,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (action.enabled) colors.foreground else colors.muted)
                                    Text(action.details, style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        )
    }
}
