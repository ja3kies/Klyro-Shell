package com.oryn.terminal.session;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.HapticFeedbackConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;

import java.nio.charset.StandardCharsets;

/** Bridges Termux's terminal engine callbacks to Oryn-owned UI and clipboard handling. */
public final class OrynTerminalSessionClient implements TerminalSessionClient {
    public interface Listener {
        void onTitleChanged(String title);
        void onSessionFinished(int exitStatus);
        void onShellPid(int pid);
    }

    private final Context context;
    private final Listener listener;
    private volatile TerminalView terminalView;

    public OrynTerminalSessionClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void attachView(@Nullable TerminalView view) {
        terminalView = view;
    }

    @Override
    public void onTextChanged(@NonNull TerminalSession session) {
        TerminalView view = terminalView;
        if (view != null) view.post(view::onScreenUpdated);
    }

    @Override
    public void onTitleChanged(@NonNull TerminalSession session) {
        if (listener != null) listener.onTitleChanged(session.getTitle());
    }

    @Override
    public void onSessionFinished(@NonNull TerminalSession session) {
        TerminalView view = terminalView;
        if (view != null) view.postInvalidate();
        if (listener != null) listener.onSessionFinished(session.getExitStatus());
    }

    @Override
    public void onCopyTextToClipboard(@NonNull TerminalSession session, String text) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Oryn Terminal", text));
        }
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable TerminalSession session) {
        if (session == null) return;
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) return;
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence text = clip.getItemAt(0).coerceToText(context);
        if (text == null) return;
        byte[] bytes = text.toString().getBytes(StandardCharsets.UTF_8);
        session.write(bytes, 0, bytes.length);
    }

    @Override
    public void onBell(@NonNull TerminalSession session) {
        TerminalView view = terminalView;
        if (view != null) view.post(() ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP));
    }

    @Override
    public void onColorsChanged(@NonNull TerminalSession session) {
        TerminalView view = terminalView;
        if (view != null) view.postInvalidate();
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
        TerminalView view = terminalView;
        if (view != null) view.postInvalidate();
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession session, int pid) {
        if (listener != null) listener.onShellPid(pid);
    }

    @Override
    public Integer getTerminalCursorStyle() {
        return TerminalEmulator.TERMINAL_CURSOR_STYLE_BAR;
    }

    @Override public void logError(String tag, String message) { Log.e(tag, message); }
    @Override public void logWarn(String tag, String message) { Log.w(tag, message); }
    @Override public void logInfo(String tag, String message) { Log.i(tag, message); }
    @Override public void logDebug(String tag, String message) { Log.d(tag, message); }
    @Override public void logVerbose(String tag, String message) { Log.v(tag, message); }

    @Override
    public void logStackTraceWithMessage(String tag, String message, Exception error) {
        Log.e(tag, message, error);
    }

    @Override
    public void logStackTrace(String tag, Exception error) {
        Log.e(tag, "Terminal engine error", error);
    }
}
