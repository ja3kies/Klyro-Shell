package com.oryn.terminal.ui;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

/** History is ours; the viewport, edit buffer and escape parser belong to the running program. */
public final class TerminalDisplayActions {
    private TerminalDisplayActions() {}
    public enum ClearResult { CLEARED, SCROLLBACK_ONLY, UNAVAILABLE }

    public static ClearResult clearSafely(TerminalSession session) {
        if (session == null || !session.isRunning()) return ClearResult.UNAVAILABLE;
        return clearEmulator(session.getEmulator());
    }

    static ClearResult clearEmulator(TerminalEmulator emulator) {
        if (emulator == null || emulator.isAlternateBufferActive()) return ClearResult.UNAVAILABLE;
        // Never feed synthetic CSI into a parser that may be halfway through real PTY output.
        emulator.getScreen().clearTranscript();
        return ClearResult.SCROLLBACK_ONLY;
    }

    public static boolean clear(TerminalSession session) {
        return clearSafely(session) != ClearResult.UNAVAILABLE;
    }
}
