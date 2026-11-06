package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class TerminalActionPolicyTest {
    @Test public void emptyOrPromptOnlyUsesPasteAction() {
        assertFalse(TerminalActionPolicy.hasCopyableOutput(null));
        assertFalse(TerminalActionPolicy.hasCopyableOutput(""));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("   "));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("klyro ~ $"));
        assertFalse(TerminalActionPolicy.hasCopyableOutput("klyro ~/imports $   "));
    }

    @Test public void outputOrTypedCommandsUsesSelectionAction() {
        assertTrue(TerminalActionPolicy.hasCopyableOutput("hello"));
        assertTrue(TerminalActionPolicy.hasCopyableOutput("klyro ~ $ ls"));
        assertTrue(TerminalActionPolicy.hasCopyableOutput("klyro ~ $\nfile.txt\nklyro ~ $"));
        assertTrue(TerminalActionPolicy.hasCopyableOutput("bash-5.3$"));
    }
}
