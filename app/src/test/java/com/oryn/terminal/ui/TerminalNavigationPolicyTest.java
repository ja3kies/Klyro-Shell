package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class TerminalNavigationPolicyTest {
    @Test public void unmodifiedNavigation() {
        assertEquals("\u001B[A", TerminalNavigationPolicy.arrow('A', false, false, false));
        assertEquals("\u001B[B", TerminalNavigationPolicy.arrow('B', false, false, false));
        assertEquals("\u001B[C", TerminalNavigationPolicy.arrow('C', false, false, false));
        assertEquals("\u001B[D", TerminalNavigationPolicy.arrow('D', false, false, false));
        assertEquals("\u001B[F", TerminalNavigationPolicy.end(false, false, false));
    }
    @Test public void modifiedNavigation() {
        assertEquals("\u001B[1;5D", TerminalNavigationPolicy.arrow('D', true, false, false));
        assertEquals("\u001B[1;3C", TerminalNavigationPolicy.arrow('C', false, true, false));
        assertEquals("\u001B[1;2A", TerminalNavigationPolicy.arrow('A', false, false, true));
        assertEquals("\u001B[1;8F", TerminalNavigationPolicy.end(true, true, true));
    }
    @Test public void longPressUsesReadlineOnMainShellWithoutModifiers() {
        assertEquals("\u0001", TerminalNavigationPolicy.horizontalLongPress(false, false, false, false, false));
        assertEquals("\u0005", TerminalNavigationPolicy.horizontalLongPress(true, false, false, false, false));
    }
    @Test public void longPressUsesHomeEndForFullScreenApps() {
        assertEquals("\u001B[H", TerminalNavigationPolicy.horizontalLongPress(false, true, false, false, false));
        assertEquals("\u001B[F", TerminalNavigationPolicy.horizontalLongPress(true, true, false, false, false));
    }
    @Test public void longPressPreservesActiveModifiers() {
        assertEquals("\u001B[1;5H", TerminalNavigationPolicy.horizontalLongPress(false, false, true, false, false));
        assertEquals("\u001B[1;4F", TerminalNavigationPolicy.horizontalLongPress(true, true, false, true, true));
        assertEquals("\u001B[1;2H", TerminalNavigationPolicy.horizontalLongPress(false, false, false, false, true));
    }
    @Test public void rejectsInvalidArrow() {
        try {
            TerminalNavigationPolicy.arrow('X', false, false, false);
            fail("Expected rejection");
        } catch (IllegalArgumentException expected) {}
    }
}
