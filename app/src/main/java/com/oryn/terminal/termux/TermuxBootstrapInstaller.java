package com.oryn.terminal.termux;

import android.content.Context;
import android.os.Build;

import com.oryn.terminal.core.OrynPaths;
import com.oryn.terminal.core.KlyroDiagnostics;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installs a pinned official Termux bootstrap into Oryn's private sandbox.
 *
 * The Termux paths below are virtual paths required by upstream binaries.
 * proot will bind Oryn's private usr/home directories onto those paths;
 * this installer never reads, writes, or creates files in Termux's app data.
 */
public final class TermuxBootstrapInstaller {
    public interface Listener {
        void onProgress(String stage, int percent);
        void onComplete(boolean success, String message);
    }

    public static final String BOOTSTRAP_TAG = "bootstrap-2026.10.25-r1+apt.android-7";
    public static final String BOOTSTRAP_ASSET = "bootstrap-aarch64.zip";
    public static final String BOOTSTRAP_SHA256 =
            "964e0473a9fa881600927c0151b99896089fb595b81afdcd74b1a6743b8b5889";
    public static final String BOOTSTRAP_URL =
            "https://github.com/termux/termux-packages/releases/download/"
                    + "bootstrap-2026.10.25-r1%2Bapt.android-7/bootstrap-aarch64.zip";

    private static final long MAX_ARCHIVE_BYTES = 80L * 1024L * 1024L;
    private static final long MAX_EXTRACTED_BYTES = 700L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 150000;
    private static final long DOWNLOAD_DEADLINE_NANOS = java.util.concurrent.TimeUnit.MINUTES.toNanos(4);
    private static final long EXTRACTION_DEADLINE_NANOS = java.util.concurrent.TimeUnit.MINUTES.toNanos(8);

    private final OrynPaths paths;
    // Process-wide guard survives Activity recreation and prevents two writers
    // replacing the same bootstrap staging directory concurrently.
    private static final AtomicBoolean installing = new AtomicBoolean(false);
    private volatile String lastErrorDetails;

    public static boolean isBusy() { return installing.get(); }

    public String getLastErrorDetails() {
        return lastErrorDetails;
    }

    public TermuxBootstrapInstaller(Context context) throws IOException {
        this(new OrynPaths(
                context.getApplicationContext().getFilesDir(),
                new File(context.getApplicationContext().getApplicationInfo().nativeLibraryDir)
        ));
    }

    public TermuxBootstrapInstaller(OrynPaths paths) {
        if (paths == null) throw new IllegalArgumentException("OrynPaths is required");
        this.paths = paths;
    }

    public boolean isInstalled() {
        return paths.isBootstrapInstalled();
    }

    /** Safely updates only the generated legacy prompt in an existing private profile. */
    public void migrateShellProfile() throws IOException {
        if (isInstalled()) {
            KlyroStorageCommandBridge.installHelper(paths.getPrefix());
            KlyroTermuxInfo.installHelper(paths.getPrefix());
            writeKlyroBashProfile(new File(paths.getHome(), ".bashrc"));
        }
    }

    /** Installs asynchronously. Listener callbacks run on the installer thread. */
    public void install(Listener listener) {
        if (!installing.compareAndSet(false, true)) {
            complete(listener, false, "Bootstrap installation is already running.");
            return;
        }
        lastErrorDetails = null;
        Thread worker = new Thread(() -> {
            try {
                installInternal(listener);
            } catch (Exception error) {
                lastErrorDetails = KlyroDiagnostics.summarize(error);
                complete(listener, false,
                        "Klyro couldn't finish setting up its private environment. "
                        + "Your existing Termux installation was not modified. You can retry.");
            } finally {
                installing.set(false);
            }
        }, "oryn-bootstrap-installer");
        worker.start();
    }

