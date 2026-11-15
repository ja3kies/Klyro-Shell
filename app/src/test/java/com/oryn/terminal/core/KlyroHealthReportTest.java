package com.oryn.terminal.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class KlyroHealthReportTest {
    @Test public void formatsVerifiedAndUnverifiedStatesTruthfully() {
        String report = KlyroHealthReport.format("0.4.5-dev", 37, true, true,
                false, true, 2, 3, 2048L * 1024 * 1024, "");
        assertTrue(report.contains("NOT CONFIRMED"));
        assertTrue(report.contains("Runtime warnings: PRESENT"));
        assertTrue(report.contains("Open PTY sessions: 2"));
        assertTrue(report.contains("Saved workspaces: 3"));
    }

    @Test public void redactsPrivateDataFromUserFacingReport() {
        String report = KlyroHealthReport.format("0.4.5-dev", 37, true, false,
                false, false, -1, -3, -4, "error at /data/data/com.klyro.shell/files/secrets");
        assertFalse(report.contains("/data/data/"));
        assertTrue(report.contains("details hidden"));
        assertTrue(report.contains("Open PTY sessions: 0"));
        assertTrue(report.contains("Private storage available: 0 MiB"));
    }
}
