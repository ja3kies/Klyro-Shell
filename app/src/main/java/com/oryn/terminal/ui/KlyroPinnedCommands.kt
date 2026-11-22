package com.oryn.terminal.ui

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.oryn.terminal.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal class KlyroPinnedCommandStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("klyro_pinned_commands", Context.MODE_PRIVATE)
    var commands by mutableStateOf(read())
        private set

    private fun read(): List<KlyroPinnedCommand> = runCatching {
        val json = JSONArray(preferences.getString("commands", "[]"))
        val result = (0 until minOf(json.length(), KlyroPinnedCommandPolicy.MAX_STORED)).mapNotNull { i ->
            val entry = json.optJSONObject(i) ?: return@mapNotNull null
            val label = entry.optString("label")
            val command = entry.optString("command")
            val id = entry.optString("id")
            if (id.isBlank() || !KlyroPinnedCommandPolicy.valid(label, command)) null
            else KlyroPinnedCommand(id, label, command, entry.optString("workspace").takeIf { it.isNotBlank() })
        }.distinctBy { it.id }
        result.fold(emptyList<KlyroPinnedCommand>()) { accepted, next ->
            (accepted + next).takeIf(KlyroPinnedCommandPolicy::fits) ?: accepted
        }
    }.getOrDefault(emptyList())

    fun save(next: List<KlyroPinnedCommand>): Boolean {
        if (!KlyroPinnedCommandPolicy.fits(next) || next.any { !KlyroPinnedCommandPolicy.valid(it.label, it.command) }) return false
        val json = JSONArray()
        next.forEach {
            json.put(JSONObject().put("id", it.id).put("label", it.label).put("command", it.command)
                .put("workspace", it.workspaceId ?: ""))
        }
        preferences.edit().putString("commands", json.toString()).apply()
        commands = next
        return true
    }

    fun removeWorkspace(id: String) {
        save(commands.filterNot { it.workspaceId == id })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun KlyroPinnedCommandBar(
    store: KlyroPinnedCommandStore,
    workspaceId: String,
    workspaceName: String,
    compact: Boolean,
    canExecute: Boolean,
    onExecute: (KlyroPinnedCommand) -> Unit
) {
    val colors = LocalKlyroColors.current
    val shown = KlyroPinnedCommandPolicy.visible(store.commands, workspaceId)
    var menuId by remember(workspaceId) { mutableStateOf<String?>(null) }
    var editorOpen by remember(workspaceId) { mutableStateOf(false) }
    var editing by remember(workspaceId) { mutableStateOf<KlyroPinnedCommand?>(null) }
    var reorder by remember(workspaceId) { mutableStateOf(false) }
    var remove by remember(workspaceId) { mutableStateOf<KlyroPinnedCommand?>(null) }

    fun edit(command: KlyroPinnedCommand?) {
        menuId = null
        editing = command
        editorOpen = true
    }
    val columns = if (compact) 4 else 2
    Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(4 / columns) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(columns) { col ->
                    val index = row * columns + col
                    val command = shown.getOrNull(index)
                    Box(Modifier.weight(1f).heightIn(min = KlyroUiMetrics.minTouch)) {
                        when {
                            command != null -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
                                        .clip(RoundedCornerShape(KlyroUiMetrics.controlCorner))
                                        .combinedClickable(
                                            role = Role.Button, onClickLabel = "Run " + command.label,
                                            onLongClickLabel = "Options for " + command.label,
                                            onClick = { if (canExecute) onExecute(command) },
                                            onLongClick = { menuId = command.id }
                                        ).testTag("pinned-command-" + command.id),
                                    color = colors.key,
                                    shape = RoundedCornerShape(KlyroUiMetrics.controlCorner)
                                ) {
                                    Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center) {
                                        Text(command.label, color = if (canExecute) colors.foreground else colors.muted,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(if (command.workspaceId == null) "Global" else "Workspace",
                                            color = colors.muted, style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                DropdownMenu(expanded = menuId == command.id, onDismissRequest = { menuId = null },
                                    containerColor = colors.sheet) {
                                    KlyroMenuItem("Edit", R.drawable.hugeicons_pen_01) { edit(command) }
                                    KlyroMenuItem("Delete", R.drawable.hugeicons_trash, destructive = true) {
                                        menuId = null; remove = command
                                    }
                                    KlyroMenuItem("Reorder", R.drawable.hugeicons_more_vertical, enabled = shown.size > 1) {
                                        menuId = null; reorder = true
                                    }
                                }
                            }
                            index == shown.size -> Surface(
                                modifier = Modifier.fillMaxWidth().heightIn(min = KlyroUiMetrics.minTouch)
                                    .clip(RoundedCornerShape(KlyroUiMetrics.controlCorner))
                                    .clickable(role = Role.Button) { edit(null) },
                                shape = RoundedCornerShape(KlyroUiMetrics.controlCorner), color = colors.key
                            ) {
                                Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                                    Text("+ Add command", color = colors.accent,
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (editorOpen) {
        var label by remember(editing?.id) { mutableStateOf(editing?.label.orEmpty()) }
        var command by remember(editing?.id) { mutableStateOf(editing?.command.orEmpty()) }
        var global by remember(editing?.id) { mutableStateOf(editing?.workspaceId == null) }
        var error by remember { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { editorOpen = false }, containerColor = colors.sheet,
            title = { Text(if (editing == null) "Pin command" else "Edit command") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it.take(40) }, label = { Text("Short label") }, singleLine = true)
                    OutlinedTextField(command, { if (it.length <= 8192) command = it },
                        label = { Text("Shell command") }, minLines = 2, maxLines = 5)
                    Text("Runs immediately in the current terminal. Use at an empty Bash prompt.", color = colors.muted,
                        style = MaterialTheme.typography.bodySmall)
                    listOf(true to "Global", false to workspaceName).forEach { (scope, name) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { global = scope },
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = global == scope, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                    error?.let { Text(it, color = colors.danger) }
                }
            },
            confirmButton = {
                Button(enabled = KlyroPinnedCommandPolicy.valid(label, command), onClick = {
                    val original = editing
                    val item = KlyroPinnedCommand(original?.id ?: UUID.randomUUID().toString(),
                        label.trim(), command, if (global) null else workspaceId)
                    val next = if (original == null) store.commands + item
                    else store.commands.map { if (it.id == original.id) item else it }
                    if (store.save(next)) editorOpen = false
                    else error = "Each workspace can show four commands, including global commands. Remove one or change its scope."
                }) { Text("Save") }
            }, dismissButton = { TextButton(onClick = { editorOpen = false }) { Text("Cancel") } })
    }
    remove?.let { item ->
        AlertDialog(onDismissRequest = { remove = null }, containerColor = colors.sheet,
            title = { Text("Delete pinned command?") }, text = { Text(item.label) },
            confirmButton = { TextButton(onClick = { store.save(store.commands.filterNot { it.id == item.id }); remove = null }) {
                Text("Delete", color = colors.danger)
            } }, dismissButton = { TextButton(onClick = { remove = null }) { Text("Cancel") } })
    }
    if (reorder) {
        AlertDialog(onDismissRequest = { reorder = false }, containerColor = colors.sheet,
            title = { Text("Reorder commands") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    shown.forEachIndexed { index, item ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.label, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = {
                                store.save(KlyroPinnedCommandPolicy.move(store.commands, workspaceId, item.id, -1))
                            }, enabled = index > 0) { Text("Up") }
                            TextButton(onClick = {
                                store.save(KlyroPinnedCommandPolicy.move(store.commands, workspaceId, item.id, 1))
                            }, enabled = index < shown.lastIndex) { Text("Down") }
                        }
                    }
                }
            }, confirmButton = { TextButton(onClick = { reorder = false }) { Text("Done") } })
    }
}
