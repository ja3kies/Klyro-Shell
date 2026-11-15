package com.oryn.terminal.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class KlyroCommandSearchTest {
    @Test public void tokenOrderIndependent() {
        assertTrue(KlyroCommandSearch.matches("terminal new", "New terminal", "Create Bash session"));
        assertTrue(KlyroCommandSearch.matches("BASH  new", "New terminal", "Create Bash session"));
        assertFalse(KlyroCommandSearch.matches("shell delete", "New terminal", "Create Bash session"));
    }
    @Test public void emptyNullAndOversizedQueries() {
        assertTrue(KlyroCommandSearch.matches("", "", ""));
        assertTrue(KlyroCommandSearch.matches(null, null, null));
        assertFalse(KlyroCommandSearch.matches("x".repeat(97), "x".repeat(200), ""));
    }
    @Test public void searchesActionDescriptions() {
        assertTrue(KlyroCommandSearch.matches("scrollback", "Find", "Search terminal scrollback"));
        assertFalse(KlyroCommandSearch.matches("export", "Find", "Search terminal scrollback"));
    }
}
