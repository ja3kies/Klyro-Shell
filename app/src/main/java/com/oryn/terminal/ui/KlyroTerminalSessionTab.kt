package com.oryn.terminal.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One terminal session tab: selection is the click; management is a separate action. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun KlyroTerminalSessionTab(
    title: String,
    workspaceName: String,
    active: Boolean,
    activityTag: String?,
    activityAnnouncement: String,
    showUnreadOutput: Boolean,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onManage: () -> Unit
) {
    val colors = LocalKlyroColors.current
    val safeTitle = title.ifBlank { "Session" }
    val contentInk = if (active) colors.onAccent else colors.foreground
    val interactions = remember { MutableInteractionSource() }
    val pressed = interactions.collectIsPressedAsState()
    val focused = interactions.collectIsFocusedAsState()
    Box(
        modifier = modifier
            .height(48.dp)
            .widthIn(min = KlyroUiMetrics.minTouch)
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                interactionSource = interactions,
                indication = null,
                onClickLabel = "Select session",
                onLongClickLabel = "Manage session",
                onClick = onSelect,
                onLongClick = onManage
            )
            .semantics {
                role = Role.Tab
                selected = active
                contentDescription = "$safeTitle, project $workspaceName"
                stateDescription = buildString {
                    append(if (active) "Selected terminal" else "Open terminal")
                    append(", ")
                    append(activityAnnouncement)
                    if (showUnreadOutput && !active) append(", New terminal output")
                }
                customActions = listOf(
                    CustomAccessibilityAction("Manage session") {
                        onManage()
                        true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.height(36.dp).widthIn(max = 140.dp).terminalPressFace(pressed.value),
            color = if (active) colors.accent else colors.key,
            contentColor = contentInk,
            border = if (focused.value) BorderStroke(2.dp, colors.foreground) else null,
            shape = RoundedCornerShape(19.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showUnreadOutput && !active) {
                    Box(
                        Modifier.size(7.dp).background(colors.accent, CircleShape)
                    )
                }
                Text(
                    safeTitle,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (activityTag != null) {
                    Box(
                        modifier = Modifier.background(
                            if (active) colors.onAccent.copy(alpha = 0.14f)
                            else colors.accent.copy(alpha = 0.12f),
                            RoundedCornerShape(6.dp)
                        ).padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            activityTag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (active) colors.onAccent else if (activityTag == "JOB")
                                colors.readyInk else colors.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
