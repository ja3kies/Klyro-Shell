package com.oryn.terminal.ui

internal data class KlyroPinnedCommand(
    val id: String, val label: String, val command: String, val workspaceId: String?
)

internal object KlyroPinnedCommandPolicy {
    const val MAX_VISIBLE = 4
    const val MAX_STORED = 256
    fun visible(commands: List<KlyroPinnedCommand>, workspace: String) =
        commands.filter { it.workspaceId == null || it.workspaceId == workspace }

    // No stored command is silently hidden by global/workspace collisions.
    fun fits(commands: List<KlyroPinnedCommand>): Boolean {
        val global = commands.count { it.workspaceId == null }
        return commands.size <= MAX_STORED && global <= MAX_VISIBLE &&
            commands.filter { it.workspaceId != null }.groupingBy { it.workspaceId }.eachCount()
                .values.all { it + global <= MAX_VISIBLE }
    }

    fun valid(label: String, command: String): Boolean =
        label.isNotBlank() && label.length <= 40 &&
            command.isNotBlank() && command.toByteArray(Charsets.UTF_8).size <= 8192 &&
            command.none { it < ' ' && it !in "\t\r\n" || it in '\u007f'..'\u009f' }

    fun move(commands: List<KlyroPinnedCommand>, workspace: String, id: String, direction: Int): List<KlyroPinnedCommand> {
        val shown = visible(commands, workspace)
        val index = shown.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || direction !in listOf(-1, 1) || target !in shown.indices) return commands
        val a = commands.indexOf(shown[index])
        val b = commands.indexOf(shown[target])
        return commands.toMutableList().apply {
            val previous = this[a]
            this[a] = this[b]
            this[b] = previous
        }
    }
}
