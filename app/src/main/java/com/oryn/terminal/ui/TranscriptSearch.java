package com.oryn.terminal.ui;

import com.termux.terminal.TerminalBuffer;
import java.util.ArrayList;
import java.util.List;

/** Search coordinates use buffer rows: negative history, zero-based visible screen. */
public final class TranscriptSearch {
    private TranscriptSearch() {}

    // UI search should never allocate memory for arbitrarily large queries.
    public static final int MAX_QUERY_CHARS = 512;

    public static List<Integer> findRows(TerminalBuffer screen, int columns, int rows, String query) {
        return findRows(screen, columns, rows, query, false);
    }

    public static List<Integer> findRows(TerminalBuffer screen, int columns, int rows,
            String query, boolean caseSensitive) {
        List<Integer> matches = new ArrayList<>();
        if (screen == null || columns <= 0 || rows <= 0 || query == null
                || query.length() > MAX_QUERY_CHARS || query.trim().isEmpty()) return matches;
        StringBuilder line = new StringBuilder();
        List<Integer> starts = new ArrayList<>();
        int firstRow = -screen.getActiveTranscriptRows();
        for (int row = firstRow; row < rows; row++) {
            starts.add(line.length());
            // Explicitly disable joining: coordinates must refer to physical buffer rows.
            line.append(screen.getSelectedText(0, row, columns - 1, row, false, false));
            if (screen.getLineWrap(row) && row + 1 < rows) continue;
            String text = line.toString();
            int physicalRow = 0;
            for (int offset = 0; offset <= text.length() - query.length(); offset++) {
                if (!text.regionMatches(!caseSensitive, offset, query, 0, query.length())) continue;
                while (physicalRow + 1 < starts.size() && starts.get(physicalRow + 1) <= offset) physicalRow++;
                matches.add(firstRow + physicalRow);
            }
            line.setLength(0);
            starts.clear();
            firstRow = row + 1;
        }
        return matches;
    }


    /** Maximum result details retained per page, never the entire transcript. */
    public static final int MAX_VISIBLE_MATCHES = 300;
    private static final int PREVIEW_BEFORE = 42;
    private static final int PREVIEW_AFTER = 84;

    /** Exact matched span, including matches that cross wrapped physical rows. */
    public static final class Occurrence {
        public final int row;
        public final int logicalOffset;
        public final String preview;
        public final int highlightStart;
        public final int highlightEnd;
        private Occurrence(int row, int logicalOffset, String preview,
                int highlightStart, int highlightEnd) {
            this.row = row;
            this.logicalOffset = logicalOffset;
            this.preview = preview;
            this.highlightStart = highlightStart;
            this.highlightEnd = highlightEnd;
        }
    }

    public static final class Snapshot {
        public final List<Occurrence> newest;
        public final int totalCount;
        public final boolean limited;
        /** Zero-based ordinal of the first result within the entire transcript. */
        public final int firstOrdinal;
        public final int pageFromNewest;
        public final int pageCount;

        private Snapshot(List<Occurrence> matches, int count, int firstOrdinal, int page) {
            this.newest = java.util.Collections.unmodifiableList(matches);
            this.totalCount = count;
            this.limited = count > matches.size();
            this.firstOrdinal = firstOrdinal;
            this.pageFromNewest = page;
            this.pageCount = count == 0 ? 0 : (count - 1) / MAX_VISIBLE_MATCHES + 1;
        }
    }

    private interface MatchVisitor {
        void onMatch(int row, int offset, String logicalText);
    }

    /** Scans the physical buffer only; does not persist or modify terminal data. */
    private static int traverse(TerminalBuffer screen, int columns, int rows,
            String query, boolean caseSensitive, MatchVisitor visitor) {
        if (screen == null || columns <= 0 || rows <= 0 || query == null ||
                query.length() > MAX_QUERY_CHARS || query.trim().isEmpty()) return 0;
        int count = 0;
        StringBuilder line = new StringBuilder();
        List<Integer> starts = new ArrayList<>();
        int logicalFirstRow = -screen.getActiveTranscriptRows();
        for (int row = logicalFirstRow; row < rows; row++) {
            starts.add(line.length());
            line.append(screen.getSelectedText(0, row, columns - 1, row, false, false));
            if (screen.getLineWrap(row) && row + 1 < rows) continue;
            String text = line.toString();
            int physicalRow = 0;
            for (int offset = 0; offset <= text.length() - query.length(); offset++) {
                if (!text.regionMatches(!caseSensitive, offset, query, 0, query.length())) continue;
                while (physicalRow + 1 < starts.size() &&
                        starts.get(physicalRow + 1) <= offset) physicalRow++;
                if (visitor != null) visitor.onMatch(logicalFirstRow + physicalRow, offset, text);
                if (count < Integer.MAX_VALUE) count++;
            }
            line.setLength(0);
            starts.clear();
            logicalFirstRow = row + 1;
        }
        return count;
    }

