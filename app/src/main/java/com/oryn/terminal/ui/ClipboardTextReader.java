package com.oryn.terminal.ui;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import androidx.annotation.Nullable;

/** Read explicit text only; never coerce URIs, intents or images into shell input. */
public final class ClipboardTextReader {
    private ClipboardTextReader() {}

    public static boolean hasPlainText(@Nullable ClipboardManager manager) {
        if (manager == null || !manager.hasPrimaryClip()) return false;
        ClipDescription description = manager.getPrimaryClipDescription();
        return description != null &&
                (description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                 description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML));
    }

    @Nullable
    public static String readPlainText(@Nullable ClipboardManager manager) {
        if (!hasPlainText(manager)) return null;
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() != 1) return null;
        ClipData.Item item = clip.getItemAt(0);
        if (item == null || item.getIntent() != null || item.getUri() != null) return null;
        CharSequence raw = item.getText();
        if (raw == null) return null;
        String text = raw.toString();
        return ClipboardPastePolicy.canPaste(text) ? text : null;
    }
}
