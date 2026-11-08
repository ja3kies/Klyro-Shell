package com.oryn.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.oryn.terminal.workspace.KlyroWorkspaceCatalog
import com.oryn.terminal.session.OrynTerminalSessionClient
import com.oryn.terminal.ui.OrynTerminalViewClient
import com.termux.terminal.TerminalSession

/**
 * Owns independent PTY sessions across Activity recreation.
 * Session callbacks are bound to their originating session, never the selected tab.
 */
class TerminalSessionViewModel : ViewModel(), OrynTerminalSessionClient.Listener {
    data class ShellTab(
        val id: Long,
        val session: TerminalSession,
        val client: OrynTerminalSessionClient,
        val viewClient: OrynTerminalViewClient,
        val title: String = "Bash",
        val customTitle: String? = null,
        val workspaceId: String = KlyroWorkspaceCatalog.GENERAL
    )

    private val tabs = mutableStateListOf<ShellTab>()
    private val unreadActivity = mutableStateMapOf<Long, Boolean>()
    // Native TerminalView draws outside Compose. Notify the smart ALL key when
    // the active terminal transcript changes, including output from child jobs.
    private val outputRevisions = mutableStateMapOf<Long, Long>()
    fun outputRevision(tabId: Long?): Long = tabId?.let { outputRevisions[it] } ?: 0L
    val tabsWithUnreadOutput: Set<Long> get() = unreadActivity.keys.toSet()
    private val invalidTitleCharacters = Regex("[\\p{Cntrl}\\p{Cf}]")
    val sessions: List<ShellTab> get() = tabs
    private var nextId = 1L
    var activeTabId by mutableStateOf<Long?>(null)
        private set

    private val selected: ShellTab? get() = tabs.firstOrNull { it.id == activeTabId }
    val activeSession: TerminalSession? get() = selected?.session
    val sessionClient: OrynTerminalSessionClient? get() = selected?.client
    val viewClient: OrynTerminalViewClient? get() = selected?.viewClient
    val terminalTitle: String get() = selected?.let { displayName(it.id) } ?: "Bash"

    // Stable PTY identifiers never change; user-facing default numbering stays dense.
    fun displayName(id: Long): String? =
        displayTabs.firstOrNull { it.first == id }?.second

    val displayTabs: List<Pair<Long, String>>
        get() = KlyroSessionNames.display(tabs.map { it.id to it.customTitle })

    /** Workspace membership is transient, exactly like its live PTY tab. */
    val tabWorkspaceIds: Map<Long, String>
        get() = tabs.associate { it.id to it.workspaceId }