    private static Occurrence occurrence(int row, int offset, String text, int queryLength) {
        int from = Math.max(0, offset - PREVIEW_BEFORE);
        int to = Math.min(text.length(), offset + queryLength + PREVIEW_AFTER);
        // Do not cut a Unicode surrogate pair at preview boundaries.
        if (from > 0 && Character.isLowSurrogate(text.charAt(from)) &&
                Character.isHighSurrogate(text.charAt(from - 1))) from--;
        if (to < text.length() && to > 0 &&
                Character.isHighSurrogate(text.charAt(to - 1)) &&
                Character.isLowSurrogate(text.charAt(to))) to++;
        return new Occurrence(row, offset, text.substring(from, to),
                offset - from, offset - from + queryLength);
    }

    /**
     * Default page is the newest 300 matches; count is exact even for huge logs.
     * Older pages are available through scanPage without storing every match.
     */
    public static Snapshot scanRecent(TerminalBuffer screen, int columns, int rows,
            String query, boolean caseSensitive) {
        java.util.ArrayDeque<Occurrence> tail = new java.util.ArrayDeque<>(MAX_VISIBLE_MATCHES);
        int count = traverse(screen, columns, rows, query, caseSensitive, (row, offset, text) -> {
            if (tail.size() == MAX_VISIBLE_MATCHES) tail.removeFirst();
            tail.addLast(occurrence(row, offset, text, query.length()));
        });
        return new Snapshot(new ArrayList<>(tail), count,
                Math.max(0, count - tail.size()), 0);
    }

    /**
     * Move backward through ALL matches in fixed-size pages. Only older pages
     * need a second scan, but retained memory is always bounded to 300 results.
     */
    public static Snapshot scanPage(TerminalBuffer screen, int columns, int rows,
            String query, boolean caseSensitive, int pageFromNewest) {
        Snapshot latest = scanRecent(screen, columns, rows, query, caseSensitive);
        if (pageFromNewest <= 0 || latest.pageCount <= 1) return latest;
        int page = Math.min(pageFromNewest, latest.pageCount - 1);
        int end = latest.totalCount - page * MAX_VISIBLE_MATCHES;
        int start = Math.max(0, end - MAX_VISIBLE_MATCHES);
        List<Occurrence> pageMatches = new ArrayList<>(MAX_VISIBLE_MATCHES);
        final int[] position = {0};
        traverse(screen, columns, rows, query, caseSensitive, (row, offset, text) -> {
            int index = position[0]++;
            if (index >= start && index < end) {
                pageMatches.add(occurrence(row, offset, text, query.length()));
            }
        });
        return new Snapshot(pageMatches, latest.totalCount, start, page);
    }

    /** Preserve the closest selected occurrence as new output arrives. */
    public static int nearestIndex(List<Occurrence> matches, int row, int offset) {
        if (matches == null || matches.isEmpty()) return 0;
        int best = 0;
        long bestDistance = Long.MAX_VALUE;
        for (int i = 0; i < matches.size(); i++) {
            Occurrence match = matches.get(i);
            long distance = Math.abs((long) match.row - row) * 100000L
                    + Math.min(99999L, Math.abs((long) match.logicalOffset - offset));
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    public static int nextIndex(int position, int direction, int count) {
        return count <= 0 ? 0 : Math.floorMod(position + direction, count);
    }

    public static int topRow(int matchRow, int visibleRows, int historyRows) {
        return Math.max(-Math.max(0, historyRows),
                Math.min(0, matchRow - Math.max(0, visibleRows) / 2));
    }
}
