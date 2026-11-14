package com.oryn.terminal.ui;

import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

/** Immutable, tab-scoped review of a validated clipboard payload. */
public final class TerminalPasteReview {
    private static final int PREVIEW_LIMIT = 3072;
    private final long tabId;
    private final String text;
    private final String preview;
    private final int lineCount;
    private final int utf8ByteCount;

    public TerminalPasteReview(long tabId, String text) {
        if (tabId <= 0 || !ClipboardPastePolicy.canPaste(text)) {
            throw new IllegalArgumentException("Paste must have a valid tab and safe text");
        }
        this.tabId = tabId;
        this.text = text;
        int end = Math.min(PREVIEW_LIMIT, text.length());
        if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        preview = text.substring(0, end);
        utf8ByteCount = text.getBytes(StandardCharsets.UTF_8).length;
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\r') {
                lines++;
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
            } else if (c == '\n') {
                lines++;
            }
        }
        lineCount = lines;
    }

    public long getTabId() { return tabId; }
    public String getText() { return text; }
    public String getPreview() { return preview; }
    public boolean isPreviewTruncated() { return preview.length() < text.length(); }
    public int getLineCount() { return lineCount; }
    public int getUtf8ByteCount() { return utf8ByteCount; }

    /** Refuse a stale confirmation or a full input queue without sending anything. */
    public boolean sendIfCurrent(Long activeId, Long displayedId, boolean running,
                                 Predicate<String> writer) {
        if (!running || writer == null || activeId == null || displayedId == null ||
                activeId.longValue() != tabId || displayedId.longValue() != tabId) return false;
        return TerminalActionPolicy.pasteIfCurrent(tabId, displayedId, running, text, writer);
    }
}
