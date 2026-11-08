package com.oryn.terminal.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
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
    activeModifiers: Set<String>,
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
    var cannotOpenPermissions by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf(KlyroBrandChoice.DEFAULT) }
    var brandSwitchError by remember { mutableStateOf(false) }
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(context) {
        selectedBrand = KlyroBrandChoice.selected(context)
    }
    val density = LocalDensity.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "development"
    }

    // Compact content stays fixed; long reports, large fonts and small screens retain scrolling.
    val needsScroll = KlyroSheetLayoutPolicy.needsScroll(
        rows = 6,
        screenHeightDp = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp,
        fontScale = density.fontScale
    )
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth()
                    .heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .imePadding()
                    .then(if (needsScroll) Modifier.verticalScroll(remember(section) { ScrollState(0) }) else Modifier)
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
                            Text("Launcher icon", fontWeight = FontWeight.SemiBold,
                                color = colors.foreground)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                            ) {
                                listOf(
                                    KlyroBrandChoice.DEFAULT to "B · Default",
                                    KlyroBrandChoice.ALTERNATIVE to "A · Animal"
                                ).forEach { (choice, title) ->
                                    Surface(
                                        modifier = Modifier.weight(1f)
                                            .heightIn(min = 96.dp)
                                            .clickable {
                                                if (selectedBrand != choice) {
                                                    brandSwitchError = !KlyroBrandChoice.select(context, choice)
                                                    selectedBrand = KlyroBrandChoice.selected(context)
                                                }
                                            }.semantics {
                                                contentDescription = "Launcher icon $title"
                                                selected = selectedBrand == choice
                                            },
                                        shape = RoundedCornerShape(KlyroUiMetrics.cardCorner),
                                        color = if (selectedBrand == choice)
                                            colors.accent.copy(alpha = .14f) else colors.tile,
                                        border = BorderStroke(1.dp,
                                            if (selectedBrand == choice) colors.accent else colors.muted.copy(alpha = .35f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                painterResource(
                                                    if (choice == KlyroBrandChoice.DEFAULT)
                                                        R.drawable.ic_klyro_brand_b
                                                    else R.drawable.ic_klyro_brand_a
                                                ),
                                                contentDescription = null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(58.dp)
                                            )
                                            Text(title, color = colors.foreground,
                                                style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                            if (brandSwitchError) {
                                Text("Icon switch failed. Both launcher entries were restored where possible; retry from Settings.",
                                    color = colors.danger,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Some launchers refresh home-screen icons after reopening the launcher.",
                                color = colors.muted, style = MaterialTheme.typography.labelSmall)
                            Text("App theme", fontWeight = FontWeight.SemiBold,
                                color = colors.foreground)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                            ) {
                                listOf("system" to "System", "light" to "Light",
                                    "dark" to "Dark").forEach { (mode, label) ->
                                    KlyroThemePreviewCard(
                                        mode = mode, label = label, active = themeMode == mode,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onThemeModeChange(mode) }
                                    )
                                }
                            }
                            Text("Monochrome", fontWeight = FontWeight.SemiBold,
                                color = colors.foreground)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                            ) {
                                listOf("mono-contrast" to "Contrast", "mono-black" to "OLED",
                                    "mono-paper" to "Paper").forEach { (mode, label) ->
                                    KlyroThemePreviewCard(
                                        mode = mode, label = label, active = themeMode == mode,
                                        modifier = Modifier.weight(1f),
                                        onClick = { onThemeModeChange(mode) }
                                    )
                                }
                            }
                            if (!themeMode.startsWith("mono-")) {
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
                                }
                            }
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
                                            if (fontSizeSp % 1f == 0f) "${fontSizeSp.toInt()} sp" else "$fontSizeSp sp",
                                            color = colors.accent, fontWeight = FontWeight.Bold
                                        )
                                        if (fontSizeSp != 13.5f) {
                                            TextButton(onClick = onResetFont) { Text("Reset") }
                                        }
                                    }
                                }
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
                    }
                    "keyboard" -> {
                        KlyroSectionHeading("Keyboard")
                        Text("Modifier status", color = colors.muted,
                            style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                        ) {
                            listOf("Ctrl", "Alt", "Shift", "Fn").forEach { label ->
                                val active = label in activeModifiers
                                Surface(
                                    modifier = Modifier.weight(1f).heightIn(
                                        min = KlyroUiMetrics.minTouch
                                    ).semantics {
                                        contentDescription = "$label modifier"
                                        stateDescription = if (active) "Active" else "Inactive"
                                    },
                                    color = if (active) colors.accent.copy(alpha = .17f) else colors.key,
                                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                    border = if (active) BorderStroke(
                                        KlyroUiMetrics.selectedBorder, colors.accent
                                    ) else null
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(label,
                                            color = if (active) colors.accent else colors.muted,
                                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                        Button(
                            onClick = onResetModifiers,
                            enabled = activeModifiers.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().heightIn(
                                min = KlyroUiMetrics.minTouch
                            ),
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            )
                        ) {
                            Text(if (activeModifiers.isEmpty()) "All modifiers released"
                                else "Release active modifiers")
                        }
                        Text("Hold a direction key for its extended action; swipe the shortcut row in landscape.",
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
                        SettingsRowLink("Export klyro-export.txt",
                            "Save ~/imports/klyro-export.txt using Android's picker.",
                            R.drawable.hugeicons_upload_01) {
                            onDismiss()
                            onExportFile()
                        }
                        Text("Android restricts the Download root. Use Import a file for downloads; SAF does not mount ~/storage/downloads.",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    "privacy" -> {
                        KlyroSectionHeading("Privacy")
                        Text("Terminal output may contain secrets. Use the ALL shortcut to select and copy it. Multiline pastes require confirmation; diagnostics exclude transcripts.",
                            style = MaterialTheme.typography.bodyMedium, color = colors.foreground)
                        Surface(color = colors.tile,
                            shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                            ) {
                                Icon(painterResource(R.drawable.hugeicons_shield_01),
                                    contentDescription = null, tint = colors.accent,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
                                Text("Shell files stay inside Klyro's private app storage.",
                                    modifier = Modifier.weight(1f),
                                    color = colors.foreground,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        SettingsRowLink("Android permissions",
                            "Review app access in Android system settings.",
                            R.drawable.hugeicons_settings_01) {
                            cannotOpenPermissions = !runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                )
                            }.isSuccess
                        }
                        if (cannotOpenPermissions) {
                            Text("Android settings cannot be opened on this device.",
                                color = colors.danger, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    "diagnostics" -> {
                        KlyroSectionHeading("Diagnostics", "Local checks; no commands run automatically.")
                        Surface(
                            color = colors.tile,
                            shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                        ) {
                            Column(Modifier.fillMaxWidth().heightIn(max = 260.dp)
                                .verticalScroll(rememberScrollState())) {
                                Text(
                                    diagnosticReport,
                                    modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                                    color = colors.foreground,
                                    fontFamily = FontFamily.Monospace,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)
                        ) {
                            OutlinedButton(
                                onClick = onCopyDiagnostic,
                                modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = .35f))
                            ) {
                                Icon(painterResource(R.drawable.hugeicons_clipboard_list),
                                    contentDescription = null,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                Text("Copy")
                            }
                            OutlinedButton(
                                onClick = onShareDiagnostic,
                                modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                                border = BorderStroke(1.dp, colors.accent.copy(alpha = .35f))
                            ) {
                                Icon(painterResource(R.drawable.hugeicons_upload_01),
                                    contentDescription = null,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                Text("Share")
                            }
                        }
                        Text("Review before sharing. Raw errors and terminal transcripts are excluded.",
                            color = colors.muted, style = MaterialTheme.typography.labelSmall)
                    }
                    "about" -> {
                        KlyroSectionHeading("About Klyro Shell")
                        Surface(shape = RoundedCornerShape(KlyroUiMetrics.cardCorner), color = colors.tile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                                ) {
                                    Box(
                                        modifier = Modifier.size(64.dp).background(
                                            Color(0xFF0C0B13),
                                            RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                        ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painterResource(
                                                if (selectedBrand == KlyroBrandChoice.ALTERNATIVE)
                                                    R.drawable.ic_klyro_brand_a
                                                else R.drawable.ic_klyro_brand_b
                                            ),
                                            contentDescription = "Klyro Shell logo",
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(56.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("Klyro Shell", fontWeight = FontWeight.Bold,
                                            color = colors.foreground)
                                        Text("Version $version · ARM64", color = colors.accent,
                                            style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                Text("Termux TerminalView / TerminalSession",
                                    color = colors.foreground,
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Private ARM64 Linux runtime · stored in the app sandbox",
                                    color = colors.muted,
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
