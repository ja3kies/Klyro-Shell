package com.oryn.terminal.session;

import android.content.Context;

import com.oryn.terminal.core.OrynPaths;
import com.oryn.terminal.termux.TermuxBootstrapInstaller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Creates Bash sessions only when Oryn's own bootstrap and private proot
 * executable are both present. It never falls back to the installed Termux app.
 */
public final class TerminalSessionManager {
    private static final String VERIFY_MARKER = "__KLYRO_BASH_START_OK__";
    private static final int VERIFY_TIMEOUT_SECONDS = 20;
    private static final int MAX_DIAGNOSTIC_BYTES = 64 * 1024;


    public static final class VerificationResult {
        public final boolean bashStarted;
        public final boolean uidValid;
        public final boolean aptResponds;
        public final boolean pkgResponds;
        public final boolean hasWarnings;
        public final int exitCode;
        public final String diagnostics;

        VerificationResult(boolean bashStarted, boolean uidValid, boolean aptResponds,
                           boolean pkgResponds, int exitCode, String diagnostics,
                           boolean hasWarnings) {
            this.bashStarted = bashStarted;
            this.uidValid = uidValid;
            this.aptResponds = aptResponds;
            this.pkgResponds = pkgResponds;
            this.hasWarnings = hasWarnings;
            this.exitCode = exitCode;
            this.diagnostics = diagnostics == null ? "" : diagnostics;
        }

        public boolean isEnvironmentReady() {
            return exitCode == 0 && bashStarted && uidValid && aptResponds && pkgResponds;
        }
    }

    private final OrynPaths paths;
    private final TermuxBootstrapInstaller bootstrap;

    public TerminalSessionManager(Context context) throws IOException {
        Context app = context.getApplicationContext();
        this.paths = new OrynPaths(
                app.getFilesDir(),
                new File(app.getApplicationInfo().nativeLibraryDir)
        );
        this.bootstrap = new TermuxBootstrapInstaller(paths);
        this.paths.prepareLayout();
    }

    public String getReadinessMessage() {
        if (!paths.isBootstrapInstalled()) {
            return "Klyro's private Bash environment is not installed yet.";
        }
        if (!paths.isProotInstalled()) {
            return "Bootstrap is installed, but Klyro's complete private PRoot runtime is missing.";
        }
        return "Runtime installed · Bash startup has not been verified on this device.";
    }

    public boolean isRuntimeInstalled() {
        return paths.isBootstrapInstalled() && paths.isProotInstalled();
    }

    /** Runs Bash and verifies UID plus both Termux package-manager entry points. */
    public VerificationResult verifyBash() throws IOException {
        if (!isRuntimeInstalled()) {
            throw new IOException("Klyro's private bootstrap and APK-native PRoot runtime are required.");
        }
        File stdoutFile = File.createTempFile("klyro-check-out-", ".log", paths.getTmp());
        File stderrFile = File.createTempFile("klyro-check-err-", ".log", paths.getTmp());
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    bootstrap.buildVerificationCommand(VERIFY_MARKER));
            builder.directory(paths.getRoot());
            builder.redirectOutput(stdoutFile);
            builder.redirectError(stderrFile);
            builder.environment().clear();
            builder.environment().putAll(buildHostEnvironment(paths));
            process = builder.start();

            boolean finished;
            try {
                finished = process.waitFor(VERIFY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException interrupted) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new IOException("Klyro environment verification was interrupted.", interrupted);
            }
            if (!finished) {
                process.destroyForcibly();
                try {
                    process.waitFor(2, TimeUnit.SECONDS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
                throw new IOException("Klyro environment verification timed out after "
                        + VERIFY_TIMEOUT_SECONDS + " seconds. "
                        + diagnostics(stdoutFile, stderrFile, -1));
            }

            int exitCode = process.exitValue();
            String stdout = readOutput(stdoutFile);
            String stderr = readOutput(stderrFile);
            boolean bashStarted = stdout.contains(VERIFY_MARKER);
            boolean uidValid = stdout.contains("KLYRO_UID_OK");
            boolean aptResponds = stdout.contains("KLYRO_APT_OK");
            boolean pkgResponds = stdout.contains("KLYRO_PKG_OK");
            return new VerificationResult(
                    bashStarted, uidValid, aptResponds, pkgResponds, exitCode,
                    diagnostics(stdout, stderr, exitCode), !stderr.isEmpty());
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
            stdoutFile.delete();
            stderrFile.delete();
        }
    }

