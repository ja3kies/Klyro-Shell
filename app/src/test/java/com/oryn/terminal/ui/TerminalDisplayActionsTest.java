package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** CLEAR must not desynchronize Readline input or replay untrusted output. */
public class TerminalDisplayActionsTest {
    @Test public void missingTerminalCannotBeCleared() {
        assertFalse(TerminalDisplayActions.clear(null));
    }

    @Test public void redrawsOnlyIdleKlyroPrompt() {
        assertEquals("klyro ~ $ ", TerminalDisplayActions.promptToRestore("klyro ~ $"));
        assertEquals("klyro ~/a $ ", TerminalDisplayActions.promptToRestore("klyro ~/a $ "));
    }

    @Test public void keepsPromptDetectionIndependentOfAnsiColor() {
        assertEquals("klyro ~ $ ", TerminalDisplayActions.promptToRestore("klyro ~ $ "));
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~ $ echo pending"));
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~ $\\u001b[36m"));
    }

    @Test public void refusesActiveInputAndArbitraryOutput() {
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~/a $ pwd"));
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~ $ rm -rf /"));
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~ $secret"));
        assertEquals("", TerminalDisplayActions.promptToRestore("Password: secret"));
        assertEquals("", TerminalDisplayActions.promptToRestore("some process output"));
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~ $\nunsafe"));
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~ $\u001b[31m"));
    }
}
