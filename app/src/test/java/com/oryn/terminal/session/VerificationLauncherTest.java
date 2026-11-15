package com.oryn.terminal.session;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class VerificationLauncherTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test(timeout = 5000) public void wrapperPreservesThePidAcrossExecAndQuotesTheFilePath() throws Exception {
        File pid = new File(temporary.getRoot(), "pid é ' $(not-a-command).txt");
        String[] launcher = TerminalSessionManager.buildVerificationLauncher(
                new String[]{"sh", "-c", "printf '%s\\n' \"$$\""}, pid);
        launcher[0] = "sh"; // Run the production POSIX wrapper on the JVM build host.
        Process process = new ProcessBuilder(launcher).start();
        try {
            assertTrue(process.waitFor(3, TimeUnit.SECONDS));
            assertEquals(0, process.exitValue());
            String actual = new BufferedReader(new InputStreamReader(process.getInputStream())).readLine();
            String recorded = new String(Files.readAllBytes(pid.toPath()), StandardCharsets.UTF_8).trim();
            assertTrue(recorded.matches("[1-9][0-9]*"));
            assertEquals(actual, recorded);
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    @Test(timeout = 5000) public void pidWriteFailureDoesNotLaunchTheProbe() throws Exception {
        File untouched = new File(temporary.getRoot(), "must-not-exist");
        String[] launcher = TerminalSessionManager.buildVerificationLauncher(
                new String[]{"touch", untouched.getAbsolutePath()}, temporary.getRoot());
        launcher[0] = "sh";
        Process process = new ProcessBuilder(launcher).start();
        try {
            assertTrue(process.waitFor(3, TimeUnit.SECONDS));
            assertEquals(125, process.exitValue());
            assertFalse(untouched.exists());
        } finally { if (process.isAlive()) process.destroyForcibly(); }
    }
}
