package com.oryn.terminal.ui;

/** Conservative limits for text pasted into an interactive shell. */
public final class ClipboardPastePolicy {
    public static final int MAX_CHARS = 65536;
    private ClipboardPastePolicy() {}

    public static boolean canPaste(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_CHARS) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Reject terminal control sequences; allow tabs and line endings.
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') return false;
            if (c == 0x7f) return false;
        }
        return true;
    }

    public static boolean requiresConfirmation(String value) {
        return value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0);
    }
}
