package com.oryn.terminal.termux;

import android.content.Context;

import com.oryn.terminal.core.OrynPaths;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Installs the ARM64 proot runtime packaged by CI from signed official Termux
 * APT packages. Every copied asset is checked against the generated manifest.
 */
public final class ProotRuntimeInstaller {
    public interface Listener {
        void onProgress(String stage, int percent);
        void onComplete(boolean success, String message);
    }

    private static final String MANIFEST = "proot/runtime-manifest.txt";
    private static final long MAX_FILE_BYTES = 8L * 1024L * 1024L;
    private static final int MAX_FILES = 32;

    private final Context context;
    private final OrynPaths paths;
    private final AtomicBoolean installing = new AtomicBoolean(false);
    private volatile String lastErrorDetails;

    public String getLastErrorDetails() {
        return lastErrorDetails;
    }

    public ProotRuntimeInstaller(Context context) throws IOException {
        this.context = context.getApplicationContext();
        this.paths = new OrynPaths(this.context.getFilesDir());
    }

    public void install(Listener listener) {
        if (!installing.compareAndSet(false, true)) {
            complete(listener, false, "Proot runtime installation is already running.");
            return;
        }
        lastErrorDetails = null;
        Thread worker = new Thread(() -> {
            try {
                installInternal(listener);
            } catch (Exception error) {
                lastErrorDetails = diagnosticDetails(error);
                complete(listener, false,
                        "Oryn couldn't finish installing its private runtime. "
                        + "Your Termux installation was not modified. You can retry.");
            } finally {
                installing.set(false);
            }
        }, "oryn-proot-installer");
        worker.start();
    }

    private void installInternal(Listener listener) throws Exception {
        if (!"aarch64".equals(TermuxBootstrapInstaller.archName())) {
            throw new IOException("The packaged proot runtime supports ARM64 only.");
        }
        if (!paths.isBootstrapInstalled()) {
            throw new IOException("Install Oryn's private bootstrap before the proot runtime.");
        }

        paths.prepareLayout();
        File binDir = paths.getProot().getParentFile();
        File stagingBin = new File(paths.getFilesDir(), "bin.staging");
        File previousBin = new File(paths.getFilesDir(), "bin.previous");
        File stagingLoader = new File(paths.getFilesDir(), "proot-loader.staging");
        File previousLoader = new File(paths.getFilesDir(), "proot-loader.previous");
        File loader = paths.getProotLoader();
        File transactionMarker = new File(paths.getFilesDir(), "proot-runtime.transaction");

        // Recover any interrupted swap before touching staging or backup paths.
        recoverInterruptedActivation(transactionMarker, binDir, previousBin, stagingBin,
                loader, previousLoader, stagingLoader);

        paths.deletePrivateTree(stagingBin);
        if (!stagingBin.mkdirs() && !stagingBin.isDirectory()) {
            throw new IOException("Cannot create proot staging directory.");
        }
        File loaderParent = loader.getParentFile();
        if (!loaderParent.mkdirs() && !loaderParent.isDirectory()) {
            throw new IOException("Cannot create the private proot loader directory.");
        }

        List<ManifestEntry> entries = readManifest();
        if (entries.isEmpty()) throw new IOException("Runtime manifest contains no files.");

        int index = 0;
        for (ManifestEntry entry : entries) {
            index++;
            File destination;
            if ("loader".equals(entry.path)) {
                destination = stagingLoader;
            } else if (entry.path.startsWith("bin/")) {
                destination = new File(stagingBin, entry.path.substring("bin/".length()));
            } else if (entry.path.startsWith("lib/")) {
                destination = new File(stagingBin, entry.path);
            } else {
                throw new IOException("Unsupported runtime asset path: " + entry.path);
            }
            ensureInside(stagingBin, destination, "loader".equals(entry.path));
            File parent = destination.getParentFile();
            if (!parent.mkdirs() && !parent.isDirectory()) {
                throw new IOException("Cannot create runtime directory: " + parent);
            }
            copyAndVerify(entry, destination);
            if ("bin/proot".equals(entry.path) || "loader".equals(entry.path)) {
                if (!destination.setExecutable(true, false) && !destination.canExecute()) {
                    throw new IOException("Cannot mark runtime executable: " + entry.path);
                }
            }
            progress(listener, "Verifying " + entry.path,
                    10 + (index * 70 / entries.size()));
        }

        File stagedProot = new File(stagingBin, "proot");
        File stagedLibs = new File(stagingBin, "lib");
        if (!stagedProot.isFile() || !stagedProot.canExecute()
                || !stagedLibs.isDirectory() || !stagingLoader.isFile()
                || !stagingLoader.canExecute()) {
            throw new IOException("Runtime is incomplete after asset verification.");
        }
        if (!containsLibrary(stagedLibs, "libtalloc.so")
                || !containsLibrary(stagedLibs, "libandroid-shmem.so")) {
            throw new IOException("Proot shared-library dependencies are missing.");
        }

        progress(listener, "Activating verified private runtime", 88);
        boolean hadBin = binDir.exists();
        boolean hadLoader = loader.exists();
        writeTransactionMarker(transactionMarker, hadBin, hadLoader, false);

        try {
            if (hadBin) move(binDir, previousBin);
            if (hadLoader) move(loader, previousLoader);
            move(stagingBin, binDir);
            move(stagingLoader, loader);

            if (!paths.isProotInstalled()) {
                throw new IOException("Installed runtime failed its final readiness check.");
            }

            // Commit is durable before backup cleanup. A crash after this point
            // keeps the new runtime and only retries cleanup on the next install.
            writeTransactionMarker(transactionMarker, hadBin, hadLoader, true);
        } catch (Exception activationError) {
            try {
                recoverInterruptedActivation(transactionMarker, binDir, previousBin, stagingBin,
                        loader, previousLoader, stagingLoader);
            } catch (Exception recoveryError) {
                activationError.addSuppressed(recoveryError);
            }
            throw activationError;
        }

        // The committed marker remains until both backups are safely removed.
        paths.deletePrivateTree(previousBin);
        if (previousLoader.exists() && !previousLoader.delete()) {
            throw new IOException("Runtime committed, but old loader backup cleanup failed.");
        }
        if (transactionMarker.exists() && !transactionMarker.delete()) {
            throw new IOException("Runtime committed, but transaction marker cleanup failed.");
        }
        progress(listener, "Oryn's private proot runtime is installed", 100);
        complete(listener, true, "Verified ARM64 proot runtime installed in Oryn's private storage.");
    }

