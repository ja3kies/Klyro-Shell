package com.oryn.terminal.session;

import com.oryn.terminal.core.OrynPaths;

import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TerminalSessionManagerTest {
    @Test
    public void hostEnvironmentUsesOnlyOrynWritableDirectories() throws Exception {
        File appFiles = Files.createTempDirectory("oryn-session-test").toFile();
        OrynPaths paths = new OrynPaths(appFiles);
        paths.prepareLayout();

        Map<String, String> environment =
                TerminalSessionManager.buildHostEnvironment(paths);

        assertEquals(paths.getHome().getAbsolutePath(), environment.get("HOME"));
        assertEquals(paths.getTmp().getAbsolutePath(), environment.get("TMPDIR"));
        assertEquals(paths.getProotLibDir().getAbsolutePath(),
                environment.get("LD_LIBRARY_PATH"));
        assertEquals("/system/bin:/system/xbin:/vendor/bin", environment.get("PATH"));
        assertEquals("/system", environment.get("ANDROID_ROOT"));
        assertEquals("/data", environment.get("ANDROID_DATA"));
        assertFalse(environment.get("HOME").contains("/data/data/com.termux"));
        assertFalse(environment.get("TMPDIR").contains("/data/data/com.termux"));
        assertTrue(paths.getHome().getCanonicalPath().startsWith(
                appFiles.getCanonicalPath() + File.separator));
    }
}
