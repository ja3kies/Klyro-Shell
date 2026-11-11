package com.oryn.terminal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    // Long Settings pages must open in the expanded detent instead of
    // requiring a second swipe that can collide with nested scrolling.
    // Compact root sheets still size naturally to their content.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
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

/** Consistent 22sp title for all root Klyro bottom sheets. */
@Composable
internal fun KlyroSheetTitle(title: String, modifier: Modifier = Modifier) {
    val colors = LocalKlyroColors.current
    Text(title, modifier = modifier, style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold, color = colors.foreground,
        maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    iconWell: Boolean = false,
    enabled: Boolean = true
) {
    val colors = LocalKlyroColors.current
    val ink = if (!enabled) colors.muted else if (destructive) colors.danger else colors.muted
    Row(
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = KlyroUiMetrics.minTouch)
            .clickable(enabled = enabled, role = Role.Button,
                onClickLabel = label, onClick = onClick)
            .semantics {
                contentDescription = if (subtitle.isNullOrBlank()) label else "$label. $subtitle"
                if (!enabled) stateDescription = "Unavailable"
                else if (destructive) stateDescription = "Destructive action"
            }
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
            Text(label, color = if (!enabled) colors.muted else if (destructive)
                colors.danger else colors.foreground,
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
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val ink = if (!enabled) colors.muted else if (destructive) colors.danger else colors.foreground
    DropdownMenuItem(
        text = { Text(label, color = ink) },
        leadingIcon = { Icon(painterResource(icon), contentDescription = null, tint = ink,
            modifier = Modifier.size(KlyroUiMetrics.glyphSmall)) },
        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
            .semantics {
                contentDescription = label
                if (!enabled) stateDescription = "Unavailable"
                else if (destructive) stateDescription = "Destructive action"
            },
        enabled = enabled,
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

/** Reusable, screen-reader-visible feedback without a modal or repeated toast. */
@Composable
internal fun KlyroInlineNotice(message: String, isError: Boolean = false) {
    val colors = LocalKlyroColors.current
    Surface(
        color = if (isError) colors.dangerContainer else colors.tile,
        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd)
                .semantics { liveRegion = LiveRegionMode.Polite },
            color = if (isError) colors.danger else colors.foreground,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Shared empty state with an explicit, accessible recovery action. */
@Composable
internal fun KlyroEmptyState(
    title: String,
    detail: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    val colors = LocalKlyroColors.current
    Surface(color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
        ) {
            Text(title, color = colors.foreground, fontWeight = FontWeight.SemiBold)
            Text(detail, color = colors.muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onAction,
                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)) {
                Text(actionLabel)
            }
        }
    }
}

/** iOS-inspired restrained slider shared by font, hue and opacity settings. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    showTrack: Boolean = true
) {
    val colors = LocalKlyroColors.current
    Slider(
        value = value.coerceIn(valueRange.start, valueRange.endInclusive),
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = colors.foreground,
            activeTrackColor = colors.accent,
            inactiveTrackColor = colors.key
        ),
        thumb = {
            Box(
                Modifier.size(KlyroUiMetrics.iosSliderThumb)
                    .background(colors.foreground, CircleShape)
                    .border(KlyroUiMetrics.iosSliderBorder,
                        colors.muted.copy(alpha = .35f), CircleShape)
            )
        },
        track = { state ->
            if (showTrack) {
                Box(
                    Modifier.fillMaxWidth().height(KlyroUiMetrics.iosSliderTrack)
                        .background(colors.key, CircleShape)
                ) {
                    val range = valueRange.endInclusive - valueRange.start
                    val progress = if (range > 0f) {
                        ((state.value - valueRange.start) / range).coerceIn(0f, 1f)
                    } else 0f
                    Box(
                        Modifier.fillMaxWidth(progress)
                            .height(KlyroUiMetrics.iosSliderTrack)
                            .background(colors.accent, CircleShape)
                    )
                }
            } else {
                Spacer(Modifier.fillMaxWidth().height(KlyroUiMetrics.iosSliderTrack))
            }
        }
    )
}
