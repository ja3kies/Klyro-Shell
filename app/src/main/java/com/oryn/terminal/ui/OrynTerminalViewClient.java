package com.oryn.terminal.ui;

import android.content.Context;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.termux.terminal.TerminalSession;
import com.termux.view.TerminalViewClient;

/** Minimal input/gesture bridge for Termux's terminal view, with Oryn-owned modifier state. */
public final class OrynTerminalViewClient implements TerminalViewClient {
    private final Context context;
    private volatile View terminalView;
    private volatile boolean controlDown;
    private volatile boolean altDown;
    private volatile boolean shiftDown;
    private volatile boolean fnDown;

    public OrynTerminalViewClient(Context context) {
        this.context = context.getApplicationContext();
    }

    public void attachView(View view) { terminalView = view; }

    public void setControlDown(boolean value) { controlDown = value; }
    public void setAltDown(boolean value) { altDown = value; }
    public void setShiftDown(boolean value) { shiftDown = value; }
    public void setFnDown(boolean value) { fnDown = value; }

    public boolean isControlDown() { return controlDown; }
    public boolean isAltDown() { return altDown; }

    @Override public float onScale(float scale) { return 1.0f; }

    @Override
    public void onSingleTapUp(MotionEvent event) {
        InputMethodManager manager = (InputMethodManager)
                context.getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = terminalView;
        if (view != null) {
            view.requestFocus();
            if (manager != null) {
                manager.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    @Override public boolean shouldBackButtonBeMappedToEscape() { return false; }
    @Override public boolean shouldEnforceCharBasedInput() { return false; }
    @Override public boolean shouldUseCtrlSpaceWorkaround() { return false; }
    @Override public boolean isTerminalViewSelected() { return true; }
    @Override public void copyModeChanged(boolean copyMode) { }
    @Override public boolean onKeyDown(int keyCode, KeyEvent event, TerminalSession session) { return false; }
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) { return false; }
    @Override public boolean onLongPress(MotionEvent event) { return false; }
    @Override public boolean readControlKey() { return controlDown; }
    @Override public boolean readAltKey() { return altDown; }
    @Override public boolean readShiftKey() { return shiftDown; }
    @Override public boolean readFnKey() { return fnDown; }

    @Override
    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
        return false;
    }

    @Override public void onEmulatorSet() { }
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
        Log.e(tag, "Terminal view error", error);
    }
}
