package com.oryn.terminal.ui;

import org.junit.Test;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class KlyroCommandQuickAccessTest {
    @Test public void acceptsOnlyCuratedIdsNeverShellOrPluginText() {
        String raw = "v1\npwd\npwd\nrm -rf /\nsecret=hello\npkg install\n";
        assertEquals(Arrays.asList("pwd", "pkg install"),
                KlyroCommandQuickAccess.decode(raw, KlyroCommandQuickAccess.MAX_PINNED));
        assertFalse(KlyroCommandQuickAccess.isKnown("pwd && echo secret"));
        assertFalse(KlyroCommandQuickAccess.isKnown("plugin:fake"));
        assertFalse(KlyroCommandQuickAccess.isKnown("pwd\ncat token"));
    }

    @Test public void corruptedOrOversizedStorageFailsClosed() {
        assertTrue(KlyroCommandQuickAccess.decode(null, 8).isEmpty());
        assertTrue(KlyroCommandQuickAccess.decode("v0\npwd", 8).isEmpty());
        assertTrue(KlyroCommandQuickAccess.decode("v1\n" + "x".repeat(3000), 8).isEmpty());
        assertEquals(Collections.singletonList("pwd"),
                KlyroCommandQuickAccess.decode("v1\npwd\npwd", 8));
    }

    @Test public void pinningNeverEvictsAtCapacityAndToggles() {
        List<String> pinned = new ArrayList<>();
        for (KlyroCommandCatalog.Entry entry : KlyroCommandCatalog.all()) {
            pinned = KlyroCommandQuickAccess.togglePinned(pinned, entry.command);
            if (pinned.size() == KlyroCommandQuickAccess.MAX_PINNED) break;
        }
        List<String> full = pinned;
        assertEquals(8, full.size());
        assertEquals(full, KlyroCommandQuickAccess.togglePinned(full, "pkg install"));
        String remove = full.get(0);
        List<String> smaller = KlyroCommandQuickAccess.togglePinned(full, remove);
        assertEquals(7, smaller.size());
        assertFalse(smaller.contains(remove));
        assertEquals(8, full.size());
    }

    @Test public void recentsAreBoundedAndMostRecentFirst() {
        List<String> recent = Collections.emptyList();
        for (KlyroCommandCatalog.Entry entry : KlyroCommandCatalog.all()) {
            recent = KlyroCommandQuickAccess.recordInsertion(recent, entry.command);
        }
        assertEquals(6, recent.size());
        List<String> next = KlyroCommandQuickAccess.recordInsertion(recent, "pwd");
        assertEquals("pwd", next.get(0));
        assertEquals(6, next.size());
        assertEquals(1, Collections.frequency(next, "pwd"));
        assertEquals(next, KlyroCommandQuickAccess.recordInsertion(next, "bad command"));
    }

    @Test public void encodingRoundTripsOnlyCatalogIdentifiers() {
        List<String> wanted = Arrays.asList("pkg install", "pwd", "termux-info", "unknown");
        String serialized = KlyroCommandQuickAccess.encode(wanted, 8);
        assertEquals(Arrays.asList("pkg install", "pwd", "termux-info"),
                KlyroCommandQuickAccess.decode(serialized, 8));
        assertFalse(serialized.contains("unknown"));
    }

    @Test public void rankingIsStableAndNonDuplicating() {
        List<KlyroCommandCatalog.Entry> catalog = KlyroCommandCatalog.search("");
        List<KlyroCommandCatalog.Entry> sorted = KlyroCommandQuickAccess.rank(
                catalog, Arrays.asList("git status", "pwd"),
                Arrays.asList("pwd", "pkg install"));
        assertEquals("git status", sorted.get(0).command);
        assertEquals("pwd", sorted.get(1).command);
        assertEquals("pkg install", sorted.get(2).command);
        assertEquals(catalog.size(), sorted.size());
        assertEquals("cd", sorted.get(3).command);
        assertEquals("pwd", catalog.get(0).command);
    }

    @Test public void readyFilterDoesNotAdvertiseUnknownBinaries() {
        List<KlyroCommandCatalog.Entry> list = KlyroCommandQuickAccess.available(
                KlyroCommandCatalog.all(), null);
        assertTrue(list.stream().anyMatch(e -> "pwd".equals(e.command)));
        assertFalse(list.stream().anyMatch(e -> "git status".equals(e.command)));
        assertTrue(KlyroCommandQuickAccess.available(null, null).isEmpty());
    }
}
