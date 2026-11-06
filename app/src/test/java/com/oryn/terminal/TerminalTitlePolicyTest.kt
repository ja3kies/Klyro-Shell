package com.oryn.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalTitlePolicyTest {
    @Test fun defaultsToBash() {
        assertEquals("Bash", TerminalTitlePolicy.safeTitle(null))
        assertEquals("Bash", TerminalTitlePolicy.safeTitle("  \u001B\u202E\n"))
    }

    @Test fun boundsAndSanitizesUntrustedTitles() {
        assertEquals("Build logs", TerminalTitlePolicy.safeTitle("\u001BBuild\r logs\u202E"))
        assertEquals(64, TerminalTitlePolicy.safeTitle("A".repeat(100000)).length)
        assertEquals("tiny", TerminalTitlePolicy.safeTitle("  tiny  "))
    }
}
