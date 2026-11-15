package com.oryn.terminal

/** Keep runtime progress distinct from events emitted by terminal sessions. */
internal data class KlyroHomeStatus(
    val runtimeMessage: String,
    val terminalFailure: String?
)

internal object KlyroHomeStatusPolicy {
    fun resolve(runtimeMessage: String, terminalFailure: String?): KlyroHomeStatus =
        KlyroHomeStatus(
            runtimeMessage = runtimeMessage,
            terminalFailure = terminalFailure?.takeIf { it.isNotBlank() }
        )
}
