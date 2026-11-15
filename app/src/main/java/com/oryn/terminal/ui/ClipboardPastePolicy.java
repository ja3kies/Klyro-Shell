package com.oryn.terminal.ui;

/** Conservative limits for text pasted into an interactive shell. */
public final class ClipboardPastePolicy {
    public static final int MAX_CHARS = 65536;
    public static final int MAX_UTF8_BYTES = 65536;
    private ClipboardPastePolicy() {}

    public static boolean canPaste(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_CHARS) return false;
        // Large multibyte pastes are bounded by bytes as well as UTF-16 code units.
        if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // Reject Unicode noncharacters, which can be invisible in previews.
            if (c >= '\uFDD0' && c <= '\uFDEF') return false;
            if (c == '\uFFFE' || c == '\uFFFF') return false;
            // Reject all Unicode ISO control characters except tab/newline.
            if (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t') return false;
            // Keep Unicode shaping and emoji joiners, but reject invisible edits
            // to ASCII commands, isolated joiners, bidi overrides and other format controls.
            if (c == '\u200C' || c == '\u200D') {
                if (i == 0 || i + 1 == value.length()) return false;
                int before = value.codePointBefore(i);
                int after = value.codePointAt(i + 1);
                if (before <= 0x7f || after <= 0x7f ||
                        Character.getType(before) == Character.FORMAT ||
                        Character.getType(after) == Character.FORMAT) return false;
            } else if (Character.getType(c) == Character.FORMAT ||
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

    /** OSC 52 output may request a system clipboard write; only honor it
     * for a live, visible session and a bounded, safe text payload. */
    public static boolean canCopyFromTerminal(String value, boolean uiVisible, boolean running) {
        return uiVisible && running && canPaste(value);
    }

    public static boolean requiresConfirmation(String value) {
        return value != null && (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0);
    }
}
