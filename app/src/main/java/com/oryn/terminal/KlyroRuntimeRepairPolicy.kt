package com.oryn.terminal

/** Reinstalling a private bootstrap must never interrupt an active PTY silently. */
internal object KlyroRuntimeRepairPolicy {
    fun canOffer(
        runtimeInstalled: Boolean,
        runtimeVerified: Boolean,
        hasDiagnostics: Boolean,
        hasLiveSessions: Boolean,
        installing: Boolean,
        verifying: Boolean
    ): Boolean = runtimeInstalled && !runtimeVerified && hasDiagnostics &&
        !hasLiveSessions && !installing && !verifying
}
