package com.termux.terminal;

/** Regression: CSI 3 J discards scrollback; CSI 2 J clears the visible screen. */
public final class TerminalClearSequenceTest extends TerminalTestCase {
    public void testClearScreenAndScrollbackWithoutWritingToProcess() {
        withTerminalSized(20, 3);
        enterString("first\r\nsecond\r\nthird\r\nfourth");
        assertTrue(mTerminal.getScreen().getActiveTranscriptRows() > 0);
        assertTrue(mTerminal.getScreen().getTranscriptText().contains("fourth"));
        mOutput.getOutputAndClear();

        enterString("\u001b[3J\u001b[2J\u001b[H");

        assertEquals(0, mTerminal.getScreen().getActiveTranscriptRows());
        assertEquals("", mTerminal.getScreen().getTranscriptText());
        assertEquals("", mOutput.getOutputAndClear());
    }
}
