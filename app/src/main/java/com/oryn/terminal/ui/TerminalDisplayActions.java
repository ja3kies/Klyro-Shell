package com.oryn.terminal.ui;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import java.nio.charset.StandardCharsets;

/** Clear the emulator output only: never inject control sequences into a running shell. */
public final class TerminalDisplayActions {
    private static final byte[] CLEAR_SCREEN_AND_SCROLLBACK =
            "\u001B[3J\u001B[2J\u001B[H".getBytes(StandardCharsets.US_ASCII);

    private TerminalDisplayActions() {}

    static String promptToRestore(String line) {
        if (line == null || line.length() > 512 || line.indexOf('\n') >= 0 ||
                line.indexOf('\r') >= 0 || line.indexOf('\u001b') >= 0) return "";
        // Only redraw an *idle* Klyro prompt. Replaying arbitrary process output
        // or partially typed commands would make the displayed PTY diverge from
        // Readline's real input buffer (and may expose hidden text).
        if (!line.matches("^klyro [^\\\\p{Cntrl}\\\\r\\\\n]{1,96} \\\\$ ?$")) return "";
        return line.trim() + " ";
    }

    public static void clear(TerminalSession session) {
        if (session == null) return;
        TerminalEmulator emulator = session.getEmulator();
        if (emulator == null) return;
        // Keep Readline's visible prompt without injecting an Enter or Ctrl+L.
        final int row = emulator.getCursorRow();
        final String promptLine = emulator.getScreen().getSelectedText(
                0, row, emulator.getCursorCol(), row);
        final String prompt = promptToRestore(promptLine);
        // Refuse to erase a line containing a partially typed command: the
        // shell still owns those bytes even when we clear the emulator screen.
        if (prompt.isEmpty()) return;
        emulator.append(CLEAR_SCREEN_AND_SCROLLBACK, CLEAR_SCREEN_AND_SCROLLBACK.length);
        if (!prompt.isEmpty()) {
            byte[] bytes = prompt.getBytes(StandardCharsets.UTF_8);
            emulator.append(bytes, bytes.length);
        }
    }
}
