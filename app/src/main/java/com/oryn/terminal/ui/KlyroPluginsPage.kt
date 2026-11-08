package com.oryn.terminal.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryn.terminal.R
import com.oryn.terminal.plugins.KlyroPluginManager
import com.oryn.terminal.plugins.KlyroPluginRecord

/** Acode-inspired Installed / Explore manager, intentionally no remote code execution. */
@Composable
internal fun KlyroPluginsPage(onBack: () -> Unit) {
    val colors = LocalKlyroColors.current
    val context = LocalContext.current
    val manager = remember(context) { KlyroPluginManager(context) }
    var installed by remember { mutableStateOf(manager.installed()) }
    var page by remember { mutableStateOf("explore") }
    var error by remember { mutableStateOf<String?>(null) }
    var removeId by remember { mutableStateOf<String?>(null) }
    fun applyChange(action: () -> Unit) {
        runCatching(action).onSuccess {
            installed = manager.installed()
            error = null
        }.onFailure { failure ->
            error = failure.message?.take(160) ?: "Action failed"
        }
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) applyChange { manager.importManifest(uri) }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = KlyroUiMetrics.sheetGutter, vertical = KlyroUiMetrics.spaceLg),
        verticalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceSm)) {
            TextButton(onClick = onBack) {
                Icon(painterResource(R.drawable.hugeicons_arrow_left_01),
                    contentDescription = null, modifier = Modifier.size(20.dp))
                Text("Home")
            }
            Spacer(Modifier.weight(1f))
            Text("LOCAL EXTENSIONS", color = colors.muted, fontSize = 11.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        Text("Plugins", fontSize = 28.sp, fontWeight = FontWeight.Bold,
            color = colors.foreground)
        Text("Add command packs to your terminal palette. Suggestions are inserted, never run automatically.",
            color = colors.muted, fontSize = 13.sp, lineHeight = 18.sp)

        Row(
            modifier = Modifier.fillMaxWidth()
                .background(colors.key, RoundedCornerShape(KlyroUiMetrics.selectedCorner))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("installed" to "Installed (${installed.size})",
                "explore" to "Explore").forEach { (id, label) ->
                val selected = page == id
                Surface(
                    modifier = Modifier.weight(1f).clickable(role = Role.Tab,
                        onClickLabel = "Show $label") { page = id },
                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner),
                    color = if (selected) colors.accent else colors.key,
                    contentColor = if (selected) colors.onAccent else colors.foreground
                ) {
                    Text(label, modifier = Modifier.padding(vertical = 13.dp),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        if (error != null) {
            Surface(color = colors.dangerContainer,
                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                Text(error.orEmpty(), color = colors.danger,
                    modifier = Modifier.fillMaxWidth().padding(KlyroUiMetrics.spaceMd))
            }
        }
        if (page == "installed") {
            if (installed.isEmpty()) {
                Surface(color = colors.tile,
                    shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(painterResource(R.drawable.hugeicons_grid2x2),
                            contentDescription = null, tint = colors.accent,
                            modifier = Modifier.size(28.dp))
                        Text("Nothing installed yet", fontWeight = FontWeight.SemiBold,
                            color = colors.foreground)
                        Text("Explore curated packs or import a local JSON manifest.",
                            color = colors.muted, fontSize = 13.sp)
                        TextButton(onClick = { page = "explore" }) { Text("Explore plugins") }
                    }
                }
            }
            installed.forEach { plugin ->
                Surface(
                    color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                ) {
                    Column(Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PluginHeading(plugin, "v${plugin.version} · ${plugin.commands.size} commands")
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(if (plugin.enabled) "Enabled" else "Disabled",
                                color = colors.muted, fontSize = 12.sp)
                            Switch(checked = plugin.enabled,
                                onCheckedChange = { checked ->
                                    applyChange { manager.setEnabled(plugin.id, checked) }
                                })
                            TextButton(onClick = { removeId = plugin.id }) {
                                Text("Remove", color = colors.danger)
                            }
                        }
                    }
                }
            }
        } else {
            KlyroPluginManager.bundled.forEach { plugin ->
                val isInstalled = installed.any { it.id == plugin.id }
                Surface(
                    color = colors.tile, shape = RoundedCornerShape(KlyroUiMetrics.cardCorner)
                ) {
                    Column(Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PluginHeading(plugin, "${plugin.commands.size} read-only commands")
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("Bundled · Offline", color = colors.muted, fontSize = 12.sp)
                            Button(
                                enabled = !isInstalled,
                                onClick = { applyChange { manager.installBundled(plugin.id) } },
                                colors = ButtonDefaults.buttonColors(containerColor = colors.accent,
                                    contentColor = colors.onAccent),
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                            ) { Text(if (isInstalled) "Installed" else "Add") }
                        }
                    }
                }
            }
            Surface(color = colors.chrome,
                shape = RoundedCornerShape(KlyroUiMetrics.cardCorner),
                border = BorderStroke(1.dp, colors.muted.copy(alpha = .2f))) {
                Column(Modifier.fillMaxWidth().padding(KlyroUiMetrics.cardPadding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Local manifest", color = colors.foreground,
                        fontWeight = FontWeight.SemiBold)
                    Text("Import a declarative JSON command pack (16 KiB maximum). No JavaScript or shell code is executed by the plugin manager.",
                        color = colors.muted, fontSize = 12.sp)
                    OutlinedButton(onClick = { importPicker.launch(arrayOf("*/*")) },
                        shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
                        Icon(painterResource(R.drawable.hugeicons_download_01),
                            contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Import JSON")
                    }
                }
            }
        }
    }

    removeId?.let { id ->
        val item = installed.firstOrNull { it.id == id }
        AlertDialog(
            onDismissRequest = { removeId = null },
            containerColor = colors.sheet,
            title = { Text("Remove ${item?.name ?: "plugin"}?") },
            text = { Text("This removes its command suggestions. Terminal sessions and user files are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    removeId = null
                    applyChange { manager.uninstall(id) }
                }) { Text("Remove", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { removeId = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun PluginHeading(plugin: KlyroPluginRecord, detail: String) {
    val colors = LocalKlyroColors.current
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(KlyroUiMetrics.spaceMd),
        verticalAlignment = Alignment.CenterVertically) {
        Surface(color = colors.accent.copy(alpha = .12f),
            shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)) {
            Icon(painterResource(R.drawable.hugeicons_grid2x2),
                contentDescription = null, tint = colors.accent,
                modifier = Modifier.padding(12.dp).size(24.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(plugin.name, fontWeight = FontWeight.SemiBold, color = colors.foreground)
            Text(plugin.description, color = colors.muted, fontSize = 12.sp)
            Text(detail, color = colors.accent, fontSize = 11.sp)
        }
    }
}
