package com.oryn.terminal.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.Test;

public class OrynPathsTest {

    @Test
    public void deletesDanglingSymlinksWithoutFollowingThem() throws Exception {
        File files = Files.createTempDirectory("oryn-delete-test").toFile();
        OrynPaths paths = new OrynPaths(files);
        paths.prepareLayout();

        File staging = paths.getStagingPrefix();
        File lib = new File(staging, "lib");
        assertTrue(lib.mkdirs());
        Path dangling = new File(lib, "ncursesw").toPath();
        Files.createSymbolicLink(dangling, java.nio.file.Paths.get("missing-libncursesw.so"));

        File outside = Files.createTempDirectory("oryn-outside-test").toFile();
        File marker = new File(outside, "keep.txt");
        assertTrue(marker.createNewFile());
        Path externalLink = new File(lib, "external").toPath();
        Files.createSymbolicLink(externalLink, outside.toPath());

        paths.deletePrivateTree(staging);

        assertFalse(Files.exists(staging.toPath(), LinkOption.NOFOLLOW_LINKS));
        assertTrue(marker.isFile());
        assertTrue(Files.exists(outside.toPath()));
    }

    @Test
    public void refusesToDeleteOutsidePrivateStorage() throws Exception {
        File files = Files.createTempDirectory("oryn-boundary-test").toFile();
        OrynPaths paths = new OrynPaths(files);
        File outside = Files.createTempDirectory("oryn-outside-boundary").toFile();
        boolean rejected = false;
        try {
            paths.deletePrivateTree(outside);
        } catch (java.io.IOException expected) {
            rejected = true;
        }
        assertTrue(rejected);
        assertTrue(outside.isDirectory());
    }

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
        assertTrue(paths.getProotLibDir().getCanonicalPath().startsWith(base));
        assertTrue(paths.getProotDependencyDir().getCanonicalPath().startsWith(base));
        assertTrue(paths.getProotLoader().getCanonicalPath().startsWith(base));
        // The JVM build itself may run in Termux; containment is relative to filesDir.
        assertEquals(new File(files, "termux").getCanonicalFile(), paths.getRoot());

        assertTrue(new File(paths.getRoot(), "data/data/com.termux/files/usr").isDirectory());
        assertTrue(new File(paths.getRoot(), "data/data/com.termux/files/home").isDirectory());
        File archives = new File(paths.getRoot(), "data/data/com.termux/cache/apt/archives/partial");
        assertTrue(archives.isDirectory());
        assertTrue(archives.getCanonicalPath().startsWith(base));
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
        assertTrue(loader.getParentFile().isDirectory() || loader.getParentFile().mkdirs());
        assertTrue(loader.createNewFile());
        loader.setExecutable(true, false);
        assertTrue(new File(paths.getProotDependencyDir(), "libtalloc.so.2").createNewFile());
        assertFalse(paths.isProotInstalled());

        File shmem = new File(paths.getProotLibDir(), "libandroid-shmem.so");
        assertTrue(shmem.createNewFile());
        assertFalse("Zero-byte PRoot runtime must not be called installed", paths.isProotInstalled());
        for (File part : new File[]{paths.getProot(), loader,
                new File(paths.getProotDependencyDir(), "libtalloc.so.2"), shmem}) {
            Files.write(part.toPath(), new byte[]{1});
        }
        assertTrue(paths.isProotInstalled());
        Files.write(shmem.toPath(), new byte[0]);
        assertFalse("Truncated shared library must invalidate runtime", paths.isProotInstalled());
    }

    @Test
    public void termuxCompiledPathsAreVirtualCompatibilityTargets() {
        assertEquals("/data/data/com.termux/files/usr", OrynPaths.TERMUX_VIRTUAL_PREFIX);
        assertEquals("/data/data/com.termux/files/home", OrynPaths.TERMUX_VIRTUAL_HOME);
    }

    @Test
    public void bootstrapRejectsTruncatedExecutableAndVersionMarker() throws Exception {
        File files = Files.createTempDirectory("oryn-bootstrap-corrupt").toFile();
        OrynPaths paths = new OrynPaths(files);
        paths.prepareLayout();
        File bin = new File(paths.getPrefix(), "bin");
        assertTrue(bin.mkdirs());
        File bash = new File(bin, "bash");
        File pkg = new File(bin, "pkg");
        File apt = new File(bin, "apt");
        File marker = new File(paths.getPrefix(), ".oryn-bootstrap-version");
        for (File file : new File[]{bash, pkg, apt, marker}) {
            assertTrue(file.createNewFile());
        }
        assertTrue(bash.setExecutable(true, false));
        assertFalse("A zero-byte executable cannot start a shell", paths.isBootstrapInstalled());
        for (File file : new File[]{bash, pkg, apt, marker}) {
            Files.write(file.toPath(), new byte[]{1});
        }
        assertFalse("Non-executable pkg and apt must not be accepted", paths.isBootstrapInstalled());
        assertTrue(pkg.setExecutable(true, false));
        assertTrue(apt.setExecutable(true, false));
        assertTrue(paths.isBootstrapInstalled());
        assertTrue(apt.setExecutable(false, false));
        assertFalse("Lost apt execute permission must invalidate bootstrap", paths.isBootstrapInstalled());
        assertTrue(apt.setExecutable(true, false));
        Files.write(pkg.toPath(), new byte[0]);
        assertFalse("A truncated package manager cannot form a valid bootstrap", paths.isBootstrapInstalled());
        Files.write(pkg.toPath(), new byte[]{1});
        Files.write(marker.toPath(), new byte[0]);
        assertFalse("An empty bootstrap marker must not pass validation", paths.isBootstrapInstalled());
    }

    @Test
    public void bootstrapIsNotMarkedInstalledBeforeValidation() throws Exception {
        File files = Files.createTempDirectory("oryn-bootstrap-test").toFile();
        OrynPaths paths = new OrynPaths(files);
        paths.prepareLayout();
        assertFalse(paths.isBootstrapInstalled());
    }
}
