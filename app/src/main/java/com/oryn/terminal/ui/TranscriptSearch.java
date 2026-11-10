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


    /** Maximum result details retained for an interactive UI; total is still exact. */
    public static final int MAX_VISIBLE_MATCHES = 300;
    private static final int PREVIEW_BEFORE = 42;
    private static final int PREVIEW_AFTER = 84;

    /** Each result captures the exact matched text, including matches across wrapped rows. */
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

        private Snapshot(List<Occurrence> newest, int totalCount) {
            this.newest = java.util.Collections.unmodifiableList(newest);
            this.totalCount = totalCount;
            this.limited = totalCount > newest.size();
        }
    }

    /**
     * Finds every occurrence but retains details only for the newest matches.
     * This avoids unbounded UI state on a long transcript while allowing an
     * honest total count; no shell text is persisted or modified.
     */
    public static Snapshot scanRecent(TerminalBuffer screen, int columns, int rows,
            String query, boolean caseSensitive) {
        java.util.ArrayDeque<Occurrence> tail = new java.util.ArrayDeque<>(MAX_VISIBLE_MATCHES);
        if (screen == null || columns <= 0 || rows <= 0 || query == null ||
                query.length() > MAX_QUERY_CHARS || query.trim().isEmpty()) {
            return new Snapshot(new ArrayList<>(), 0);
        }
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
                if (count < Integer.MAX_VALUE) count++;
                int from = Math.max(0, offset - PREVIEW_BEFORE);
                int to = Math.min(text.length(), offset + query.length() + PREVIEW_AFTER);
                // Avoid slicing a surrogate pair at the context boundaries.
                if (from > 0 && Character.isLowSurrogate(text.charAt(from)) &&
                        Character.isHighSurrogate(text.charAt(from - 1))) from--;
                if (to < text.length() && to > 0 &&
                        Character.isHighSurrogate(text.charAt(to - 1)) &&
                        Character.isLowSurrogate(text.charAt(to))) to++;
                Occurrence occurrence = new Occurrence(logicalFirstRow + physicalRow, offset,
                        text.substring(from, to), offset - from, offset - from + query.length());
                if (tail.size() == MAX_VISIBLE_MATCHES) tail.removeFirst();
                tail.addLast(occurrence);
            }
            line.setLength(0);
            starts.clear();
            logicalFirstRow = row + 1;
        }
        return new Snapshot(new ArrayList<>(tail), count);
    }

    /** Preserve the selected physical position as new terminal output arrives. */
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
