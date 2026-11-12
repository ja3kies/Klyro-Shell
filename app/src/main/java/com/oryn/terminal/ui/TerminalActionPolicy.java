package com.oryn.terminal.ui;

import java.util.regex.Pattern;

/** Pure policy for deciding when the terminal's extra key can select output. */
public final class TerminalActionPolicy {
    private TerminalActionPolicy() {}
    public static final int MAX_SELECTION_UTF16_UNITS = 2 * 1024 * 1024;

    /**
     * Keep the latest bounded output only. Avoid retaining an entire huge PTY
     * scrollback in Compose state and never split a surrogate pair.
     */
    public static String selectionTail(String text) {
        if (text == null || text.isEmpty()) return "";
        if (text.length() <= MAX_SELECTION_UTF16_UNITS) return text;
        int start = text.length() - MAX_SELECTION_UTF16_UNITS;
        if (start > 0 && Character.isLowSurrogate(text.charAt(start)) &&
                Character.isHighSurrogate(text.charAt(start - 1))) start++;
        return text.substring(start);
    }



    // Empty default prompt is not useful terminal output to copy.
    private static final Pattern EMPTY_KLYRO_PROMPT =
            Pattern.compile("^(?:klyro[ \\t]+[^\\r\\n]*\\$[ \\t]*(?:\\r?\\n|\\r|$))+$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * Single smart key: short tap selects when there is output and pastes when
     * blank. Long press ALWAYS pastes, including after a command produced
     * history. Actual clipboard contents are validated at execution time.
     */
    public static boolean shouldSelect(boolean copyableOutput, boolean longPress) {
        return copyableOutput && !longPress;
    }

    public static boolean canUseSmartKey(boolean copyableOutput, boolean livePty) {
        return copyableOutput || livePty;
    }

    public static boolean hasCopyableOutput(String transcript) {
        if (transcript == null) return false;
        String text = transcript.trim();
        return !text.isEmpty() && !EMPTY_KLYRO_PROMPT.matcher(text).matches();
    }
}
