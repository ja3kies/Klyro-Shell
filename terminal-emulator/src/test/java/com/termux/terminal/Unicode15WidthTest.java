package com.termux.terminal;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;

public class Unicode15WidthTest extends TerminalTestCase {
    public void testNewEmojiUseTwoColumnsForCodePointAndSurrogateApis() {
        for (int codePoint : new int[] {0x1FAE0, 0x1FAF6, 0x1FAE8, 0x1FAF8}) {
            assertEquals(2, WcWidth.width(codePoint));
            assertEquals(2, WcWidth.width(Character.toChars(codePoint), 0));
        }
    }

    public void testNewCombiningMarksUseZeroColumns() {
        for (int codePoint : new int[] {0x11F00, 0x11F01, 0x1E4EC, 0x1E4EF}) {
            assertEquals(0, WcWidth.width(codePoint));
            assertEquals(0, WcWidth.width(Character.toChars(codePoint), 0));
        }
        assertEquals(2, WcWidth.width(0x754C));
        assertEquals(1, WcWidth.width('A'));
        assertEquals(0, WcWidth.width(0x301));
        assertEquals(0, WcWidth.width(27));
    }

    public void testTablesRemainOrderedAndNonOverlappingForBinarySearch() throws Exception {
        for (String name : new String[] {"ZERO_WIDTH", "WIDE_EASTASIAN"}) {
            Field field = WcWidth.class.getDeclaredField(name);
            field.setAccessible(true);
            int[][] intervals = (int[][]) field.get(null);
            int previousEnd = -1;
            for (int[] interval : intervals) {
                assertEquals(2, interval.length);
                assertTrue(name + " interval reversed", interval[0] <= interval[1]);
                assertTrue(name + " overlaps or is unsorted", previousEnd < interval[0]);
                assertTrue(interval[0] >= 0 && interval[1] <= 0x10FFFF);
                previousEnd = interval[1];
            }
        }
    }

    public void testSplitUtf8OutputUpdatesCursorAndDeviceStatusReport() {
        withTerminalSized(20, 4);
        String text = "A\uD83E\uDEE0B\uD83E\uDEF6e\uD839\uDCEC";
        for (byte value : text.getBytes(StandardCharsets.UTF_8)) mTerminal.append(new byte[] {value}, 1);
        assertInvariants();
        assertCursorAt(0, 7);
        assertEquals(text, mTerminal.getScreen().getTranscriptText());
        assertEnteringStringGivesResponse("\033[6n", "\033[1;8R");
    }

    public void testNewWideEmojiWrapBeforeTheRightMargin() {
        withTerminalSized(5, 4);
        enterString("abcd\uD83E\uDEE0");
        assertCursorAt(1, 2);
        assertEquals("abcd ", mTerminal.getScreen().getSelectedText(0, 0, 4, 0));
        assertEquals("\uD83E\uDEE0", mTerminal.getScreen().getSelectedText(0, 1, 4, 1));
    }

    public void testResizeReflowAndCopyPreserveMixedUnicodeText() {
        withTerminalSized(10, 4);
        String text = "abc\uD83E\uDEE0\uD83E\uDEF6e\uD839\uDCEC";
        enterString(text).assertCursorAt(0, 8);
        resize(5, 4);
        assertEquals(text, mTerminal.getScreen().getTranscriptText());
        resize(10, 4);
        assertEquals(text, mTerminal.getScreen().getTranscriptText());
    }

    public void testZwjRemainsZeroWidthWithoutClaimingGraphemeClusterRendering() {
        withTerminalSized(10, 4);
        String sequence = "\uD83D\uDC69\u200D\uD83D\uDCBB";
        assertEquals(0, WcWidth.width(0x200D));
        enterString(sequence).assertCursorAt(0, 4);
        assertEquals(sequence, mTerminal.getScreen().getTranscriptText());
    }
}
