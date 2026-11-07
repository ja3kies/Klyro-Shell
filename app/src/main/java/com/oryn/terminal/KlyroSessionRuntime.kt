package com.oryn.terminal

import android.os.SystemClock

/** Process-owned sessions; never persisted across device restart or force-stop. */
object KlyroSessionRuntime {
    const val IDLE_TIMEOUT_MS = 6L * 60L * 60L * 1000L
    val sessions = TerminalSessionViewModel()
    @Volatile private var lastInteractionElapsed = SystemClock.elapsedRealtime()

    fun noteInteraction() { lastInteractionElapsed = SystemClock.elapsedRealtime() }

    /** Pure timeout decision, testable without waiting six real hours. */
    fun isIdleExpired(nowElapsed: Long, lastElapsed: Long): Boolean {
        // SystemClock elapsedRealtime is monotonic during a boot. An inverted
        // clock or overflow must not trigger a destructive session shutdown.
        if (nowElapsed < lastElapsed) return false
        return nowElapsed - lastElapsed >= IDLE_TIMEOUT_MS
    }

    fun expireIfIdle(): Boolean {
        if (!sessions.hasLiveSessions()) return false
        if (!isIdleExpired(SystemClock.elapsedRealtime(), lastInteractionElapsed)) return false
        sessions.closeAllSessions()
        return true
    }
}
