package com.oryn.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.oryn.terminal.session.OrynTerminalSessionClient
import com.oryn.terminal.ui.OrynTerminalViewClient
import com.termux.terminal.TerminalSession

/**
 * Retains the live PTY session across Activity recreation and owns its teardown.
 * The session client uses the application context and this ViewModel as its listener,
 * so callbacks never retain a destroyed Activity.
 */
class TerminalSessionViewModel : ViewModel(), OrynTerminalSessionClient.Listener {
    var activeSession by mutableStateOf<TerminalSession?>(null)
        private set
    var terminalTitle by mutableStateOf("Bash")
        private set
    var sessionStatus by mutableStateOf("")
        private set
    var sessionFailureMessage by mutableStateOf<String?>(null)
        private set

    var sessionClient: OrynTerminalSessionClient? = null
        private set
    var viewClient: OrynTerminalViewClient? = null
        private set

    fun attachSession(
        session: TerminalSession,
        client: OrynTerminalSessionClient,
        terminalViewClient: OrynTerminalViewClient
    ) {
        activeSession = session
        sessionClient = client
        viewClient = terminalViewClient
        terminalTitle = "Bash"
        sessionStatus = "Starting private Bash session…"
        sessionFailureMessage = null
    }

    fun closeSession() {
        activeSession?.finishIfRunning()
        sessionClient?.attachView(null)
        viewClient?.attachView(null)
        activeSession = null
        sessionClient = null
        viewClient = null
        terminalTitle = "Bash"
        sessionStatus = sessionFailureMessage ?: ""
    }

    fun clearSessionFailure() {
        sessionFailureMessage = null
        sessionStatus = ""
    }

    override fun onTitleChanged(title: String?) {
        terminalTitle = title?.takeIf { it.isNotBlank() } ?: "Bash"
    }

    override fun onSessionFinished(session: TerminalSession, exitStatus: Int) {
        if (activeSession !== session) return
        sessionFailureMessage = if (exitStatus == 0) null else
            "Bash couldn't start or exited unexpectedly (code $exitStatus). Return to setup and verify the runtime."
        sessionStatus = sessionFailureMessage ?: "Bash session ended."
    }

    override fun onShellPid(pid: Int) {
        sessionStatus = "Bash session started (PID $pid)."
    }

    override fun onCleared() {
        closeSession()
        super.onCleared()
    }
}
