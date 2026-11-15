package com.oryn.terminal.core;

/** Character limits for user-visible text use Unicode code points, not UTF-16 units. */
public final class KlyroTextLimit {
    private KlyroTextLimit() {}

    public static int codePointCount(String value) {
        return value.codePointCount(0, value.length());
    }

    public static String takeCodePoints(String value, int limit) {
        if (limit <= 0) return "";
        int count = codePointCount(value);
        return count <= limit ? value : value.substring(0, value.offsetByCodePoints(0, limit));
    }
}
