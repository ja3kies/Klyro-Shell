package com.oryn.terminal.session;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.oryn.terminal.ui.ClipboardPastePolicy;
import android.view.HapticFeedbackConstants;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bridges Termux's terminal engine callbacks to Oryn-owned UI and clipboard handling. */
public final class OrynTerminalSessionClient implements TerminalSessionClient {
    public interface Listener {
        void onTitleChanged(String title);
        void onSessionFinished(com.termux.terminal.TerminalSession session, int exitStatus);
        void onShellPid(int pid);
        void onNativePasteRequiresConfirmation(String text);
    }

    private final Context context;
    private final Listener listener;
    private volatile TerminalView terminalView;
    private volatile Runnable outputObserver;
    private final AtomicBoolean pendingOutputRefresh = new AtomicBoolean(false);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public OrynTerminalSessionClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void attachView(@Nullable TerminalView view) {
        terminalView = view;
    }

    /** Observer belongs to the visible pane and is removed on disposal. */
    public void setOutputObserver(@Nullable Runnable observer) {
        outputObserver = observer;
    }

    @Override
    public void onTextChanged(@NonNull com.termux.terminal.TerminalSession session) {
        // A noisy PTY can emit thousands of chunks. One main-thread refresh
        // consumes the latest buffer and always targets the currently attached view.
        if (pendingOutputRefresh.compareAndSet(false, true)) {
            uiHandler.post(() -> {
                pendingOutputRefresh.set(false);
                TerminalView view = terminalView;
                if (view != null) view.onScreenUpdated();
                Runnable observer = outputObserver;
                if (observer != null) observer.run();
            });
        }
    }

    @Override
    public void onTitleChanged(@NonNull com.termux.terminal.TerminalSession session) {
        if (listener != null) {
            String title = session.getTitle();
            uiHandler.post(() -> listener.onTitleChanged(title));
        }
    }

    @Override
    public void onSessionFinished(@NonNull com.termux.terminal.TerminalSession session) {
        TerminalView view = terminalView;
        if (view != null) view.postInvalidate();
        if (listener != null) {
            int status = session.getExitStatus();
            uiHandler.post(() -> listener.onSessionFinished(session, status));
        }
    }

    @Override
    public void onCopyTextToClipboard(@NonNull com.termux.terminal.TerminalSession session, String text) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Klyro Shell", text));
        }
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable com.termux.terminal.TerminalSession session) {
        if (session == null) return;
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        String pasted = com.oryn.terminal.ui.ClipboardTextReader.readPlainText(clipboard);
        if (pasted == null) return;
        if (!ClipboardPastePolicy.canPaste(pasted)) {
            uiHandler.post(() ->
                Toast.makeText(context, "Clipboard text is unsupported or too long.",
                    Toast.LENGTH_SHORT).show());
            return;
        }
        if (ClipboardPastePolicy.requiresConfirmation(pasted)) {
            if (listener != null) {
                uiHandler.post(() ->
                    listener.onNativePasteRequiresConfirmation(pasted));
            }
            return;
        }
        byte[] bytes = pasted.getBytes(StandardCharsets.UTF_8);
        session.write(bytes, 0, bytes.length);
    }

    @Override
    public void onBell(@NonNull com.termux.terminal.TerminalSession session) {
        TerminalView view = terminalView;
        if (view != null) view.post(() ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP));
    }

    @Override
    public void onColorsChanged(@NonNull com.termux.terminal.TerminalSession session) {
        TerminalView view = terminalView;
        if (view != null) view.postInvalidate();
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
        TerminalView view = terminalView;
        if (view != null) view.postInvalidate();
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
