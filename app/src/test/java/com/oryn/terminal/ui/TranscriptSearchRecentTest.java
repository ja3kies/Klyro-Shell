package com.oryn.terminal.ui;

import com.termux.terminal.TerminalBuffer;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class TranscriptSearchRecentTest {
    private static final int COLUMNS = 20;
    private static final int ROWS = 4;

    private static void write(TerminalBuffer screen, int row, String text) {
        int x = 0;
        for (int i = 0; i < text.length();) {
            int point = text.codePointAt(i);
            screen.setChar(x, row, point, screen.getStyleAt(0, 0));
            x += com.termux.terminal.WcWidth.width(point);
            i += Character.charCount(point);
        }
    }

    @Test public void emptyAndInvalidQueriesAreSafe() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        assertEquals(0, TranscriptSearch.scanRecent(screen, COLUMNS, ROWS, "", false).totalCount);
        assertEquals(0, TranscriptSearch.scanRecent(null, COLUMNS, ROWS, "hit", false).totalCount);
        assertEquals(0, TranscriptSearch.scanRecent(screen, COLUMNS, ROWS,
                "z".repeat(TranscriptSearch.MAX_QUERY_CHARS + 1), false).totalCount);
    }

    @Test public void preservesEveryOccurrenceAndExactPreview() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "Alpha alpha ALPHA");
        TranscriptSearch.Snapshot result = TranscriptSearch.scanRecent(
                screen, COLUMNS, ROWS, "alpha", false);
        assertEquals(3, result.totalCount);
        assertFalse(result.limited);
        assertEquals(3, result.newest.size());
        assertEquals(0, result.newest.get(0).row);
        for (TranscriptSearch.Occurrence match : result.newest) {
            assertEquals("alpha", match.preview.substring(match.highlightStart,
                    match.highlightEnd).toLowerCase(java.util.Locale.ROOT));
        }
        assertEquals(1, TranscriptSearch.scanRecent(screen, COLUMNS, ROWS, "Alpha", true).totalCount);
    }

    @Test public void wrappedMatchStartsOnCorrectRowAndHasContinuousContext() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "123456789012345678hi");
        screen.setLineWrap(0);
        write(screen, 1, "t here");
        TranscriptSearch.Snapshot result = TranscriptSearch.scanRecent(
                screen, COLUMNS, ROWS, "hit", false);
        assertEquals(1, result.totalCount);
        TranscriptSearch.Occurrence match = result.newest.get(0);
        assertEquals(0, match.row);
        assertEquals("hit", match.preview.substring(match.highlightStart, match.highlightEnd));
    }

    @Test public void hugeTranscriptRetainsOnlyNewestButKeepsTotalExact() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 12200, ROWS);
        for (int i = 0; i < 13000; i++) {
            write(screen, ROWS - 1, "hit");
            screen.scrollDownOneLine(0, ROWS, screen.getStyleAt(0, 0));
        }
        TranscriptSearch.Snapshot result = TranscriptSearch.scanRecent(
                screen, COLUMNS, ROWS, "hit", false);
        assertTrue(result.totalCount > 10000);
        assertTrue(result.limited);
        assertEquals(TranscriptSearch.MAX_VISIBLE_MATCHES, result.newest.size());
        assertEquals(ROWS - 2, result.newest.get(result.newest.size() - 1).row);
        assertTrue(result.newest.get(0).row > -screen.getActiveTranscriptRows());
    }

    @Test public void navigationAnchorsSelectedMatchInUpdatedOutput() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "hit hit hit");
        List<TranscriptSearch.Occurrence> matches = TranscriptSearch.scanRecent(
                screen, COLUMNS, ROWS, "hit", false).newest;
        assertEquals(0, TranscriptSearch.nearestIndex(matches, 0, 0));
        assertEquals(2, TranscriptSearch.nearestIndex(matches, 0, 8));
        assertEquals(2, TranscriptSearch.nearestIndex(matches, 1, 0));
        assertEquals(0, TranscriptSearch.nearestIndex(null, 0, 0));
    }

    @Test public void scanLeavesTheBufferUnmodified() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 2, "private output hit");
        String original = screen.getSelectedText(0, 2, COLUMNS - 1, 2, false, false);
        TranscriptSearch.scanRecent(screen, COLUMNS, ROWS, "hit", false);
        assertEquals(original, screen.getSelectedText(0, 2, COLUMNS - 1, 2, false, false));
    }
}
