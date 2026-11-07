package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TerminalDisplayActionsTest {
    @Test public void restoresOnlyKnownPromptLines() {
        assertEquals("klyro ~ $", TerminalDisplayActions.promptToRestore("klyro ~ $"));
        assertEquals("klyro ~/a $ pwd", TerminalDisplayActions.promptToRestore("klyro ~/a $ pwd"));
        assertEquals("", TerminalDisplayActions.promptToRestore("Password: secret"));
        assertEquals("", TerminalDisplayActions.promptToRestore("some process output"));
        assertEquals("", TerminalDisplayActions.promptToRestore("klyro ~ $\nunsafe"));
    }
}
