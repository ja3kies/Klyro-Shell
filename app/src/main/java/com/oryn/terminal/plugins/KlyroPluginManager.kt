package com.oryn.terminal.plugins

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/** Declarative command packs only: no WebView, scripting engine or third-party code execution. */
data class KlyroPluginCommand(val label: String, val insert: String, val description: String)
data class KlyroPluginRecord(
    val id: String,
    val name: String,
    val description: String,
    val version: String,
    val commands: List<KlyroPluginCommand>,
    val enabled: Boolean = true,
    val source: String = "local"
)

class KlyroPluginManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "klyro_plugins_v1", Context.MODE_PRIVATE
    )
    private val resolver = context.applicationContext.contentResolver

    companion object {
        const val MAX_PLUGINS = 12
        const val MAX_COMMANDS = 8
        const val MAX_MANIFEST_BYTES = 16 * 1024
        private val validId = Regex("[a-z][a-z0-9._-]{2,60}")
        private val safeText = Regex("[A-Za-z0-9_./~ -]{1,160}")
        // Exact reviewed templates only: allowing arbitrary arguments to "git diff"
        // admits --ext-diff, which can execute a configured helper.
        private val readOnlyCommands = setOf(
            "pwd", "ls", "ls -la", "git status", "git diff", "git diff --stat",
            "git log", "git log -5 --oneline", "pkg list-installed",
            "uname", "uname -a", "whoami", "date", "id",
            "git branch", "git remote -v", "git diff --cached --stat",
            "git status --short", "git log -1 --oneline", "git rev-parse --show-toplevel",
            "ls -lh", "df -h", "du -sh .", "pkg list-installed"
        )
        private val safePackageSearch = Regex("pkg search [A-Za-z0-9][A-Za-z0-9._-]{0,39}")

        fun safeInsertion(text: String): Boolean {
            val value = text.trim()
            return value == text && safeText.matches(value) &&
                (readOnlyCommands.contains(value) || safePackageSearch.matches(value))
        }

        val bundled: List<KlyroPluginRecord> = listOf(
            KlyroPluginRecord(
                "app.klyro.git", "Git essentials", "Read-only repository inspections", "1.0",
                listOf(
                    KlyroPluginCommand("Status", "git status", "Inspect changed files"),
                    KlyroPluginCommand("Diff", "git diff", "Review unstaged changes"),
                    KlyroPluginCommand("Recent commits", "git log -5 --oneline", "Recent history")
                ), source = "bundled"
            ),
            KlyroPluginRecord(
                "app.klyro.files", "File explorer", "Quick directory inspections", "1.0",
                listOf(
                    KlyroPluginCommand("Location", "pwd", "Current directory"),
                    KlyroPluginCommand("Files", "ls -la", "List hidden and visible files")
                ), source = "bundled"
            ),
            KlyroPluginRecord(
                "app.klyro.system", "System snapshot", "Read-only shell checks", "1.0",
                listOf(
                    KlyroPluginCommand("Architecture", "uname -a", "Kernel and architecture"),
                    KlyroPluginCommand("Identity", "whoami", "Active shell user"),
                    KlyroPluginCommand("Date", "date", "Shell clock")
                ), source = "bundled"
            ),
            KlyroPluginRecord(
                "app.klyro.git-review", "Git review", "Inspect branches and remote state", "1.0",
                listOf(
                    KlyroPluginCommand("Branches", "git branch", "Local branches"),
                    KlyroPluginCommand("Remotes", "git remote -v", "Configured remotes"),
                    KlyroPluginCommand("Staged summary", "git diff --cached --stat", "Staged changes"),
                    KlyroPluginCommand("Short status", "git status --short", "Concise working tree")
                ), source = "bundled"
            ),
            KlyroPluginRecord(
                "app.klyro.storage", "Storage overview", "Inspect available disk space", "1.0",
                listOf(
                    KlyroPluginCommand("Disk usage", "df -h", "Mounted volume space"),
                    KlyroPluginCommand("Project size", "du -sh .", "Current folder size"),
                    KlyroPluginCommand("Detailed files", "ls -lh", "File sizes")
                ), source = "bundled"
            ),
            KlyroPluginRecord(
                "app.klyro.project", "Project context", "Inspect active Git repository", "1.0",
                listOf(
                    KlyroPluginCommand("Project root", "git rev-parse --show-toplevel", "Git root"),
                    KlyroPluginCommand("Latest commit", "git log -1 --oneline", "Latest commit"),
                    KlyroPluginCommand("Installed packages", "pkg list-installed", "Available packages")
                ), source = "bundled"
            )
        )
    }

    fun installed(): List<KlyroPluginRecord> = runCatching {
        val array = JSONArray(prefs.getString("manifest", "[]") ?: "[]")
        (0 until minOf(array.length(), MAX_PLUGINS)).mapNotNull { index ->
            parse(array.optJSONObject(index))
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    // A LazyColumn key must be unique even if different packs reuse a label.
    fun activeCommands(): List<KlyroPluginCommand> = installed()
        .filter { it.enabled }.flatMap { it.commands }
        .distinctBy { it.label to it.insert }

    private fun parse(json: JSONObject?): KlyroPluginRecord? {
        if (json == null) return null
        val id = json.optString("id")
        val name = json.optString("name")
        val description = json.optString("description")
        val version = json.optString("version", "1.0")
        if (!validId.matches(id) || name.isBlank() || name.length > 40 ||
            description.length > 100 || version.length !in 1..20 ||
            (name + description + version).any { it.isISOControl() }) return null
        val commandsJson = json.optJSONArray("commands") ?: return null
        if (commandsJson.length() !in 1..MAX_COMMANDS) return null
        val commands = (0 until commandsJson.length()).map { i ->
            val command = commandsJson.optJSONObject(i) ?: return null
            val label = command.optString("label")
            val insert = command.optString("insert")
            val help = command.optString("description")
            if (label.isBlank() || label.length > 36 || help.length > 100 ||
                (label + help).any { it.isISOControl() } ||
                !safeInsertion(insert)) return null
            KlyroPluginCommand(label, insert, help)
        }
        return KlyroPluginRecord(
            id, name, description, version, commands,
            enabled = json.optBoolean("enabled", true),
            source = if (json.optString("source") == "bundled") "bundled" else "local"
        )
    }

    private fun encode(record: KlyroPluginRecord): JSONObject = JSONObject().apply {
        put("id", record.id)
        put("name", record.name)
        put("description", record.description)
        put("version", record.version)
        put("enabled", record.enabled)
        put("source", record.source)
        put("commands", JSONArray().apply {
            record.commands.forEach { command ->
                put(JSONObject().apply {
                    put("label", command.label)
                    put("insert", command.insert)
                    put("description", command.description)
                })
            }
        })
    }

    private fun save(records: List<KlyroPluginRecord>) {
        val array = JSONArray()
        records.forEach { array.put(encode(it)) }
        check(prefs.edit().putString("manifest", array.toString()).commit()) {
            "Could not persist plugin registry"
        }
    }

    private fun upsert(record: KlyroPluginRecord) {
        val existing = installed()
        require(existing.any { it.id == record.id } || existing.size < MAX_PLUGINS) {
            "At most $MAX_PLUGINS plugins may be installed"
        }
        save(existing.filterNot { it.id == record.id } + record)
    }

    fun installBundled(id: String) {
        val item = bundled.firstOrNull { it.id == id } ?: error("Unknown bundled plugin")
        upsert(item)
    }

    fun previewManifest(uri: Uri): KlyroPluginRecord {
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(2048)
            var emptyReads = 0
            while (true) {
                val n = input.read(chunk)
                if (n < 0) break
                if (n == 0) {
                    require(++emptyReads <= 16) { "Document source stalled" }
                    continue
                }
                emptyReads = 0
                require(output.size() + n <= MAX_MANIFEST_BYTES) { "Manifest exceeds 16 KiB" }
                output.write(chunk, 0, n)
            }
            output.toByteArray()
        } ?: error("Cannot read selected file")
        val objectJson = JSONObject(KlyroManifestText.decodeUtf8(bytes))
        val record = parse(objectJson) ?: error("Unsupported manifest or unsafe commands")
        require(record.id !in bundled.map { it.id }) { "Reserved plugin identifier" }
        require(installed().none { it.id == record.id }) {
            "Plugin already installed. Remove it before importing a replacement."
        }
        return record.copy(enabled = true, source = "local")
    }

    /** Explicit user confirmation is required between parsing and committing. */
    fun installPreview(record: KlyroPluginRecord) {
        require(record.id !in bundled.map { it.id }) { "Reserved plugin identifier" }
        require(record.commands.size in 1..MAX_COMMANDS &&
            record.commands.all { safeInsertion(it.insert) }) { "Unsafe command template" }
        require(installed().none { it.id == record.id }) { "Plugin already installed" }
        upsert(record.copy(enabled = true, source = "local"))
    }

    fun setEnabled(id: String, enabled: Boolean) {
        val current = installed()
        require(current.any { it.id == id }) { "Plugin not installed" }
        save(current.map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    fun uninstall(id: String) {
        save(installed().filterNot { it.id == id })
    }
}
