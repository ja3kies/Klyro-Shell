package com.oryn.terminal

import android.os.SystemClock

/** Process-owned sessions; never persisted across device restart or force-stop. */
object KlyroSessionRuntime {
    const val IDLE_TIMEOUT_MS = 6L * 60L * 60L * 1000L
    val sessions = TerminalSessionViewModel()
    @Volatile private var lastInteractionElapsed = SystemClock.elapsedRealtime()

    fun noteInteraction() { lastInteractionElapsed = SystemClock.elapsedRealtime() }

    fun expireIfIdle(): Boolean {
        if (!sessions.hasLiveSessions()) return false
        val idle = SystemClock.elapsedRealtime() - lastInteractionElapsed
        if (idle < 0L || idle < IDLE_TIMEOUT_MS) return false
        sessions.closeAllSessions()
        return true
    }
}
