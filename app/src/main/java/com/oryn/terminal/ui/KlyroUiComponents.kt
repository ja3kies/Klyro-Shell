package com.oryn.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.oryn.terminal.R

/**
 * Shared presentation primitives for Klyro sheets, menus and action rows.
 * The caller controls state and effects; no terminal session is ever started here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroSheet(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalKlyroColors.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.sheet,
        contentColor = colors.foreground,
        shape = RoundedCornerShape(
            topStart = KlyroUiMetrics.sheetCorner,
            topEnd = KlyroUiMetrics.sheetCorner
        ),
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.muted) },
        content = content
    )
}

@Composable
internal fun KlyroSectionHeading(title: String, subtitle: String? = null) {
    val colors = LocalKlyroColors.current
    Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.foreground
        )
        if (!subtitle.isNullOrBlank()) {
            Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun KlyroActionRow(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    subtitle: String? = null,
    destructive: Boolean = false,
    trailing: Boolean = false,
    iconWell: Boolean = false
) {
    val colors = LocalKlyroColors.current
    val ink = if (destructive) colors.danger else if (iconWell) colors.accent else colors.muted
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = KlyroUiMetrics.minTouch)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = KlyroUiMetrics.spaceLg, vertical = KlyroUiMetrics.spaceSm),
        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconWell) {
            Box(
                Modifier.size(KlyroUiMetrics.iconWell)
                    .background(ink.copy(alpha = .09f),
                        RoundedCornerShape(KlyroUiMetrics.controlCorner)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = ink,
                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
            }
        } else {
            Icon(painterResource(icon), contentDescription = null, tint = ink,
                modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
        }
        Column(Modifier.weight(1f)) {
            Text(label, color = if (destructive) colors.danger else colors.foreground,
                fontWeight = if (subtitle == null) FontWeight.Medium else FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = colors.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        if (trailing) {
            Icon(painterResource(R.drawable.hugeicons_chevron_right),
                contentDescription = null, tint = colors.muted,
                modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
        }
    }
}

@Composable
internal fun KlyroMenuItem(
    label: String,
    icon: Int,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    val colors = LocalKlyroColors.current
    val ink = if (destructive) colors.danger else colors.foreground
    DropdownMenuItem(
        text = { Text(label, color = ink) },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null, tint = ink,
            modifier = Modifier.size(KlyroUiMetrics.glyphSmall)) },
        modifier = Modifier.heightIn(min = KlyroUiMetrics.compactRow),
        onClick = onClick
    )
}

@Composable
internal fun KlyroLicenseDialog(
    title: String,
    assetPath: String,
    onDismissRequest: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    val license = remember(context, assetPath) {
        runCatching {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        }.getOrElse { "Licence unavailable in this build." }
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
        containerColor = colors.sheet,
        title = { Text(title) },
        text = {
            Column(Modifier.heightIn(max = KlyroUiMetrics.paletteListMax)
                .verticalScroll(rememberScrollState())) {
                Text(license, style = MaterialTheme.typography.bodySmall,
                    color = colors.foreground)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text("Close") }
        }
    )
}
