package com.termux.terminal;

import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Real PTYs in Klyro's Android UID; these tests never use a Termux installation. */
@RunWith(AndroidJUnit4.class)
public class NativePtyTest {
    private static final class Pty implements AutoCloseable {
        final int fd;
        final int pid;
        final ParcelFileDescriptor inputFd;
        final InputStream input;
        final OutputStream output;
        final ExecutorService waiter = Executors.newSingleThreadExecutor();
        final Future<Integer> exitResult;
        boolean reaped;

        Pty(String script, String cwd, String... extra) throws Exception {
            int[] process = new int[1];
            String[] args = new String[4 + extra.length];
            args[0] = "sh"; args[1] = "-c"; args[2] = script; args[3] = "sh";
            System.arraycopy(extra, 0, args, 4, extra.length);
            fd = JNI.createSubprocess("/system/bin/sh", cwd, args,
                    new String[]{"PATH=/system/bin", "TERM=xterm-256color", "VALUE=é😀"}, process, 24, 80);
            pid = process[0];
            exitResult = waiter.submit(() -> JNI.waitFor(pid));
            inputFd = ParcelFileDescriptor.fromFd(fd);
            input = new ParcelFileDescriptor.AutoCloseInputStream(inputFd);
            output = new ParcelFileDescriptor.AutoCloseOutputStream(ParcelFileDescriptor.fromFd(fd));
        }
        String until(String marker) throws Exception {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            StructPollfd poll = new StructPollfd();
            poll.fd = inputFd.getFileDescriptor(); poll.events = (short) OsConstants.POLLIN;
            while (System.nanoTime() < deadline) {
                if (Os.poll(new StructPollfd[]{poll}, 200) == 0) continue;
                byte[] chunk = new byte[4096];
                int read = input.read(chunk);
                if (read < 0) break;
                bytes.write(chunk, 0, read);
                String text = bytes.toString("UTF-8");
                if (text.contains(marker)) return text;
                assertTrue("Unbounded PTY output", bytes.size() < 256 * 1024);
            }
            throw new AssertionError("PTY marker missing: " + marker + "; got " + bytes.toString("UTF-8"));
        }
        int exit() throws Exception {
            int result = exitResult.get(5, TimeUnit.SECONDS);
            reaped = true;
            return result;
        }
        public void close() throws Exception {
            if (!reaped) {
                try { Os.kill(-pid, OsConstants.SIGKILL); } catch (Exception ignored) { }
                try { Os.kill(pid, OsConstants.SIGKILL); } catch (Exception ignored) { }
                if (!waiter.isShutdown()) {
                    try { exit(); } catch (Exception ignored) { }
                }
            }
            input.close(); output.close(); JNI.close(fd);
            waiter.shutdownNow();
        }
    }

    @Test public void unicodeArgumentsEnvironmentAndPrivateWorkingDirectory() throws Exception {
        File folder = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getFilesDir(),
                "pty-test é 😀 " + System.nanoTime());
        assertTrue(folder.mkdir());
        try (Pty pty = new Pty("test -t 0 && test -t 1 && test -t 2 && " +
                "test \"$VALUE\" = \"$1\" && printf 'OK-é😀'", folder.getPath(), "é😀")) {
            assertTrue(pty.until("OK-é😀").contains("OK-é😀"));
            assertEquals(0, pty.exit());
        } finally { assertTrue(folder.delete()); }
    }

    @Test public void resizeUpdatesTheActualPty() throws Exception {
        try (Pty pty = new Pty("stty size; read answer; stty size; printf DONE", "/")) {
            pty.until("24 80");
            JNI.setPtyWindowSize(pty.fd, 12, 90);
            pty.output.write("continue\n".getBytes(StandardCharsets.UTF_8));
            assertTrue(pty.until("DONE").contains("12 90"));
            assertEquals(0, pty.exit());
        }
    }

    @Test public void controlCInterruptsForegroundProcess() throws Exception {
        try (Pty pty = new Pty("printf READY; exec sleep 30", "/")) {
            pty.until("READY");
            pty.output.write(3);
            assertEquals(-OsConstants.SIGINT, pty.exit());
        }
    }

    @Test public void eofAndSeparateSessions() throws Exception {
        try (Pty first = new Pty("read answer; printf 'FIRST:%s' \"$answer\"", "/");
             Pty second = new Pty("read answer; printf 'SECOND:%s' \"$answer\"", "/")) {
            first.output.write("one\n".getBytes(StandardCharsets.UTF_8));
            first.until("FIRST:one");
            assertEquals(0, first.exit());
            second.output.write(4); // canonical-mode EOF, with no pending bytes
            second.until("SECOND:");
            assertEquals(0, second.exit());
        }
    }
}
