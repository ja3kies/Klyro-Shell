package com.oryn.terminal.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.oryn.terminal.R
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity

/**
 * A settings sheet deliberately isolated from TerminalView: it never restarts
 * the shell and all file actions remain delegated to Android's SAF handlers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroSettingsSheet(
    initialSection: String,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    accentId: String,
    onAccentIdChange: (String) -> Unit,
    fontSizeSp: Float,
    fontFamilyId: String,
    onFontFamilyChange: (String) -> Unit,
    onFontChange: (Float) -> Unit,
    onResetFont: () -> Unit,
    onResetModifiers: () -> Unit,
    onSetupStorage: () -> Unit,
    onImportStorageFolder: () -> Unit,
    onImportFile: () -> Unit,
    onExportFile: () -> Unit,
    diagnosticReport: String,
    onRecheckEnvironment: () -> Unit,
    onCopyDiagnostic: () -> Unit,
    onShareDiagnostic: () -> Unit,
    onDismiss: () -> Unit
) {
    var section by remember(initialSection) { mutableStateOf(initialSection) }
    var showFontLicense by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "development"
    }

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
            Column(
                modifier = Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth()
                    .heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.bentoGridGap)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (section == "home") {
                        Text("Settings", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold)
                    } else {
                        TextButton(onClick = { section = "home" }) { Text("‹  Settings") }
                    }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                when (section) {
                    "home" -> {
                        Text("Terminal settings and Android integration",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.bentoGridGap)) {
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Appearance", "Themes & type", "Aa",
                                    prominent = true) { section = "appearance" }
                            }
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Keyboard", "Keys & modifiers", "⌨") {
                                    section = "keyboard"
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.bentoGridGap)) {
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Storage", "Files & folders", "▣") {
                                    section = "files"
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Privacy", "Local controls", "◇") {
                                    section = "privacy"
                                }
                            }
                        }
                        SettingsRowLink("Diagnostics", "Runtime health and shareable report",
                            R.drawable.hugeicons_information_square) {
                            section = "diagnostics"
                        }
                        SettingsRowLink("About", "Build information and licences",
                            R.drawable.hugeicons_information_square) {
                            section = "about"
                        }
                    }
                    "appearance" -> {
                        SectionHeading("Appearance", "Changes apply to the terminal immediately.")
                        Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.sectionGap)) {
                            Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                Text("App theme", fontWeight = FontWeight.SemiBold,
                                    color = colors.foreground)
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(colors.key, RoundedCornerShape(KlyroUiMetrics.controlCorner))
                                        .padding(KlyroUiMetrics.spaceXs),
                                    horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)
                                ) {
                                    listOf(
                                        "system" to "System", "light" to "Light", "dark" to "Dark"
                                    ).forEach { (mode, title) ->
                                        val active = themeMode == mode
                                        Surface(
                                            modifier = Modifier.weight(1f).heightIn(
                                                min = KlyroUiMetrics.optionRow
                                            ).clickable(role = Role.RadioButton,
                                                onClickLabel = "Use " + title + " theme") {
                                                onThemeModeChange(mode)
                                            }.semantics { selected = active },
                                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                            color = if (active) colors.accent else colors.key,
                                            contentColor = if (active) colors.onAccent else colors.foreground
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    title,
                                                    modifier = Modifier.padding(vertical = KlyroUiMetrics.spaceMd),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                Text("Monochrome", fontWeight = FontWeight.SemiBold,
                                    color = colors.foreground)
                                Row(horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                    listOf(
                                        Triple("mono-contrast", "Contrast", "◩"),
                                        Triple("mono-black", "OLED", "●"),
                                        Triple("mono-paper", "Paper", "○")
                                    ).forEach { (id, label, glyph) ->
                                        val active = themeMode == id
                                        Surface(
                                            modifier = Modifier.weight(1f)
                                                .heightIn(min = KlyroUiMetrics.optionRow)
                                                .clickable(role = Role.RadioButton,
                                                    onClickLabel = "Use " + label + " monochrome theme") {
                                                    onThemeModeChange(id)
                                                }.semantics { selected = active },
                                            color = if (active) colors.accent.copy(alpha = .15f) else colors.tile,
                                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                            border = if (active) BorderStroke(
                                                KlyroUiMetrics.selectedBorder, colors.accent
                                            ) else null
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth()
                                                    .padding(horizontal = 6.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(glyph + " " + label,
                                                    color = colors.foreground,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (active) FontWeight.Bold
                                                        else FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                Text("Accent", color = colors.foreground,
                                    fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KlyroColors.primary.forEach { preset ->
                                        val swatch = KlyroColors.create(colors.dark, preset.id)
                                        val active = accentId == preset.id
                                        Box(
                                            modifier = Modifier.size(KlyroUiMetrics.minTouch)
                                                .clickable(role = Role.RadioButton,
                                                    onClickLabel = "Select " + preset.label + " accent") {
                                                    onAccentIdChange(preset.id)
                                                }
                                                .semantics {
                                                    selected = active
                                                    contentDescription = preset.label + " accent"
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier.size(KlyroUiMetrics.accentSwatch)
                                                    .background(swatch.accent, androidx.compose.foundation.shape.CircleShape)
                                                    .then(if (active) Modifier.border(
                                                        KlyroUiMetrics.selectedBorder,
                                                        colors.foreground,
                                                        androidx.compose.foundation.shape.CircleShape
                                                    ) else Modifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (active) {
                                                    Text("✓", color = swatch.onAccent,
                                                        fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    Box(
                                        modifier = Modifier.size(KlyroUiMetrics.minTouch)
                                            .background(colors.key, androidx.compose.foundation.shape.CircleShape)
                                            .clickable(role = Role.Button,
                                                onClickLabel = "Choose custom accent") {
                                                showColorPicker = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", color = colors.accent,
                                            style = MaterialTheme.typography.titleLarge)
                                    }
                                }
                                val chosen = KlyroColors.selectedOption(accentId)?.label
                                    ?: if (accentId.startsWith("custom:")) "Custom" else "Lime"
                                Text("Selected: " + chosen, color = colors.muted,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            HorizontalDivider(color = colors.muted.copy(alpha = 0.18f))
                            Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                Text("Terminal font", color = colors.foreground,
                                    fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                                ) {
                                    KlyroTerminalFonts.options.forEach { option ->
                                        val active = fontFamilyId == option.id
                                        Surface(
                                            modifier = Modifier.weight(1f)
                                                .heightIn(min = KlyroUiMetrics.optionRow)
                                                .clickable(role = Role.RadioButton,
                                                    onClickLabel = "Use " + option.label) {
                                                    onFontFamilyChange(option.id)
                                                }.semantics { selected = active },
                                            color = if (active) colors.accent.copy(alpha = .14f)
                                                else colors.key,
                                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                            border = if (active) BorderStroke(
                                                KlyroUiMetrics.selectedBorder, colors.accent
                                            ) else null
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(option.label, modifier = Modifier.padding(
                                                    horizontal = KlyroUiMetrics.spaceSm,
                                                    vertical = KlyroUiMetrics.spaceMd
                                                ), color = colors.foreground,
                                                    fontWeight = if (active) FontWeight.SemiBold
                                                        else FontWeight.Normal,
                                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Font size", color = colors.foreground,
                                        fontWeight = FontWeight.SemiBold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            if (fontSizeSp % 1f == 0f) fontSizeSp.toInt().toString() + " sp"
                                            else fontSizeSp.toString() + " sp",
                                            color = colors.accent,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (fontSizeSp != 13.5f) {
                                            TextButton(onClick = onResetFont) { Text("Reset") }
                                        }
                                    }
                                }
                                Surface(color = colors.tile,
                                    shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                                    Column(Modifier.fillMaxWidth().padding(
                                        horizontal = KlyroUiMetrics.spaceLg,
                                        vertical = KlyroUiMetrics.spaceSm)) {
                                        Text("Aa  The quick brown fox",
                                            color = colors.foreground,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = fontSizeSp.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis)
                                        Slider(
                                    value = fontSizeSp.coerceIn(9f, 24f),
                                    onValueChange = { onFontChange(it - fontSizeSp) },
                                    valueRange = 9f..24f,
                                    steps = 29,
                                    modifier = Modifier.fillMaxWidth().semantics {
                                        contentDescription = "Terminal font size"
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = colors.accent,
                                        inactiveTrackColor = colors.key
                                    ),
                                    thumb = {
                                        Box(
                                            Modifier.size(KlyroUiMetrics.iosSliderThumb)
                                                .background(Color.White,
                                                    androidx.compose.foundation.shape.CircleShape)
                                                .border(KlyroUiMetrics.iosSliderBorder,
                                                    colors.muted.copy(alpha = .35f),
                                                    androidx.compose.foundation.shape.CircleShape)
                                        )
                                    },
                                    track = { state ->
                                        Box(Modifier.fillMaxWidth()
                                            .height(KlyroUiMetrics.iosSliderTrack)
                                            .background(colors.key,
                                                androidx.compose.foundation.shape.CircleShape)) {
                                            Box(Modifier.fillMaxWidth(
                                                ((state.value - 9f) / 15f).coerceIn(0f, 1f))
                                                .height(KlyroUiMetrics.iosSliderTrack)
                                                .background(colors.accent,
                                                    androidx.compose.foundation.shape.CircleShape))
                                        }
                                    }
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("9 sp", color = colors.muted,
                                        style = MaterialTheme.typography.labelSmall)
                                    Text("24 sp", color = colors.muted,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    "keyboard" -> {
                        SectionHeading("Keyboard", "Controls for interactive terminal programs.")
                        SettingsRowLink("Release modifiers",
                            "Clear Ctrl, Alt, Shift and Fn if they remain latched.",
                            R.drawable.hugeicons_keyboard) {
                            onResetModifiers()
                        }
                        Text("Swipe the shortcuts row in compact layouts.",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    "files" -> {
                        val stored = context.getSharedPreferences(
                            "klyro_storage", Context.MODE_PRIVATE)
                        val savedTrees = stored.getStringSet("tree_uris", emptySet()).orEmpty() +
                            listOfNotNull(stored.getString("tree_uri", null))
                        val authorizedCount = context.contentResolver.persistedUriPermissions
                            .count { it.isReadPermission && it.uri.toString() in savedTrees }
                        val authorized = authorizedCount > 0
                        SectionHeading("Files & storage",
                            "Android folder access is limited to what you authorize.")
                        Surface(
                            color = if (authorized) colors.readyInk.copy(alpha = .10f) else colors.tile,
                            shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(KlyroUiMetrics.spaceMd),
                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painterResource(R.drawable.hugeicons_folder_01),
                                    contentDescription = null,
                                    tint = if (authorized) colors.readyInk else colors.muted,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphMedium)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(if (authorized) "$authorizedCount folders connected" else "No folder connected",
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.foreground)
                                    Text("Klyro SAF access · no Unix mount",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted)
                                }
                            }
                        }
                        Button(
                            onClick = { onDismiss(); onSetupStorage() },
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(min = KlyroUiMetrics.optionRow),
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent, contentColor = colors.onAccent
                            )
                        ) { Text(if (authorized) "Add another folder" else "Choose a folder") }
                        SettingsRowLink("Import from folder",
                            "Copy selected folder files into ~/storage-imports.",
                            R.drawable.hugeicons_folder_01) {
                            onDismiss()
                            onImportStorageFolder()
                        }
                        SettingsRowLink("Import a file",
                            "Choose a document through Android's file picker.",
                            glyph = "↓") {
                            onDismiss()
                            onImportFile()
                        }
                        SettingsRowLink("Export a file",
                            "Save a file using Android's document picker.",
                            glyph = "↑") {
                            onDismiss()
                            onExportFile()
                        }
                        Text("Run klyro-setup-storage or setup-storage in Bash. " +
                            "Each new SAF grant preserves previous folders. Android restricts selecting the Download root; use Import a file for individual downloads.",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    "privacy" -> {
                        SectionHeading("Privacy", "Terminal output stays on your device by default.")
                        Text("Copy output using the terminal's ALL key or native text selection. " +
                            "Transcripts may contain secrets. Multiline pastes require confirmation.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                    }
                    "diagnostics" -> {
                        SectionHeading("Diagnostics",
                            "A local status snapshot. No shell commands are run automatically.")
                        Surface(
                            color = colors.tile,
                            shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                        ) {
                            Text(
                                diagnosticReport,
                                modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                                color = colors.foreground,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = onRecheckEnvironment,
                            modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch),
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            )
                        ) { Text("Recheck environment") }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = onCopyDiagnostic) { Text("Copy report") }
                            TextButton(onClick = onShareDiagnostic) { Text("Share report") }
                        }
                        Text("Review before sharing. Raw errors and terminal transcripts are excluded.",
                            color = colors.muted, style = MaterialTheme.typography.labelSmall)
                    }
                    "about" -> {
                        SectionHeading("About Klyro Shell", "Private terminal environment for Android.")
                        Surface(shape = RoundedCornerShape(KlyroUiMetrics.cardCorner), color = colors.tile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Version $version", fontWeight = FontWeight.SemiBold)
                                Text("Terminal engine: Termux TerminalView / TerminalSession",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Linux runtime: isolated ARM64 environment", color = colors.muted,
                                    style = MaterialTheme.typography.bodySmall)

                            }
                        }
                        SettingsRowLink(
                            "JetBrains Mono licence",
                            "SIL Open Font License 1.1 · included in this APK",
                            R.drawable.hugeicons_information_square
                        ) { showFontLicense = true }
                    }
                }
            }
        }
        if (showColorPicker) {
            val initialColor = remember(accentId) {
                val hex = if (accentId.startsWith("custom:#")) accentId.removePrefix("custom:")
                    else (KlyroColors.selectedOption(accentId)?.sourceHex ?: "#BDCC64")
                AndroidColor.parseColor(hex)
            }
            KlyroColorPickerDialog(
                initialArgb = initialColor,
                onApply = { onAccentIdChange(it); showColorPicker = false },
                onDismiss = { showColorPicker = false }
            )
        }
        if (showFontLicense) {
            val licence = remember(context) {
                runCatching {
                    context.assets.open("fonts/OFL.txt").bufferedReader().use { it.readText() }
                }.getOrElse { "Licence unavailable in this build." }
            }
            AlertDialog(
                onDismissRequest = { showFontLicense = false },
                containerColor = colors.sheet,
                shape = RoundedCornerShape(KlyroUiMetrics.modalCorner),
                title = { Text("JetBrains Mono · OFL 1.1") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) { Text(licence, style = MaterialTheme.typography.bodySmall) }
                },
                confirmButton = {
                    TextButton(onClick = { showFontLicense = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    val colors = LocalKlyroColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SettingsRowLink(title: String, subtitle: String, icon: Int? = null, glyph: String = "Aa", onClick: () -> Unit) {
    val colors = LocalKlyroColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
            .clickable(role = Role.Button, onClickLabel = title, onClick = onClick),
        color = colors.tile,
        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(KlyroUiMetrics.iconWell)
                    .background(colors.accent.copy(alpha = .09f),
                        RoundedCornerShape(KlyroUiMetrics.controlCorner)),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Icon(painterResource(icon), null, tint = colors.accent,
                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                } else {
                    Text(glyph, fontWeight = FontWeight.SemiBold,
                        color = colors.accent, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, color = colors.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
            Text("›", color = colors.accent, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun SettingsBentoTile(
    title: String,
    subtitle: String,
    glyph: String,
    prominent: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalKlyroColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.bentoMinHeight)
            .clickable(role = Role.Button, onClickLabel = "Open " + title, onClick = onClick),
        shape = RoundedCornerShape(KlyroUiMetrics.cardCorner),
        color = if (prominent) colors.accent.copy(alpha = .13f) else colors.tile,
        border = if (prominent) BorderStroke(1.dp, colors.accent.copy(alpha = .35f)) else null
    ) {
        Column(Modifier.padding(KlyroUiMetrics.bentoInnerPadding),
            verticalArrangement = Arrangement.SpaceBetween) {
            Text(glyph, color = colors.accent, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold)
            Column {
                Text(title, color = colors.foreground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = colors.muted,
                    style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
