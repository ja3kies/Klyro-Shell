package com.oryn.terminal.ui;

/** Standard xterm modifier CSI sequences for the extra-key arrows and End. */
public final class TerminalNavigationPolicy {
    private TerminalNavigationPolicy() {}

    public static String arrow(char direction, boolean ctrl, boolean alt, boolean shift) {
        if ("ABCD".indexOf(direction) < 0) {
            throw new IllegalArgumentException("Unsupported direction");
        }
        return csi(direction, ctrl, alt, shift);
    }

    /** Long-press horizontal arrows: Readline on the main shell, Home/End in full-screen apps.
     * Modified long-presses preserve xterm modifiers instead of injecting Ctrl+A/Ctrl+E.
     */
    public static String horizontalLongPress(boolean end, boolean alternateScreen,
            boolean ctrl, boolean alt, boolean shift) {
        if (alternateScreen || ctrl || alt || shift) return csi(end ? 'F' : 'H', ctrl, alt, shift);
        return end ? "\u0005" : "\u0001";
    }

    public static String end(boolean ctrl, boolean alt, boolean shift) {
        return csi('F', ctrl, alt, shift);
    }

    private static String csi(char key, boolean ctrl, boolean alt, boolean shift) {
        int modifier = 1 + (shift ? 1 : 0) + (alt ? 2 : 0) + (ctrl ? 4 : 0);
        if (modifier == 1) return "\u001B[" + key;
        return "\u001B[1;" + modifier + key;
    }
}