    fun assignWorkspace(tabId: Long, workspaceId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index >= 0 && workspaceId.isNotBlank() && tabs[index].workspaceId != workspaceId) {
            tabs[index] = tabs[index].copy(workspaceId = workspaceId)
        }
    }

    fun resetWorkspace(workspaceId: String) {
        tabs.indices.forEach { index ->
            if (tabs[index].workspaceId == workspaceId) {
                tabs[index] = tabs[index].copy(workspaceId = KlyroWorkspaceCatalog.GENERAL)
            }
        }
    }

    var sessionStatus by mutableStateOf("")
        private set
    var sessionFailureMessage by mutableStateOf<String?>(null)
        private set

    data class NativePasteRequest(val tabId: Long, val text: String)
    var pendingNativePaste by mutableStateOf<NativePasteRequest?>(null)
        private set

    fun clearNativePaste(tabId: Long) {
        if (pendingNativePaste?.tabId == tabId) pendingNativePaste = null
    }

    fun hasLiveSessions(): Boolean = tabs.isNotEmpty()

    fun isSessionActive(id: Long): Boolean =
        activeTabId == id && tabs.any { it.id == id }

    fun listenerFor(tabId: Long): OrynTerminalSessionClient.Listener =
        object : OrynTerminalSessionClient.Listener {
            override fun onOutputActivity() {
                if (tabs.any { it.id == tabId }) {
                    outputRevisions[tabId] = (outputRevisions[tabId] ?: 0L) + 1L
                    if (activeTabId != tabId) unreadActivity[tabId] = true
                }
            }
            override fun onTitleChanged(title: String?) {
                updateTitle(tabId, title)
            }
            override fun onSessionFinished(session: TerminalSession, exitStatus: Int) {
                finishTab(tabId, session, exitStatus)
            }
            override fun onNativePasteRequiresConfirmation(text: String) {
                if (isSessionActive(tabId)) {
                    pendingNativePaste = NativePasteRequest(tabId, text)
                }
            }
            override fun onShellPid(pid: Int) {
                if (pid > 0 && activeTabId == tabId && tabs.any { it.id == tabId }) {
                    sessionStatus = "Bash session started (PID $pid)."
                }
            }
        }

    fun reserveTabId(): Long {
        check(nextId > 0 && nextId < Long.MAX_VALUE) { "Terminal tab ID space exhausted" }
        return nextId++
    }

    fun attachSession(
        session: TerminalSession,
        client: OrynTerminalSessionClient,
        terminalViewClient: OrynTerminalViewClient,
        tabId: Long = reserveTabId(),
        workspaceId: String = KlyroWorkspaceCatalog.GENERAL
    ) {
        require(tabs.none { it.id == tabId }) { "Duplicate terminal tab ID" }
        require(tabId > 0) { "Terminal tab ID must be positive" }
        tabs.add(ShellTab(tabId, session, client, terminalViewClient,
            title = "Session $tabId", workspaceId = workspaceId))
        selectTab(tabId)
        sessionStatus = "Starting private Bash session…"
        sessionFailureMessage = null
    }

    fun selectTab(id: Long) {
        if (activeTabId == id || tabs.none { it.id == id }) return
        // Never confirm an old tab's multiline clipboard payload on a new tab.
        pendingNativePaste = null
        selected?.let {
            it.client.attachView(null)
            it.viewClient.attachView(null)
        }
        activeTabId = id
        unreadActivity.remove(id)
        sessionFailureMessage = null
        sessionStatus = ""
    }

    fun renameTab(id: Long, rawName: String) {
        val name = rawName.trim().replace(invalidTitleCharacters, "").take(32).trim()
        if (name.isBlank()) return
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0 && tabs[index].customTitle != name) {
            tabs[index] = tabs[index].copy(customTitle = name)
        }
    }

    fun closeTab(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        if (pendingNativePaste?.tabId == id) pendingNativePaste = null
        val entry = tabs.removeAt(index)
        unreadActivity.remove(id)
        outputRevisions.remove(id)
        entry.client.dispose()
        entry.viewClient.attachView(null)
        entry.session.finishIfRunning()
        if (activeTabId == id) {
            activeTabId = null
            tabs.getOrNull(index.coerceAtMost(tabs.lastIndex))?.let { selectTab(it.id) }
        }
    }

    fun closeSession() {
        activeTabId?.let(::closeTab)
        sessionStatus = "Bash session closed."
    }

    fun clearSessionFailure() {
        sessionFailureMessage = null
        sessionStatus = ""
    }

    private fun updateTitle(id: Long, title: String?) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) {
            val next = TerminalTitlePolicy.safeTitle(title)
            if (tabs[index].title != next) tabs[index] = tabs[index].copy(title = next)
        }
    }

    private fun finishTab(id: Long, session: TerminalSession, exitStatus: Int) {
        val entry = tabs.firstOrNull { it.id == id } ?: return
        if (entry.session !== session) return
        val wasActive = activeTabId == id
        closeTab(id)
        if (wasActive) {
            sessionFailureMessage = if (exitStatus == 0) null else
                "Bash exited unexpectedly (code $exitStatus). Verify the runtime before starting another session."
            sessionStatus = sessionFailureMessage ?: "Bash session ended."
        }
    }

    // Legacy interface compatibility; normal sessions use listenerFor(tabId)
    // so each native paste is routed to its owning tab.
    override fun onNativePasteRequiresConfirmation(text: String) {
        // Legacy unscoped callbacks cannot prove which session requested the paste.
        // Production clients always use listenerFor(tabId).
    }

    // Legacy listener compatibility for existing single-session startup.
    override fun onTitleChanged(title: String?) {
        // Never attribute an unscoped OSC title to whichever tab is now active.
    }
    override fun onSessionFinished(session: TerminalSession, exitStatus: Int) {
        tabs.firstOrNull { it.session === session }?.let { finishTab(it.id, session, exitStatus) }
    }
    override fun onShellPid(pid: Int) {
        // Unscoped legacy PID events must not change the active tab's status.
    }

    /** The foreground service, not the Activity, owns the PTY lifetime. */
    fun closeAllSessions() {
        tabs.toList().forEach { closeTab(it.id) }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
