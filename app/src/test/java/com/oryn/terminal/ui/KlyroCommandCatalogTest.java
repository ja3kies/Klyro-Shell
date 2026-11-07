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

    @Test public void unsupportedStorageIsNeverPastedAsShellCode() {
        KlyroCommandCatalog.Entry storage = KlyroCommandCatalog.search("termux-setup-storage").get(0);
        assertFalse(storage.insertable);
        assertEquals("", KlyroCommandCatalog.safeInsertion(storage));
        assertTrue(storage.note.contains("Settings > Files & storage"));
    }

    @Test public void exitAndUpgradeOnlyInsertWithoutEnter() {
        assertEquals("exit", KlyroCommandCatalog.safeInsertion(KlyroCommandCatalog.search("exit").get(0)));
        assertEquals("pkg upgrade",
                KlyroCommandCatalog.safeInsertion(KlyroCommandCatalog.search("pkg upgrade").get(0)));
        assertTrue(KlyroCommandCatalog.search("pkg upgrade").get(0).caution);
    }

    @Test public void boundedQueriesAndCategories() {
        assertTrue(KlyroCommandCatalog.search("nonexistent-xyz").isEmpty());
        assertTrue(KlyroCommandCatalog.search("x".repeat(97)).isEmpty());
        assertFalse(KlyroCommandCatalog.search("bash built-ins").isEmpty());
    }
}
