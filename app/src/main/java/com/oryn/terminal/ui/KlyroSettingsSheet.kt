package com.oryn.terminal.ui

import android.content.Context
import android.graphics.Color as AndroidColor
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.viewinterop.AndroidView
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
    tabs: List<Pair<Long, String>>,
    onEditSession: (Long, String) -> Unit,
    onResetModifiers: () -> Unit,
    onSetupStorage: () -> Unit,
    onImportStorageFolder: () -> Unit,
    onImportFile: () -> Unit,
    onExportFile: () -> Unit,
    onSelectOutput: () -> Unit,
    onDismiss: () -> Unit
) {
    var section by remember(initialSection) { mutableStateOf(initialSection) }
    var showFontLicense by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val previewTypeface = remember(context, fontFamilyId) {
        KlyroTerminalFonts.load(context, fontFamilyId)
    }
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "development"
    }

    KlyroModalTheme {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = colors.sheet,
            contentColor = colors.foreground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = colors.muted) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        if (context.resources.getBoolean(R.bool.klyro_experimental_bento)) {
                            KlyroExperimentalBento(
                                tabsCount = tabs.size,
                                accentLabel = KlyroColors.selectedOption(accentId)?.label
                                    ?: "Custom",
                                onOpen = { section = it }
                            )
                        } else {
                        Text("Make Klyro yours",
                            style = MaterialTheme.typography.bodyMedium, color = colors.muted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsTile("Appearance", "Themes & typography", null,
                                Modifier.weight(1f), fallbackGlyph = "Aa") { section = "appearance" }
                            SettingsTile("Keyboard", "Extra keys & modifiers",
                                R.drawable.hugeicons_keyboard,
                                Modifier.weight(1f)) { section = "keyboard" }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsTile("Sessions", "Switch, edit & manage",
                                R.drawable.hugeicons_terminal,
                                Modifier.weight(1f)) { section = "sessions" }
                            SettingsTile("Files", "Android storage access",
                                R.drawable.hugeicons_folder_01,
                                Modifier.weight(1f)) { section = "files" }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsTile("Privacy", "Clipboard & data",
                                R.drawable.hugeicons_shield_01,
                                Modifier.weight(1f)) { section = "privacy" }
                            SettingsTile("About", "Build & terminal engine",
                                R.drawable.hugeicons_information_square,
                                Modifier.weight(1f)) { section = "about" }
                        }
                        }
                    }
                    "appearance" -> {
                        SectionHeading("Appearance", "Select a theme or follow Android's appearance.")
                        Surface(shape = RoundedCornerShape(22.dp), color = colors.tile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("App theme", fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(
                                        "system" to "System",
                                        "light" to "Light",
                                        "dark" to "Dark"
                                    ).forEach { (mode, label) ->
                                        Button(
                                            onClick = { onThemeModeChange(mode) },
                                            modifier = Modifier.weight(1f)
                                                .semantics { selected = themeMode == mode },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                containerColor = if (themeMode == mode)
                                                    colors.accent else colors.key,
                                                contentColor = if (themeMode == mode)
                                                    colors.onAccent else colors.foreground
                                            ),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                horizontal = 1.dp, vertical = 4.dp
                                            )
                                        ) { Text((if (themeMode == mode) "✓ " else "") + label,
                                            fontSize = 12.sp, maxLines = 1) }
                                    }
                                }
                                Text("Accent preset", fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    KlyroColors.primary.forEach { preset ->
                                        val swatch = KlyroColors.create(colors.dark, preset.id)
                                        Button(
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                                .semantics {
                                                    selected = accentId == preset.id
                                                    contentDescription = preset.label + " accent"
                                                },
                                            onClick = { onAccentIdChange(preset.id) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = swatch.accent,
                                                contentColor = swatch.onAccent
                                            ),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                horizontal = 1.dp, vertical = 3.dp
                                            ),
                                            border = if (accentId == preset.id)
                                                androidx.compose.foundation.BorderStroke(2.dp, colors.foreground)
                                                else null
                                        ) { Text(
                                            (if (accentId == preset.id) "✓ " else "") + preset.label,
                                            fontSize = 11.sp, maxLines = 1
                                        ) }
                                    }
                                    Button(
                                        onClick = { showColorPicker = true },
                                        modifier = Modifier.weight(.68f).heightIn(min = 48.dp)
                                            .semantics { contentDescription = "Choose custom accent color" },
                                        shape = RoundedCornerShape(14.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = colors.key, contentColor = colors.accent
                                        ),
                                        border = if (accentId.startsWith("custom:#") ||
                                            KlyroColors.extended.any { it.id == accentId })
                                            androidx.compose.foundation.BorderStroke(2.dp, colors.accent)
                                        else null
                                    ) { Text("+", fontSize = 22.sp) }
                                }
                                if (accentId.startsWith("custom:#") ||
                                    KlyroColors.extended.any { it.id == accentId }) {
                                    Text(
                                        "Selected: " + (KlyroColors.selectedOption(accentId)?.label
                                            ?: accentId.removePrefix("custom:")),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted
                                    )
                                }
                                Text("Changes save instantly; sessions keep running.",
                                    style = MaterialTheme.typography.bodySmall, color = colors.muted)
                            }
                        }
                        Surface(shape = RoundedCornerShape(22.dp), color = colors.tile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Text("Terminal font", fontWeight = FontWeight.SemiBold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    KlyroTerminalFonts.options.forEach { choice ->
                                        Button(
                                            onClick = { onFontFamilyChange(choice.id) },
                                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                                .semantics { selected = fontFamilyId == choice.id },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (fontFamilyId == choice.id) colors.accent else colors.key,
                                                contentColor = if (fontFamilyId == choice.id) colors.onAccent else colors.foreground
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                                horizontal = 6.dp, vertical = 6.dp
                                            )
                                        ) { Text(
                                            (if (fontFamilyId == choice.id) "✓ " else "") + choice.label,
                                            fontSize = 12.sp, maxLines = 2, lineHeight = 15.sp
                                        ) }
                                    }
                                }
                                Text("Preview each typeface before choosing.",
                                    style = MaterialTheme.typography.bodySmall, color = colors.muted)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = colors.terminal,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Native TextView uses the exact bundled font loaded
                                        // by TerminalView, not Compose's system monospace.
                                        AndroidView(
                                            factory = { ctx -> TextView(ctx) },
                                            modifier = Modifier.fillMaxWidth(),
                                            update = { view ->
                                                val preview = "klyro ~ $ echo \"Ready.\""
                                                val styled = SpannableString(preview).apply {
                                                    setSpan(
                                                        ForegroundColorSpan(colors.accent.toArgb()),
                                                        0, "klyro ~ $".length,
                                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                                    )
                                                }
                                                view.text = styled
                                                view.setTextColor(colors.foreground.toArgb())
                                                view.typeface = previewTypeface
                                                view.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                                                view.contentDescription = "Terminal font preview: $preview"
                                            }
                                        )
                                        Text(
                                            "Preview: ${KlyroTerminalFonts.options.firstOrNull {
                                                it.id == fontFamilyId
                                            }?.label ?: "System Mono"}",
                                            color = colors.muted,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Text("Terminal font size", fontWeight = FontWeight.SemiBold)
                                Text(if (fontSizeSp % 1f == 0f) "${fontSizeSp.toInt()} sp" else "$fontSizeSp sp",
                                    color = colors.muted)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(onClick = { onFontChange(-1f) }, enabled = fontSizeSp > 9f,
                                        modifier = Modifier.semantics { contentDescription = "Decrease terminal font size" },
                                        shape = RoundedCornerShape(25.dp)) { Text("A−") }
                                    Button(onClick = { onFontChange(1f) }, enabled = fontSizeSp < 24f,
                                        modifier = Modifier.semantics { contentDescription = "Increase terminal font size" },
                                        shape = RoundedCornerShape(25.dp)) { Text("A+") }
                                    TextButton(onClick = onResetFont,
                                        enabled = fontSizeSp != 13.5f) { Text("Reset") }
                                }
                                // Font changes are persisted without interrupting the terminal.
                            }
                        }
                    }
                    "keyboard" -> {
                        SectionHeading("Keyboard", "Two rows in portrait; one scrollable row when space is limited.")
                        ActionTile(
                            title = "Release modifiers",
                            subtitle = "Turn off CTRL, ALT, SHIFT and FN if a key stays active."
                        ) { onResetModifiers() }
                        Text("In compact windows, swipe the shortcut row to reach the remaining keys.",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    "sessions" -> {
                        SectionHeading("Sessions", "${tabs.size} active shell session(s).")
                        tabs.forEach { (id, name) ->
                            ActionTile(
                                title = name.ifBlank { "Session" },
                                subtitle = "Rename, switch or close with confirmation"
                            ) {
                                onDismiss()
                                onEditSession(id, name)
                            }
                        }
                    }
                    "files" -> {
                        SectionHeading("Files & storage", "Access only Android folders you authorize.")
                        ActionTile("Set up storage folder", "Choose a folder with Android's file picker") {
                            onDismiss(); onSetupStorage()
                        }
                        ActionTile("Import from authorized folder", "Import into Klyro private storage") {
                            onDismiss(); onImportStorageFolder()
                        }
                        ActionTile("Import file", "Choose a document from Android") {
                            onDismiss(); onImportFile()
                        }
                        ActionTile("Export file", "Save a file through Android's file picker") {
                            onDismiss(); onExportFile()
                        }
                    }
                    "privacy" -> {
                        SectionHeading("Privacy", "Terminal output stays on your device by default.")
                        ActionTile("Select terminal output", "Review and copy from the available transcript") {
                            onDismiss(); onSelectOutput()
                        }
                        Text("Multi-line clipboard commands require confirmation before execution. " +
                            "Copying terminal output may include secrets.",
                            style = MaterialTheme.typography.bodySmall, color = colors.muted)
                    }
                    "about" -> {
                        SectionHeading("About Klyro Shell", "Private terminal environment for Android.")
                        Surface(shape = RoundedCornerShape(26.dp), color = colors.tile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Version $version", fontWeight = FontWeight.SemiBold)
                                Text("Terminal engine: Termux TerminalView / TerminalSession",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Linux runtime: isolated ARM64 environment", color = colors.muted,
                                    style = MaterialTheme.typography.bodySmall)
                                if (context.resources.getBoolean(R.bool.klyro_experimental_bento)) {
                                    // Read-only inset telemetry; do not remove camera/status
                                    // protection without evidence from the user's device.
                                    Text(
                                        "Display safe inset · top: ${WindowInsets.safeDrawing.getTop(density)} px",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.muted
                                    )
                                }
                            }
                        }
                        ActionTile(
                            "JetBrains Mono licence",
                            "SIL Open Font License 1.1 · included in this APK"
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
                shape = RoundedCornerShape(28.dp),
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
private fun SettingsTile(
    title: String,
    subtitle: String,
    icon: Int?,
    modifier: Modifier,
    fallbackGlyph: String = "Aa",
    onClick: () -> Unit
) {
    val colors = LocalKlyroColors.current
    Surface(
        modifier = modifier.heightIn(min = 140.dp)
            .clickable(onClickLabel = "Open $title settings", onClick = onClick),
        color = colors.tile,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp)
                    .background(colors.accent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (icon == null) {
                    Text(fallbackGlyph, color = colors.onAccent, fontSize = 19.sp)
                } else {
                    Icon(
                        painter = painterResource(icon),
                        tint = colors.onAccent,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(title, color = colors.foreground, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = colors.muted, fontSize = 11.sp,
                maxLines = 2, lineHeight = 15.sp)
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
private fun ActionTile(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalKlyroColors.current
    Surface(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClickLabel = title, onClick = onClick),
        color = colors.tile,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, color = colors.foreground)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.muted)
        }
    }
}
