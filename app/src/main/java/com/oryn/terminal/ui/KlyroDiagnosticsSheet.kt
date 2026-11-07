package com.oryn.terminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroDiagnosticsSheet(
    report: String,
    onVerify: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKlyroColors.current
    KlyroModalTheme {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = colors.sheet,
            contentColor = colors.foreground,
            shape = RoundedCornerShape(
                topStart = KlyroUiMetrics.sheetCorner,
                topEnd = KlyroUiMetrics.sheetCorner
            ),
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.muted) }
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Diagnostics", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold)
                Text("Local snapshot. Recheck launches an explicit runtime verification.",
                    color = colors.muted, style = MaterialTheme.typography.bodySmall)
                Surface(color = colors.tile,
                    shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)) {
                    Text(report, modifier = Modifier.fillMaxWidth().padding(14.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { onVerify(); onDismiss() },
                    modifier = Modifier.fillMaxWidth()) { Text("Recheck environment") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onCopy) { Text("Copy report") }
                    TextButton(onClick = onShare) { Text("Share report") }
                }
                Text("Review the report before sharing. No private paths or shell transcripts are included.",
                    color = colors.muted, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
