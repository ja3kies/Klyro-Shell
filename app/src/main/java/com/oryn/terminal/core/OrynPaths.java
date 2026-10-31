package com.oryn.terminal.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

/**
 * Canonical private-storage layout for Oryn Terminal.
 *
 * Nothing in this class resolves to Termux's application data. Termux's
 * compiled-in prefix is only a virtual path used later inside proot.
 */
public final class OrynPaths {
    public static final String TERMUX_VIRTUAL_PREFIX =
            "/data/data/com.termux/files/usr";
    public static final String TERMUX_VIRTUAL_HOME =
            "/data/data/com.termux/files/home";

    private final File filesDir;
    private final File root;
    private final File prefix;
    private final File home;
    private final File tmp;
    private final File proot;
    private final File prootLibDir;
    private final File prootLoader;
    private final File stagingPrefix;
    private final File previousPrefix;

    public OrynPaths(File appFilesDir) throws IOException {
        if (appFilesDir == null) throw new IllegalArgumentException("appFilesDir is required");
        this.filesDir = appFilesDir.getCanonicalFile();
        this.root = child(filesDir, "termux");
        this.prefix = child(root, "usr");
        this.home = child(root, "home");
        this.tmp = child(root, "tmp");
        this.proot = child(filesDir, "bin/proot");
        this.prootLibDir = child(filesDir, "bin/lib");
        this.prootLoader = child(prefix, "libexec/proot/loader");
        this.stagingPrefix = child(root, "usr.staging");
        this.previousPrefix = child(root, "usr.previous");
    }

    public File getFilesDir() { return filesDir; }
    public File getRoot() { return root; }
    public File getPrefix() { return prefix; }
    public File getHome() { return home; }
    public File getTmp() { return tmp; }
    public File getProot() { return proot; }
    public File getProotLibDir() { return prootLibDir; }
    public File getProotLoader() { return prootLoader; }
    public File getStagingPrefix() { return stagingPrefix; }
    public File getPreviousPrefix() { return previousPrefix; }

    /** Creates only Oryn-owned directories, including virtual proot bind targets. */
    public void prepareLayout() throws IOException {
        mkdir(root);
        mkdir(home);
        mkdir(tmp);
        mkdir(prefix.getParentFile());
        mkdir(proot.getParentFile());
        mkdir(prootLibDir);

        // proot resolves bind destinations inside Oryn's private rootfs.
        mkdir(child(root, "data/data/com.termux/files/usr"));
        mkdir(child(root, "data/data/com.termux/files/home"));
        mkdir(child(root, "dev"));
        mkdir(child(root, "proc"));
        mkdir(child(root, "sys"));
        mkdir(child(root, "system"));
        mkdir(child(root, "apex"));
        mkdir(child(root, "vendor"));
        mkdir(child(root, "product"));
        mkdir(child(root, "sdcard"));
        mkdir(child(root, "storage"));
    }

    public boolean isBootstrapInstalled() {
        return new File(prefix, "bin/bash").canExecute()
                && new File(prefix, "bin/pkg").exists()
                && new File(prefix, "bin/apt").exists()
                && new File(prefix, ".oryn-bootstrap-version").isFile();
    }

    /** True only when every Oryn-owned proot runtime component is present. */
    public boolean isProotInstalled() {
        return proot.isFile()
                && proot.canExecute()
                && prootLoader.isFile()
                && prootLoader.canExecute()
                && hasLibrary("libtalloc.so")
                && hasLibrary("libandroid-shmem.so");
    }

    private boolean hasLibrary(String prefix) {
        File[] files = prootLibDir.listFiles();
        if (files == null) return false;
        for (File file : files) {
            if (file.isFile() && file.getName().startsWith(prefix)) return true;
        }
        return false;
    }


    /**
     * Deletes one Oryn-owned path without following symbolic links.
     * The target's parent must resolve inside this app's private files directory.
     */
    public void deletePrivateTree(File target) throws IOException {
        if (target == null) throw new IllegalArgumentException("target is required");
        Path rootPath = filesDir.toPath();
        File parentFile = target.getAbsoluteFile().getParentFile();
        if (parentFile == null) throw new IOException("Invalid Oryn cleanup target.");
        Path canonicalParent = parentFile.getCanonicalFile().toPath();
        if (!canonicalParent.startsWith(rootPath)) {
            throw new IOException("Refusing to delete a path outside Oryn private storage.");
        }
        Path targetPath = canonicalParent.resolve(target.getName()).normalize();
        if (targetPath.equals(rootPath) || !targetPath.startsWith(rootPath)) {
            throw new IOException("Refusing unsafe Oryn cleanup target.");
        }
        deleteTreeNoFollow(targetPath);
    }

    private static void deleteTreeNoFollow(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> children = Files.newDirectoryStream(path)) {
                for (Path child : children) deleteTreeNoFollow(child);
            }
        }
        Files.delete(path);
    }

    private File child(File parent, String relative) throws IOException {
        File result = new File(parent, relative).getCanonicalFile();
        String base = parent.getCanonicalPath() + File.separator;
        if (!result.getPath().startsWith(base)) {
            throw new IOException("Path escapes Oryn private storage: " + relative);
        }
        return result;
    }

    private void mkdir(File dir) throws IOException {
        if (dir == null) throw new IOException("Missing parent directory");
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Cannot create Oryn directory: " + dir);
        }
        String path = dir.getCanonicalPath();
        String base = filesDir.getCanonicalPath();
        if (!path.equals(base) && !path.startsWith(base + File.separator)) {
            throw new IOException("Directory escaped Oryn private storage: " + path);
        }
    }
}
