package com.oryn.terminal.termux;

import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class KlyroShellHelperInstallerTest {
    private static final String MARKER = "KLYRO_SECURITY_TEST_V1";
    private static final String SCRIPT = "#!/bin/sh\n# " + MARKER + "\nprintf '%s\\n' safe\n";

    @Test public void rejectsInvalidCommandNamesBeforeFilesystemChanges() throws Exception {
        Path prefix = Files.createTempDirectory("klyro-safe");
        Path bin = Files.createDirectory(prefix.resolve("bin"));
        try {
            for (String name : new String[] {"../escape", "termux-setup-storage",
                    "klyro-../../escape", "klyro-SPACE", "klyro-foo/bar"}) {
                try {
                    KlyroShellHelperInstaller.install(prefix.toFile(), name, MARKER, SCRIPT);
                    fail("Accepted command " + name);
                } catch (IOException expected) { }
            }
            assertEquals(0, Files.list(bin).count());
        } finally {
            Files.delete(bin);
            Files.delete(prefix);
        }
    }

    @Test public void refusesSymlinkTargetAndPreservesExternalFile() throws Exception {
        Path prefix = Files.createTempDirectory("klyro-safe");
        Path bin = Files.createDirectory(prefix.resolve("bin"));
        Path outside = Files.createTempFile("klyro-external", ".sh");
        Path link = bin.resolve("klyro-info");
        Files.write(outside, "untouched\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            Files.createSymbolicLink(link, outside);
            try {
                KlyroShellHelperInstaller.install(prefix.toFile(), "klyro-info",
                        MARKER, SCRIPT);
                fail("Accepted linked helper");
            } catch (IOException expected) { }
            assertEquals("untouched\n", new String(Files.readAllBytes(outside), java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(link);
            Files.deleteIfExists(outside);
            Files.delete(bin);
            Files.delete(prefix);
        }
    }

    @Test public void customHelperIsNotOverwrittenAndManagedHelperIsIdempotent()
            throws Exception {
        Path prefix = Files.createTempDirectory("klyro-safe");
        Path bin = Files.createDirectory(prefix.resolve("bin"));
        Path helper = bin.resolve("klyro-info");
        try {
            Files.write(helper, "#!/bin/sh\n# user-owned\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            KlyroShellHelperInstaller.install(prefix.toFile(), "klyro-info",
                    MARKER, SCRIPT);
            assertEquals("#!/bin/sh\n# user-owned\n", new String(Files.readAllBytes(helper), java.nio.charset.StandardCharsets.UTF_8));
            Files.delete(helper);
            KlyroShellHelperInstaller.install(prefix.toFile(), "klyro-info",
                    MARKER, SCRIPT);
            assertEquals(SCRIPT, new String(Files.readAllBytes(helper), java.nio.charset.StandardCharsets.UTF_8));
            KlyroShellHelperInstaller.install(prefix.toFile(), "klyro-info",
                    MARKER, SCRIPT);
            assertEquals(SCRIPT, new String(Files.readAllBytes(helper), java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(helper);
            Files.delete(bin);
            Files.delete(prefix);
        }
    }
}
