package com.oryn.terminal

/** Keep session navigation independent from the health of a new shell runtime. */
internal enum class KlyroHomeAction { RESUME_SESSION, START_BASH, NONE }

internal object KlyroHomeActionPolicy {
    fun decide(runtimeVerified: Boolean, hasSessions: Boolean): KlyroHomeAction = when {
        hasSessions -> KlyroHomeAction.RESUME_SESSION
        runtimeVerified -> KlyroHomeAction.START_BASH
        else -> KlyroHomeAction.NONE
    }
}
