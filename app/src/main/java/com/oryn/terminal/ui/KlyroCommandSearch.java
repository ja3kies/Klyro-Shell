package com.oryn.terminal.ui;

import java.util.Locale;

/** Bounded, locale-stable, token-order-independent filtering of trusted app actions. */
public final class KlyroCommandSearch {
    public static final int MAX_QUERY_LENGTH = 96;
    private KlyroCommandSearch() {}

    public static boolean matches(String query, String title, String details) {
        if (query == null || query.trim().isEmpty()) return true;
        if (query.length() > MAX_QUERY_LENGTH) return false;
        String content = ((title == null ? "" : title) + " " +
                (details == null ? "" : details)).toLowerCase(Locale.ROOT);
        for (String token : query.trim().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (!content.contains(token)) return false;
        }
        return true;
    }
}
