package com.oryn.terminal.termux;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.oryn.terminal.core.OrynPaths;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class LinkerConfigBindTest {
    @Test
    public void bindsOnlyExistingLinkerConfigFilesAndNeverTheDirectory() {
        List<String> args = new ArrayList<>();
        TermuxBootstrapInstaller.addLinkerConfigBinds(args, path -> true);
        assertEquals(Arrays.asList(
                "-b", "/linkerconfig/ld.config.txt",
                "-b", "/linkerconfig/com.android.art/ld.config.txt"), args);
        assertFalse(args.contains("/linkerconfig"));
    }

    @Test
    public void addsNothingWhenHostFilesAreMissing() {
        List<String> args = new ArrayList<>();
        TermuxBootstrapInstaller.addLinkerConfigBinds(args, path -> false);
        assertTrue(args.isEmpty());
    }

    @Test
    public void bindTargetStaysInsideOrynRootfs() throws Exception {
        File files = Files.createTempDirectory("klyro-linker-test").toFile();
        OrynPaths paths = new OrynPaths(files);
        paths.ensureFileBindTarget("/linkerconfig/ld.config.txt");
        assertTrue(new File(paths.getRoot(), "linkerconfig/ld.config.txt").isFile());
        try {
            paths.ensureFileBindTarget("/../escape.txt");
            org.junit.Assert.fail("path traversal must be rejected");
        } catch (java.io.IOException expected) {
            // expected
        }
    }
}
