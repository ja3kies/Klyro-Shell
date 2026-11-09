package com.oryn.terminal

/** Labels must describe what onInstall actually does; no implied cleanup or repair. */
internal object KlyroHomeSetupLabelPolicy {
    fun label(
        installing: Boolean,
        verifying: Boolean,
        runtimeInstalled: Boolean,
        bootstrapInstalled: Boolean,
        hasDiagnostics: Boolean,
        hasSessions: Boolean
    ): String = when {
        installing -> "Installing environment…"
        verifying -> "Checking Bash…"
        hasSessions && !runtimeInstalled -> "Close sessions to install runtime"
        runtimeInstalled && hasDiagnostics -> "Recheck environment"
        runtimeInstalled -> "Verify environment"
        bootstrapInstalled && hasDiagnostics -> "Retry PRoot installation"
        bootstrapInstalled -> "Install PRoot runtime"
        hasDiagnostics -> "Retry private environment setup"
        else -> "Install private environment"
    }
}
