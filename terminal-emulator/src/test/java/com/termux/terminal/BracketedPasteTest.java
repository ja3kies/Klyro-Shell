package com.termux.terminal;

public class BracketedPasteTest extends TerminalTestCase {
    public void testPasteIsOneTransactionAndStripsTerminalEscapes() throws Exception {
        withTerminalSized(20, 4);
        enterString("\033[?2004h");
        int writes = mOutput.writeCalls;
        mTerminal.paste("one\ntwo\033\u0085");
        assertEquals(writes + 1, mOutput.writeCalls);
        assertEquals("\033[200~one\rtwo\033[201~", mOutput.getOutputAndClear());
    }
}
