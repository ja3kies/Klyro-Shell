package com.oryn.terminal.session;

import android.content.Context;

import com.oryn.terminal.core.OrynPaths;
import com.oryn.terminal.termux.TermuxBootstrapInstaller;

import java.io.File;
import java.io.IOException;
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
        File proot = paths.getProot();
        if (!proot.isFile() || !proot.canExecute()) {
            return "Bash is installed, but Oryn's private proot runtime is missing.";
        }
        return "Bash runtime ready.";
    }

    public boolean isReady() {
        return paths.isBootstrapInstalled()
                && paths.getProot().isFile()
                && paths.getProot().canExecute();
    }

    public TerminalSession startBash(String name, TerminalSession.Listener listener)
            throws IOException {
        if (!paths.isBootstrapInstalled()) {
            throw new IOException("Install Oryn's private Termux-compatible environment first.");
        }

        // buildLoginCommand refuses to start unless Oryn's own proot binary exists.
        String[] command = bootstrap.buildLoginCommand();
        String id = UUID.randomUUID().toString();
        return TerminalSession.start(
                id,
                name == null || name.trim().isEmpty() ? "Bash" : name.trim(),
                command,
                buildHostEnvironment(paths),
                paths.getRoot(),
                24,
                80,
                listener
        );
    }

    /** Host-side environment for proot; all writable paths stay inside Oryn. */
    static Map<String, String> buildHostEnvironment(OrynPaths paths) {
        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("HOME", paths.getHome().getAbsolutePath());
        environment.put("TMPDIR", paths.getTmp().getAbsolutePath());
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
