package com.oryn.terminal.workspace;

import org.junit.Test;
import static org.junit.Assert.*;

public class KlyroWorkspaceCatalogTest {
    @Test public void generalIsAlwaysAvailable() {
        KlyroWorkspaceCatalog catalog = new KlyroWorkspaceCatalog();
        assertEquals(KlyroWorkspaceCatalog.GENERAL, catalog.activeId());
        assertEquals(1, catalog.entries().size());
        assertFalse(catalog.delete("general"));
        assertFalse(catalog.rename("general", "Renamed"));
    }

    @Test public void createSelectRenameAndRemove() {
        KlyroWorkspaceCatalog catalog = new KlyroWorkspaceCatalog();
        String id = catalog.create("Oryn");
        assertNotNull(id);
        assertTrue(catalog.contains(id));
        assertEquals(id, catalog.activeId());
        assertTrue(catalog.rename(id, "Oryn Reader"));
        assertFalse(catalog.rename(id, "General"));
        assertFalse(catalog.create("oryn reader") != null);
        assertTrue(catalog.delete(id));
        assertEquals(KlyroWorkspaceCatalog.GENERAL, catalog.activeId());
        assertFalse(catalog.select(id));
    }

    @Test public void roundTripRetainsNamesAndActiveWorkspace() {
        KlyroWorkspaceCatalog catalog = new KlyroWorkspaceCatalog();
        String first = catalog.create("Oryn – ebooks");
        String second = catalog.create("Klyro / Shell");
        assertTrue(catalog.select(first));
        KlyroWorkspaceCatalog restored = KlyroWorkspaceCatalog.decode(catalog.encode(), catalog.activeId());
        assertEquals(first, restored.activeId());
        assertEquals(3, restored.entries().size());
        assertEquals("Oryn – ebooks", restored.entries().get(1).name);
        assertEquals("Klyro / Shell", restored.entries().get(2).name);
        assertTrue(restored.contains(second));
    }

    @Test public void corruptedRecordsDoNotCrashOrCreateGhostSessions() {
        KlyroWorkspaceCatalog empty = KlyroWorkspaceCatalog.decode("corrupted", "nonexistent");
        assertEquals(1, empty.entries().size());
        assertEquals(KlyroWorkspaceCatalog.GENERAL, empty.activeId());
        KlyroWorkspaceCatalog partiallyValid = KlyroWorkspaceCatalog.decode(
            "v1\nnot-an-id\tabc\n12345678-1234-1234-1234-123456789abc\t@@@@\n",
            "12345678-1234-1234-1234-123456789abc"
        );
        assertEquals(1, partiallyValid.entries().size());
        assertEquals(KlyroWorkspaceCatalog.GENERAL, partiallyValid.activeId());
    }

    @Test public void rejectsControlCharactersDuplicatesAndEmptyNames() {
        KlyroWorkspaceCatalog catalog = new KlyroWorkspaceCatalog();
        assertNull(catalog.create("\n\t"));
        assertEquals("hello", KlyroWorkspaceCatalog.cleanName("\u001bhello\u202e"));
        String id = catalog.create("Project");
        assertNotNull(id);
        assertNull(catalog.create("project"));
        assertNull(catalog.create(" General "));
    }

    @Test public void enforcesProjectLimitAndNameLength() {
        KlyroWorkspaceCatalog catalog = new KlyroWorkspaceCatalog();
        for (int i = 1; i < KlyroWorkspaceCatalog.MAX_PROJECTS; i++) {
            assertNotNull(catalog.create("Project " + i));
        }
        assertNull(catalog.create("Project 99"));
        assertTrue(KlyroWorkspaceCatalog.cleanName("x".repeat(64)).length() <= 32);
    }
}
