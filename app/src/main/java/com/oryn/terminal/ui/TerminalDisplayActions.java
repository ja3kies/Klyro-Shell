package com.oryn.terminal.ui;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import java.nio.charset.StandardCharsets;

/** Clear the emulator output only: never inject control sequences into a running shell. */
public final class TerminalDisplayActions {
    private static final byte[] CLEAR_SCREEN_AND_SCROLLBACK =
            "\u001B[3J\u001B[2J\u001B[H".getBytes(StandardCharsets.US_ASCII);

    private TerminalDisplayActions() {}

    public static void clear(TerminalSession session) {
        if (session == null) return;
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;
        emulator.append(CLEAR_SCREEN_AND_SCROLLBACK, CLEAR_SCREEN_AND_SCROLLBACK.length);
    }
}
