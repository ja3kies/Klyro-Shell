package com.oryn.terminal.session;

import android.content.Context;

import com.oryn.terminal.core.OrynPaths;
import com.oryn.terminal.termux.TermuxBootstrapInstaller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Creates Bash sessions only when Oryn's own bootstrap and private proot
 * executable are both present. It never falls back to the installed Termux app.
 */
public final class TerminalSessionManager {
    private final OrynPaths paths;
    private final TermuxBootstrapInstaller bootstrap;

    public TerminalSessionManager(Context context) throws IOException {
        Context app = context.getApplicationContext();
        this.paths = new OrynPaths(app.getFilesDir());
        this.bootstrap = new TermuxBootstrapInstaller(paths);
        this.paths.prepareLayout();
    }

    public String getReadinessMessage() {
        if (!paths.isBootstrapInstalled()) {
            return "Oryn's private Bash environment is not installed yet.";
        }
        if (!paths.isProotInstalled()) {
            return "Bootstrap is installed, but Oryn's complete private proot runtime is missing.";
        }
        return "Private proot runtime installed; Bash startup still needs device validation.";
    }

    public boolean isReady() {
        return paths.isBootstrapInstalled() && paths.isProotInstalled();
    }

    public com.termux.terminal.TerminalSession startBash(
            String name,
            com.termux.terminal.TerminalSessionClient client
    ) throws IOException {
        if (!isRuntimeInstalled()) {
            throw new IOException("Oryn's private bootstrap and complete proot runtime are required.");
        }

        // Use the maintained Termux PTY + VT terminal engine, but only Oryn-owned paths.
        String[] command = bootstrap.buildLoginCommand();
        String[] args = Arrays.copyOfRange(command, 1, command.length);
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

    /** Host-side environment for proot; all writable paths stay inside Oryn. */
    static Map<String, String> buildHostEnvironment(OrynPaths paths) {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("HOME", paths.getHome().getAbsolutePath());
        environment.put("TMPDIR", paths.getTmp().getAbsolutePath());
        environment.put("LD_LIBRARY_PATH", paths.getProotLibDir().getAbsolutePath());
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
