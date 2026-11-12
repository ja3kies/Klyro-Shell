package com.oryn.terminal.ui;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import java.nio.charset.StandardCharsets;

/** Clear the emulator output only: never inject control sequences into a running shell. */
public final class TerminalDisplayActions {
    private static final byte[] CLEAR_SCREEN_AND_SCROLLBACK =
            "\u001B[3J\u001B[2J\u001B[H".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] ACCENT_PROMPT = "\u001B[36m".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RESET_COLORS = "\u001B[0m".getBytes(StandardCharsets.US_ASCII);

    private TerminalDisplayActions() {}

    static String promptToRestore(String line) {
        if (line == null || line.length() > 512 || line.indexOf('\n') >= 0 ||
                line.indexOf('\r') >= 0 || line.indexOf('\u001b') >= 0) return "";
        // Only redraw an *idle* Klyro prompt. Replaying arbitrary process output
        // or partially typed commands would make the displayed PTY diverge from
        // Readline's real input buffer (and may expose hidden text).
        String normalized = line.trim();
        if (!normalized.startsWith("klyro ") || !normalized.endsWith(" $")) return "";
        if (normalized.indexOf('$') != normalized.lastIndexOf('$')) return "";
        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isISOControl(normalized.charAt(i))) return "";
        }
        return line.trim() + " ";
    }

    /** A full clear is safe when the current Bash input can be repainted. */
    public enum ClearResult { CLEARED, SCROLLBACK_ONLY, UNAVAILABLE }

    /**
     * Preserve the *visible* Bash command without writing to stdin. Readline
     * retains its private edit buffer even when the emulator repaints. A full
     * screen clear must not silently erase a partially typed command.
     *
     * Keep this strict: wrapped/long lines, other app prompts, terminal control
     * bytes and full-screen alternate buffers must not be reconstructed.
     */
    static String bashLineToPreserve(String line) {
        if (line == null || line.length() > 512 || !line.startsWith("klyro ")) return "";
        int promptEnd = line.indexOf(" $");
        if (promptEnd < 6) return "";
        int after = promptEnd + 2;
        if (after < line.length() && line.charAt(after) != ' ') return "";
        for (int i = 0; i < line.length(); i++) {
            if (Character.isISOControl(line.charAt(i))) return "";
        }
        return line;
    }

    public static ClearResult clearSafely(TerminalSession session) {
        if (session == null) return ClearResult.UNAVAILABLE;
        final TerminalEmulator emulator = session.getEmulator();
        if (emulator == null || emulator.isAlternateBufferActive()) {
            return ClearResult.UNAVAILABLE;
        }
        final int row = emulator.getCursorRow();
        final int col = emulator.getCursorCol();
        final String visible = emulator.getScreen().getSelectedText(
                0, row, emulator.mColumns - 1, row);
        final String line = bashLineToPreserve(visible);
        if (!line.isEmpty() && !emulator.getScreen().getLineWrap(row)) {
            emulator.append(CLEAR_SCREEN_AND_SCROLLBACK, CLEAR_SCREEN_AND_SCROLLBACK.length);
            final byte[] paint = line.getBytes(StandardCharsets.UTF_8);
            // Accent affects only the prefix; never insert shell data via PTY.
            emulator.append(ACCENT_PROMPT, ACCENT_PROMPT.length);
            emulator.append(paint, paint.length);
            emulator.append(RESET_COLORS, RESET_COLORS.length);
            // Bash/Readline may have a cursor inside the already typed line.
            final byte[] cursor = ("\u001B[" + (col + 1) + "G")
                    .getBytes(StandardCharsets.US_ASCII);
            emulator.append(cursor, cursor.length);
            return ClearResult.CLEARED;
        }
        // A foreground program, wrapped input or unfamiliar prompt may own
        // the screen. Purge old scrollback only; preserve the live viewport
        // and its current input state. CSI 3 J never writes to the child PTY.
        final byte[] historyOnly = "\u001B[3J".getBytes(StandardCharsets.US_ASCII);
        emulator.append(historyOnly, historyOnly.length);
        return ClearResult.SCROLLBACK_ONLY;
    }

    /** Kept for callers expecting the original boolean contract. */
    public static boolean clear(TerminalSession session) {
        return clearSafely(session) != ClearResult.UNAVAILABLE;
    }

}
