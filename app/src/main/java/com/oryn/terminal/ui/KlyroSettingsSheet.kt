package com.oryn.terminal.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

/**
 * A settings sheet deliberately isolated from TerminalView: it never restarts
 * the shell and all file actions remain delegated to Android's SAF handlers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    verifyingRuntime: Boolean = false,
    onCopyDiagnostic: () -> Unit,
    onShareDiagnostic: () -> Unit,
    onDismiss: () -> Unit
) {
    var section by rememberSaveable(initialSection) {
        mutableStateOf(initialSection.takeIf {
            it in setOf("home", "appearance", "keyboard", "files",
                "privacy", "diagnostics", "freeze", "about")
        } ?: "home")
    }
    DisposableEffect(Unit) {
        com.oryn.terminal.diagnostics.FreezeRecorder.settingsVisible.value = true
        onDispose { com.oryn.terminal.diagnostics.FreezeRecorder.settingsVisible.value = false }
    }
    val requestedReports by com.oryn.terminal.diagnostics.FreezeRecorder.openReports.collectAsState()
    LaunchedEffect(requestedReports) {
        if (requestedReports) {
            section = "freeze"
            com.oryn.terminal.diagnostics.FreezeRecorder.openReports.value = false
        }
    }
    var showFontLicense by rememberSaveable { mutableStateOf(false) }
    var showHugeiconsLicense by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    var showMonochromeThemes by rememberSaveable { mutableStateOf(themeMode.startsWith("mono-")) }
    var cannotOpenPermissions by rememberSaveable { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf(KlyroBrandChoice.DEFAULT) }
    var brandSwitchError by rememberSaveable { mutableStateOf(false) }
    var brandReady by remember { mutableStateOf(false) }
    var brandChecking by remember { mutableStateOf(true) }
    var brandRevision by remember { mutableStateOf(0) }
    var brandUpdating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    var authorizedFolderCount by remember { mutableStateOf(-1) }
    var folderRevision by remember { mutableStateOf(0) }
    LaunchedEffect(context, section, folderRevision) {
        if (section == "files") {
            authorizedFolderCount = -1
            authorizedFolderCount = withContext(Dispatchers.IO) {
                runCatching {
                    val stored = context.getSharedPreferences(
                        "klyro_storage", Context.MODE_PRIVATE
                    )
                    val savedTrees = stored.getStringSet("tree_uris", emptySet()).orEmpty() +
                        listOfNotNull(stored.getString("tree_uri", null))
                    context.contentResolver.persistedUriPermissions.count {
                        it.isReadPermission && it.uri.toString() in savedTrees
                    }
                }.getOrDefault(-2)
            }
        }
    }
    LaunchedEffect(context, section, brandRevision) {
        if (section != "appearance") return@LaunchedEffect
        // Launcher alias verification calls PackageManager across Binder.
        // Never block the Settings opening frame on this query.
        brandChecking = true
        brandReady = false
        val actual = withContext(Dispatchers.IO) {
            KlyroBrandChoice.confirmedSelection(context)
        }
        if (actual != null) selectedBrand = actual
        brandReady = actual != null
        brandSwitchError = actual == null
        brandChecking = false
    }
    fun navigateBack() {
        when {
            showColorPicker -> showColorPicker = false
            showFontLicense -> showFontLicense = false
            showHugeiconsLicense -> showHugeiconsLicense = false
            cannotOpenPermissions -> cannotOpenPermissions = false
            section == "freeze" -> section = "diagnostics"
            section != "home" -> section = "home"
            else -> onDismiss()
        }
    }
    // System Back is an external dismissal; the labelled Settings button navigates internally.
    val canDismissSheet = !showColorPicker && !showFontLicense &&
        !showHugeiconsLicense && !brandUpdating
    BackHandler(enabled = canDismissSheet) { onDismiss() }
    val version = com.oryn.terminal.BuildConfig.VERSION_NAME
    LaunchedEffect(section) {
        com.oryn.terminal.diagnostics.FreezeRecorder.event(com.oryn.terminal.diagnostics.FreezeEvents.Code.SETTINGS,
            listOf("home", "appearance", "keyboard", "files", "privacy", "diagnostics", "freeze", "about").indexOf(section).toLong())
    }

    // Use actual measured overflow instead of guessing page height from row counts.
    KlyroModalTheme {
        KlyroSheet(onDismissRequest = onDismiss, dismissEnabled = canDismissSheet) {
            Column(
                modifier = Modifier.widthIn(max = KlyroUiMetrics.sheetContentWidth)
                    .fillMaxWidth()
                    .heightIn(max = KlyroUiMetrics.sheetContentMax)
                    .imePadding()
                    // Keep bottom actions above gesture navigation. On Pixel 9
                    // the Diagnostics recheck button was partly cropped.
                    .navigationBarsPadding()
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
                        KlyroSheetTitle("Settings", modifier = Modifier.weight(1f))
                    } else {
                        TextButton(
                            onClick = ::navigateBack,
                            modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.hugeicons_arrow_left_01),
                                    contentDescription = null, tint = colors.muted,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                Text(if (section == "freeze") "Diagnostics" else "Settings")
                            }
                        }
                    }
                    TextButton(onClick = onDismiss, enabled = canDismissSheet,
                        modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)) {
                        Text("Close")
                    }
                }
                if (section == "appearance") KlyroSectionHeading("Appearance")
                // Keep Back and Close reachable while large Settings sections scroll.
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(remember(section) { ScrollState(0) }),
                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.bentoGridGap)
                ) {
                when (section) {
                    "home" -> {
                        KlyroSettingsChoiceGrid(columns = 2, selectionGroup = false) {
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Appearance", "Themes & type", R.drawable.hugeicons_palette) { section = "appearance" }
                            }
                            Box(Modifier.weight(1f)) {
                                SettingsBentoTile("Keyboard", "Keys & modifiers", R.drawable.ic_klyro_keyboard_keys) {
                                    section = "keyboard"
                                }
                            }
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
                        val appearanceInlineWidth = 240.dp
                        Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceLg)) {
                            KlyroAppearanceGroup("App theme") {
                                KlyroSettingsChoiceGrid(columns = 3, minimumInlineWidth = appearanceInlineWidth) {
                                    listOf("system" to "System", "light" to "Light",
                                        "dark" to "Dark").forEach { (mode, label) ->
                                        KlyroThemePreviewCard(
                                            mode = mode, label = label, active = themeMode == mode,
                                            accentId = accentId,
                                            modifier = Modifier.weight(1f),
                                            onClick = { onThemeModeChange(mode) }
                                        )
                                    }
                                }
                                val selectedMonochrome = when (themeMode) {
                                    "mono-contrast" -> "Contrast"
                                    "mono-black" -> "OLED"
                                    "mono-paper" -> "Paper"
                                    else -> null
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth()
                                        .heightIn(min = KlyroUiMetrics.minTouch)
                                        .clickable(
                                            role = Role.Button,
                                            onClickLabel = if (showMonochromeThemes)
                                                "Hide monochrome themes" else "Show monochrome themes"
                                        ) { showMonochromeThemes = !showMonochromeThemes }
                                        .semantics {
                                            contentDescription = "Monochrome themes"
                                            stateDescription = (if (showMonochromeThemes) "Expanded" else "Collapsed") +
                                                (selectedMonochrome?.let { ", $it selected" } ?: "")
                                        },
                                    color = colors.sheet,
                                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(
                                            horizontal = KlyroUiMetrics.spaceMd
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            selectedMonochrome?.let { "Monochrome · $it" }
                                                ?: "Monochrome themes",
                                            modifier = Modifier.weight(1f),
                                            color = colors.foreground,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Icon(
                                            painterResource(R.drawable.hugeicons_chevron_right),
                                            contentDescription = null,
                                            tint = colors.muted,
                                            modifier = Modifier.size(KlyroUiMetrics.glyphSmall)
                                                .rotate(if (showMonochromeThemes) 90f else 0f)
                                        )
                                    }
                                }
                                if (showMonochromeThemes) {
                                    KlyroSettingsChoiceGrid(columns = 3, minimumInlineWidth = appearanceInlineWidth) {
                                        listOf("mono-contrast" to "Contrast", "mono-black" to "OLED",
                                            "mono-paper" to "Paper").forEach { (mode, label) ->
                                            KlyroThemePreviewCard(
                                                mode = mode, label = label, active = themeMode == mode,
                                                accentId = accentId,
                                                modifier = Modifier.weight(1f),
                                                onClick = { onThemeModeChange(mode) }
                                            )
                                        }
                                    }
                                }
                                if (!themeMode.startsWith("mono-")) {
                                    Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                        Text("Accent", color = colors.muted,
                                            style = MaterialTheme.typography.bodyMedium)
                                    FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs),
                                        verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)
                                        ) {
                                            Row(
                                                modifier = Modifier.selectableGroup(),
                                                horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs),
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
                                            }
                                            Surface(
                                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                                                    .clickable(
                                                        role = Role.Button,
                                                        onClickLabel = "Choose custom accent"
                                                    ) { showColorPicker = true }
                                                    .semantics {
                                                        contentDescription = "Choose custom accent"
                                                    },
                                                color = Color.Transparent,
                                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                                border = BorderStroke(
                                                    1.dp, colors.muted.copy(alpha = .35f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = KlyroUiMetrics.spaceMd
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        painterResource(R.drawable.hugeicons_plus_sign),
                                                        contentDescription = null,
                                                        tint = colors.accent,
                                                        modifier = Modifier.size(KlyroUiMetrics.glyphSmall)
                                                    )
                                                    Spacer(Modifier.width(KlyroUiMetrics.spaceXs))
                                                    Text("Custom", style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            KlyroAppearanceGroup("Text") {
                                Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
                                    Text("Terminal font", color = colors.muted,
                                        style = MaterialTheme.typography.bodyMedium)
                                    KlyroSettingsChoiceGrid(columns = 2, minimumInlineWidth = appearanceInlineWidth) {
                                        KlyroTerminalFonts.options.forEach { option ->
                                            val active = fontFamilyId == option.id
                                            Surface(
                                                modifier = Modifier.weight(1f)
                                                    .heightIn(min = KlyroUiMetrics.optionRow)
                                                    .clickable(role = Role.RadioButton,
                                                        onClickLabel = "Use " + option.label) {
                                                        onFontFamilyChange(option.id)
                                                    }.semantics { selected = active },
                                                color = if (active) colors.sheet else Color.Transparent,
                                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                                border = null
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
                                                        style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)) {
                                    KlyroSettingsChoiceGrid(columns = 2, selectionGroup = false, minimumInlineWidth = appearanceInlineWidth) {
                                        Box(
                                            modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text("Font size", color = colors.muted,
                                                style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Row(Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                if (fontSizeSp % 1f == 0f) "${fontSizeSp.toInt()} sp" else "$fontSizeSp sp",
                                                color = colors.foreground, fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (fontSizeSp != 13.5f) {
                                                TextButton(
                                                    onClick = onResetFont,
                                                    modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)
                                                ) { Text("Reset", style = MaterialTheme.typography.bodyMedium) }
                                            }
                                        }
                                    }
                                    KlyroThinSlider(
                                        value = fontSizeSp.coerceIn(9f, 24f),
                                        onValueChange = onFontChange,
                                        valueRange = 9f..24f,
                                        valueLabel = "$fontSizeSp sp",
                                        steps = 29,
                                        modifier = Modifier.fillMaxWidth().semantics {
                                            contentDescription = "Terminal font size"
                                        }
                                    )
                                    KlyroSettingsChoiceGrid(columns = 3, minimumInlineWidth = appearanceInlineWidth) {
                                        listOf(12f, 13.5f, 16f).forEach { preset ->
                                            val active = fontSizeSp == preset
                                            val label = if (preset % 1f == 0f) "${preset.toInt()} sp" else "$preset sp"
                                            Surface(
                                                modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch)
                                                    .clickable(role = Role.RadioButton,
                                                        onClickLabel = "Set terminal font to $label") { onFontChange(preset) }
                                                    .semantics { selected = active; contentDescription = "Font size $label" },
                                                color = if (active) colors.sheet else Color.Transparent,
                                                border = null,
                                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                            ) {
                                                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                                    Text(label, color = colors.foreground,
                                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                                        style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            KlyroAppearanceGroup("Launcher icon") {
                                if (brandChecking) {
                                    KlyroInlineNotice("Checking current launcher icon…")
                                } else if (brandUpdating) {
                                    KlyroInlineNotice("Updating launcher icon…")
                                }
                                KlyroSettingsChoiceGrid(columns = 2, minimumInlineWidth = appearanceInlineWidth) {
                                    listOf(
                                        KlyroBrandChoice.DEFAULT to "B · Default",
                                        KlyroBrandChoice.ALTERNATIVE to "A · Animal"
                                    ).forEach { (choice, title) ->
                                        Surface(
                                            modifier = Modifier.weight(1f)
                                                .heightIn(min = KlyroUiMetrics.minTouch)
                                                .clickable(
                                                    enabled = !brandUpdating && !brandChecking,
                                                    role = Role.RadioButton,
                                                    onClickLabel = "Select launcher icon $title"
                                                ) {
                                                    brandUpdating = true
                                                    brandSwitchError = false
                                                    scope.launch {
                                                        val (ok, actual) = withContext(Dispatchers.IO) {
                                                            val succeeded = runCatching {
                                                                KlyroBrandChoice.isVerifiedSelection(context, choice) ||
                                                                    KlyroBrandChoice.select(context, choice)
                                                            }.getOrDefault(false)
                                                            succeeded to KlyroBrandChoice.confirmedSelection(context)
                                                        }
                                                        if (actual != null) selectedBrand = actual
                                                        brandReady = actual != null
                                                        brandSwitchError = !ok || actual == null
                                                        brandUpdating = false
                                                    }
                                                }.semantics {
                                                    contentDescription = "Launcher icon $title"
                                                    selected = brandReady && selectedBrand == choice
                                                    stateDescription = when {
                                                        brandChecking -> "Checking launcher icon"
                                                        brandUpdating -> "Updating launcher icon"
                                                        !brandReady -> "Current icon unconfirmed"
                                                        selectedBrand == choice -> "Selected"
                                                        else -> "Not selected"
                                                    }
                                                },
                                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                            color = if (brandReady && selectedBrand == choice)
                                                colors.sheet else Color.Transparent,
                                            border = if (brandReady && selectedBrand == choice) BorderStroke(1.dp, colors.accent) else null
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
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Text(title, color = colors.foreground,
                                                    style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                                if (brandSwitchError && !brandChecking && !brandUpdating) {
                                    KlyroInlineNotice(if (brandReady)
                                        "The icon change could not be completed. The confirmed selection is shown. Choose an icon to try again."
                                        else "The current launcher icon could not be confirmed. Choose an icon or retry the check.",
                                        isError = true)
                                    if (!brandReady) {
                                        TextButton(onClick = { brandRevision += 1 },
                                            modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)) {
                                            Text("Retry icon check")
                                        }
                                    }
                                }
                                Text("Reopen your launcher if the icon has not refreshed.",
                                    color = colors.muted, style = MaterialTheme.typography.labelSmall)
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
                        OutlinedButton(
                            onClick = onResetModifiers,
                            enabled = activeModifiers.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth().heightIn(
                                min = KlyroUiMetrics.minTouch
                            ),
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = colors.foreground,
                                disabledContentColor = colors.muted
                            ),
                            border = BorderStroke(1.dp, colors.muted.copy(alpha = .35f))
                        ) {
                            Text(if (activeModifiers.isEmpty()) "All modifiers released"
                                else "Release active modifiers")
                        }
                        Text("Hold a direction key for its extended action; swipe the shortcut row in landscape.",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    "files" -> {
                        val authorized = authorizedFolderCount > 0
                        KlyroSectionHeading("Files & storage")
                        KlyroFolderAccessSummary(authorizedFolderCount)
                        if (authorizedFolderCount == -2) {
                            TextButton(onClick = { folderRevision += 1 },
                                modifier = Modifier.heightIn(min = KlyroUiMetrics.minTouch)) {
                                Text("Retry folder check")
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
                        SettingsRowLink("Export a private file",
                            "Choose a file in HOME and save it with Android's picker.",
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
                            KlyroInlineNotice(
                                "Android settings could not be opened. You can open App info from " +
                                    "the Android Settings app instead.",
                                isError = true
                            )
                            TextButton(onClick = { cannotOpenPermissions = false }) {
                                Text("Dismiss")
                            }
                        }
                    }
                    "freeze" -> FreezeReportsPane(onStabilityTestStarted = onDismiss)
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
                        if (verifyingRuntime) {
                            KlyroInlineNotice(
                                "Checking the private environment. The current report " +
                                    "will refresh when verification finishes."
                            )
                        }
                        Button(
                            onClick = onRecheckEnvironment,
                            enabled = !verifyingRuntime,
                            modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch),
                            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accent,
                                contentColor = colors.onAccent
                            )
                        ) {
                            Text(if (verifyingRuntime) "Checking environment…" else "Recheck environment")
                        }
                        KlyroSettingsChoiceGrid(columns = 2, selectionGroup = false) {
                            OutlinedButton(
                                onClick = onCopyDiagnostic,
                                modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground),
                                border = BorderStroke(1.dp, colors.muted.copy(alpha = .35f))
                            ) {
                                Icon(painterResource(R.drawable.hugeicons_clipboard_list),
                                    contentDescription = null,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                Spacer(Modifier.width(KlyroUiMetrics.spaceSm))
                                Text("Copy")
                            }
                            OutlinedButton(
                                onClick = onShareDiagnostic,
                                modifier = Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.foreground),
                                border = BorderStroke(1.dp, colors.muted.copy(alpha = .35f))
                            ) {
                                Icon(painterResource(R.drawable.hugeicons_upload_01),
                                    contentDescription = null,
                                    modifier = Modifier.size(KlyroUiMetrics.glyphSmall))
                                Spacer(Modifier.width(KlyroUiMetrics.spaceSm))
                                Text("Share")
                            }
                        }
                        Text("Review before sharing. Raw errors and terminal transcripts are excluded.",
                            color = colors.muted, style = MaterialTheme.typography.labelSmall)
                        SettingsRowLink("Freeze reports", "Inspect saved reports and stability checks",
                            R.drawable.hugeicons_pulse_rectangle_01) { section = "freeze" }
                    }
                    "about" -> {
                        KlyroSectionHeading("About Klyro Shell")
                        Surface(shape = RoundedCornerShape(KlyroUiMetrics.cardCorner), color = colors.tile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                                verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceXs)
                                ) {
                                    Text("Klyro Shell", fontWeight = FontWeight.Bold,
                                        color = colors.foreground)
                                    Text("Version $version · ARM64", color = colors.muted,
                                        style = MaterialTheme.typography.labelMedium)
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
internal fun KlyroFolderAccessSummary(count: Int) {
    val colors = LocalKlyroColors.current
    val connected = count > 0
    val failed = count < -1
    val label = when {
        count == -1 -> "Checking connected folders…"
        failed -> "Folder status unavailable"
        count == 0 -> "No folder connected"
        count == 1 -> "1 folder connected"
        else -> "$count folders connected"
    }
    Surface(
        color = if (failed) colors.dangerContainer else if (connected)
            colors.readyInk.copy(alpha = .10f) else colors.tile,
        shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd),
            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.hugeicons_folder_01), contentDescription = null,
                tint = if (failed) colors.danger else if (connected) colors.readyInk else colors.muted,
                modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
            Text(label, modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                fontWeight = FontWeight.SemiBold, color = colors.foreground)
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
            .clickable(role = Role.Button, onClickLabel = "Open " + title, onClick = onClick)
            .semantics { contentDescription = title },
        shape = RoundedCornerShape(KlyroUiMetrics.selectedCorner),
        color = colors.tile
    ) {
        Column(Modifier.padding(KlyroUiMetrics.bentoInnerPadding),
            verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                modifier = Modifier.size(KlyroUiMetrics.iconWell)
                    .background(colors.sheet, RoundedCornerShape(KlyroUiMetrics.controlCorner)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = null,
                    tint = colors.muted, modifier = Modifier.size(KlyroUiMetrics.glyphMedium))
            }
            Column {
                Text(title, color = colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = colors.muted,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
