package com.oryn.terminal.ui;

/** Conservative limits for text pasted into an interactive shell. */
public final class ClipboardPastePolicy {
    public static final int MAX_CHARS = 65536;
    private ClipboardPastePolicy() {}

    public static boolean canPaste(String value) {
        return value != null && !value.isEmpty() && value.length() <= MAX_CHARS
            && value.indexOf('\u0000') < 0;
    }

    public static boolean requiresConfirmation(String value) {
        return value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0);
    }
}
