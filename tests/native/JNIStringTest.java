package com.termux.terminal;

import java.nio.file.Files;
import java.nio.file.Path;

/** Actual JNI execution on the host; does not assert Android app/SELinux compatibility. */
public final class JNIStringTest {
    public static void main(String[] args) throws Exception {
        String shell = args.length == 0 ? "/bin/sh" : args[0];
        Path directory = Files.createTempDirectory("klyro é 😀 ");
        try {
            int[] pid = new int[1];
            String unicode = "é😀";
            String script = "test \"$VALUE\" = 'é😀' && test \"$1\" = 'é😀' && test \"$(pwd)\" = \"$2\"";
            int fd = JNI.createSubprocess(shell, directory.toString(),
                    new String[]{"sh", "-c", script, "sh", unicode, directory.toString()},
                    new String[]{"PATH=/system/bin", "VALUE=" + unicode}, pid, 24, 80);
            try {
                if (JNI.waitFor(pid[0]) != 0) throw new AssertionError("UTF-8 argv/env/cwd failed");
            } finally {
                JNI.close(fd);
            }
            try {
                JNI.createSubprocess(shell, directory.toString(),
                        new String[]{"sh", "bad\u0000argument"}, new String[0], pid, 24, 80);
                throw new AssertionError("NUL argument accepted");
            } catch (RuntimeException expected) {
                if (!expected.getMessage().contains("NUL")) throw expected;
            }
            System.out.println("PASS: real JNI UTF-8 argv, env, emoji/spaced cwd and NUL rejection");
        } finally {
            Files.delete(directory);
        }
    }
}
