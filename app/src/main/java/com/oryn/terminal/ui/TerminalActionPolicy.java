package com.oryn.terminal.ui;

import java.util.regex.Pattern;

/** Pure policy for deciding when the terminal's extra key can select output. */
public final class TerminalActionPolicy {
    private TerminalActionPolicy() {}

    // Empty default prompt is not useful terminal output to copy.
    private static final Pattern EMPTY_KLYRO_PROMPT =
            Pattern.compile("^klyro\\s+[^\\r\\n]*\\$\\s*$", Pattern.CASE_INSENSITIVE);

    public static boolean hasCopyableOutput(String transcript) {
        if (transcript == null) return false;
        String text = transcript.trim();
        return !text.isEmpty() && !EMPTY_KLYRO_PROMPT.matcher(text).matches();
    }
}
