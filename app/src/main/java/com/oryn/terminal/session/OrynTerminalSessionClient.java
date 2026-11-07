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
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public OrynTerminalSessionClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void attachView(@Nullable TerminalView view) {
        if (!disposed.get()) terminalView = view;
    }

    /** Drop stale UI work when the owning PTY tab is permanently closed. */
    public void dispose() {
        disposed.set(true);
        terminalView = null;
        outputObserver = null;
        uiHandler.removeCallbacksAndMessages(null);
        pendingOutputRefresh.set(false);
    }

    /** Observer belongs to the visible pane and is removed on disposal. */
    public void setOutputObserver(@Nullable Runnable observer) {
        if (!disposed.get()) outputObserver = observer;
    }

    @Override
    public void onTextChanged(@NonNull com.termux.terminal.TerminalSession session) {
        // A noisy PTY can emit thousands of chunks. One main-thread refresh
        // consumes the latest buffer and always targets the currently attached view.
        if (!disposed.get() && pendingOutputRefresh.compareAndSet(false, true)) {
            uiHandler.post(() -> {
                pendingOutputRefresh.set(false);
                if (disposed.get()) return;
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
            uiHandler.post(() -> {
                if (!disposed.get()) listener.onTitleChanged(title);
            });
        }
    }

    @Override
    public void onSessionFinished(@NonNull com.termux.terminal.TerminalSession session) {
        TerminalView view = terminalView;
        if (!disposed.get() && view != null) view.postInvalidate();
        if (listener != null) {
            int status = session.getExitStatus();
            uiHandler.post(() -> {
                if (!disposed.get()) listener.onSessionFinished(session, status);
            });
        }
    }

    @Override
    public void onCopyTextToClipboard(@NonNull com.termux.terminal.TerminalSession session, String text) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (!disposed.get() && clipboard != null && text != null && !text.isEmpty()) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Klyro Shell", text));
        }
    }

    @Override
    public void onPasteTextFromClipboard(@Nullable com.termux.terminal.TerminalSession session) {
        if (session == null || disposed.get() || !session.isRunning()) return;
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
                uiHandler.post(() -> {
                    if (!disposed.get() && session.isRunning()) {
                        listener.onNativePasteRequiresConfirmation(pasted);
                    }
                });
            }
            return;
        }
        // Respect Bash DECSET 2004: native paste must not bypass bracketed mode.
        if (!disposed.get() && session.isRunning() && session.getEmulator() != null) {
            session.getEmulator().paste(pasted);
        }
    }

    @Override
    public void onBell(@NonNull com.termux.terminal.TerminalSession session) {
        TerminalView view = terminalView;
        if (!disposed.get() && view != null) view.post(() -> {
            if (!disposed.get() && terminalView == view && view.isAttachedToWindow()) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
            }
        });
    }

    @Override
    public void onColorsChanged(@NonNull com.termux.terminal.TerminalSession session) {
        TerminalView view = terminalView;
        if (!disposed.get() && view != null) view.postInvalidate();
    }

    @Override
    public void onTerminalCursorStateChange(boolean state) {
        TerminalView view = terminalView;
        if (!disposed.get() && view != null) view.postInvalidate();
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
