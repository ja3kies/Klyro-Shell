package com.oryn.terminal

/** Pure monotonic idle-clock policy; does not load Android services or PTYs. */
object KlyroIdlePolicy {
    const val IDLE_TIMEOUT_MS = 6L * 60L * 60L * 1000L

    fun isExpired(nowElapsed: Long, lastElapsed: Long): Boolean {
        // On reboot or clock anomaly, do not infer six hours from a negative delta.
        if (nowElapsed < lastElapsed) return false
        return nowElapsed - lastElapsed >= IDLE_TIMEOUT_MS
    }
}
