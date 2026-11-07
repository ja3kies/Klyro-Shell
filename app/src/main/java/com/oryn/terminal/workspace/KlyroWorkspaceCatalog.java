package com.oryn.terminal.workspace;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Pure project catalog. Only project metadata is persisted, never PTY identities. */
public final class KlyroWorkspaceCatalog {
    public static final String GENERAL = "general";
    public static final int MAX_PROJECTS = 12;
    public static final int MAX_NAME_CHARS = 32;
    private static final int MAX_RECORD_BYTES = 16 * 1024;

    public static final class Entry {
        public final String id;
        public final String name;
        public Entry(String id, String name) { this.id = id; this.name = name; }
    }

    private final List<Entry> entries = new ArrayList<>();
    private String activeId = GENERAL;

    public KlyroWorkspaceCatalog() {
        entries.add(new Entry(GENERAL, "General"));
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public String activeId() { return activeId; }

    public boolean contains(String id) {
        if (id == null) return false;
        for (Entry entry : entries) if (entry.id.equals(id)) return true;
        return false;
    }

    public static String cleanName(String value) {
        if (value == null) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length();) {
            int point = value.codePointAt(i);
            i += Character.charCount(point);
            int type = Character.getType(point);
            if (Character.isISOControl(point) || type == Character.FORMAT ||
                    type == Character.LINE_SEPARATOR || type == Character.PARAGRAPH_SEPARATOR)
                continue;
            if (result.length() + Character.charCount(point) > MAX_NAME_CHARS) break;
            result.appendCodePoint(point);
        }
        return result.toString().trim();
    }

    private boolean nameAvailable(String name, String excludingId) {
        if (name.isEmpty()) return false;
        for (Entry entry : entries) {
            if (!entry.id.equals(excludingId) &&
                    entry.name.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
                return false;
        }
        return true;
    }

    public String create(String rawName) {
        String name = cleanName(rawName);
        if (entries.size() >= MAX_PROJECTS || !nameAvailable(name, null)) return null;
        String id = UUID.randomUUID().toString();
        entries.add(new Entry(id, name));
        activeId = id;
        return id;
    }

    public boolean rename(String id, String rawName) {
        if (id == null || GENERAL.equals(id)) return false;
        String name = cleanName(rawName);
        if (!nameAvailable(name, id)) return false;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).id.equals(id)) {
                if (entries.get(i).name.equals(name)) return false;
                entries.set(i, new Entry(id, name));
                return true;
            }
        }
        return false;
    }

    public boolean delete(String id) {
        if (id == null || GENERAL.equals(id)) return false;
        boolean removed = entries.removeIf(entry -> entry.id.equals(id));
        if (removed && activeId.equals(id)) activeId = GENERAL;
        return removed;
    }

    public boolean select(String id) {
        if (!contains(id)) return false;
        if (activeId.equals(id)) return false;
        activeId = id;
        return true;
    }

    public String encode() {
        StringBuilder output = new StringBuilder("v1\n");
        for (Entry entry : entries) {
            if (GENERAL.equals(entry.id)) continue;
            output.append(entry.id).append('\t')
                    .append(Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(entry.name.getBytes(StandardCharsets.UTF_8)))
                    .append('\n');
        }
        return output.toString();
    }

    public static KlyroWorkspaceCatalog decode(String source, String requestedActive) {
        KlyroWorkspaceCatalog catalog = new KlyroWorkspaceCatalog();
        if (source != null && source.length() <= MAX_RECORD_BYTES && source.startsWith("v1\n")) {
            String[] lines = source.split("\n");
            for (int i = 1; i < lines.length && catalog.entries.size() < MAX_PROJECTS; i++) {
                int separator = lines[i].indexOf('\t');
                if (separator < 1) continue;
                String id = lines[i].substring(0, separator);
                if (!id.matches("[0-9a-fA-F-]{36}") || catalog.contains(id)) continue;
                try {
                    byte[] raw = Base64.getUrlDecoder().decode(lines[i].substring(separator + 1));
                    if (raw.length > 128) continue;
                    String name = new String(raw, StandardCharsets.UTF_8);
                    if (catalog.nameAvailable(name, null) && name.equals(cleanName(name)))
                        catalog.entries.add(new Entry(id, name));
                } catch (IllegalArgumentException ignored) {
                    // Bad or partially written preferences must not break startup.
                }
            }
        }
        if (catalog.contains(requestedActive)) catalog.activeId = requestedActive;
        return catalog;
    }
}
