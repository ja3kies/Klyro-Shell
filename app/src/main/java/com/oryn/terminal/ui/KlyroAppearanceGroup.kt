package com.oryn.terminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/** Quiet grouped settings: one surface per subject, rather than per control. */
@Composable
internal fun KlyroAppearanceGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalKlyroColors.current
    Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)) {
        Text(title, color = colors.muted, style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = KlyroUiMetrics.spaceXs).semantics { heading() })
        Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = KlyroUiMetrics.cardPadding, vertical = KlyroUiMetrics.spaceSm),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm),
                content = content
            )
        }
    }
}