    private static String diagnostics(String stdout, String stderr, int exitCode) {
        return "Exit code: " + exitCode
                + "\nSTDOUT:\n" + (stdout == null || stdout.isEmpty() ? "(empty)" : stdout)
                + "\nSTDERR:\n" + (stderr == null || stderr.isEmpty() ? "(empty)" : stderr);
    }

    private static String diagnostics(File stdout, File stderr, int exitCode) throws IOException {
        return diagnostics(readOutput(stdout), readOutput(stderr), exitCode);
    }

    private static String readOutput(File file) throws IOException {
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1 && total < MAX_DIAGNOSTIC_BYTES) {
                int accepted = Math.min(count, MAX_DIAGNOSTIC_BYTES - total);
                output.write(buffer, 0, accepted);
                total += accepted;
            }
            return output.toString(StandardCharsets.UTF_8.name()).trim();
        }
    }

    public com.termux.terminal.TerminalSession startBash(
            String name,
            com.termux.terminal.TerminalSessionClient client
    ) throws IOException {
        if (!isRuntimeInstalled()) {
            throw new IOException("Klyro's private bootstrap and complete PRoot runtime are required.");
        }

        // Use the maintained Termux PTY + VT terminal engine, but only Oryn-owned paths.
        String[] command = bootstrap.buildLoginCommand();
        // JNI calls execvp(cmd, argv): argv[0] must name the executable, not the first option.
        String[] args = buildTerminalArgv(command);
        Map<String, String> hostEnvironment = buildHostEnvironment(paths);
        String[] environment = new String[hostEnvironment.size()];
        int index = 0;
        for (Map.Entry<String, String> entry : hostEnvironment.entrySet()) {
            environment[index++] = entry.getKey() + "=" + entry.getValue();
        }

        com.termux.terminal.TerminalSession session =
                new com.termux.terminal.TerminalSession(
                        command[0],
                        paths.getRoot().getAbsolutePath(),
                        args,
                        environment,
                        5000,
                        client
                );
        session.mSessionName = name == null || name.trim().isEmpty()
                ? "Bash" : name.trim();
        return session;
    }

    static String[] buildTerminalArgv(String[] command) {
        if (command == null || command.length == 0 || command[0] == null
                || command[0].trim().isEmpty()) {
            throw new IllegalArgumentException("A complete command with argv[0] is required.");
        }
        return Arrays.copyOf(command, command.length);
    }

    /** Host-side environment for proot; all writable paths stay inside Oryn. */
    static Map<String, String> buildHostEnvironment(OrynPaths paths) {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("HOME", paths.getHome().getAbsolutePath());
        environment.put("TMPDIR", paths.getTmp().getAbsolutePath());
        environment.put("LD_LIBRARY_PATH", paths.getProotDependencyDir().getAbsolutePath()
                + File.pathSeparator + paths.getProotLibDir().getAbsolutePath());
        environment.put("PROOT_LOADER", paths.getProotLoader().getAbsolutePath());
        environment.put("PROOT_TMP_DIR", paths.getTmp().getAbsolutePath());
        environment.put("PATH", "/system/bin:/system/xbin:/vendor/bin");
        environment.put("ANDROID_ROOT", "/system");
        environment.put("ANDROID_DATA", "/data");
        environment.put("TERM", "xterm-256color");
        environment.put("COLORTERM", "truecolor");
        environment.put("LANG", "en_US.UTF-8");
        return Collections.unmodifiableMap(environment);
    }

    public OrynPaths getPaths() {
        return paths;
    }
}
