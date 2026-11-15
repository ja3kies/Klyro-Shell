package com.oryn.terminal.ui;

import com.termux.terminal.TerminalSession;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Read-only, best-effort foreground process group detection for a live PTY.
 * Never injects shell commands, scans other users' processes or persists output.
 * A foreground shell group is not proof that Bash is idle.
 */
public final class KlyroJobProbe {
    public enum State { FOREGROUND_JOB, SHELL_FOREGROUND, UNAVAILABLE, ENDED }

    public static final class Sample {
        public final State state;
        public final int shellPid;
        public final int foregroundGroup;

        private Sample(State state, int shellPid, int foregroundGroup) {
            this.state = state;
            this.shellPid = shellPid;
            this.foregroundGroup = foregroundGroup;
        }

        public boolean hasForegroundJob() { return state == State.FOREGROUND_JOB; }
    }

    private KlyroJobProbe() {}

    public static Sample parseStat(int expectedPid, String stat) {
        if (expectedPid <= 0 || stat == null || stat.length() > 4096) {
            return new Sample(State.UNAVAILABLE, expectedPid, 0);
        }
        // /proc/PID/stat "comm" can contain whitespace and parentheses. Fields
        // after the LAST closing parenthesis start with state, ppid, pgrp,
        // session, tty_nr and tpgid (Linux proc(5)).
        int open = stat.indexOf('(');
        int close = stat.lastIndexOf(')');
        if (open < 1 || close <= open || close + 1 >= stat.length()) {
            return new Sample(State.UNAVAILABLE, expectedPid, 0);
        }
        try {
            int actualPid = Integer.parseInt(stat.substring(0, open).trim());
            if (actualPid != expectedPid) {
                return new Sample(State.UNAVAILABLE, expectedPid, 0);
            }
            String[] columns = stat.substring(close + 1).trim().split("\\s+");
            if (columns.length < 6) return new Sample(State.UNAVAILABLE, expectedPid, 0);
            int group = Integer.parseInt(columns[2]);
            int tty = Integer.parseInt(columns[4]);
            int foreground = Integer.parseInt(columns[5]);
            if (group <= 0 || tty <= 0 || foreground <= 0) {
                return new Sample(State.UNAVAILABLE, expectedPid, 0);
            }
            return new Sample(foreground == group
                    ? State.SHELL_FOREGROUND : State.FOREGROUND_JOB,
                    expectedPid, foreground);
        } catch (NumberFormatException exception) {
            return new Sample(State.UNAVAILABLE, expectedPid, 0);
        }
    }

    /** Must be called off the UI thread; never blocks shell input or output. */
    public static Sample sample(TerminalSession session) {
        if (session == null || !session.isRunning()) {
            return new Sample(State.ENDED, -1, 0);
        }
        int pid = session.getPid();
        if (pid <= 0) return new Sample(State.UNAVAILABLE, pid, 0);
        // Limit scope to the owning TerminalSession PID; do not enumerate /proc.
        File statFile = new File("/proc/" + pid + "/stat");
        try (BufferedReader reader = new BufferedReader(new FileReader(statFile))) {
            Sample result = parseStat(pid, reader.readLine());
            // A closed PTY may disappear while /proc is being read.
            return session.isRunning() ? result : new Sample(State.ENDED, pid, 0);
        } catch (IOException | SecurityException exception) {
            return session.isRunning()
                    ? new Sample(State.UNAVAILABLE, pid, 0)
                    : new Sample(State.ENDED, pid, 0);
        }
    }

    public static boolean recentOutput(long lastOutputElapsedMs, long nowElapsedMs) {
        return lastOutputElapsedMs > 0 && nowElapsedMs >= lastOutputElapsedMs
                && nowElapsedMs - lastOutputElapsedMs < 4_000L;
    }

    public static String statusLabel(Sample sample, boolean recentOutput) {
        if (sample == null) return "Checking";
        switch (sample.state) {
            case FOREGROUND_JOB: return "Foreground job";
            case ENDED: return "Session ended";
            case SHELL_FOREGROUND: return recentOutput ? "Recent output" : "Shell foreground";
            default: return recentOutput ? "Recent output · job status unknown" : "Status unavailable";
        }
    }

    public static String elapsedLabel(long startedElapsedMs, long nowElapsedMs) {
        if (startedElapsedMs <= 0 || nowElapsedMs < startedElapsedMs) return "";
        long seconds = (nowElapsedMs - startedElapsedMs) / 1000L;
        if (seconds < 60L) return seconds + "s";
        if (seconds < 3600L) return seconds / 60 + "m " + seconds % 60 + "s";
        return seconds / 3600 + "h " + (seconds / 60) % 60 + "m";
    }
}
