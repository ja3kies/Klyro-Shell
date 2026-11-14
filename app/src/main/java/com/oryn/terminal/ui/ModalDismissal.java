package com.oryn.terminal.ui;

/** Main-thread lifetime gate: a completed or disposed sheet cannot dispatch a stale close. */
public final class ModalDismissal {
    private boolean consumed;
    public boolean canDismiss(boolean enabled) { return enabled && !consumed; }
    public void dismiss(boolean enabled, boolean hidden, Runnable closeOwner) {
        // A committed Hidden transition must remove its owner even if protection changed mid-animation.
        if (consumed || (!enabled && !hidden)) return;
        consumed = true;
        closeOwner.run();
    }
    public void dispose() { consumed = true; }
}
