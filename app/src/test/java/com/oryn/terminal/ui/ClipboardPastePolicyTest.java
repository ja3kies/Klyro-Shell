package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClipboardPastePolicyTest {
    @Test public void rejectsMissingOversizedAndNulText() {
        assertFalse(ClipboardPastePolicy.canPaste(null));
        assertFalse(ClipboardPastePolicy.canPaste(""));
        assertFalse(ClipboardPastePolicy.canPaste("abc\u0000def"));
        assertFalse(ClipboardPastePolicy.canPaste("a".repeat(ClipboardPastePolicy.MAX_CHARS + 1)));
    }

    @Test public void acceptsBoundedTextAndConfirmsNewlines() {
        assertTrue(ClipboardPastePolicy.canPaste("a".repeat(ClipboardPastePolicy.MAX_CHARS)));
        assertFalse(ClipboardPastePolicy.requiresConfirmation("echo hello"));
        assertTrue(ClipboardPastePolicy.requiresConfirmation("echo hello\nrm -i file"));
        assertTrue(ClipboardPastePolicy.requiresConfirmation("echo hello\r"));
    }
}
