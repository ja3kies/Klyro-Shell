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
                if (!text.regionMatches(true, offset, query, 0, query.length())) continue;
                while (physicalRow + 1 < starts.size() && starts.get(physicalRow + 1) <= offset) physicalRow++;
                matches.add(firstRow + physicalRow);
            }
            line.setLength(0);
            starts.clear();
            firstRow = row + 1;
        }
        return matches;
    }

    public static int nextIndex(int position, int direction, int count) {
        return count <= 0 ? 0 : Math.floorMod(position + direction, count);
    }

    public static int topRow(int matchRow, int visibleRows, int historyRows) {
        return Math.max(-Math.max(0, historyRows),
                Math.min(0, matchRow - Math.max(0, visibleRows) / 2));
    }
}
