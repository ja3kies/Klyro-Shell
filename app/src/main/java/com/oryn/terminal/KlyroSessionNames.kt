package com.oryn.terminal

/**
 * IDs identify long-lived PTYs and must never be renumbered or reused.
 * Default visual names follow the current session order only.
 */
internal object KlyroSessionNames {
    fun display(sessions: List<Pair<Long, String?>>): List<Pair<Long, String>> =
        sessions.mapIndexed { index, (id, customTitle) ->
            id to (customTitle?.takeIf { it.isNotBlank() } ?: "Session ${index + 1}")
        }
}
