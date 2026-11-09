package com.oryn.terminal

internal enum class KlyroRuntimeBadgeSeverity { READY, WARNING, ERROR, NEUTRAL }
internal data class KlyroRuntimeBadge(val label: String, val severity: KlyroRuntimeBadgeSeverity)

/** Operation-first status precedence; failed verification must not masquerade as startup. */
internal object KlyroRuntimeBadgePolicy {
    fun resolve(
        installing: Boolean,
        verifying: Boolean,
        verified: Boolean,
        warnings: Boolean,
        bashStarted: Boolean,
        runtimeInstalled: Boolean,
        bootstrapInstalled: Boolean,
        hasFailure: Boolean
    ): KlyroRuntimeBadge = when {
        installing -> KlyroRuntimeBadge("INSTALLING", KlyroRuntimeBadgeSeverity.WARNING)
        verifying -> KlyroRuntimeBadge("CHECKING ENVIRONMENT", KlyroRuntimeBadgeSeverity.WARNING)
        verified && warnings -> KlyroRuntimeBadge("READY WITH WARNINGS", KlyroRuntimeBadgeSeverity.WARNING)
        verified -> KlyroRuntimeBadge("ENVIRONMENT READY", KlyroRuntimeBadgeSeverity.READY)
        hasFailure -> KlyroRuntimeBadge("VERIFICATION FAILED", KlyroRuntimeBadgeSeverity.ERROR)
        bashStarted -> KlyroRuntimeBadge("BASH INCOMPLETE", KlyroRuntimeBadgeSeverity.WARNING)
        runtimeInstalled -> KlyroRuntimeBadge("NOT VERIFIED", KlyroRuntimeBadgeSeverity.WARNING)
        bootstrapInstalled -> KlyroRuntimeBadge("BOOTSTRAP INSTALLED", KlyroRuntimeBadgeSeverity.WARNING)
        else -> KlyroRuntimeBadge("SETUP REQUIRED", KlyroRuntimeBadgeSeverity.NEUTRAL)
    }
}
