package com.oryn.terminal.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Local shortcuts for the curated command catalog. Stores catalog identifiers,
 * never user command lines, plugin scripts, shell arguments or terminal output.
 * This is NOT a shell command history. Insertion never executes a command.
 */
public final class KlyroCommandQuickAccess {
    public static final int MAX_PINNED = 8;
    public static final int MAX_RECENT = 6;
    private static final int MAX_STORED_CHARS = 2048;
    private static final String VERSION = "v1\n";

    private KlyroCommandQuickAccess() {}

    public static boolean isKnown(String commandId) {
        if (commandId == null) return false;
        for (KlyroCommandCatalog.Entry entry : KlyroCommandCatalog.all()) {
            if (entry.command.equals(commandId)
                    && !KlyroCommandCatalog.safeInsertion(entry).isEmpty()) return true;
        }
        return false;
    }

    private static List<String> sanitize(Iterable<String> input, int limit) {
        if (input == null || limit <= 0) return Collections.emptyList();
        List<String> output = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String id : input) {
            if (isKnown(id) && seen.add(id)) {
                output.add(id);
                if (output.size() >= limit) break;
            }
        }
        return Collections.unmodifiableList(output);
    }

    public static List<String> decode(String serialized, int limit) {
        if (serialized == null || serialized.length() > MAX_STORED_CHARS
                || !serialized.startsWith(VERSION)) return Collections.emptyList();
        String[] lines = serialized.substring(VERSION.length()).split("\n", -1);
        List<String> input = new ArrayList<>();
        Collections.addAll(input, lines);
        return sanitize(input, limit);
    }

    public static String encode(List<String> ids, int limit) {
        return VERSION + String.join("\n", sanitize(ids, limit));
    }

    /** When full, keep every previous pin; the UI must explain the limit. */
    public static List<String> togglePinned(List<String> pinned, String id) {
        List<String> old = sanitize(pinned, MAX_PINNED);
        if (!isKnown(id)) return old;
        List<String> result = new ArrayList<>(old);
        if (result.remove(id)) return Collections.unmodifiableList(result);
        if (result.size() >= MAX_PINNED) return old;
        result.add(0, id);
        return Collections.unmodifiableList(result);
    }

    /** Records an insertion from the curated catalog, never a shell execution. */
    public static List<String> recordInsertion(List<String> recent, String id) {
        List<String> old = sanitize(recent, MAX_RECENT);
        if (!isKnown(id)) return old;
        List<String> result = new ArrayList<>();
        result.add(id);
        for (String previous : old) {
            if (!previous.equals(id) && result.size() < MAX_RECENT) result.add(previous);
        }
        return Collections.unmodifiableList(result);
    }

    public static List<KlyroCommandCatalog.Entry> resolve(List<String> ids) {
        List<KlyroCommandCatalog.Entry> result = new ArrayList<>();
        for (String id : sanitize(ids, MAX_PINNED)) {
            for (KlyroCommandCatalog.Entry entry : KlyroCommandCatalog.all()) {
                if (entry.command.equals(id)) {
                    result.add(entry);
                    break;
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Pinned first, then recently inserted, then original catalog order. */
    public static List<KlyroCommandCatalog.Entry> rank(
            List<KlyroCommandCatalog.Entry> entries, List<String> pinned, List<String> recent) {
        if (entries == null || entries.isEmpty()) return Collections.emptyList();
        List<KlyroCommandCatalog.Entry> ordered = new ArrayList<>();
        Set<String> used = new HashSet<>();
        List<String> priority = new ArrayList<>();
        priority.addAll(sanitize(pinned, MAX_PINNED));
        priority.addAll(sanitize(recent, MAX_RECENT));
        for (String id : priority) {
            for (KlyroCommandCatalog.Entry entry : entries) {
                if (entry.command.equals(id) && used.add(id)) ordered.add(entry);
            }
        }
        for (KlyroCommandCatalog.Entry entry : entries) {
            if (used.add(entry.command)) ordered.add(entry);
        }
        return Collections.unmodifiableList(ordered);
    }

    public static List<KlyroCommandCatalog.Entry> available(
            List<KlyroCommandCatalog.Entry> entries, File prefix) {
        if (entries == null) return Collections.emptyList();
        List<KlyroCommandCatalog.Entry> result = new ArrayList<>();
        for (KlyroCommandCatalog.Entry entry : entries) {
            if (KlyroCommandCatalog.isInstalled(entry, prefix)) result.add(entry);
        }
        return Collections.unmodifiableList(result);
    }
}
