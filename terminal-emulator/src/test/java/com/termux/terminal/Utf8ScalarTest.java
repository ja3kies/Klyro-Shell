package com.termux.terminal;

/** Unicode scalar validity is independent of the Android/JVM character catalogue. */
public class Utf8ScalarTest extends TerminalTestCase {
    public void testUnassignedAndNoncharactersRoundTrip() {
        withTerminalSized(20, 4);
        String text = "A\u0378\uFDD0\uDBFF\uDFFF";
        enterString(text).assertCursorAt(0, 4);
        assertEquals(text, mTerminal.getScreen().getTranscriptText());
    }

    public void testValidUtf8BoundaryValuesRoundTrip() {
        withTerminalSized(20, 4);
        String text = "\u07FF\u0800\uD7FF\uE000\uFFFF\uD800\uDC00\uDBFF\uDFFF";
        enterString(text);
        assertEquals(text, mTerminal.getScreen().getTranscriptText());
        assertInvariants();
    }

    public void testSurrogatesAndAboveMaximumBecomeReplacementThenResume() {
        byte[][] invalid = {
                {(byte)0xED, (byte)0xA0, (byte)0x80},
                {(byte)0xF4, (byte)0x90, (byte)0x80, (byte)0x80},
                {(byte)0xF7, (byte)0xBF, (byte)0xBF, (byte)0xBF}
        };
        assertInvalidSequencesResume(invalid);
    }

    public void testOverlongBoundaryEncodingsAreNeverInterpretedAsCharacters() {
        byte[][] invalid = {
                {(byte)0xC1, (byte)0xBF},
                {(byte)0xE0, (byte)0x9F, (byte)0xBF},
                {(byte)0xF0, (byte)0x8F, (byte)0xBF, (byte)0xBF}
        };
        assertInvalidSequencesResume(invalid);
    }

    public void testOverlongEscapeCannotIntroduceAnAnsiCommand() {
        withTerminalSized(20, 4);
        mTerminal.append(new byte[]{(byte)0xC0, (byte)0x9B}, 2);
        enterString("[31mX");
        assertEquals("\uFFFD[31mX", mTerminal.getScreen().getTranscriptText());
        assertCursorAt(0, 6);
    }

    private void assertInvalidSequencesResume(byte[][] sequences) {
        for (byte[] invalid : sequences) {
            withTerminalSized(20, 4);
            for (byte value : invalid) mTerminal.append(new byte[]{value}, 1);
            enterString("X").assertCursorAt(0, 2);
            assertEquals("\uFFFDX", mTerminal.getScreen().getTranscriptText());
        }
    }
}