    private void installInternal(Listener listener) throws Exception {
        if (!"aarch64".equals(archName())) {
            throw new IOException("This build currently supports ARM64 (aarch64) only.");
        }

        paths.prepareLayout();
        File archive = new File(paths.getRoot(), BOOTSTRAP_ASSET);
        File partial = new File(paths.getRoot(), BOOTSTRAP_ASSET + ".part");
        File staging = paths.getStagingPrefix();
        File prefix = paths.getPrefix();
        File previous = paths.getPreviousPrefix();

        // Recover if Android killed the app during the two-rename activation.
        recoverPrefix(prefix, previous);

        if (isValidArchive(archive)) {
            progress(listener, "Verified cached bootstrap archive", 12);
        } else {
            archive.delete();
            partial.delete();
            progress(listener, "Downloading official Termux bootstrap", 3);
            download(BOOTSTRAP_URL, partial, listener);
            String digest = sha256(partial);
            if (!BOOTSTRAP_SHA256.equals(digest)) {
                partial.delete();
                throw new IOException("Bootstrap SHA-256 mismatch; archive rejected.");
            }
            move(partial.toPath(), archive.toPath());
            progress(listener, "Bootstrap checksum verified", 18);
        }

        paths.deletePrivateTree(staging);
        if (!staging.mkdirs() && !staging.isDirectory()) {
            throw new IOException("Cannot create staging directory.");
        }

        progress(listener, "Extracting into isolated staging directory", 22);
        int extracted = extractArchive(archive, staging);
        if (extracted < 1) throw new IOException("Bootstrap archive is empty.");

        progress(listener, "Creating Termux-compatible symlinks", 82);
        createSymlinks(staging);

        File bash = new File(staging, "bin/bash");
        File pkg = new File(staging, "bin/pkg");
        File apt = new File(staging, "bin/apt");
        if (!bash.isFile() || !pkg.isFile() || !apt.isFile()) {
            throw new IOException("Bootstrap validation failed (bash/pkg/apt missing).");
        }
        bash.setExecutable(true, false);
        pkg.setExecutable(true, false);
        apt.setExecutable(true, false);

        File stagingTmp = new File(staging, "tmp");
        if (!stagingTmp.mkdirs() && !stagingTmp.isDirectory()) {
            throw new IOException("Cannot create the private Termux TMPDIR.");
        }

        KlyroStorageCommandBridge.installHelper(staging);
        KlyroTermuxInfo.installHelper(staging);
        writeKlyroBashProfile(new File(paths.getHome(), ".bashrc"));
        writeIfAbsent(new File(paths.getHome(), ".profile"),
                "[ -f \"$HOME/.bashrc\" ] && . \"$HOME/.bashrc\"\n");
        writeIfAbsent(new File(staging, ".oryn-bootstrap-version"),
                BOOTSTRAP_TAG + "\n");

        // Swap only within Oryn's private filesDir. Restore the old prefix if
        // the final rename fails; never touch /data/data/com.termux.
        boolean replaced = prefix.exists();
        activatePrefix(staging, prefix, previous);
        progress(listener, "Klyro environment installed and validated", 100);
        complete(listener, true, "Installed " + BOOTSTRAP_TAG
                + " (ARM64, " + extracted + " files) in Klyro private storage."
                + (replaced ? " Previous packages and files retained in /usr.previous; review before another repair." : ""));
    }

    static void recoverPrefix(File prefix, File previous) throws IOException {
        rejectLinkedPrefix(prefix, previous);
        if (!prefix.exists() && previous.exists()) move(previous.toPath(), prefix.toPath());
        if (previous.exists()) {
            throw new IOException("Previous environment backup exists at /usr.previous. "
                    + "Archive or review it before repairing again; no files were removed.");
        }
    }

    static void activatePrefix(File staging, File prefix, File previous) throws IOException {
        rejectLinkedPrefix(staging, prefix, previous);
        if (!staging.isDirectory()) throw new IOException("Staged prefix is not a directory.");
        if (previous.exists()) throw new IOException("Refusing to overwrite previous environment backup.");
        if (prefix.exists()) move(prefix.toPath(), previous.toPath());
        try {
            move(staging.toPath(), prefix.toPath());
        } catch (IOException swapError) {
            if (previous.exists() && !prefix.exists()) move(previous.toPath(), prefix.toPath());
            throw swapError;
        }
        // A prefix may contain user projects and installed packages, not just bootstrap files.
    }

