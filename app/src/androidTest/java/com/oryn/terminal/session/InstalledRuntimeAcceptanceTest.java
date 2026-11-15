package com.oryn.terminal.session;

import android.content.Context;
import android.os.Process;
import androidx.test.platform.app.InstrumentationRegistry;
import com.oryn.terminal.core.OrynPaths;
import com.oryn.terminal.termux.TermuxBootstrapInstaller;
import com.termux.terminal.TerminalSession;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Opt-in on a dedicated device: real APK-owned PRoot/Bash/PTY, no package mutation.
 * Does not read user startup profiles or send input to an existing session. */
public class InstalledRuntimeAcceptanceTest {
    private void runIsolated(String body) throws Exception {
        assumeTrue("Requires explicit runtime acceptance opt-in on a dedicated device",
                "true".equals(InstrumentationRegistry.getArguments().getString("klyroRuntimeAcceptance")));
        Context app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.klyro.shell", app.getPackageName());
        assertTrue("Klyro must have a non-system Android UID", Process.myUid() >= 10000);
        TerminalSessionManager manager = new TerminalSessionManager(app);
        assertTrue("Complete first-install setup before runtime acceptance", manager.isRuntimeInstalled());
        OrynPaths paths = manager.getPaths();
        ArrayList<String> command = new ArrayList<>(Arrays.asList(
                new TermuxBootstrapInstaller(paths).buildLoginCommand()));
        assertEquals("--login", command.remove(command.size() - 1));
        command.addAll(Arrays.asList("--noprofile", "--norc", "-c", "set -e; " + body + "; printf '\\nKLYRO_ACCEPTANCE_DONE\\n'"));
        Map<String,String> host = TerminalSessionManager.buildHostEnvironment(paths);
        String[] environment = host.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toArray(String[]::new);
        CountDownLatch completed = new CountDownLatch(1);
        AtomicInteger exit = new AtomicInteger(Integer.MIN_VALUE);
        OrynTerminalSessionClient client = new OrynTerminalSessionClient(app,
                new OrynTerminalSessionClient.Listener() {
                    @Override public void onTitleChanged(String title) { }
                    @Override public void onShellPid(int pid) { }
                    @Override public void onNativePasteRequiresConfirmation(String text) { }
                    @Override public void onSessionFinished(TerminalSession session, int code) {
                        exit.set(code); completed.countDown();
                    }
                });
        TerminalSession[] owned = new TerminalSession[1];
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                owned[0] = new TerminalSession(command.get(0), paths.getRoot().getAbsolutePath(),
                        command.toArray(new String[0]), environment, 2000, client);
                owned[0].updateSize(100, 30);
            });
            assertTrue("Isolated runtime did not complete in 90 seconds", completed.await(90, TimeUnit.SECONDS));
            assertEquals("Runtime command failed; no terminal content collected", 0, exit.get());
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                String output = owned[0].getEmulator().getScreen().getTranscriptText();
                assertTrue("Expected bounded test output", output.length() < 64 * 1024);
                assertTrue("Final PTY marker missing", output.contains("KLYRO_ACCEPTANCE_DONE"));
            });
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                // This handle belongs exclusively to this test, never to the app's session model.
                if (owned[0] != null) owned[0].finishIfRunning();
                client.dispose();
            });
        }
    }

    @Test public void apkUidBashPtyFilesystemAndPackageQueries() throws Exception {
        runIsolated("test \"$(id -u)\" = " + Process.myUid() + "; " +
                "test -t 0; test -t 1; test -t 2; " +
                "test \"$HOME\" = /data/data/com.termux/files/home; " +
                "test \"$PREFIX\" = /data/data/com.termux/files/usr; " +
                "test \"$TMPDIR\" = \"$PREFIX/tmp\"; " +
                "test \"$PATH\" = \"$PREFIX/bin:/system/bin:/system/xbin\"; " +
                "work=$(mktemp -d \"$TMPDIR/klyro-acceptance.XXXXXXXX\"); " +
                "trap 'rm -rf -- \"$work\"' EXIT; cd \"$work\"; mkdir 'space é'; " +
                "printf 'hello 😀\\n' > 'space é/file 😀'; cat 'space é/file 😀' | grep -qx 'hello 😀'; " +
                "read -r value < 'space é/file 😀'; test \"$value\" = 'hello 😀'; " +
                "printf '#!%s/bin/bash\\nprintf executable' \"$PREFIX\" > script; chmod 700 script; " +
                "test \"$(./script)\" = executable; apt --version >/dev/null; pkg list-installed >/dev/null");
    }

    @Test public void installedEssentialDeveloperToolsExecute() throws Exception {
        runIsolated("git --version >/dev/null; python -I -S -c 'print(1 + 1)' >/dev/null; " +
                "node -e 'if (1 + 1 !== 2) process.exit(1)'; npm --version >/dev/null; " +
                "curl --version >/dev/null; ssh -V 2>/dev/null; clang --version >/dev/null; " +
                "make --version >/dev/null; nano --version >/dev/null; vim --version >/dev/null");
    }
}
