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