    private static void rejectLinkedPrefix(File... files) throws IOException {
        for (File file : files) {
            if (Files.isSymbolicLink(file.toPath())) throw new IOException("Refusing a linked prefix transaction path.");
        }
    }

    public static String archName() {
        for (String abi : Build.SUPPORTED_ABIS) {
            if ("arm64-v8a".equals(abi)) return "aarch64";
        }
        return "unsupported";
    }

    /**
     * Constructs the future proot argv. The virtual /data/data/com.termux
     * paths are destinations inside Oryn's rootfs, not host filesystem paths.
     */
    public String[] buildLoginCommand() throws IOException {
        return buildCommand(null);
    }

    /** Builds the same isolated login path with a one-shot marker for startup verification. */
    public String[] buildVerificationCommand(String marker) throws IOException {
        if (marker == null || !marker.matches("[A-Z0-9_]{1,64}")) {
            throw new IllegalArgumentException("A safe verification marker is required.");
        }
        return buildCommand(marker);
    }

    private String[] buildCommand(String verificationMarker) throws IOException {
        if (!isInstalled()) throw new IOException("Klyro bootstrap is not installed.");
        if (!paths.isProotInstalled()) {
            throw new IOException("Klyro's APK-native PRoot runtime is not installed yet.");
        }
        String prefix = OrynPaths.TERMUX_VIRTUAL_PREFIX;
        String home = OrynPaths.TERMUX_VIRTUAL_HOME;

        java.util.ArrayList<String> args = new java.util.ArrayList<>();
        args.add(paths.getProot().getAbsolutePath());
        args.add("--link2symlink");
        args.add("--kill-on-exit");
        args.add("-r");
        args.add(paths.getRoot().getAbsolutePath());
        addBind(args, "/dev");
        addBind(args, "/proc");
        addBind(args, "/sys");
        addBind(args, "/system");
        addBindIfPresent(args, "/apex");
        addBindIfPresent(args, "/vendor");
        addBindIfPresent(args, "/product");
        // Android's linker reads its generated config from /linkerconfig. Bind only the
        // read-only, system-owned config files (never the directory), and only if they exist.
        addLinkerConfigBinds(args, hostFile -> new File(hostFile).isFile());
        for (String hostFile : LINKER_CONFIG_FILES) {
            if (new File(hostFile).isFile()) paths.ensureFileBindTarget(hostFile);
        }
        args.add("-b");
        args.add(paths.getPrefix().getAbsolutePath() + ":" + prefix);
        args.add("-b");
        args.add(paths.getHome().getAbsolutePath() + ":" + home);
        args.add("-w");
        args.add(home);
        args.add("/usr/bin/env");
        args.add("-i");
        args.add("HOME=" + home);
        args.add("PREFIX=" + prefix);
        args.add("PATH=" + prefix + "/bin:/system/bin:/system/xbin");
        args.add("TERM=xterm-256color");
        args.add("COLORTERM=truecolor");
        // Keep the configured signed HTTPS source instead of probing/randomizing upstream mirrors.
        args.add("TERMUX_PKG_NO_MIRROR_SELECT=1");
        args.add("LANG=en_US.UTF-8");
        args.add("TMPDIR=" + prefix + "/tmp");
        args.add("LD_PRELOAD=" + prefix + "/lib/libtermux-exec.so");
        args.add(prefix + "/bin/bash");
        args.add("--login");
        if (verificationMarker != null) {
            args.add("-c");
            args.add("printf '%s\\n' '" + verificationMarker + "'; "
                    + "if [[ \"$HOME\" == '" + home + "' && \"$PREFIX\" == '" + prefix + "' "
                    + "&& \"$TMPDIR\" == '" + prefix + "/tmp' "
                    + "&& -d \"$HOME\" && -d \"$PREFIX/bin\" && -d \"$TMPDIR\" "
                    + "&& -x '" + prefix + "/bin/bash' ]]; "
                    + "then printf '%s\\n' 'KLYRO_PATHS_OK'; "
                    + "else printf '%s\\n' 'KLYRO_PATHS_INVALID'; exit 24; fi; "
                    + "uid=\"$(id -u)\"; "
                    + "if [[ \"$uid\" =~ ^[0-9]+$ ]] && [ \"$uid\" -ne 0 ]; "
                    + "then printf '%s\\n' 'KLYRO_UID_OK'; "
                    + "else printf 'KLYRO_UID_INVALID=%s\\n' \"$uid\"; exit 21; fi; "
                    + "if apt --version >/dev/null 2>&1; then printf '%s\\n' 'KLYRO_APT_OK'; "
                    + "else printf '%s\\n' 'KLYRO_APT_FAILED'; exit 22; fi; "
                    + "if command -v pkg >/dev/null 2>&1 && pkg list-installed >/dev/null 2>&1; "
                    + "then printf '%s\\n' 'KLYRO_PKG_OK'; "
                    + "else printf '%s\\n' 'KLYRO_PKG_FAILED'; exit 23; fi");
        }
        return args.toArray(new String[0]);
    }

