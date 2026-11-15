package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class TerminalActionPolicyTest {
    @Test public void emptyOrPromptOnlyUsesPasteAction() {
        assertFalse(TerminalActionPolicy.hasCopyableOutput(null));
        assertFalse(TerminalActionPolicy.hasCopyableOutput(""));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("   "));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("klyro ~ $"));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("klyro ~/imports $   "));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("klyro ~ $\nklyro ~ $"));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("klyro ~ $\r\nklyro ~/imports $"));
        assertTrue(TerminalActionPolicy.hasCopyableOutput("klyro ~ $\nhello\nklyro ~ $"));
    }

    @Test public void pasteRemainsAccessibleAfterOutputExists() {
        assertTrue(TerminalActionPolicy.shouldSelect(true, false));
        assertFalse("Long press always pastes even with history",
                TerminalActionPolicy.shouldSelect(true, true));
        assertFalse("Blank prompt defaults to paste",
                TerminalActionPolicy.shouldSelect(false, false));
        assertFalse(TerminalActionPolicy.shouldSelect(false, true));
    }

    @Test public void smartKeyDependsOnPtyNotStaleClipboardFlag() {
        assertTrue(TerminalActionPolicy.canUseSmartKey(true, false));
        assertTrue(TerminalActionPolicy.canUseSmartKey(false, true));
        assertFalse(TerminalActionPolicy.canUseSmartKey(false, false));
    }

    @Test public void selectionTailPreservesShortCopyableText() {
        assertEquals("hello", TerminalActionPolicy.selectionTail("hello"));
        assertEquals("", TerminalActionPolicy.selectionTail(null));
        assertEquals("", TerminalActionPolicy.selectionTail(""));
    }

    @Test public void selectionTailIsBounded() {
        String longText = "A".repeat(TerminalActionPolicy.MAX_SELECTION_UTF16_UNITS + 1000);
        String bounded = TerminalActionPolicy.selectionTail(longText);
        assertEquals(TerminalActionPolicy.MAX_SELECTION_UTF16_UNITS, bounded.length());
        assertEquals("A".repeat(TerminalActionPolicy.MAX_SELECTION_UTF16_UNITS), bounded);
    }

    @Test public void selectionTailDoesNotSplitEmojiAtBoundary() {
        String text = "😀" + "x".repeat(TerminalActionPolicy.MAX_SELECTION_UTF16_UNITS - 1);
        String bounded = TerminalActionPolicy.selectionTail(text);
        assertEquals("x".repeat(TerminalActionPolicy.MAX_SELECTION_UTF16_UNITS - 1), bounded);
        assertFalse(Character.isLowSurrogate(bounded.charAt(0)));
    }

    @Test public void outputOrTypedCommandsUsesSelectionAction() {
        assertTrue(TerminalActionPolicy.hasCopyableOutput("hello"));
        assertTrue(TerminalActionPolicy.hasCopyableOutput("klyro ~ $ ls"));
        assertTrue(TerminalActionPolicy.hasCopyableOutput("klyro ~ $\nfile.txt\nklyro ~ $"));
        assertTrue(TerminalActionPolicy.hasCopyableOutput("bash-5.3$"));
    }
}
