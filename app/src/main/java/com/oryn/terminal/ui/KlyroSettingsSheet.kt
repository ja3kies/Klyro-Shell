package com.oryn.terminal.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

private val MintInk = Color(0xFF18282B)
private val MintMuted = Color(0xFF557168)
private val MintSheet = Color(0xFFF8FBFA)
private val MintTile = Color(0xFFEAF3EF)
private val MintAccent = Color(0xFFB9DAD4)
private val MintTheme = lightColorScheme(
    primary = Color(0xFF244F43),
    onPrimary = Color.White,
    surface = MintSheet,
    onSurface = MintInk,
    surfaceVariant = Color(0xFFDDEFEA),
    onSurfaceVariant = MintMuted
)

/**
 * A settings sheet deliberately isolated from TerminalView: it never restarts
 * the shell and all file actions remain delegated to Android's SAF handlers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KlyroSettingsSheet(
    initialSection: String,
    fontSizeSp: Float,
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
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "development"
    }

    MaterialTheme(colorScheme = MintTheme) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MintSheet,
            contentColor = MintInk,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF8DAAA2)) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Text("Make Klyro yours",
                            style = MaterialTheme.typography.bodyMedium, color = MintMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsTile("Appearance", "Terminal font & size", "Aa",
                                Modifier.weight(1f)) { section = "appearance" }
                            SettingsTile("Keyboard", "Extra keys & modifiers", "⌨",
                                Modifier.weight(1f)) { section = "keyboard" }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsTile("Sessions", "Switch, edit & manage", "▣",
                                Modifier.weight(1f)) { section = "sessions" }
                            SettingsTile("Files", "Android storage access", "▤",
                                Modifier.weight(1f)) { section = "files" }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingsTile("Privacy", "Clipboard & data", "◇",
                                Modifier.weight(1f)) { section = "privacy" }
                            SettingsTile("About", "Build & terminal engine", "ⓘ",
                                Modifier.weight(1f)) { section = "about" }
                        }
                    }
                    "appearance" -> {
                        SectionHeading("Appearance", "Adjust the terminal, not the keyboard toolbar.")
                        Surface(shape = RoundedCornerShape(26.dp), color = MintTile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Terminal font size", fontWeight = FontWeight.SemiBold)
                                Text("${fontSizeSp.toInt()} sp", color = MintMuted)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(onClick = { onFontChange(-1f) }, enabled = fontSizeSp > 9f,
                                        shape = RoundedCornerShape(25.dp)) { Text("A−") }
                                    Button(onClick = { onFontChange(1f) }, enabled = fontSizeSp < 24f,
                                        shape = RoundedCornerShape(25.dp)) { Text("A+") }
                                    TextButton(onClick = onResetFont,
                                        enabled = fontSizeSp != 13.5f) { Text("Reset") }
                                }
                                Text("Changes are saved automatically.", color = MintMuted,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    "keyboard" -> {
                        SectionHeading("Keyboard", "Two fixed rows of terminal shortcuts.")
                        ActionTile(
                            title = "Release modifiers",
                            subtitle = "Turn off CTRL, ALT, SHIFT and FN if a key stays active."
                        ) { onResetModifiers() }
                        Text("Shortcut keys are always visible; there are no hidden horizontal pages.",
                            style = MaterialTheme.typography.bodySmall, color = MintMuted)
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
                            style = MaterialTheme.typography.bodySmall, color = MintMuted)
                    }
                    "about" -> {
                        SectionHeading("About Klyro Shell", "Private terminal environment for Android.")
                        Surface(shape = RoundedCornerShape(26.dp), color = MintTile) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Version $version", fontWeight = FontWeight.SemiBold)
                                Text("Terminal engine: Termux TerminalView / TerminalSession",
                                    style = MaterialTheme.typography.bodyMedium)
                                Text("Linux runtime: isolated ARM64 environment", color = MintMuted,
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTile(
    title: String,
    subtitle: String,
    symbol: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 140.dp)
            .clickable(onClickLabel = "Open $title settings", onClick = onClick),
        color = MintTile,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp)
                    .background(MintAccent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) { Text(symbol, color = MintInk, fontSize = 19.sp) }
            Text(title, color = MintInk, fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = MintMuted, fontSize = 11.sp,
                maxLines = 2, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MintMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ActionTile(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClickLabel = title, onClick = onClick),
        color = MintTile,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MintInk)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MintMuted)
        }
    }
}
