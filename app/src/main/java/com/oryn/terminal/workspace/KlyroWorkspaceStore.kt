package com.oryn.terminal.workspace

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Persists project names and the selected project. PTY processes and tab IDs
 * deliberately stay in the live SessionViewModel; they never survive process death.
 */
class KlyroWorkspaceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "klyro_workspaces_v1", Context.MODE_PRIVATE
    )
    private val catalog = KlyroWorkspaceCatalog.decode(
        prefs.getString("records", ""),
        prefs.getString("active", KlyroWorkspaceCatalog.GENERAL)
    )

    var entries: List<KlyroWorkspaceCatalog.Entry> by mutableStateOf(catalog.entries().toList())
        private set

    var activeId by mutableStateOf(catalog.activeId())
        private set

    private fun persist() {
        entries = catalog.entries().toList()
        activeId = catalog.activeId()
        prefs.edit().putString("records", catalog.encode())
            .putString("active", activeId).apply()
    }

    fun create(name: String): String? = catalog.create(name)?.also { persist() }
    fun rename(id: String, name: String): Boolean =
        catalog.rename(id, name).also { if (it) persist() }
    fun delete(id: String): Boolean =
        catalog.delete(id).also { if (it) persist() }
    fun select(id: String): Boolean =
        catalog.select(id).also { if (it) persist() }
}
