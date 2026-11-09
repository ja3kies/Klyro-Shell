package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroHomeSetupLabelPolicyTest {
    private fun label(
        installing: Boolean = false,
        verifying: Boolean = false,
        runtime: Boolean = false,
        bootstrap: Boolean = false,
        error: Boolean = false,
        sessions: Boolean = false
    ): String = KlyroHomeSetupLabelPolicy.label(
        installing, verifying, runtime, bootstrap, error, sessions)

    @Test fun anInstalledRuntimeAlwaysOffersVerificationNotReinstallation() {
        assertEquals("Recheck environment", label(runtime = true, error = true))
        assertEquals("Verify environment", label(runtime = true))
        assertEquals("Recheck environment", label(runtime = true, error = true, sessions = true))
    }

    @Test fun bootstrapAndRuntimeRepairAreExplicitlyDistinct() {
        assertEquals("Retry PRoot installation", label(bootstrap = true, error = true))
        assertEquals("Install PRoot runtime", label(bootstrap = true))
        assertEquals("Retry private environment setup", label(error = true))
    }

    @Test fun activeSessionsAndOperationsHaveAccuratePriorities() {
        assertEquals("Close sessions to install runtime", label(sessions = true))
        assertEquals("Installing environment…", label(installing = true, sessions = true))
        assertEquals("Checking Bash…", label(verifying = true, sessions = true))
    }
}
