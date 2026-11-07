package com.oryn.terminal

import android.os.SystemClock

/** Process-owned sessions; never persisted across device restart or force-stop. */
object KlyroSessionRuntime {
    const val IDLE_TIMEOUT_MS = KlyroIdlePolicy.IDLE_TIMEOUT_MS
    val sessions = TerminalSessionViewModel()
    @Volatile private var lastInteractionElapsed = SystemClock.elapsedRealtime()

    fun noteInteraction() { lastInteractionElapsed = SystemClock.elapsedRealtime() }

    fun expireIfIdle(): Boolean {
        if (!sessions.hasLiveSessions()) return false
        if (!KlyroIdlePolicy.isExpired(SystemClock.elapsedRealtime(), lastInteractionElapsed)) return false
        sessions.closeAllSessions()
        return true
    }
}
