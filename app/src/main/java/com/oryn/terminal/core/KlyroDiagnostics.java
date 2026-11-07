package com.oryn.terminal.core;

import java.util.regex.Pattern;

/** User-shareable short error diagnostics without stack traces or private paths. */
public final class KlyroDiagnostics {
    private static final int MAX_LENGTH = 700;
    private static final Pattern PRIVATE_PATH =
            Pattern.compile("/(?:data/(?:data|user/[0-9]+)/[^\\s:]+|storage/emulated/[0-9]+/[^\\s:]+)");
    private KlyroDiagnostics() {}

    public static String sanitize(String message) {
        if (message == null || message.trim().isEmpty()) return "No further details";
        String redacted = PRIVATE_PATH.matcher(message).replaceAll("[private-path]");
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < redacted.length() && output.length() < MAX_LENGTH; i++) {
            char c = redacted.charAt(i);
            if (c == '\n' || c == '\t' || !Character.isISOControl(c)) {
                output.append(c);
            }
        }
        if (redacted.length() > MAX_LENGTH) output.append(" [truncated]");
        return output.toString();
    }

    public static String summarize(Throwable problem) {
        if (problem == null) return "Unknown error";
        return problem.getClass().getSimpleName() + ": " + sanitize(problem.getMessage());
    }
}
