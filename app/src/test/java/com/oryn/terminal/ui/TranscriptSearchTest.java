package com.oryn.terminal.ui;

import com.termux.terminal.TerminalBuffer;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class TranscriptSearchTest {
    private static final int COLUMNS = 20;
    private static final int ROWS = 4;

    private void write(TerminalBuffer screen, int row, String value) {
        int column = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            screen.setChar(column, row, codePoint, screen.getStyleAt(0, 0));
            column += com.termux.terminal.WcWidth.width(codePoint);
            offset += Character.charCount(codePoint);
        }
    }

    @Test public void emptyAndMissingQueries() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        assertTrue(TranscriptSearch.findRows(screen, COLUMNS, ROWS, "").isEmpty());
        assertTrue(TranscriptSearch.findRows(screen, COLUMNS, ROWS, "  ").isEmpty());
        assertTrue(TranscriptSearch.findRows(screen, COLUMNS, ROWS, "missing").isEmpty());
    }

    @Test public void rejectsOversizedOrInvalidQueriesWithoutTouchingTheBuffer() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        assertTrue(TranscriptSearch.findRows(screen, 0, ROWS, "x").isEmpty());
        assertTrue(TranscriptSearch.findRows(screen, COLUMNS, 0, "x").isEmpty());
        assertTrue(TranscriptSearch.findRows(screen, COLUMNS, ROWS, null).isEmpty());
        assertTrue(TranscriptSearch.findRows(screen, COLUMNS, ROWS,
            "x".repeat(TranscriptSearch.MAX_QUERY_CHARS + 1)).isEmpty());
        assertEquals(0, TranscriptSearch.nextIndex(10, 1, -1));
        assertEquals(0, TranscriptSearch.topRow(5, -5, -1));
    }

    @Test public void historyScreenAndRepeatedMatchesKeepBufferCoordinates() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "Hit hit");
        screen.scrollDownOneLine(0, ROWS, screen.getStyleAt(0, 0));
        write(screen, 2, "界😀 HIT");
        assertEquals(Arrays.asList(-1, -1, 2), TranscriptSearch.findRows(screen, COLUMNS, ROWS, "hit"));
        write(screen, 3, "hit");
        assertEquals(Arrays.asList(-1, -1, 2, 3), TranscriptSearch.findRows(screen, COLUMNS, ROWS, "hit"));
    }

    @Test public void wrappedTextMapsMatchToItsStartingPhysicalRow() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "123456789012345678hi");
        screen.setLineWrap(0);
        write(screen, 1, "t and hit");
        assertEquals(Arrays.asList(0, 1), TranscriptSearch.findRows(screen, COLUMNS, ROWS, "hit"));
        assertEquals(Arrays.asList(0, 1), TranscriptSearch.findRows(screen, COLUMNS, ROWS, "hi"));
    }

    @Test public void largeCircularTranscriptIncludesNewestMatchesWithoutTruncation() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 12020, ROWS);
        for (int i = 0; i < 15000; i++) {
            write(screen, ROWS - 1, "hit");
            screen.scrollDownOneLine(0, ROWS, screen.getStyleAt(0, 0));
        }
        java.util.List<Integer> matches = TranscriptSearch.findRows(screen, COLUMNS, ROWS, "hit");
        assertTrue(matches.size() > 10000);
        assertEquals(Integer.valueOf(-screen.getActiveTranscriptRows()), matches.get(0));
        assertEquals(Integer.valueOf(ROWS - 2), matches.get(matches.size() - 1));
    }

    @Test public void caseInsensitiveSearchFindsEachOccurrence() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "Git GIT git");
        assertEquals(Arrays.asList(0, 0, 0),
            TranscriptSearch.findRows(screen, COLUMNS, ROWS, "gIt"));
    }

    @Test public void searchDoesNotMutateTerminalBuffer() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 1, "immutable output");
        String before = screen.getSelectedText(0, 1, COLUMNS - 1, 1, false, false);
        assertEquals(Arrays.asList(1),
            TranscriptSearch.findRows(screen, COLUMNS, ROWS, "output"));
        assertEquals(before,
            screen.getSelectedText(0, 1, COLUMNS - 1, 1, false, false));
    }

    @Test public void searchIsCaseInsensitiveButDoesNotMatchPartialDifferentWords() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "alpha beta ALPHA");
        assertEquals(Arrays.asList(0, 0),
            TranscriptSearch.findRows(screen, COLUMNS, ROWS, "alpha"));
        assertTrue(TranscriptSearch.findRows(screen, COLUMNS, ROWS, "gamma").isEmpty());
    }

    @Test public void searchHandlesUnicodeAndAdjacentOccurrences() {
        TerminalBuffer screen = new TerminalBuffer(COLUMNS, 100, ROWS);
        write(screen, 0, "界界 testtest");
        assertEquals(Arrays.asList(0, 0),
            TranscriptSearch.findRows(screen, COLUMNS, ROWS, "test"));
        assertEquals(Arrays.asList(0, 0),
            TranscriptSearch.findRows(screen, COLUMNS, ROWS, "界"));
    }

    @Test public void navigationWrapsAndScrollClampsAtBothEnds() {
        assertEquals(2, TranscriptSearch.nextIndex(0, -1, 3));
        assertEquals(0, TranscriptSearch.nextIndex(2, 1, 3));
        assertEquals(0, TranscriptSearch.nextIndex(0, 1, 0));
        assertEquals(0, TranscriptSearch.nextIndex(0, -1, 1));
        assertEquals(-100, TranscriptSearch.topRow(-100, 24, 100));
        assertEquals(-62, TranscriptSearch.topRow(-50, 24, 100));
        assertEquals(0, TranscriptSearch.topRow(23, 24, 100));
        assertEquals(0, TranscriptSearch.topRow(0, 4, 0));
    }
}
