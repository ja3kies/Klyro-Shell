package com.oryn.terminal.ui;

/** Conservative limits for text pasted into an interactive shell. */
public final class ClipboardPastePolicy {
    public static final int MAX_CHARS = 65536;
    private ClipboardPastePolicy() {}

    public static boolean canPaste(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_CHARS) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Reject all Unicode ISO control characters except tab/newline.
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') return false;
            // Reject malformed UTF-16 rather than emitting replacement glyphs
            // into shell commands or accidentally corrupting pasted scripts.
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) {
                    return false;
                }
            } else if (Character.isLowSurrogate(c)) {
                return false;
            }
        }
        return true;
    }

    public static boolean requiresConfirmation(String value) {
        return value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0);
    }
}