    /**
     * Restores the last known-good runtime after an interrupted two-component swap.
     * The marker records whether each component existed before activation. A committed
     * marker means the new runtime passed readiness checks and only backup cleanup remains.
     */
    private void recoverInterruptedActivation(
            File marker,
            File binDir,
            File previousBin,
            File stagingBin,
            File loader,
            File previousLoader,
            File stagingLoader) throws IOException {
        boolean markerExists = marker.isFile();
        boolean committed = false;
        boolean hadBin = false;
        boolean hadLoader = false;

        if (markerExists) {
            String state = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8);
            committed = state.contains("state=COMMITTED");
            hadBin = state.contains("hadBin=true");
            hadLoader = state.contains("hadLoader=true");
        }

        if (committed) {
            // Keep the verified new runtime; finish only the interrupted cleanup.
            paths.deletePrivateTree(previousBin);
            if (previousLoader.exists() && !previousLoader.delete()) {
                throw new IOException("Cannot finish cleanup of previous proot loader.");
            }
        } else {
            // Legacy recovery: older installer versions had no marker. If a backup
            // exists, prefer restoring it over retaining a possibly partial activation.
            if (previousBin.exists()) {
                paths.deletePrivateTree(binDir);
                move(previousBin, binDir);
            } else if (markerExists && !hadBin) {
                paths.deletePrivateTree(binDir);
            }

            if (previousLoader.exists()) {
                if (loader.exists() && !loader.delete()) {
                    throw new IOException("Cannot remove interrupted proot loader.");
                }
                File parent = loader.getParentFile();
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Cannot recreate private proot loader directory.");
                }
                move(previousLoader, loader);
            } else if (markerExists && !hadLoader && loader.exists() && !loader.delete()) {
                throw new IOException("Cannot remove interrupted new proot loader.");
            }
        }

        paths.deletePrivateTree(stagingBin);
        if (stagingLoader.exists() && !stagingLoader.delete()) {
            throw new IOException("Cannot remove staged proot loader.");
        }
        if (markerExists && marker.exists() && !marker.delete()) {
            throw new IOException("Cannot clear proot runtime transaction marker.");
        }
    }

    private static void writeTransactionMarker(
            File marker, boolean hadBin, boolean hadLoader, boolean committed) throws IOException {
        String value = "hadBin=" + hadBin + "\\n"
                + "hadLoader=" + hadLoader + "\\n"
                + "state=" + (committed ? "COMMITTED" : "PREPARED") + "\\n";
        try (FileOutputStream out = new FileOutputStream(marker)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        }
    }

    private List<ManifestEntry> readManifest() throws IOException {
        List<ManifestEntry> entries = new ArrayList<>();
        try (InputStream in = context.getAssets().open(MANIFEST);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+", 2);
                if (parts.length != 2 || !parts[0].matches("[0-9a-fA-F]{64}")) {
                    throw new IOException("Invalid runtime manifest entry.");
                }
                String path = parts[1].trim();
                if (!isAllowedAssetPath(path)) {
                    throw new IOException("Rejected runtime asset path: " + path);
                }
                entries.add(new ManifestEntry(path, parts[0].toLowerCase(Locale.US)));
                if (entries.size() > MAX_FILES) {
                    throw new IOException("Runtime manifest has too many files.");
                }
            }
        }
        return entries;
    }

    private static boolean isAllowedAssetPath(String path) {
        if (path.startsWith("/") || path.contains("..") || path.contains("\\")
                || path.contains(":")) return false;
        if ("loader".equals(path) || "bin/proot".equals(path)) return true;
        return path.matches("lib/[A-Za-z0-9._+\\-]+");
    }

    private void copyAndVerify(ManifestEntry entry, File destination) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long total = 0;
        try (InputStream in = context.getAssets().open("proot/" + entry.path);
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = in.read(buffer)) != -1) {
                total += count;
                if (total > MAX_FILE_BYTES) {
                    throw new IOException("Runtime asset exceeds size limit: " + entry.path);
                }
                digest.update(buffer, 0, count);
                out.write(buffer, 0, count);
            }
            out.getFD().sync();
        }
        String actual = toHex(digest.digest());
        if (!entry.sha256.equals(actual)) {
            destination.delete();
            throw new IOException("SHA-256 mismatch for " + entry.path);
        }
    }

    private static void ensureInside(File root, File target, boolean loader) throws IOException {
        File canonicalRoot = root.getCanonicalFile();
        File canonicalTarget = target.getCanonicalFile();
        if (loader) {
            // The loader is installed separately under Oryn's private Termux prefix.
            if (!canonicalTarget.getPath().startsWith(
                    canonicalRoot.getParentFile().getCanonicalPath() + File.separator)) {
                throw new IOException("Loader target escaped Oryn private storage.");
            }
        } else if (!canonicalTarget.getPath().startsWith(
                canonicalRoot.getPath() + File.separator)) {
            throw new IOException("Runtime target escaped staging directory.");
        }
    }

    private static boolean containsLibrary(File directory, String prefix) {
        File[] files = directory.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(prefix)) return true;
        }
        return false;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte item : bytes) value.append(String.format(Locale.US, "%02x", item & 0xff));
        return value.toString();
    }

    private static void move(File from, File to) throws IOException {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(from.toPath(), to.toPath());
        }
    }

    private static String diagnosticDetails(Exception error) {
        java.io.StringWriter buffer = new java.io.StringWriter();
        error.printStackTrace(new java.io.PrintWriter(buffer));
        return buffer.toString();
    }

    private static void progress(Listener listener, String stage, int percent) {
        if (listener != null) listener.onProgress(stage, Math.min(99, Math.max(0, percent)));
    }

    private static void complete(Listener listener, boolean success, String message) {
        if (listener != null) listener.onComplete(success, message);
    }

    private static final class ManifestEntry {
        final String path;
        final String sha256;

        ManifestEntry(String path, String sha256) {
            this.path = path;
            this.sha256 = sha256;
        }
    }
}
