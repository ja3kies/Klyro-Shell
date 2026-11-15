package com.termux.terminal;

import org.junit.Test;
import static org.junit.Assert.*;

public class OutputBoundsTest {
    @Test public void smallTranscriptMatchesExistingExtraction() {
        TerminalBuffer b=new TerminalBuffer(20,100,4);
        b.setChar(0,0,'A',TextStyle.NORMAL);
        assertEquals(b.getTranscriptText(),b.getTranscriptTail(1000).text.trim());
        assertFalse(b.getTranscriptTail(1000).truncated);
    }
    @Test public void saturatedHistorySelectionIsBoundedAndKeepsNewestOutput() {
        TerminalBuffer b=new TerminalBuffer(100,50000,4);
        for(int i=0;i<50000;i++){b.setChar(0,3,'X',TextStyle.NORMAL);b.scrollDownOneLine(0,4,TextStyle.NORMAL);}
        long started=System.nanoTime();
        TerminalBuffer.TranscriptText tail=b.getTranscriptTail(4096);
        System.out.println("bounded_tail_ns="+(System.nanoTime()-started));
        assertTrue(tail.text.length()<=4096);assertTrue(tail.truncated);assertTrue(tail.text.contains("X"));
    }
    @Test public void selectionBoundaryNeverSplitsUtf16Pair() {
        TerminalBuffer b=new TerminalBuffer(10,30,2);
        b.setChar(0,0,0x1f600,TextStyle.NORMAL);
        b.setChar(2,0,'x',TextStyle.NORMAL);
        String text=b.getTranscriptTail(3).text;
        assertTrue(text.isEmpty() || !Character.isLowSurrogate(text.charAt(0)));
    }
    @Test(timeout=3000) public void combiningFloodCannotGrowACellWithoutBound() {
        TerminalRow row=new TerminalRow(80,TextStyle.NORMAL);
        row.setChar(0,'a',TextStyle.NORMAL);
        for(int i=0;i<100000;i++)row.setChar(0,0x301,TextStyle.NORMAL);
        assertTrue(row.getSpaceUsed()<=80+64);assertTrue(row.mText.length<400);
    }
    @Test public void normalGraphemeAndFollowingColumnsSurviveTheBound() {
        TerminalRow row=new TerminalRow(20,TextStyle.NORMAL);
        row.setChar(0,'a',TextStyle.NORMAL);row.setChar(0,0x301,TextStyle.NORMAL);
        row.setChar(1,0x754c,TextStyle.NORMAL);row.setChar(3,'Z',TextStyle.NORMAL);
        assertEquals('a',row.mText[0]);assertEquals(0x301,row.mText[1]);
        assertEquals('Z',row.mText[row.findStartOfColumn(3)]);
    }
}
