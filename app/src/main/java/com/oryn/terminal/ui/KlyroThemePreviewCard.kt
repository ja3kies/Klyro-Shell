package com.oryn.terminal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.oryn.terminal.ui.motion.klyroPressScale

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The actual theme palettes are represented by miniature terminal surfaces,
 * rather than by icon-only action buttons that cannot communicate a theme.
 */
/** Preview palettes use the exact same generator as Klyro's active themes. */
internal object KlyroThemePreviewPalette {
    fun forMode(mode: String, accentId: String): KlyroColors = when (mode) {
        "system", "light" -> KlyroColors.create(false, accentId)
        "dark" -> KlyroColors.create(true, accentId)
        "mono-black", "mono-paper", "mono-contrast" -> KlyroColors.monochrome(mode)
        else -> KlyroColors.create(false, accentId)
    }

    fun systemPair(accentId: String): Pair<KlyroColors, KlyroColors> =
        KlyroColors.create(false, accentId) to KlyroColors.create(true, accentId)
}

@Composable
internal fun KlyroThemePreviewCard(
    mode: String,
    label: String,
    active: Boolean,
    accentId: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val current = LocalKlyroColors.current
    val labelColor by animateColorAsState(if (active) current.accent else current.foreground,
        tween(220), label = "theme-preview-label")
    // Generate previews from the selected accent instead of a fixed Lime palette.
    val preview = KlyroThemePreviewPalette.forMode(mode, accentId)
    val (lightPreview, darkPreview) = KlyroThemePreviewPalette.systemPair(accentId)
    val shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
    val pressSource = remember { MutableInteractionSource() }
    val pressed by pressSource.collectIsPressedAsState()
    Surface(
        modifier = modifier
            .heightIn(min = 78.dp)
            .clickable(interactionSource = pressSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                role = Role.RadioButton, onClickLabel = "Use $label theme", onClick = onClick)
            .semantics { selected = active }
            .testTag("theme-preview-$mode"),
        shape = shape,
        color = androidx.compose.ui.graphics.Color.Transparent,
        border = null
    ) {
        Column(
            modifier = Modifier.klyroPressScale(pressed).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(40.dp)
                    .then(if (active) Modifier.border(KlyroUiMetrics.selectedBorder, current.accent, RoundedCornerShape(8.dp)) else Modifier)
                    .padding(2.dp).clearAndSetSemantics { },
                contentAlignment = Alignment.Center
            ) {
                if (mode == "system") {
                    // One preview for BOTH device appearances, never inherit a stale mono preview.
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        listOf(lightPreview, darkPreview).forEach { palette ->
                            Box(
                                modifier = Modifier.weight(1f).fillMaxHeight()
                                    .background(palette.terminal)
                                    .padding(start = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("›_", color = palette.terminalPrompt, fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                            .background(preview.terminal, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("› _", color = preview.terminalPrompt, fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(label, modifier = Modifier.fillMaxWidth(),
                color = labelColor,
                fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
