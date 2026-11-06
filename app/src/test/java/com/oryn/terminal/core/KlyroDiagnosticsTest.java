package com.oryn.terminal.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class KlyroDiagnosticsTest {
    @Test public void avoidsSensitivePathsAndStackTraces() {
        String text = KlyroDiagnostics.summarize(
                new IllegalStateException("Cannot read /data/data/com.klyro.shell/files/private.key"));
        assertTrue(text.startsWith("IllegalStateException:"));
        assertFalse(text.contains("private.key"));
        assertFalse(text.contains("/data/data"));
        assertTrue(text.contains("[private-path]"));
    }
    @Test public void limitsAndFiltersControls() {
        assertEquals("No further details", KlyroDiagnostics.sanitize(null));
        assertFalse(KlyroDiagnostics.sanitize("x\u001by").contains("\u001b"));
        assertTrue(KlyroDiagnostics.sanitize("x".repeat(1200)).length() < 800);
    }
}
