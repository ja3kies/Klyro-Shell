package com.oryn.terminal.diagnostics;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/** Execute the exact guest Bash stress script, replacing only its waits for a fast JVM test. */
public final class StabilityScenarioTest {
    @Test public void scenarioEmitsExactSequenceAndEndMarker() throws Exception {
        final String fastCommand = StabilityScenario.COMMAND
            .replace("sleep 5;", "sleep 0;")
            .replace("sleep 0.5;", "sleep 0;");
        final Process process = new ProcessBuilder("bash", "-c", fastCommand)
            .redirectErrorStream(true).start();
        final byte[] output = process.getInputStream().readAllBytes();
        assertTrue("Bash stress scenario timed out", process.waitFor(15, TimeUnit.SECONDS));
        assertEquals("Bash stress scenario failed", 0, process.exitValue());
        final String[] lines = new String(output, StandardCharsets.UTF_8).stripTrailing().split("\\R");
        assertEquals("START + 1,500 numbered lines + END", 1502, lines.length);
        assertEquals("P0-STRESS 1500 OUTPUT TEST START", lines[0]);
        for (int i = 1; i <= 1500; i++) {
            assertEquals(String.format(Locale.ROOT, "P0-STRESS %04d", i), lines[i]);
        }
        assertEquals("P0-STRESS 1500 OUTPUT TEST END", lines[1501]);
    }
}
