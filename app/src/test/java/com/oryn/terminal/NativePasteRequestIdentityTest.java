package com.oryn.terminal;

import org.junit.Test;
import static org.junit.Assert.*;

/** Acknowledgements must address an individual native UI event, not its tab. */
public class NativePasteRequestIdentityTest {
    @Test public void identicalClipboardTextStillCreatesDistinctRequests() {
        TerminalSessionViewModel.NativePasteRequest first =
                new TerminalSessionViewModel.NativePasteRequest(7L, "echo hello\necho world", 10L);
        TerminalSessionViewModel.NativePasteRequest second =
                new TerminalSessionViewModel.NativePasteRequest(7L, "echo hello\necho world", 11L);
        assertNotEquals("Duplicate payloads need unique identities", first, second);
        assertEquals(7L, second.getTabId());
        assertEquals("echo hello\necho world", second.getText());
        assertEquals(11L, second.getRequestId());
    }

    @Test public void previousTabAndNewTabNeverShareRequestIdentity() {
        TerminalSessionViewModel.NativePasteRequest previous =
                new TerminalSessionViewModel.NativePasteRequest(7L, "echo ok", 12L);
        TerminalSessionViewModel.NativePasteRequest next =
                new TerminalSessionViewModel.NativePasteRequest(8L, "echo ok", 13L);
        assertNotEquals(previous, next);
        assertNotEquals(previous.getTabId(), next.getTabId());
        assertFalse(previous == next);
    }
}
