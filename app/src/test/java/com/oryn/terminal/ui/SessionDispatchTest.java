package com.oryn.terminal.ui;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SessionDispatchTest {
    final List<String> sent=new ArrayList<>();
    boolean paste(Long expected,Long current,boolean running,String text){return TerminalActionPolicy.pasteIfCurrent(expected,current,running,text,sent::add);}
    @Test public void refusedInputIsNotReportedAsInserted(){
        assertFalse(TerminalActionPolicy.pasteIfCurrent(1L,1L,true,"echo hello",text->false));
    }
    @Test public void renamedTargetKeepsItsIdentity(){
        assertTrue(TerminalActionPolicy.runIfCurrent(42L,42L,true,()->sent.add("closed renamed")));
        assertEquals(Arrays.asList("closed renamed"),sent);
    }
    @Test public void equalTitlesCannotAuthorizeDifferentSessionIds(){
        assertFalse(TerminalActionPolicy.runIfCurrent(42L,43L,true,()->sent.add("closed duplicate title")));
        assertTrue(sent.isEmpty());
    }
    @Test public void liveTargetReceivesExactPayload(){assertTrue(paste(1L,1L,true,"echo hello"));assertEquals(Arrays.asList("echo hello"),sent);}
    @Test public void switchedTabNeverReceivesOldPanePaste(){assertFalse(paste(1L,2L,true,"echo old"));assertTrue(sent.isEmpty());}
    @Test public void endedSessionRejectsPaste(){assertFalse(paste(1L,1L,false,"echo old"));assertTrue(sent.isEmpty());}
    @Test public void absentExpectedIdRejects(){assertFalse(paste(null,1L,true,"x"));}
    @Test public void lastTabClosedRejects(){assertFalse(paste(1L,null,true,"x"));}
    @Test public void invalidZeroIdRejects(){assertFalse(paste(0L,0L,true,"x"));}
    @Test public void negativeIdRejects(){assertFalse(paste(-1L,-1L,true,"x"));}
    @Test public void longIdsAreNotTruncated(){assertTrue(paste(1L<<40,1L<<40,true,"x"));}
    @Test public void confirmedSnapshotDoesNotReadNewClipboard(){String snapshot="echo A\necho B";String clipboardNow="rm -rf ~";assertTrue(paste(1L,1L,true,snapshot));assertEquals(snapshot,sent.get(0));assertNotEquals(clipboardNow,sent.get(0));}
    @Test public void staleCloseCannotSelectCurrentTab(){assertFalse(TerminalActionPolicy.runIfCurrent(1L,2L,true,()->sent.add("closed")));assertTrue(sent.isEmpty());}
    @Test public void closedTargetCannotInvokeDestructiveCallback(){assertFalse(TerminalActionPolicy.runIfCurrent(1L,1L,false,()->sent.add("closed")));}
    @Test public void ansiControlCannotReachConsumer(){assertFalse(paste(1L,1L,true,"echo\033[31m"));assertTrue(sent.isEmpty());}
    @Test public void malformedUtf16CannotReachConsumer(){assertFalse(paste(1L,1L,true,"bad\ud800"));assertTrue(sent.isEmpty());}
    @Test public void invisibleNewlineCannotReachConsumer(){assertFalse(paste(1L,1L,true,"echo\u2028rm"));assertTrue(sent.isEmpty());}
    @Test public void byteLimitAppliesBeforeConsumer(){assertFalse(paste(1L,1L,true,"😀".repeat(20000)));assertTrue(sent.isEmpty());}
}
