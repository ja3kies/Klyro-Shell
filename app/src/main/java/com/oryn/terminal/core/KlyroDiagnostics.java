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

    /** Provider exception messages can contain private URIs, paths or credentials. */
    public static String storageFailure(Throwable problem) {
        if (problem instanceof SecurityException)
            return "Storage access denied. Reconnect the folder or choose another document.";
        if (problem instanceof java.io.FileNotFoundException)
            return "Document unavailable. Choose it again and check access permissions.";
        if (problem instanceof java.io.InterruptedIOException || problem instanceof java.util.concurrent.CancellationException)
            return "Transfer interrupted. Check the destination before retrying.";
        String message = problem == null ? "" : problem.getMessage();
        if (message != null && (message.contains("ENOSPC") || message.contains("No space left")))
            return "Not enough storage space. Free space and retry.";
        if ("File exceeds 32 MiB limit".equals(message) || "Export limited to 32 MiB".equals(message))
            return "File exceeds the 32 MiB transfer limit. Choose a smaller file.";
        if (problem instanceof java.io.IOException)
            return "Storage provider could not complete the transfer. Check access, connection and free space; retry.";
        return "Storage operation failed (" + (problem == null ? "unknown" : problem.getClass().getSimpleName()) + "). Retry or review Diagnostics.";
    }

    public static String summarize(Throwable problem) {
        if (problem == null) return "Unknown error";
        return problem.getClass().getSimpleName() + ": " + sanitize(problem.getMessage());
    }
}
