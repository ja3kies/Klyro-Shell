package com.oryn.terminal.ui;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class KlyroCommandCatalogTest {
    @Test public void searchFindsShellAndPackageCommands() {
        assertEquals("pwd", KlyroCommandCatalog.search("pwd").get(0).command);
        assertEquals("pkg install", KlyroCommandCatalog.search("pkg install").get(0).command);
        assertEquals("termux-setup-storage",
                KlyroCommandCatalog.search("setup-storage").get(0).command);
    }

    @Test public void noDuplicateCommandsOrInjectedNewlines() {
        Set<String> unique = new HashSet<>();
        for (KlyroCommandCatalog.Entry entry : KlyroCommandCatalog.all()) {
            assertTrue("duplicate command " + entry.command, unique.add(entry.command));
            assertEquals(entry.insertable, !KlyroCommandCatalog.safeInsertion(entry).isEmpty());
            assertFalse(KlyroCommandCatalog.safeInsertion(entry).contains("\n"));
            assertFalse(KlyroCommandCatalog.safeInsertion(entry).contains("\r"));
        }
    }

    @Test public void storageUsesNamespacedKlyroBridgeWithoutImplicitExecution() {
        KlyroCommandCatalog.Entry storage = KlyroCommandCatalog.search("termux-setup-storage").get(0);
        assertTrue(storage.insertable);
        assertEquals("klyro-setup-storage", KlyroCommandCatalog.safeInsertion(storage));
        assertTrue(storage.note.contains("SAF"));
        assertFalse(storage.insertion.contains("\n"));
    }

    @Test public void exitAndUpgradeOnlyInsertWithoutEnter() {
        assertEquals("exit", KlyroCommandCatalog.safeInsertion(KlyroCommandCatalog.search("exit").get(0)));
        assertEquals("pkg upgrade",
                KlyroCommandCatalog.safeInsertion(KlyroCommandCatalog.search("pkg upgrade").get(0)));
        assertTrue(KlyroCommandCatalog.search("pkg upgrade").get(0).caution);
    }

    @Test public void installedStateChecksPrivatePrefixOnly() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("klyro-catalog-test");
        java.nio.file.Path bin = java.nio.file.Files.createDirectory(root.resolve("bin"));
        try {
            java.nio.file.Files.createFile(bin.resolve("pkg"));
            KlyroCommandCatalog.Entry pkg = KlyroCommandCatalog.search("pkg upgrade").get(0);
            KlyroCommandCatalog.Entry git = KlyroCommandCatalog.search("git status").get(0);
            KlyroCommandCatalog.Entry pwd = KlyroCommandCatalog.search("pwd").get(0);
            KlyroCommandCatalog.Entry storage = KlyroCommandCatalog.search("setup-storage").get(0);
            assertTrue(KlyroCommandCatalog.isInstalled(pkg, root.toFile()));
            assertFalse(KlyroCommandCatalog.isInstalled(git, root.toFile()));
            assertTrue(KlyroCommandCatalog.isInstalled(pwd, root.toFile()));
            assertFalse(KlyroCommandCatalog.isInstalled(storage, root.toFile()));
            java.nio.file.Files.createFile(bin.resolve("klyro-setup-storage"));
            assertTrue(KlyroCommandCatalog.isInstalled(storage, root.toFile()));
            assertFalse(KlyroCommandCatalog.isInstalled(pkg, null));
        } finally {
            java.nio.file.Files.deleteIfExists(bin.resolve("pkg"));
            java.nio.file.Files.deleteIfExists(bin.resolve("klyro-setup-storage"));
            java.nio.file.Files.deleteIfExists(bin);
            java.nio.file.Files.deleteIfExists(root);
        }
    }

    @Test public void boundedQueriesAndCategories() {
        assertTrue(KlyroCommandCatalog.search("nonexistent-xyz").isEmpty());
        assertTrue(KlyroCommandCatalog.search("x".repeat(97)).isEmpty());
        assertFalse(KlyroCommandCatalog.search("bash built-ins").isEmpty());
    }
}
