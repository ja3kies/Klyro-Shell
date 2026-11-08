package com.oryn.terminal.ui;

/** Conservative limits for text pasted into an interactive shell. */
public final class ClipboardPastePolicy {
    public static final int MAX_CHARS = 65536;
    public static final int MAX_UTF8_BYTES = 65536;
    private ClipboardPastePolicy() {}

    private static boolean isWhitespaceOnly(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!Character.isWhitespace(c) && !Character.isSpaceChar(c)) return false;
        }
        return true;
    }

    public static boolean canPaste(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_CHARS || isWhitespaceOnly(value)) return false;
        // Large multibyte pastes are bounded by bytes as well as UTF-16 code units.
        if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Reject Unicode noncharacters, which can be invisible in previews.
            if (c >= '\uFDD0' && c <= '\uFDEF') return false;
            if (c == '\uFFFE' || c == '\uFFFF') return false;
            // Reject all Unicode ISO control characters except tab/newline.
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') return false;
            // Bidi overrides, zero-width characters and Unicode line separators
            // can conceal an executed command or add an invisible newline.
            if (Character.getType(c) == Character.FORMAT ||
                    c == '\u2028' || c == '\u2029') return false;
            // Reject malformed UTF-16 rather than emitting replacement glyphs
            // into shell commands or accidentally corrupting pasted scripts.
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) {
                    return false;
                }
                int cp = Character.toCodePoint(c, value.charAt(i));
                if ((cp & 0xFFFE) == 0xFFFE) return false;
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