    static final String[] LINKER_CONFIG_FILES = {
            "/linkerconfig/ld.config.txt",
            "/linkerconfig/com.android.art/ld.config.txt"
    };

    static void addLinkerConfigBinds(java.util.List<String> args,
                                     java.util.function.Predicate<String> hostFileExists) {
        for (String hostFile : LINKER_CONFIG_FILES) {
            if (hostFileExists.test(hostFile)) addBind(args, hostFile);
        }
    }

    private static void addBind(java.util.List<String> args, String hostPath) {
        args.add("-b");
        args.add(hostPath);
    }

    private static void addBindIfPresent(java.util.List<String> args, String hostPath) {
        if (new File(hostPath).isDirectory()) addBind(args, hostPath);
    }

    private boolean isValidArchive(File file) throws Exception {
        return file.isFile() && file.length() > 0
                && file.length() <= MAX_ARCHIVE_BYTES
                && BOOTSTRAP_SHA256.equals(sha256(file));
    }

    static void ensureWorkAllowed(long started, long limitNanos) throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            throw new IOException("Bootstrap operation was interrupted; retry from Klyro.");
        }
        if (System.nanoTime() - started > limitNanos) {
            throw new IOException("Bootstrap operation exceeded its time limit; check storage and retry.");
        }
    }

    private void download(String url, File destination, Listener listener) throws IOException {
        final long started = System.nanoTime();
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(45000);
            connection.setUseCaches(false);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "KlyroShell/0.1");
            ensureWorkAllowed(started, DOWNLOAD_DEADLINE_NANOS);
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Bootstrap download returned HTTP " + status);
            }
            long expected = connection.getContentLengthLong();
            if (expected == 0 || expected > MAX_ARCHIVE_BYTES) {
                throw new IOException("Bootstrap archive content length is invalid.");
            }
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[65536];
                long total = 0;
                int count;
                while ((count = in.read(buffer)) != -1) {
                    ensureWorkAllowed(started, DOWNLOAD_DEADLINE_NANOS);
                    total += count;
                    if (total > MAX_ARCHIVE_BYTES) throw new IOException("Bootstrap archive limit exceeded.");
                    out.write(buffer, 0, count);
                    if (expected > 0) {
                        progress(listener, "Downloading bootstrap", 3 + (int) (total * 12 / expected));
                    }
                }
                if (expected >= 0 && total != expected) {
                    throw new IOException("Incomplete bootstrap download.");
                }
            }
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private int extractArchive(File archive, File destination) throws IOException {
        final long started = System.nanoTime();
        int count = 0;
        long totalBytes = 0;
        byte[] buffer = new byte[65536];
        String base = destination.getCanonicalPath() + File.separator;

        try (ZipInputStream zip = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                ensureWorkAllowed(started, EXTRACTION_DEADLINE_NANOS);
                if (++count > MAX_ENTRIES) throw new IOException("Too many archive entries.");
                String name = safeEntryName(entry.getName());
                if (name.isEmpty()) {
                    zip.closeEntry();
                    continue;
                }
                File target = new File(destination, name);
                String canonical = target.getCanonicalPath();
                if (!canonical.startsWith(base)) {
                    throw new IOException("Unsafe archive path rejected: " + name);
                }
                if (entry.isDirectory()) {
                    if (!target.mkdirs() && !target.isDirectory()) {
                        throw new IOException("Cannot create directory: " + name);
                    }
                } else {
                    File parent = target.getParentFile();
                    if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                        throw new IOException("Cannot create archive parent.");
                    }
                    try (OutputStream out = new FileOutputStream(target)) {
                        int n;
                        while ((n = zip.read(buffer)) != -1) {
                            ensureWorkAllowed(started, EXTRACTION_DEADLINE_NANOS);
                            totalBytes += n;
                            if (totalBytes > MAX_EXTRACTED_BYTES) {
                                throw new IOException("Extracted archive size limit exceeded.");
                            }
                            out.write(buffer, 0, n);
                        }
                    }
                    if (name.startsWith("bin/") || name.startsWith("libexec/")
                            || name.startsWith("lib/apt/methods/")) {
                        target.setExecutable(true, false);
                    }
                }
                zip.closeEntry();
            }
        }
        return count;
    }

    /** Termux stores symlink metadata as "target←link" in SYMLINKS.txt. */
    void createSymlinks(File root) throws IOException {
        File map = new File(root, "SYMLINKS.txt");
        if (!map.isFile()) throw new IOException("Bootstrap is missing SYMLINKS.txt.");
        Path rootPath = root.getCanonicalFile().toPath().normalize();
        Path virtualPrefix = java.nio.file.Paths.get(OrynPaths.TERMUX_VIRTUAL_PREFIX).normalize();
        String content = new String(Files.readAllBytes(map.toPath()), StandardCharsets.UTF_8);
        for (String line : content.split("\\r?\\n")) {
            int arrow = line.indexOf('\u2190');
            if (arrow <= 0 || arrow == line.length() - 1) continue;
            String target = line.substring(0, arrow).trim();
            String link = line.substring(arrow + 1).trim();
            while (link.startsWith("./")) link = link.substring(2);
            if (target.isEmpty() || target.indexOf(0) >= 0 || link.isEmpty()) {
                throw new IOException("Invalid symlink entry: " + line);
            }

            Path linkRelative = java.nio.file.Paths.get(link);
            if (linkRelative.isAbsolute()) {
                throw new IOException("Absolute symlink location rejected: " + link);
            }
            Path linkPath = rootPath.resolve(linkRelative).normalize();
            if (!linkPath.startsWith(rootPath) || linkPath.equals(rootPath)) {
                throw new IOException("Symlink location escapes private prefix: " + link);
            }
            Path parent = linkPath.getParent();
            if (parent == null) throw new IOException("Symlink has no parent: " + link);
            ensureNoSymlinkParents(rootPath, parent);
            Files.createDirectories(parent);
            ensureNoSymlinkParents(rootPath, parent);

            Path targetPath = java.nio.file.Paths.get(target);
            if (targetPath.isAbsolute()) {
                Path normalizedAbsolute = targetPath.normalize();
                // Termux binaries use this virtual prefix. proot maps it to
                // Oryn's private prefix; no host Termux data is accessed.
                if (!normalizedAbsolute.equals(virtualPrefix)
                        && !normalizedAbsolute.startsWith(virtualPrefix)) {
                    throw new IOException("Absolute symlink target is outside Termux prefix: "
                            + target + " (link: " + link + ")");
                }
            } else {
                Path resolvedTarget = parent.resolve(targetPath).normalize();
                if (!resolvedTarget.startsWith(rootPath)) {
                    throw new IOException("Relative symlink target escapes private prefix: "
                            + target + " (link: " + link + ")");
                }
            }

            Files.deleteIfExists(linkPath);
            Files.createSymbolicLink(linkPath, targetPath);
        }
    }

    private static void ensureNoSymlinkParents(Path root, Path directory) throws IOException {
        Path current = root;
        Path relative = root.relativize(directory);
        for (Path part : relative) {
            current = current.resolve(part);
            if (Files.isSymbolicLink(current)) {
                throw new IOException("Symlink parent rejected while creating bootstrap link: "
                        + current);
            }
        }
    }

    private static String safeEntryName(String raw) throws IOException {
        if (raw == null || raw.indexOf('\u0000') >= 0) throw new IOException("Invalid archive entry.");
        String name = raw.replace('\\', '/');
        while (name.startsWith("./")) name = name.substring(2);
        if (name.startsWith("/") || name.matches("^[A-Za-z]:.*")) {
            throw new IOException("Absolute archive entry rejected.");
        }
        for (String part : name.split("/")) {
            if ("..".equals(part)) throw new IOException("Path traversal entry rejected.");
        }
        return name;
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int n;
            while ((n = in.read(buffer)) != -1) digest.update(buffer, 0, n);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) {
            result.append(String.format(Locale.US, "%02x", value & 0xff));
        }
        return result.toString();
    }

    private static void move(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(from, to);
        }
    }

    private static String diagnosticDetails(Exception error) {
        java.io.StringWriter buffer = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(buffer));
        return buffer.toString();
    }


    /** Theme-aware ANSI prompt: never hardcode neon truecolor on a light canvas. */
    private static final String THEMED_PROMPT =
            "export PS1='\\[\\e[36m\\]klyro\\[\\e[0m\\] \\w $ '";

    /** Replace only prompts generated by older Klyro/Oryn builds; leave custom PS1 intact. */
    static String migrateGeneratedPrompt(String content) {
        String legacyOryn =
                "export PS1='\\[\\e[38;2;0;240;255m\\]oryn\\[\\e[0m\\] \\w $ '";
        String legacyKlyro =
                "export PS1='\\[\\e[38;2;0;240;255m\\]klyro\\[\\e[0m\\] \\w $ '";
        return content.replace(legacyOryn, THEMED_PROMPT)
                .replace(legacyKlyro, THEMED_PROMPT);
    }

    private static String augmentKlyroProfile(String content) {
        return KlyroTermuxInfo.augmentGeneratedProfile(
                KlyroStorageCommandBridge.augmentGeneratedProfile(
                        KlyroWelcomeGuide.augmentGeneratedProfile(content)));
    }

    /** Backup before a tightly scoped, atomic update of a generated prompt. */
    private static void writeKlyroBashProfile(File file) throws IOException {
        String defaultProfile =
                "# Klyro Shell profile\n"
                + THEMED_PROMPT + "\n"
                + "export EDITOR=nano\nexport PAGER=less\n"
                + "alias ll='ls -lah'\nalias la='ls -A'\n";
        if (!file.isFile()) {
            writeIfAbsent(file, augmentKlyroProfile(defaultProfile));
            return;
        }

        String existing = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        String migrated = augmentKlyroProfile(migrateGeneratedPrompt(existing));
        if (existing.equals(migrated)) return;

        File backup = new File(file.getParentFile(), ".bashrc.klyro-before-color.bak");
        if (!backup.exists()) Files.copy(file.toPath(), backup.toPath());

        Path staging = Files.createTempFile(file.getParentFile().toPath(), ".klyro-ps1-", ".tmp");
        try {
            Files.write(staging, migrated.getBytes(StandardCharsets.UTF_8));
            try {
                Files.move(staging, file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                // Private FS should support atomic rename; preserve functionality
                // on unusual Android vendors without deleting the backup.
                Files.move(staging, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    private static void writeIfAbsent(File file, String value) throws IOException {
        if (file.isFile() && file.length() > 0) return;
        File parent = file.getParentFile();
        if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Cannot create " + parent);
        }
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static void progress(Listener listener, String stage, int percent) {
        if (listener != null) listener.onProgress(stage, Math.min(99, Math.max(0, percent)));
    }

    private static void complete(Listener listener, boolean success, String message) {
        if (listener != null) listener.onComplete(success, message);
    }
}
