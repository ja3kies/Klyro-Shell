package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class KlyroJobProbeTest {
    private String stat(int pid, int pgrp, int tty, int tpgid) {
        return pid + " (proot (Bash) unusual) S 1 " + pgrp + " " + pgrp
                + " " + tty + " " + tpgid + " 0 0 0 0";
    }

    @Test public void detectsForegroundJobByTtyGroupNotOutput() {
        KlyroJobProbe.Sample result = KlyroJobProbe.parseStat(
                1422, stat(1422, 1422, 34818, 1435));
        assertEquals(KlyroJobProbe.State.FOREGROUND_JOB, result.state);
        assertTrue(result.hasForegroundJob());
        assertEquals(1435, result.foregroundGroup);
        assertEquals("Foreground job", KlyroJobProbe.statusLabel(result, false));
    }

    @Test public void foregroundShellIsNotClaimedIdle() {
        KlyroJobProbe.Sample shell = KlyroJobProbe.parseStat(
                1422, stat(1422, 1422, 34818, 1422));
        assertEquals(KlyroJobProbe.State.SHELL_FOREGROUND, shell.state);
        assertFalse(shell.hasForegroundJob());
        assertEquals("Shell foreground", KlyroJobProbe.statusLabel(shell, false));
        assertEquals("Recent output", KlyroJobProbe.statusLabel(shell, true));
    }

    @Test public void rejectsForgedOrMalformedProcStat() {
        assertEquals(KlyroJobProbe.State.UNAVAILABLE,
                KlyroJobProbe.parseStat(1422, stat(1421, 1421, 88, 1421)).state);
        assertEquals(KlyroJobProbe.State.UNAVAILABLE,
                KlyroJobProbe.parseStat(1422, "corrupted").state);
        assertEquals(KlyroJobProbe.State.UNAVAILABLE,
                KlyroJobProbe.parseStat(1422, stat(1422, 1422, 0, -1)).state);
        assertEquals(KlyroJobProbe.State.UNAVAILABLE,
                KlyroJobProbe.parseStat(-1, null).state);
        assertEquals(KlyroJobProbe.State.UNAVAILABLE,
                KlyroJobProbe.parseStat(42, "x".repeat(5000)).state);
    }

    @Test public void outputIsNotConflatedWithJobState() {
        KlyroJobProbe.Sample unknown = KlyroJobProbe.parseStat(42, "bad");
        assertFalse(unknown.hasForegroundJob());
        assertEquals("Status unavailable", KlyroJobProbe.statusLabel(unknown, false));
        assertEquals("Recent output · job status unknown",
                KlyroJobProbe.statusLabel(unknown, true));
        assertTrue(KlyroJobProbe.recentOutput(100L, 4099L));
        assertFalse(KlyroJobProbe.recentOutput(100L, 4100L));
        assertFalse(KlyroJobProbe.recentOutput(0L, 10L));
        assertFalse(KlyroJobProbe.recentOutput(250L, 100L));
    }

    @Test public void durationIsMonotonicAndBoundedInPrecision() {
        assertEquals("", KlyroJobProbe.elapsedLabel(0, 1200));
        assertEquals("", KlyroJobProbe.elapsedLabel(2000, 1000));
        assertEquals("9s", KlyroJobProbe.elapsedLabel(1000, 10500));
        assertEquals("1m 8s", KlyroJobProbe.elapsedLabel(1000, 69000));
        assertEquals("1h 2m", KlyroJobProbe.elapsedLabel(1000, 3721000));
    }

    @Test public void stoppedPtyIsNotRepresentedAsRunning() {
        assertEquals(KlyroJobProbe.State.ENDED, KlyroJobProbe.sample(null).state);
        assertEquals("Session ended", KlyroJobProbe.statusLabel(
                KlyroJobProbe.sample(null), false));
    }
}
