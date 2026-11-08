package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClipboardPastePolicyTest {
    @Test public void rejectsMissingOversizedAndNulText() {
        assertFalse(ClipboardPastePolicy.canPaste(null));
        assertFalse(ClipboardPastePolicy.canPaste(""));
        assertFalse(ClipboardPastePolicy.canPaste("abc\u0000def"));
        assertFalse(ClipboardPastePolicy.canPaste("echo ok\u001B[31m"));
        assertFalse(ClipboardPastePolicy.canPaste("abc\u007f"));
        assertFalse(ClipboardPastePolicy.canPaste("abc\u0085"));
        assertFalse(ClipboardPastePolicy.canPaste("bad\uD800"));
        assertFalse(ClipboardPastePolicy.canPaste("echo ok\u202Erm -rf"));
        assertFalse(ClipboardPastePolicy.canPaste("echo\u200B dangerous"));
        assertFalse(ClipboardPastePolicy.canPaste("echo ok\u2028rm file"));
        assertFalse(ClipboardPastePolicy.canPaste("echo ok\u2029rm file"));
        assertFalse(ClipboardPastePolicy.canPaste("\uDC00bad"));
        assertTrue(ClipboardPastePolicy.canPaste("emoji \uD83D\uDE00"));
        assertFalse(ClipboardPastePolicy.canPaste("a".repeat(ClipboardPastePolicy.MAX_CHARS + 1)));
    }

    @Test public void rejectsBlankAndOversizedUtf8Payloads() {
        assertFalse(ClipboardPastePolicy.canPaste("   "));
        assertFalse(ClipboardPastePolicy.canPaste("\\u00A0\\u2007"));
        assertFalse(ClipboardPastePolicy.canPaste("\\t\\n"));
        assertFalse(ClipboardPastePolicy.canPaste("😀".repeat(20000)));
        assertTrue(ClipboardPastePolicy.canPaste("😀".repeat(1000)));
    }

    @Test public void rejectsUnicodeNoncharactersIncludingSupplementaryPlanes() {
        assertFalse(ClipboardPastePolicy.canPaste("a\\uFDD0b"));
        assertFalse(ClipboardPastePolicy.canPaste("a\\uFFFEb"));
        assertFalse(ClipboardPastePolicy.canPaste("a\\uFFFFb"));
        assertFalse(ClipboardPastePolicy.canPaste("a\\uD83F\\uDFFEb"));
        assertTrue(ClipboardPastePolicy.canPaste("emoji 😀"));
    }

    @Test public void acceptsBoundedTextAndConfirmsNewlines() {
        assertTrue(ClipboardPastePolicy.canPaste("a".repeat(ClipboardPastePolicy.MAX_CHARS)));
        assertFalse(ClipboardPastePolicy.requiresConfirmation("echo hello"));
        assertTrue(ClipboardPastePolicy.canPaste("echo hello\tworld"));
        assertTrue(ClipboardPastePolicy.requiresConfirmation("echo hello\nrm -i file"));
        assertTrue(ClipboardPastePolicy.requiresConfirmation("echo hello\r"));
    }
}
