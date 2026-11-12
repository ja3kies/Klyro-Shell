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

    @Test public void acceptsAVisiblePartiallyTypedKlyroCommandWithoutChangingShellInput() {
        assertEquals("klyro ~ $ c",
                TerminalDisplayActions.bashLineToPreserve("klyro ~ $ c"));
        assertEquals("klyro ~/project $ git status",
                TerminalDisplayActions.bashLineToPreserve("klyro ~/project $ git status"));
        assertEquals("klyro ~ $ echo $HOME",
                TerminalDisplayActions.bashLineToPreserve("klyro ~ $ echo $HOME"));
    }

    @Test public void rejectsUnsafeOrUnfamiliarScreenLines() {
        assertEquals("", TerminalDisplayActions.bashLineToPreserve(null));
        assertEquals("", TerminalDisplayActions.bashLineToPreserve("Password:"));
        assertEquals("", TerminalDisplayActions.bashLineToPreserve("vim buffer"));
        assertEquals("", TerminalDisplayActions.bashLineToPreserve("klyro ~ $secret"));
        assertEquals("", TerminalDisplayActions.bashLineToPreserve("klyro ~ $ echo\nnext"));
        assertEquals("", TerminalDisplayActions.bashLineToPreserve("klyro ~ $ \u001B[31m"));
        assertEquals("", TerminalDisplayActions.bashLineToPreserve(
                "klyro ~ $ " + "x".repeat(513)));
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
