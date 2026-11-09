package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KlyroHomeStatusPolicyTest {
    @Test fun runningInstallationIsNotReplacedByTerminalFailure() {
        val state = KlyroHomeStatusPolicy.resolve(
            "Installing verified runtime…",
            "Bash exited unexpectedly (code 1)."
        )
        assertEquals("Installing verified runtime…", state.runtimeMessage)
        assertEquals("Bash exited unexpectedly (code 1).", state.terminalFailure)
    }

    @Test fun runtimeFailureDoesNotOverwriteTerminalSessionError() {
        val state = KlyroHomeStatusPolicy.resolve(
            "Runtime verification failed. Review diagnostics.",
            "Bash exited unexpectedly (code 127)."
        )
        assertEquals("Runtime verification failed. Review diagnostics.", state.runtimeMessage)
        assertEquals("Bash exited unexpectedly (code 127).", state.terminalFailure)
    }

    @Test fun emptyTerminalErrorDoesNotRenderASecondWarning() {
        assertNull(KlyroHomeStatusPolicy.resolve("Environment ready.", null).terminalFailure)
        assertNull(KlyroHomeStatusPolicy.resolve("Environment ready.", "   ").terminalFailure)
    }
}
