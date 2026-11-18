package com.oryn.terminal.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import com.oryn.terminal.R

/** Normal tap opens a session; edit mode exposes named, independent actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun KlyroWorkspaceSessionRow(
    name: String,
    current: Boolean,
    finished: Boolean,
    editing: Boolean,
    enabled: Boolean,
    animateEdit: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val title = name.ifBlank { "Terminal" }
    val shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
    val tilt = remember { Animatable(0f) }
    val view = LocalView.current
    // A short edit cue, not a continuous moving target. Compose respects duration scale.
    LaunchedEffect(editing, animateEdit, view) {
        tilt.snapTo(0f)
        if (editing && animateEdit) {
            view.findViewTreeLifecycleOwner()?.lifecycle?.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                try {
                    repeat(2) {
                        tilt.animateTo(-0.7f, tween(100))
                        tilt.animateTo(0.7f, tween(160))
                    }
                    tilt.animateTo(0f, tween(100))
                } finally {
                    withContext(NonCancellable) { tilt.snapTo(0f) }
                }
            }
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().graphicsLayer {
            rotationZ = if (editing && animateEdit) tilt.value else 0f
        },
        color = if (current) colors.accent.copy(alpha = .09f) else colors.tile,
        shape = shape
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch)
                    .clip(shape)
                    .combinedClickable(
                        enabled = enabled,
                        role = Role.Button,
                        onClickLabel = if (editing) "Rename $title" else if (current)
                            "Return to $title" else "Switch to $title",
                        onLongClickLabel = "Edit sessions",
                        onClick = if (editing) onRename else onSelect,
                        onLongClick = if (editing) null else onEdit
                    )
                    .semantics {
                        stateDescription = listOfNotNull(
                            if (current) "Current session" else null,
                            if (finished) "Finished, retained output" else null,
                            if (editing) "Editing sessions" else null
                        ).joinToString(", ")
                        if (enabled && !editing) customActions = listOf(
                            CustomAccessibilityAction("Rename $title") { onRename(); true },
                            CustomAccessibilityAction(if (finished) "Remove $title output" else "Close $title") {
                                onClose(); true
                            }
                        )
                    }
                    .padding(horizontal = KlyroUiMetrics.spaceMd, vertical = KlyroUiMetrics.spaceSm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
            ) {
                Icon(painterResource(R.drawable.hugeicons_terminal), null,
                    tint = if (current) colors.accent else colors.muted,
                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                Column(Modifier.weight(1f)) {
                    Text(title, color = colors.foreground, maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    if (current || finished) Text(
                        when { current && finished -> "Current · Finished"; current -> "Current"; else -> "Finished" },
                        color = if (current) colors.accent else colors.muted,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
            if (editing) {
                IconButton(onClick = onRename, enabled = enabled) {
                    Icon(painterResource(R.drawable.hugeicons_pen_01), "Rename $title",
                        tint = if (enabled) colors.accent else colors.muted,
                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                }
                IconButton(onClick = onClose, enabled = enabled) {
                    Icon(painterResource(R.drawable.hugeicons_cancel_circle),
                        if (finished) "Remove $title output" else "Close $title",
                        tint = if (enabled) colors.danger else colors.muted,
                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                }
            } else {
                IconButton(onClick = onRename, enabled = enabled) {
                    Icon(painterResource(R.drawable.hugeicons_pen_01), "Rename $title",
                        tint = if (enabled) colors.foreground else colors.muted,
                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                }
            }
        }
    }
}
