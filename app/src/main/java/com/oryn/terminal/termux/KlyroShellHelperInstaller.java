package com.oryn.terminal.termux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Shared installation contract for Klyro-owned CLI adapters in the private prefix. */
public final class KlyroShellHelperInstaller {
    private KlyroShellHelperInstaller() {}

    public static void install(File prefix, String command, String marker, String script)
            throws IOException {
        if (prefix == null || command == null ||
                !command.matches("klyro-[a-z0-9-]{1,48}") ||
                marker == null || !marker.matches("KLYRO_[A-Z0-9_]{1,70}") ||
                script == null || !script.contains("# " + marker)) {
            throw new IOException("Invalid Klyro helper metadata");
        }
        File bin = new File(prefix, "bin");
        if (!bin.isDirectory()) throw new IOException("Private bootstrap bin directory missing");
        Path target = new File(bin, command).toPath();
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            if (!Files.isRegularFile(target, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Refusing linked or unsafe CLI helper");
            }
            String existing = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
            if (!existing.contains("# " + marker)) return; // user-managed file
            if (existing.equals(script)) return;
        }
        Path staging = Files.createTempFile(bin.toPath(), ".klyro-helper-", ".tmp");
        try {
            Files.write(staging, script.getBytes(StandardCharsets.UTF_8));
            if (!staging.toFile().setExecutable(true, true)) {
                throw new IOException("Cannot mark Klyro CLI helper executable");
            }
            Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(staging);
        }
    }
}
