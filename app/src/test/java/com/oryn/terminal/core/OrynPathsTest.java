package com.oryn.terminal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

public class OrynPathsTest {
    @Test
    public void allManagedPathsStayInsideOrynFilesDirectory() throws Exception {
        File files = Files.createTempDirectory("oryn-paths-test").toFile();
        OrynPaths paths = new OrynPaths(files);
        paths.prepareLayout();

        String base = files.getCanonicalPath() + File.separator;
        assertTrue(paths.getRoot().getCanonicalPath().startsWith(base));
        assertTrue(paths.getPrefix().getCanonicalPath().startsWith(base));
        assertTrue(paths.getHome().getCanonicalPath().startsWith(base));
        assertTrue(paths.getTmp().getCanonicalPath().startsWith(base));
        assertTrue(paths.getProot().getParentFile().getCanonicalPath().startsWith(base));
        assertFalse(paths.getRoot().getAbsolutePath().contains("/data/data/com.termux/"));

        assertTrue(new File(paths.getRoot(), "data/data/com.termux/files/usr").isDirectory());
        assertTrue(new File(paths.getRoot(), "data/data/com.termux/files/home").isDirectory());
    }


    @Test
    public void prootRuntimeRequiresBinaryLoaderAndBothLibraries() throws Exception {
        File files = Files.createTempDirectory("oryn-proot-test").toFile();
        OrynPaths paths = new OrynPaths(files);
        paths.prepareLayout();

        assertFalse(paths.isProotInstalled());

        assertTrue(paths.getProot().createNewFile());
        paths.getProot().setExecutable(true, false);
        File loader = paths.getProotLoader();
        assertTrue(loader.getParentFile().mkdirs());
        assertTrue(loader.createNewFile());
        loader.setExecutable(true, false);
        assertTrue(new File(paths.getProotLibDir(), "libtalloc.so.2").createNewFile());
        assertFalse(paths.isProotInstalled());

        assertTrue(new File(paths.getProotLibDir(), "libandroid-shmem.so").createNewFile());
        assertTrue(paths.isProotInstalled());
    }

    @Test
    public void termuxCompiledPathsAreVirtualCompatibilityTargets() {
        assertEquals("/data/data/com.termux/files/usr", OrynPaths.TERMUX_VIRTUAL_PREFIX);
        assertEquals("/data/data/com.termux/files/home", OrynPaths.TERMUX_VIRTUAL_HOME);
    }

    @Test
    public void bootstrapIsNotMarkedInstalledBeforeValidation() throws Exception {
        File files = Files.createTempDirectory("oryn-bootstrap-test").toFile();
        OrynPaths paths = new OrynPaths(files);
        paths.prepareLayout();
        assertFalse(paths.isBootstrapInstalled());
    }
}
