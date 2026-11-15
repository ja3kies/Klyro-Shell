package com.oryn.terminal.core;

/** Snapshot of locally known app state. No hidden probing or raw filesystem paths. */
public final class KlyroHealthReport {
    private KlyroHealthReport() {}

    public static String format(
            String version, int androidApi, boolean bootstrap, boolean runtime,
            boolean verified, boolean warnings, int sessions, int projects,
            long usableBytes, String lastError) {
        StringBuilder report = new StringBuilder("KLYRO HEALTH SNAPSHOT\n");
        report.append("App: ").append(KlyroDiagnostics.sanitize(version)).append('\n');
        report.append("Android API: ").append(androidApi).append('\n');
        report.append("CPU: ARM64 target\n");
        report.append("Private bootstrap: ").append(bootstrap ? "INSTALLED" : "MISSING").append('\n');
        report.append("Private proot runtime: ").append(runtime ? "INSTALLED" : "MISSING").append('\n');
        report.append("Bash verification in current app state: ")
                .append(verified ? "PASS" : "NOT CONFIRMED").append('\n');
        report.append("Runtime warnings: ").append(warnings ? "PRESENT" : "NONE REPORTED").append('\n');
        report.append("Open PTY sessions: ").append(Math.max(0, sessions)).append('\n');
        report.append("Saved workspaces: ").append(Math.max(0, projects)).append('\n');
        report.append("Private storage available: ").append(Math.max(0L, usableBytes) / (1024 * 1024))
                .append(" MiB\n");
        if (lastError != null && !lastError.isBlank()) {
            // The shareable report reveals presence of an error, not raw text:
            // diagnostics can contain credentials, URLs, or clipboard snippets.
            report.append("Last reported error: PRESENT (details hidden)\n");
        }
        report.append("Note: a snapshot does not test network access or running commands.\n");
        return report.toString();
    }
}
