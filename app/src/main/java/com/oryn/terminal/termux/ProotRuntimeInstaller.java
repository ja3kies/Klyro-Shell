package com.oryn.terminal.termux;

import android.content.Context;

import com.oryn.terminal.core.OrynPaths;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** Verifies PRoot binaries embedded in the APK native-library directory. */
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
        this.paths = new OrynPaths(
                this.context.getFilesDir(),
                new File(this.context.getApplicationInfo().nativeLibraryDir)
        );
    }

    public void install(Listener listener) {
        if (!installing.compareAndSet(false, true)) {
            complete(listener, false, "Runtime verification is already running.");
            return;
        }
        lastErrorDetails = null;
        Thread worker = new Thread(() -> {
            try {
                verifyPackagedRuntime(listener);
                complete(listener, true,
                        "Verified ARM64 PRoot runtime from Klyro's APK-native storage.");
            } catch (Exception error) {
                lastErrorDetails = diagnosticDetails(error);
                complete(listener, false,
                        "Klyro couldn't verify its packaged ARM64 runtime. Reinstall the current APK and retry.");
            } finally {
                installing.set(false);
            }
        }, "oryn-proot-verifier");
        worker.start();
    }

    private void verifyPackagedRuntime(Listener listener) throws Exception {
        if (!"aarch64".equals(TermuxBootstrapInstaller.archName())) {
            throw new IOException("The packaged PRoot runtime supports ARM64 only.");
        }
        if (!paths.isBootstrapInstalled()) {
            throw new IOException("Install Klyro's private bootstrap before verifying PRoot.");
        }

        paths.prepareLayout();
        List<ManifestEntry> entries = readManifest();
        if (entries.isEmpty()) throw new IOException("Runtime manifest contains no files.");

        int index = 0;
        for (ManifestEntry entry : entries) {
            index++;
            File packagedFile = resolvePackagedFile(entry.path);
            verifyFile(entry, packagedFile);
            if (("bin/proot".equals(entry.path) || "loader".equals(entry.path))
                    && !packagedFile.canExecute()) {
                throw new IOException("APK-native runtime file is not executable: " + entry.path);
            }
            progress(listener, "Verifying " + entry.path,
                    10 + (index * 80 / entries.size()));
        }

        installVersionedTalloc(entries);

        if (!paths.isProotInstalled()) {
            throw new IOException("The packaged PRoot executable, loader or shared libraries are missing.");
        }
        progress(listener, "Private ARM64 runtime verified", 100);
    }

    /**
     * Android's jniLibs packaging only extracts files ending in ".so". Termux
     * PRoot links against the SONAME libtalloc.so.2, so stage the verified
     * libtalloc.so payload under that exact name in Oryn-owned private storage.
     */
    private void installVersionedTalloc(List<ManifestEntry> entries) throws Exception {
        ManifestEntry talloc = null;
        for (ManifestEntry entry : entries) {
            if ("lib/libtalloc.so".equals(entry.path)) {
                talloc = entry;
                break;
            }
        }
        if (talloc == null) {
            throw new IOException("The APK-native libtalloc.so dependency is missing from the manifest.");
        }

        File source = resolvePackagedFile(talloc.path);
        File destination = new File(paths.getProotDependencyDir(), "libtalloc.so.2");
        File staging = File.createTempFile("libtalloc-", ".part", paths.getProotDependencyDir());
        try {
            Files.copy(source.toPath(), staging.toPath(), StandardCopyOption.REPLACE_EXISTING);
            verifyFile(new ManifestEntry("private/libtalloc.so.2", talloc.sha256), staging);
            try {
                Files.move(staging.toPath(), destination.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(staging.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            if (staging.exists()) staging.delete();
        }
    }

    private File resolvePackagedFile(String manifestPath) throws IOException {
        File result;
        if ("bin/proot".equals(manifestPath)) {
            result = paths.getProot();
        } else if ("loader".equals(manifestPath)) {
            result = paths.getProotLoader();
        } else if (manifestPath.startsWith("lib/")) {
            result = new File(paths.getProotLibDir(), manifestPath.substring("lib/".length()));
        } else {
            throw new IOException("Unsupported runtime manifest path: " + manifestPath);
        }
        String root = paths.getRuntimeLibDir().getCanonicalPath() + File.separator;
        if (!result.getCanonicalPath().startsWith(root)) {
            throw new IOException("Runtime manifest path escaped nativeLibraryDir.");
        }
        return result;
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
                    throw new IOException("Rejected runtime manifest path: " + path);
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

    private static void verifyFile(ManifestEntry entry, File file) throws Exception {
        if (!file.isFile() || file.length() <= 0 || file.length() > MAX_FILE_BYTES) {
            throw new IOException("Missing or invalid APK-native runtime file: " + entry.path);
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[32768];
            int count;
            while ((count = in.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        String actual = toHex(digest.digest());
        if (!entry.sha256.equals(actual)) {
            throw new IOException("SHA-256 mismatch for APK-native runtime file: " + entry.path);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder value = new StringBuilder();
        for (byte item : bytes) value.append(String.format(Locale.US, "%02x", item & 0xff));
        return value.toString();
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
