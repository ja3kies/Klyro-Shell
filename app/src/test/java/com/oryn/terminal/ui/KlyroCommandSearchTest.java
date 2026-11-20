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
    @Test public void astralCharactersUseTheSameLimitAsTheField() {
        String label = "😀".repeat(96);
        assertTrue(KlyroCommandSearch.matches(label, label, ""));
        assertFalse(KlyroCommandSearch.matches(label + "a", label + "a", ""));
    }
    @Test public void pastedNonBreakingSpacesSeparateWords() {
        assertTrue(KlyroCommandSearch.matches("status\u00a0git", "git status", ""));
    }
    @Test public void equivalentAccentedNamesFindPluginCommands() {
        assertTrue(KlyroCommandSearch.matches("re\u0301sume\u0301", "Résumé", ""));
    }
    @Test public void actualKlyroCommandFindsItsReferenceAlias() {
        assertTrue(KlyroCommandCatalog.search("klyro-setup-storage").stream()
            .anyMatch(entry -> entry.command.equals("termux-setup-storage")));
    }
    @Test public void searchesActionDescriptions() {
        assertTrue(KlyroCommandSearch.matches("scrollback", "Find", "Search terminal scrollback"));
        assertFalse(KlyroCommandSearch.matches("export", "Find", "Search terminal scrollback"));
    }
}
