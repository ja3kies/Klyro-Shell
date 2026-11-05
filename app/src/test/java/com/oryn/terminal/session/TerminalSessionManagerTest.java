package com.oryn.terminal.session;

import com.oryn.terminal.core.OrynPaths;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TerminalSessionManagerTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void environmentReadyRequiresAllVerificationLayers() {
        TerminalSessionManager.VerificationResult partial =
                new TerminalSessionManager.VerificationResult(
                        true, true, true, false, 23, "pkg failed", false);
        assertTrue(partial.bashStarted);
        assertTrue(partial.uidValid);
        assertTrue(partial.aptResponds);
        assertTrue(!partial.pkgResponds);
        assertTrue(!partial.isEnvironmentReady());

        TerminalSessionManager.VerificationResult ready =
                new TerminalSessionManager.VerificationResult(
                        true, true, true, true, 0, "", false);
        assertTrue(ready.isEnvironmentReady());
    }

    @Test
    public void verificationMarkersRequireCompleteOutputLines() {
        assertTrue(TerminalSessionManager.hasExactLine(
                "noise\\nKLYRO_UID_OK\\nKLYRO_PKG_OK", "KLYRO_UID_OK"));
        assertTrue(TerminalSessionManager.hasExactLine(
                "KLYRO_APT_OK\\r\\n", "KLYRO_APT_OK"));
        assertTrue(!TerminalSessionManager.hasExactLine(
                "KLYRO_UID_OK_BUT_NOT_VERIFIED", "KLYRO_UID_OK"));
        assertTrue(!TerminalSessionManager.hasExactLine(
                "prefix KLYRO_PKG_OK suffix", "KLYRO_PKG_OK"));
        assertTrue(!TerminalSessionManager.hasExactLine(null, "KLYRO_UID_OK"));
        assertTrue(!TerminalSessionManager.hasExactLine("", "KLYRO_UID_OK"));
    }

    @Test
    public void terminalArgvRejectsMissingExecutableAndCopiesInput() {
        for (String[] bad : new String[][] {null, {}, {null}, {""}, {"  "}}) {
            try {
                TerminalSessionManager.buildTerminalArgv(bad);
                org.junit.Assert.fail("Expected invalid executable rejection");
            } catch (IllegalArgumentException expected) {
                // An invalid command must never reach JNI execvp.
            }
        }
        String[] input = {"/private/libproot.so", "-r", "/private/root"};
        String[] copy = TerminalSessionManager.buildTerminalArgv(input);
        copy[0] = "/system/bin/sh";
        assertEquals("/private/libproot.so", input[0]);
    }

    @Test
    public void terminalArgvRetainsExecutableAsArgvZero() {
        String[] command = {"/app/lib/libproot.so", "--link2symlink", "-r", "/private/root"};
        assertArrayEquals(command, TerminalSessionManager.buildTerminalArgv(command));
    }

    @Test
    public void runtimeExecutablesResolveFromNativeLibraryDirectory() throws Exception {
        File filesDir = temporaryFolder.newFolder("files");
        File nativeLibraryDir = temporaryFolder.newFolder("native");

        OrynPaths paths = new OrynPaths(filesDir, nativeLibraryDir);

        assertEquals(new File(nativeLibraryDir, "libproot.so"), paths.getProot());
        assertEquals(new File(nativeLibraryDir, "libproot-loader.so"), paths.getProotLoader());
        assertEquals(nativeLibraryDir, paths.getProotLibDir());
    }

    @Test
    public void hostEnvironmentUsesPrivateSonameAndNativeLibraryDirectories() throws Exception {
        File filesDir = temporaryFolder.newFolder("files");
        File nativeLibraryDir = temporaryFolder.newFolder("native");
        OrynPaths paths = new OrynPaths(filesDir, nativeLibraryDir);

        Map<String, String> environment = TerminalSessionManager.buildHostEnvironment(paths);

        assertEquals(paths.getProotLoader().getAbsolutePath(), environment.get("PROOT_LOADER"));
        assertEquals(paths.getTmp().getAbsolutePath(), environment.get("PROOT_TMP_DIR"));
        assertEquals(paths.getProotDependencyDir().getAbsolutePath() + File.pathSeparator
                + nativeLibraryDir.getAbsolutePath(), environment.get("LD_LIBRARY_PATH"));
        assertTrue(environment.get("PATH").contains("/system/bin"));
    }
}
