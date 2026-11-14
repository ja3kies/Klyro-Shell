package com.oryn.terminal.ui;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class TerminalPasteReviewTest {
    @Test public void sendsOnlyToOriginalVisibleAndActiveSession() {
        TerminalPasteReview review = new TerminalPasteReview(7L, "echo one\necho two");
        AtomicInteger sent = new AtomicInteger();
        assertFalse(review.sendIfCurrent(8L, 8L, true, value -> { sent.incrementAndGet(); return true; }));
        assertFalse(review.sendIfCurrent(7L, 8L, true, value -> { sent.incrementAndGet(); return true; }));
        assertFalse(review.sendIfCurrent(7L, 7L, false, value -> { sent.incrementAndGet(); return true; }));
        assertEquals(0, sent.get());
        assertTrue(review.sendIfCurrent(7L, 7L, true, value -> {
            assertEquals("echo one\necho two", value);
            sent.incrementAndGet();
            return true;
        }));
        assertEquals(1, sent.get());
        assertEquals(7L, review.getTabId());
    }

    @Test public void failedInputQueueIsNotReportedAsSent() {
        TerminalPasteReview review = new TerminalPasteReview(1L, "echo hello\n");
        AtomicInteger attempts = new AtomicInteger();
        assertFalse(review.sendIfCurrent(1L, 1L, true, value -> {
            attempts.incrementAndGet();
            return false;
        }));
        assertEquals(1, attempts.get());
    }

    @Test public void previewIsBoundedAndNeverSplitsEmojiPair() {
        String content = "a".repeat(3071) + "😀" + "z".repeat(16000);
        TerminalPasteReview review = new TerminalPasteReview(1L, content);
        assertTrue(review.isPreviewTruncated());
        assertTrue(review.getPreview().length() <= 3072);
        assertEquals(3071, review.getPreview().length());
        assertFalse(Character.isHighSurrogate(review.getPreview().charAt(review.getPreview().length() - 1)));
        assertEquals(content, review.getText());
    }

    @Test public void countsCrLfAndLoneBreaksWithoutDoubleCounting() {
        TerminalPasteReview review = new TerminalPasteReview(3L, "one\r\ntwo\nthree\rfour");
        assertEquals(4, review.getLineCount());
        assertEquals(review.getText().getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
                review.getUtf8ByteCount());
        assertFalse(review.isPreviewTruncated());
        assertEquals(review.getText(), review.getPreview());
    }

    @Test public void invalidPasteAndInvalidSessionCannotStartReview() {
        try {
            new TerminalPasteReview(0L, "one\ntwo");
            fail("Invalid session accepted");
        } catch (IllegalArgumentException expected) {}
        try {
            new TerminalPasteReview(4L, "bad\u0000line");
            fail("Unsafe text accepted");
        } catch (IllegalArgumentException expected) {}
        try {
            new TerminalPasteReview(4L, "x".repeat(ClipboardPastePolicy.MAX_CHARS + 1));
            fail("Oversized text accepted");
        } catch (IllegalArgumentException expected) {}
    }
}
