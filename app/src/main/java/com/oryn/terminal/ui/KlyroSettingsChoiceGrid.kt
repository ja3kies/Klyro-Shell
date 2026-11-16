package com.oryn.terminal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

/** Shared escape from squeezed Settings controls on narrow windows or large text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun KlyroSettingsChoiceGrid(
    columns: Int,
    selectionGroup: Boolean = true,
    minimumInlineWidth: Dp = 340.dp,
    content: @Composable FlowRowScope.() -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < minimumInlineWidth || LocalDensity.current.fontScale >= 1.3f
        FlowRow(
            modifier = Modifier.fillMaxWidth()
                .then(if (selectionGroup) Modifier.selectableGroup() else Modifier),
            maxItemsInEachRow = if (stacked) 1 else columns,
            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm),
            verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm),
            content = content
        )
    }
}
