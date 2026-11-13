package com.termux.terminal;

public class PasteDeliveryTest extends TerminalTestCase {
    static class Sink extends MockTerminalOutput {
        boolean accepts=true;
        public boolean tryWrite(byte[] data,int off,int count){if(!accepts)return false;write(data,off,count);return true;}
    }
    Sink sink;
    void setupSink(){sink=new Sink();mOutput=sink;withTerminalSized(40,5);}
    public void testBracketEnvelopeIsOneAcceptedWrite(){setupSink();enterString("\033[?2004h");assertTrue(mTerminal.pasteWithResult("x"));assertEquals(1,sink.writeCalls);assertEquals("\033[200~x\033[201~",sink.getOutputAndClear());}
    public void testRejectedPasteDoesNotLeaveHalfEnvelope(){setupSink();enterString("\033[?2004h");sink.accepts=false;assertFalse(mTerminal.pasteWithResult("x"));assertEquals(0,sink.writeCalls);assertEquals("",sink.getOutputAndClear());}
    public void testNormalModeDoesNotAddTerminalControls(){setupSink();assertTrue(mTerminal.pasteWithResult("normal"));assertEquals("normal",sink.getOutputAndClear());}
    public void testCrLfNormalizationIsUnchanged(){setupSink();assertTrue(mTerminal.pasteWithResult("a\r\nb\nc"));assertEquals("a\rb\rc",sink.getOutputAndClear());}
    public void testUnsafeEscapesRemainFiltered(){setupSink();assertTrue(mTerminal.pasteWithResult("a\033\u0085b"));assertEquals("ab",sink.getOutputAndClear());}
    public void testMultibyteInputIsOneExactTransaction(){setupSink();assertTrue(mTerminal.pasteWithResult("é界😀"));assertEquals("é界😀",sink.getOutputAndClear());assertEquals(1,sink.writeCalls);}
}
