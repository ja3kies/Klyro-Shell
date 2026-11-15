package com.oryn.terminal.ui;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import androidx.annotation.Nullable;

/** Read explicit text only; never coerce URIs, intents or images into shell input. */
public final class ClipboardTextReader {
    private ClipboardTextReader() {}

    /** Availability must mean that the exact same payload can actually be pasted. */
    public static boolean hasPlainText(@Nullable ClipboardManager manager) {
        return readPlainText(manager) != null;
    }

    @Nullable
    public static String readPlainText(@Nullable ClipboardManager manager) {
        if (manager == null) return null;
        // Android can deny clipboard reads when the app loses focus, while
        // clipboard-change callbacks and activity transitions are still firing.
        // A denied read must disable Paste, never crash the terminal UI.
        try {
            return readAccessiblePlainText(manager);
        } catch (SecurityException | IllegalStateException denied) {
            return null;
        }
    }

    @Nullable
    private static String readAccessiblePlainText(ClipboardManager manager) {
        if (!manager.hasPrimaryClip()) return null;
        ClipDescription description = manager.getPrimaryClipDescription();
        if (description == null ||
                !(description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                  description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))) return null;
        ClipData clip = manager.getPrimaryClip();
        // Never paste a silently truncated multi-item clipboard or an attached resource.
        if (clip == null || clip.getItemCount() != 1) return null;
        ClipData.Item item = clip.getItemAt(0);
        if (item == null || item.getIntent() != null || item.getUri() != null) return null;
        CharSequence raw = item.getText();
        if (raw == null) return null;
        String text = raw.toString();
        return ClipboardPastePolicy.canPaste(text) ? text : null;
    }
}
