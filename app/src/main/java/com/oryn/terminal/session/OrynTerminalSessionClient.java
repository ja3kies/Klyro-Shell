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
import com.oryn.terminal.OutputUpdateBatch;

/** Bridges Termux's terminal engine callbacks to Oryn-owned UI and clipboard handling. */
public final class OrynTerminalSessionClient implements TerminalSessionClient {
    public interface Listener {
        void onTitleChanged(String title);
        void onSessionFinished(com.termux.terminal.TerminalSession session, int exitStatus);
        void onShellPid(int pid);
        void onNativePasteRequiresConfirmation(String text);
        default void onOutputActivity() {}
    }

    private final Context context;
    private final Listener listener;
    private volatile TerminalView terminalView;
    private volatile Runnable outputObserver;
    private final AtomicBoolean pendingOutputRefresh = new AtomicBoolean(false);
    // The PTY continues processing output while Android backgrounds the
    // Activity. Only its UI work is paused to avoid an unbounded main queue.
    private final AtomicBoolean uiActive = new AtomicBoolean(true);
    private final AtomicBoolean outputReceivedWhilePaused = new AtomicBoolean(false);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private String latestTitle;
    private boolean titleDirty;
    private final OutputUpdateBatch titleUpdates;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    public OrynTerminalSessionClient(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        titleUpdates = new OutputUpdateBatch(task -> uiHandler.postDelayed(task, 120L),
                task -> uiHandler.removeCallbacks(task), ids -> {
                    if (!disposed.get() && uiActive.get() && listener != null) {
                        titleDirty = false;
                        listener.onTitleChanged(latestTitle);
                    }
                });
    }

    public void attachView(@Nullable TerminalView view) {
        if (!disposed.get()) terminalView = view;
    }

    /** Release only this pane's view, never a newer replacement view. */
    public void detachViewIfCurrent(@NonNull TerminalView view) {
        if (terminalView == view) terminalView = null;
    }

    /**
     * Android pauses rendering, not the shell. Collapse background output
     * into one screen update when the Activity resumes; never replay every
     * output chunk to the UI thread. The emulator retains the actual bytes.
     */
    public void setUiActive(boolean active) {
        if (disposed.get()) return;
        uiActive.set(active);
        if (active) {
            if (titleDirty) titleUpdates.mark(0L);
            // Refresh only when output arrived during pause or a terminal
            // view remains attached. Newly created views attach the current
            // emulator buffer independently through TerminalView.
            boolean deferred = outputReceivedWhilePaused.getAndSet(false);
            if (deferred || terminalView != null) scheduleOutputRefresh();
        }
    }

    private void scheduleOutputRefresh() {
        if (disposed.get() || !uiActive.get()) return;
        if (!pendingOutputRefresh.compareAndSet(false, true)) return;
        uiHandler.post(() -> {
            pendingOutputRefresh.set(false);
            if (disposed.get()) return;
            if (!uiActive.get()) {
                outputReceivedWhilePaused.set(true);
                return;
            }
            if (listener != null) listener.onOutputActivity();
            TerminalView view = terminalView;
            if (view != null && view.isAttachedToWindow()) {
                com.oryn.terminal.diagnostics.FreezeRecorder.INSTANCE.event(
                    com.oryn.terminal.diagnostics.FreezeEvents.Code.VIEW_REFRESH, 0, 0);
                com.termux.terminal.TerminalEmulator emulator = view.getCurrentSession() == null ? null : view.getCurrentSession().getEmulator();
                if (emulator != null) com.oryn.terminal.diagnostics.FreezeRecorder.INSTANCE.event(
                    com.oryn.terminal.diagnostics.FreezeEvents.Code.CURSOR, emulator.getCursorCol(), emulator.getCursorRow());
                if (emulator != null) com.oryn.terminal.diagnostics.FreezeRecorder.INSTANCE.event(
                    com.oryn.terminal.diagnostics.FreezeEvents.Code.SCROLLBACK, emulator.getScreen().getActiveTranscriptRows(), 0);
                view.onScreenUpdated();
            }
            Runnable observer = outputObserver;
            if (observer != null) observer.run();
        });
    }

    /** Drop stale UI work when the owning PTY tab is permanently closed. */
    public void dispose() {
        disposed.set(true);
        titleUpdates.close();
        terminalView = null;
        outputObserver = null;
        uiHandler.removeCallbacksAndMessages(null);
        pendingOutputRefresh.set(false);
        outputReceivedWhilePaused.set(false);
    }

    /** Observer belongs to the visible pane and is removed on disposal. */
    public void setOutputObserver(@Nullable Runnable observer) {
        if (!disposed.get()) outputObserver = observer;
    }

    @Override
    public void onTextChanged(@NonNull com.termux.terminal.TerminalSession session) {
        com.oryn.terminal.diagnostics.FreezeRecorder.INSTANCE.event(
            com.oryn.terminal.diagnostics.FreezeEvents.Code.PTY_CALLBACK, session.getPid(), 0);
        // No callbacks are queued while the Activity is paused. One update
        // reads the latest emulator state upon resume, retaining every byte
        // in the PTY's own buffer instead of replaying obsolete UI work.
        if (disposed.get()) return;
        if (!uiActive.get()) {
            outputReceivedWhilePaused.set(true);
            return;
        }
        scheduleOutputRefresh();
    }

    @Override
    public void onTitleChanged(@NonNull com.termux.terminal.TerminalSession session) {
        if (disposed.get() || listener == null) return;
        latestTitle = session.getTitle();
        titleDirty = true;
        if (uiActive.get()) titleUpdates.mark(0L);
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
        TerminalView source = terminalView;
        if (session == null || disposed.get() || !uiActive.get() || source == null
                || !source.isAttachedToWindow() || source.getCurrentSession() != session || !session.isRunning()) return;
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
    public void onInputRejected(@NonNull com.termux.terminal.TerminalSession session) {
        uiHandler.post(() -> {
            if (!disposed.get()) Toast.makeText(context,
                    "Terminal input buffer full. Input was not sent; wait and retry.",
                    Toast.LENGTH_LONG).show();
        });
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
