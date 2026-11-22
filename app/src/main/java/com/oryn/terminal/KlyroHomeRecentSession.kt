package com.oryn.terminal

/** Only an existing tab can be resumed; never synthesize or resurrect a PTY. */
internal data class HomeRecentSession(val id: Long, val title: String, val workspace: String, val finished: Boolean)

internal fun homeRecentSession(
    activeId: Long?, tabs: List<Pair<Long, String>>, membership: Map<Long, String>,
    workspaceNames: Map<String, String>, finished: Set<Long>
): HomeRecentSession? {
    val tab = tabs.firstOrNull { it.first == activeId } ?: return null
    return HomeRecentSession(tab.first, tab.second,
        workspaceNames[membership[tab.first] ?: "general"] ?: "General",
        tab.first in finished)
}

internal fun homeDisplayDirectory(path: String?, home: String): String? {
    val value = path?.takeIf { it.isNotBlank() } ?: return null
    return when {
        value == home -> "~"
        value.startsWith("$home/") -> "~" + value.removePrefix(home)
        else -> value
    }
}
