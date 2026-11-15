package com.oryn.terminal.termux;

import org.junit.Test;
import static org.junit.Assert.*;

/** Explicitly verify generated-only migration of Klyro's old neon prompt. */
public class ShellProfileThemeTest {
    private static final String THEMED =
            "export PS1='\\[\\e[36m\\]klyro\\[\\e[0m\\] \\w $ '";

    @Test public void migratesOnlyGeneratedKlyroAndOrynPrompts() {
        String klyro =
                "export PS1='\\[\\e[38;2;0;240;255m\\]klyro\\[\\e[0m\\] \\w $ '";
        String oryn =
                "export PS1='\\[\\e[38;2;0;240;255m\\]oryn\\[\\e[0m\\] \\w $ '";
        assertEquals(THEMED, TermuxBootstrapInstaller.migrateGeneratedPrompt(klyro));
        assertEquals(THEMED, TermuxBootstrapInstaller.migrateGeneratedPrompt(oryn));
        assertEquals(THEMED, TermuxBootstrapInstaller.migrateGeneratedPrompt(THEMED));
    }

    @Test public void preservesUserDefinedPromptsAndUnrelatedLines() {
        String custom = "export PS1='custom > '\nexport EDITOR=vim\n";
        assertEquals(custom, TermuxBootstrapInstaller.migrateGeneratedPrompt(custom));
        String old = "export PS1='\\[\\e[38;2;0;240;255m\\]klyro\\[\\e[0m\\] \\w $ '";
        String profile = "# user setup\n" + old + "\nalias deploy='echo ok'\n";
        assertEquals("# user setup\n" + THEMED + "\nalias deploy='echo ok'\n",
                TermuxBootstrapInstaller.migrateGeneratedPrompt(profile));
    }
}
