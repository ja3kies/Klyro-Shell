package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroVerificationWarningsTest {
    private val mandoc = "update-alternatives: warning: skipping makewhatis because mandoc is missing"

    @Test fun ignoresOnlyExactKnownMandocWarning() {
        val diag = "Exit code: 0\nSTDOUT:\nKLYRO_PATHS_OK\nSTDERR:\n$mandoc"
        assertTrue(KlyroVerificationWarnings.suppressHarmlessWarning(diag, true, true))
        assertFalse(KlyroVerificationWarnings.suppressHarmlessWarning(diag, true, false))
    }

    @Test fun unexpectedUnprefixedStderrIsNotSilenced() {
        val diag = "Exit code: 0\nSTDOUT:\nOK\nSTDERR:\n$mandoc\nmissing shared library"
        assertEquals(listOf("missing shared library"), KlyroVerificationWarnings.unexpectedLines(diag))
        assertFalse(KlyroVerificationWarnings.suppressHarmlessWarning(diag, true, true))
    }

    @Test fun malformedOrEmptyDiagnosticsNeverSilentlyClearReportedWarnings() {
        assertFalse(KlyroVerificationWarnings.suppressHarmlessWarning("bogus", true, true))
        assertFalse(KlyroVerificationWarnings.suppressHarmlessWarning("STDERR:\n(empty)", true, true))
        assertTrue(KlyroVerificationWarnings.unexpectedLines("Exit code: 0\nSTDOUT:\nx\nSTDERR:\n").isEmpty())
    }
}
