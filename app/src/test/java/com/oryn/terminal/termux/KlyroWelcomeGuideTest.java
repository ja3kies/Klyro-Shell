package com.oryn.terminal.termux;

import org.junit.Test;
import static org.junit.Assert.*;

public class KlyroWelcomeGuideTest {
    @Test public void guideIsKlyroSpecificAndAccurateAboutStorage() {
        assertTrue(KlyroWelcomeGuide.MESSAGE.startsWith("Welcome to Klyro Shell!"));
        assertTrue(KlyroWelcomeGuide.MESSAGE.contains("pkg install"));
        assertTrue(KlyroWelcomeGuide.MESSAGE.contains("Settings > Files & storage"));
        assertTrue(KlyroWelcomeGuide.MESSAGE.contains("termux.dev/docs"));
        assertFalse(KlyroWelcomeGuide.MESSAGE.contains("Donate:"));
    }

    @Test public void generatedProfileAugmentationIsIdempotent() {
        String original = "# Klyro Shell profile\nexport PS1=x\n";
        String augmented = KlyroWelcomeGuide.augmentGeneratedProfile(original);
        assertTrue(augmented.contains(KlyroWelcomeGuide.MARKER));
        assertEquals(augmented, KlyroWelcomeGuide.augmentGeneratedProfile(augmented));
        assertEquals("# My custom rc\n", KlyroWelcomeGuide.augmentGeneratedProfile("# My custom rc\n"));
    }

    @Test public void snippetIsGuardedAndDoesNotInjectCommands() {
        String snippet = KlyroWelcomeGuide.shellRcSnippet();
        assertTrue(snippet.contains("case \"$-\" in *i*)"));
        assertTrue(snippet.contains("set -C"));
        assertTrue(snippet.contains(".klyro-welcome-shown"));
        assertTrue(snippet.contains("printf '%s\\n' ' - Install:"));
        assertFalse(snippet.contains("\ntermux-setup-storage\n"));
    }
}
