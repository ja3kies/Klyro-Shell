package com.termux.terminal;

import java.nio.charset.StandardCharsets;

public class TerminalUnicodeTest extends TerminalTestCase {
    public void testTrueColorAndWideUnicodeAcrossSplitUtf8Reads() throws Exception {
        withTerminalSized(20, 4);
        byte[] output = "\033[38;2;12;34;56m界😀é".getBytes(StandardCharsets.UTF_8);
        for (byte value : output) mTerminal.append(new byte[]{value}, 1);
        assertInvariants();
        assertEquals("界😀é", mTerminal.getScreen().getSelectedText(0, 0, 19, 0, false, false));
        assertEquals(5, mTerminal.getCursorCol());
        assertEquals(0xff0c2238, TextStyle.decodeForeColor(mTerminal.getScreen().getStyleAt(0, 0)));
    }
}
