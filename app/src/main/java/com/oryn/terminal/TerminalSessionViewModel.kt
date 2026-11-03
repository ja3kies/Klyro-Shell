package com.oryn.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
        val customTitle: String? = null
    )

    private val tabs = mutableStateListOf<ShellTab>()
    val sessions: List<ShellTab> get() = tabs
    private var nextId = 1L
    var activeTabId by mutableStateOf<Long?>(null)
        private set

    private val selected: ShellTab? get() = tabs.firstOrNull { it.id == activeTabId }
    val activeSession: TerminalSession? get() = selected?.session
    val sessionClient: OrynTerminalSessionClient? get() = selected?.client
    val viewClient: OrynTerminalViewClient? get() = selected?.viewClient
    val terminalTitle: String get() = selected?.let { it.customTitle ?: it.title } ?: "Bash"

    var sessionStatus by mutableStateOf("")
        private set
    var sessionFailureMessage by mutableStateOf<String?>(null)
        private set

    fun listenerFor(tabId: Long): OrynTerminalSessionClient.Listener =
        object : OrynTerminalSessionClient.Listener {
            override fun onTitleChanged(title: String?) {
                updateTitle(tabId, title)
            }
            override fun onSessionFinished(session: TerminalSession, exitStatus: Int) {
                finishTab(tabId, session, exitStatus)
            }
            override fun onShellPid(pid: Int) {
                if (activeTabId == tabId) sessionStatus = "Bash session started (PID $pid)."
            }
        }

    fun reserveTabId(): Long = nextId++

    fun attachSession(
        session: TerminalSession,
        client: OrynTerminalSessionClient,
        terminalViewClient: OrynTerminalViewClient,
        tabId: Long = reserveTabId()
    ) {
        require(tabs.none { it.id == tabId }) { "Duplicate terminal tab ID" }
        tabs.add(ShellTab(tabId, session, client, terminalViewClient))
        selectTab(tabId)
        sessionStatus = "Starting private Bash session…"
        sessionFailureMessage = null
    }

    fun selectTab(id: Long) {
        if (activeTabId == id || tabs.none { it.id == id }) return
        selected?.let {
            it.client.attachView(null)
            it.viewClient.attachView(null)
        }
        activeTabId = id
        sessionFailureMessage = null
        sessionStatus = ""
    }

    fun renameTab(id: Long, rawName: String) {
        val name = rawName.trim().take(32)
        if (name.isBlank()) return
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) tabs[index] = tabs[index].copy(customTitle = name)
    }

    fun closeTab(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val entry = tabs.removeAt(index)
        entry.client.attachView(null)
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
            tabs[index] = tabs[index].copy(title = title?.takeIf { it.isNotBlank() } ?: "Bash")
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

    // Legacy listener compatibility for existing single-session startup.
    override fun onTitleChanged(title: String?) {
        activeTabId?.let { updateTitle(it, title) }
    }
    override fun onSessionFinished(session: TerminalSession, exitStatus: Int) {
        tabs.firstOrNull { it.session === session }?.let { finishTab(it.id, session, exitStatus) }
    }
    override fun onShellPid(pid: Int) {
        sessionStatus = "Bash session started (PID $pid)."
    }

    override fun onCleared() {
        tabs.toList().forEach { closeTab(it.id) }
        super.onCleared()
    }
}
