package com.oryn.terminal.termux;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.Assert.*;

public class KlyroTermuxInfoTest {
    @Test public void helperOnlyReadsPrivateShellState() {
        String shell = KlyroTermuxInfo.helperScript();
        assertTrue(shell.contains("# KLYRO_INFO_BRIDGE_V1"));
        assertTrue(shell.contains("command -v pkg"));
        assertTrue(shell.contains("Settings > Files & storage"));
        assertFalse(shell.contains("sudo "));
        assertFalse(shell.contains("termux-api "));
        assertFalse(shell.contains("rm -rf"));
        assertFalse(shell.contains("/data/data/com.termux/files/home"));
    }

    @Test public void generatedProfileIsIdempotentAndCustomRcsAreUntouched() {
        String generated = "# Klyro Shell profile\n";
        String result = KlyroTermuxInfo.augmentGeneratedProfile(generated);
        assertTrue(result.contains("termux-info()"));
        assertEquals(result, KlyroTermuxInfo.augmentGeneratedProfile(result));
        assertEquals("my custom file", KlyroTermuxInfo.augmentGeneratedProfile("my custom file"));
    }

    @Test public void helperInstallAndBashGrammar() throws Exception {
        Path prefix = Files.createTempDirectory("klyro-bin-test");
        Path bin = Files.createDirectory(prefix.resolve("bin"));
        try {
            KlyroTermuxInfo.installHelper(prefix.toFile());
            Path info = bin.resolve("klyro-info");
            assertTrue(Files.isExecutable(info));
            assertTrue(new String(Files.readAllBytes(info), StandardCharsets.UTF_8).contains("KLYRO_INFO_BRIDGE_V1"));
            Path storage = bin.resolve("klyro-setup-storage");
            KlyroStorageCommandBridge.installHelper(prefix.toFile());
            Process infoSyntax = new ProcessBuilder("bash", "-n", info.toString()).start();
            assertEquals(0, infoSyntax.waitFor());
            Process storageSyntax = new ProcessBuilder("bash", "-n", storage.toString()).start();
            assertEquals(0, storageSyntax.waitFor());
        } finally {
            Files.deleteIfExists(bin.resolve("klyro-info"));
            Files.deleteIfExists(bin.resolve("klyro-setup-storage"));
            Files.deleteIfExists(bin);
            Files.deleteIfExists(prefix);
        }
    }
}
