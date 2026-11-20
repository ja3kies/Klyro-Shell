package com.oryn.terminal.ui;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Bounded, locale-stable, token-order-independent filtering of trusted app actions. */
public final class KlyroCommandSearch {
    public static final int MAX_QUERY_LENGTH = 96;
    private static final Pattern WORD_BREAK = Pattern.compile("[\\s\\p{Z}]+");

    private static String fold(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFC);
    }

    private KlyroCommandSearch() {}

    public static boolean matches(String query, String title, String details) {
        if (query == null || query.trim().isEmpty()) return true;
        if (query.codePointCount(0, query.length()) > MAX_QUERY_LENGTH) return false;
        String content = fold((title == null ? "" : title) + " " +
                (details == null ? "" : details));
        for (String token : WORD_BREAK.split(fold(query.trim()))) {
            if (!content.contains(token)) return false;
        }
        return true;
    }
}
