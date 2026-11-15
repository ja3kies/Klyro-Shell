package com.oryn.terminal.termux;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class BootstrapPrefixRecoveryTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void successfulRepairRetainsAllPreviousFilesAndRefusesSecondOverwrite() throws Exception {
        File prefix = folder.newFolder("usr");
        File staging = folder.newFolder("usr.staging");
        File previous = new File(folder.getRoot(), "usr.previous");
        Files.write(new File(prefix, "personal-project").toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        Files.write(new File(staging, "new-bootstrap").toPath(), "new".getBytes(StandardCharsets.UTF_8));
        TermuxBootstrapInstaller.activatePrefix(staging, prefix, previous);
        assertEquals("keep", new String(Files.readAllBytes(new File(previous, "personal-project").toPath()), StandardCharsets.UTF_8));
        assertTrue(new File(prefix, "new-bootstrap").isFile());
        try {
            TermuxBootstrapInstaller.recoverPrefix(prefix, previous);
            fail("Backup was silently removed");
        } catch (IOException expected) { assertTrue(expected.getMessage().contains("backup")); }
        assertTrue(new File(previous, "personal-project").isFile());
    }

    @Test public void interruptedActivationRestoresOldPrefix() throws Exception {
        File prefix = new File(folder.getRoot(), "usr");
        File previous = folder.newFolder("usr.previous");
        Files.write(new File(previous, "project").toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        TermuxBootstrapInstaller.recoverPrefix(prefix, previous);
        assertTrue(new File(prefix, "project").isFile());
        assertFalse(previous.exists());
    }

    @Test public void failedActivationRestoresOldPrefixWithoutDeletingIt() throws Exception {
        File prefix = folder.newFolder("usr");
        File previous = new File(folder.getRoot(), "usr.previous");
        Files.write(new File(prefix, "project").toPath(), "keep".getBytes(StandardCharsets.UTF_8));
        try {
            TermuxBootstrapInstaller.activatePrefix(new File(folder.getRoot(), "missing"), prefix, previous);
            fail("Missing staging accepted");
        } catch (IOException expected) { }
        assertEquals("keep", new String(Files.readAllBytes(new File(prefix, "project").toPath()), StandardCharsets.UTF_8));
        assertFalse(previous.exists());
    }
}
