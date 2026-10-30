package com.oryn.terminal.session;

import android.os.Handler;
import android.os.Looper;

import com.oryn.terminal.core.PtyProcess;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns one interactive shell process and its PTY.
 *
 * Output is streamed as UTF-8 text. ANSI interpretation and scrollback rendering
 * belong to the terminal presentation layer, not to the process owner.
 */
public final class TerminalSession {
    public interface Listener {
        void onOutput(TerminalSession session, String text);
        void onFinished(TerminalSession session, int exitCode);
        void onError(TerminalSession session, String message);
    }

    private final String id;
    private final String name;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private final PtyProcess process;
    private volatile int exitCode = -1;

    private TerminalSession(String id, String name, PtyProcess process, Listener listener) {
        this.id = id;
        this.name = name;
        this.process = process;
        this.listener = listener;
        Thread reader = new Thread(this::readOutput, "oryn-pty-reader-" + id);
        reader.setDaemon(true);
        reader.start();
    }

    public static TerminalSession start(
            String id,
            String name,
            String[] command,
            Map<String, String> environment,
            File workingDirectory,
            int rows,
            int columns,
            Listener listener
    ) throws IOException {
        if (command == null || command.length == 0 || command[0] == null) {
            throw new IllegalArgumentException("A shell command is required.");
        }
        PtyProcess process = PtyProcess.spawn(
                command, environment, workingDirectory, rows, columns
        );
        return new TerminalSession(id, name, process, listener);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public boolean isFinished() { return finished.get(); }
    public int getExitCode() { return exitCode; }

    /** Sends literal terminal input. Use "\u0003" for the terminal's Ctrl+C key. */
    public void sendInput(String text) throws IOException {
        if (finished.get()) throw new IOException("This session has already exited.");
        OutputStream output = process.getOutputStream();
        output.write(text.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    public void resize(int columns, int rows) {
        if (!finished.get()) process.setWindowSize(rows, columns);
    }

    /** Gracefully stops the shell, then escalates if it does not exit. */
    public void close() {
        if (finished.get()) return;
        process.sendSignal(PtyProcess.SIGTERM);
        mainHandler.postDelayed(() -> {
            if (!finished.get()) process.sendSignal(PtyProcess.SIGKILL);
        }, 1500L);
    }

    private void readOutput() {
        try (Reader reader = new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8
        )) {
            char[] buffer = new char[4096];
            int count;
            while ((count = reader.read(buffer)) != -1) {
                if (count == 0) continue;
                String chunk = new String(buffer, 0, count);
                mainHandler.post(() -> {
                    if (listener != null) listener.onOutput(this, chunk);
                });
            }
        } catch (IOException error) {
            if (!finished.get()) {
                mainHandler.post(() -> {
                    if (listener != null) listener.onError(this, error.getMessage());
                });
            }
        } finally {
            exitCode = process.waitFor();
            finished.set(true);
            // waitFor() has reaped the child; close now only releases the PTY fd.
            process.close();
            mainHandler.post(() -> {
                if (listener != null) listener.onFinished(this, exitCode);
            });
        }
    }
}
