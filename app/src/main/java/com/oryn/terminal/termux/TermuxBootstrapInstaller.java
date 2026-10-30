package com.oryn.terminal.termux;

import android.content.Context;
import android.os.Build;

import com.oryn.terminal.core.OrynPaths;

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

    private final OrynPaths paths;
    private final AtomicBoolean installing = new AtomicBoolean(false);

    public TermuxBootstrapInstaller(Context context) throws IOException {
        this(new OrynPaths(context.getApplicationContext().getFilesDir()));
    }

    public TermuxBootstrapInstaller(OrynPaths paths) {
        if (paths == null) throw new IllegalArgumentException("OrynPaths is required");
        this.paths = paths;
    }

    public boolean isInstalled() {
        return paths.isBootstrapInstalled();
    }

    /** Installs asynchronously. Listener callbacks run on the installer thread. */
    public void install(Listener listener) {
        if (!installing.compareAndSet(false, true)) {
            complete(listener, false, "Bootstrap installation is already running.");
            return;
        }
        Thread worker = new Thread(() -> {
            try {
                installInternal(listener);
            } catch (Exception error) {
                complete(listener, false, "Bootstrap installation failed: " + error.getMessage());
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
        if (!prefix.exists() && previous.exists()) move(previous.toPath(), prefix.toPath());
        if (prefix.exists() && previous.exists()) deleteTree(previous);

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

        deleteTree(staging);
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

        writeIfAbsent(new File(paths.getHome(), ".bashrc"),
                "# Oryn Terminal shell profile\n"
                + "export PS1='\\[\\e[38;2;0;240;255m\\]oryn\\[\\e[0m\\] \\w $ '\n"
                + "export EDITOR=nano\nexport PAGER=less\n"
                + "alias ll='ls -lah'\nalias la='ls -A'\n");
        writeIfAbsent(new File(paths.getHome(), ".profile"),
                "[ -f \"$HOME/.bashrc\" ] && . \"$HOME/.bashrc\"\n");
        writeIfAbsent(new File(staging, ".oryn-bootstrap-version"),
                BOOTSTRAP_TAG + "\n");

        // Swap only within Oryn's private filesDir. Restore the old prefix if
        // the final rename fails; never touch /data/data/com.termux.
        deleteTree(previous);
        if (prefix.exists()) move(prefix.toPath(), previous.toPath());
        try {
            move(staging.toPath(), prefix.toPath());
        } catch (Exception swapError) {
            if (previous.exists() && !prefix.exists()) {
                move(previous.toPath(), prefix.toPath());
            }
            throw swapError;
        }
        deleteTree(previous);
        progress(listener, "Oryn bootstrap installed and validated", 100);
        complete(listener, true, "Installed " + BOOTSTRAP_TAG
                + " (ARM64, " + extracted + " files) in Oryn private storage.");
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
        if (!isInstalled()) throw new IOException("Oryn bootstrap is not installed.");
        File proot = paths.getProot();
        if (!proot.isFile() || !proot.canExecute()) {
            throw new IOException("Oryn's private proot binary is not installed yet.");
        }
        String prefix = OrynPaths.TERMUX_VIRTUAL_PREFIX;
        String home = OrynPaths.TERMUX_VIRTUAL_HOME;

        java.util.ArrayList<String> args = new java.util.ArrayList<>();
        args.add(proot.getAbsolutePath());
        args.add("--link2symlink");
        args.add("-r");
        args.add(paths.getRoot().getAbsolutePath());
        addBind(args, "/dev");
        addBind(args, "/proc");
        addBind(args, "/sys");
        addBind(args, "/system");
        // These mount points vary across Android releases and OEM builds.
        addBindIfPresent(args, "/apex");
        addBindIfPresent(args, "/vendor");
        addBindIfPresent(args, "/product");
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
        args.add("LANG=en_US.UTF-8");
        args.add("TMPDIR=" + prefix + "/tmp");
        args.add("LD_PRELOAD=" + prefix + "/lib/libtermux-exec.so");
        args.add(prefix + "/bin/bash");
        args.add("--login");
        return args.toArray(new String[0]);
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

    private void download(String url, File destination, Listener listener) throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(45000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent", "OrynTerminal/0.1");
            int status = connection.getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                throw new IOException("Bootstrap download returned HTTP " + status);
            }
            long expected = connection.getContentLengthLong();
            if (expected > MAX_ARCHIVE_BYTES) throw new IOException("Bootstrap archive is too large.");
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 OutputStream out = new FileOutputStream(destination)) {
                byte[] buffer = new byte[65536];
                long total = 0;
                int count;
                while ((count = in.read(buffer)) != -1) {
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
        int count = 0;
        long totalBytes = 0;
        byte[] buffer = new byte[65536];
        String base = destination.getCanonicalPath() + File.separator;

        try (ZipInputStream zip = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(archive)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
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
    private void createSymlinks(File root) throws IOException {
        File map = new File(root, "SYMLINKS.txt");
        if (!map.isFile()) throw new IOException("Bootstrap is missing SYMLINKS.txt.");
        String content = new String(Files.readAllBytes(map.toPath()), StandardCharsets.UTF_8);
        String base = root.getCanonicalPath() + File.separator;
        for (String line : content.split("\\r?\\n")) {
            int arrow = line.indexOf('\u2190');
            if (arrow <= 0 || arrow == line.length() - 1) continue;
            String target = line.substring(0, arrow).trim();
            String link = line.substring(arrow + 1).trim();
            while (link.startsWith("./")) link = link.substring(2);
            if (target.isEmpty() || target.startsWith("/") || link.isEmpty()) {
                throw new IOException("Unsafe symlink entry rejected.");
            }
            File linkFile = new File(root, link);
            String linkPath = linkFile.getCanonicalPath();
            if (!linkPath.startsWith(base)) throw new IOException("Symlink path escapes prefix.");
            Path resolvedTarget = linkFile.getParentFile().toPath().resolve(target).normalize();
            if (!resolvedTarget.toString().startsWith(root.toPath().normalize().toString()
                    + File.separator)) {
                throw new IOException("Symlink target escapes prefix.");
            }
            File parent = linkFile.getParentFile();
            if (parent != null && !parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Cannot create symlink parent.");
            }
            Files.deleteIfExists(linkFile.toPath());
            Files.createSymbolicLink(linkFile.toPath(), new File(target).toPath());
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

    private static void deleteTree(File file) throws IOException {
        if (!file.exists()) return;
        if (file.isDirectory() && !Files.isSymbolicLink(file.toPath())) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteTree(child);
        }
        if (!file.delete()) throw new IOException("Cannot remove " + file);
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
