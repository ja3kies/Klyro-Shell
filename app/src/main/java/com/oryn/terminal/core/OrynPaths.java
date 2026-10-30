package com.oryn.terminal.core;

import java.io.File;
import java.io.IOException;

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
        this.stagingPrefix = child(root, "usr.staging");
        this.previousPrefix = child(root, "usr.previous");
    }

    public File getFilesDir() { return filesDir; }
    public File getRoot() { return root; }
    public File getPrefix() { return prefix; }
    public File getHome() { return home; }
    public File getTmp() { return tmp; }
    public File getProot() { return proot; }
    public File getStagingPrefix() { return stagingPrefix; }
    public File getPreviousPrefix() { return previousPrefix; }

    /** Creates only Oryn-owned directories, including virtual proot bind targets. */
    public void prepareLayout() throws IOException {
        mkdir(root);
        mkdir(home);
        mkdir(tmp);
        mkdir(prefix.getParentFile());
        mkdir(proot.getParentFile());

        // proot resolves bind destinations inside its private rootfs.
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
