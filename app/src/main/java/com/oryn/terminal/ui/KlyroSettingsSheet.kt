package com.oryn.terminal.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.oryn.terminal.R
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.Modifier
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
    var showHugeiconsLicense by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "development"
    }

    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth()
                    .heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .imePadding()
                    .verticalScroll(remember(section) { ScrollState(0) })
                    .padding(horizontal = KlyroUiMetrics.sheetGutter)
                    .padding(bottom = KlyroUiMetrics.spaceXl),
                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.bentoGridGap)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (section == "home") {
                        KlyroSheetTitle("Settings")
                    } else {
                        TextButton(onClick = { section = "home" }) {
                            Icon(painterResource(R.drawable.hugeicons_arrow_left_01),
                                contentDescription = null, tint = colors.accent,
                                modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                            Text("Settings")
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
                when (section) {
                    "home" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.bentoGridGap)) {
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Appearance", "Themes & type", R.drawable.hugeicons_palette) { section = "appearance" }
                            }
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Keyboard", "Keys & modifiers", R.drawable.ic_klyro_keyboard_keys) {
                                    section = "keyboard"
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.bentoGridGap)) {
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Storage", "Files & folders", R.drawable.hugeicons_folder_01) {
                                    section = "files"
                                }
                            }
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Privacy", "Local controls", R.drawable.hugeicons_shield_01) {
                                    section = "privacy"
                                }
                            }
                        }
                        SettingsRowLink("Diagnostics", "Environment checks and reports",
                            R.drawable.hugeicons_pulse_rectangle_01) {
                            section = "diagnostics"
                        }
                        SettingsRowLink("About", "Build information and licences",
                            R.drawable.hugeicons_information_square) {
                            section = "about"
                        }
                    }
                    "appearance" -> {
                        KlyroSectionHeading("Appearance")
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
                                        Triple("mono-contrast", "Contrast", R.drawable.hugeicons_contrast),
                                        Triple("mono-black", "OLED", R.drawable.hugeicons_moon_01),
                                        Triple("mono-paper", "Paper", R.drawable.hugeicons_file_01)
                                    ).forEach { (id, label, icon) ->
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
                                                horizontalArrangement = Arrangement.spacedBy(
                                                    KlyroUiMetrics.spaceXs, Alignment.CenterHorizontally),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(painterResource(icon), contentDescription = null,
                                                    tint = colors.foreground,
                                                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                                Text(label, color = colors.foreground,
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
                                                    Icon(painterResource(R.drawable.hugeicons_checkmark_circle_01),
                                                        contentDescription = null, tint = swatch.onAccent,
                                                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
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
                                        Icon(painterResource(R.drawable.hugeicons_plus_sign),
                                            contentDescription = null, tint = colors.accent,
                                            modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                    }
                                }
                                val chosen = KlyroColors.selectedOption(accentId)?.label
                                    ?: if (accentId.startsWith("custom:")) "Custom" else "Lime"
                                Text(chosen, color = colors.muted,
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
                                        val previewTypeface = remember(context, fontFamilyId) {
                                            KlyroTerminalFonts.load(context, fontFamilyId)
                                        }
                                        AndroidView(
                                            factory = { ctx ->
                                                TextView(ctx).apply {
                                                    text = "Aa  The quick brown fox"
                                                    setSingleLine(true)
                                                    ellipsize = android.text.TextUtils.TruncateAt.END
                                                    includeFontPadding = false
                                                }
                                            },
                                            update = { view ->
                                                view.typeface = previewTypeface
                                                view.setTextColor(colors.foreground.toArgb())
                                                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        KlyroThinSlider(
                                            value = fontSizeSp.coerceIn(9f, 24f),
                                            onValueChange = { onFontChange(it - fontSizeSp) },
                                            valueRange = 9f..24f,
                                            steps = 29,
                                            modifier = Modifier.fillMaxWidth().semantics {
                                                contentDescription = "Terminal font size"
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
                        KlyroSectionHeading("Keyboard")
                        SettingsRowLink("Release modifiers",
                            "Clear Ctrl, Alt, Shift and Fn if they remain latched.",
                            R.drawable.ic_klyro_keyboard_keys) {
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
                        KlyroSectionHeading("Files & storage")
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
                            "Copy up to 30 files into ~/storage-imports.",
                            R.drawable.hugeicons_folder_01) {
                            onDismiss()
                            onImportStorageFolder()
                        }
                        SettingsRowLink("Import a file",
                            "Choose a document through Android's file picker.",
                            R.drawable.hugeicons_download_01) {
                            onDismiss()
                            onImportFile()
                        }
                        SettingsRowLink("Export a file",
                            "Save a file using Android's document picker.",
                            R.drawable.hugeicons_upload_01) {
                            onDismiss()
                            onExportFile()
                        }
                        Text("Android restricts the Download root. Use Import a file for downloads; SAF does not mount ~/storage/downloads.",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    "privacy" -> {
                        KlyroSectionHeading("Privacy")
                        Text("Transcripts can contain secrets. Use Select output in the terminal menu to copy text; multiline pastes require confirmation.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                    }
                    "diagnostics" -> {
                        KlyroSectionHeading("Diagnostics", "Local checks; no commands run automatically.")
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
                        KlyroSectionHeading("About Klyro Shell")
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
                        SettingsRowLink(
                            "Hugeicons licence", "MIT · vector icon set",
                            R.drawable.hugeicons_information_square
                        ) { showHugeiconsLicense = true }
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
        if (showHugeiconsLicense) {
            KlyroLicenseDialog("Hugeicons · MIT", "licenses/hugeicons-MIT.txt") {
                showHugeiconsLicense = false
            }
        }
        if (showFontLicense) {
            KlyroLicenseDialog("JetBrains Mono · OFL 1.1", "fonts/OFL.txt") {
                showFontLicense = false
            }
        }
    }
}

@Composable
private fun SettingsRowLink(title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    val colors = LocalKlyroColors.current
    Surface(
        color = colors.tile,
        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
    ) {
        KlyroActionRow(
            label = title, subtitle = subtitle, icon = icon, onClick = onClick,
            trailing = true, iconWell = true
        )
    }
}

@Composable
private fun SettingsBentoTile(
    title: String,
    subtitle: String,
    icon: Int,
    onClick: () -> Unit
) {
    val colors = LocalKlyroColors.current
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.bentoMinHeight)
            .clickable(role = Role.Button, onClickLabel = "Open " + title, onClick = onClick),
        shape = RoundedCornerShape(KlyroUiMetrics.cardCorner),
        color = colors.tile
    ) {
        Column(Modifier.padding(KlyroUiMetrics.bentoInnerPadding),
            verticalArrangement = Arrangement.SpaceBetween) {
            Icon(painterResource(icon), contentDescription = null,
                tint = colors.accent, modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
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
