package com.oryn.terminal.termux;

import org.junit.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.Assert.*;

public class KlyroStorageCommandBridgeTest {
    @Test public void consumesRequestOnceAndReturnsStatus() throws Exception {
        Path home = Files.createTempDirectory("klyro-home");
        try {
            KlyroStorageCommandBridge bridge = new KlyroStorageCommandBridge(home.toFile());
            assertNull(bridge.poll());
            Path request = home.resolve(".klyro-control/storage.request");
            String token = "1234-234-456";
            Files.write(request, (token + "\n").getBytes(StandardCharsets.UTF_8));
            assertEquals(token, bridge.poll());
            assertNull(bridge.poll());
            bridge.finish(token, "OK");
            String response = new String(Files.readAllBytes(
                    home.resolve(".klyro-control/storage.response." + token)),
                    StandardCharsets.UTF_8);
            assertEquals("OK\n", response);
        } finally {
            try (java.util.stream.Stream<Path> walk = Files.walk(home)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }
    }

    @Test public void rejectsUntrustedTokensAndStatuses() throws Exception {
        Path home = Files.createTempDirectory("klyro-home");
        try {
            KlyroStorageCommandBridge bridge = new KlyroStorageCommandBridge(home.toFile());
            try {
                bridge.finish("../../escape", "OK");
                fail("unsafe token accepted");
            } catch (IOException expected) {}
            try {
                bridge.finish("123-4-5", "PATH=/data/data/secret");
                fail("unsafe status accepted");
            } catch (IOException expected) {}
            Path request = home.resolve(".klyro-control/storage.request");
            Files.write(request, "../../escape\n".getBytes(StandardCharsets.UTF_8));
            assertNull(bridge.poll());
        } finally {
            try (java.util.stream.Stream<Path> walk = Files.walk(home)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        }
    }

    @Test public void rejectsSymlinkControlDirectory() throws Exception {
        Path home = Files.createTempDirectory("klyro-home");
        Path external = Files.createTempDirectory("klyro-external");
        try {
            Files.createSymbolicLink(home.resolve(".klyro-control"), external);
            try {
                new KlyroStorageCommandBridge(home.toFile()).poll();
                fail("linked control directory accepted");
            } catch (IOException expected) {}
        } finally {
            Files.deleteIfExists(home.resolve(".klyro-control"));
            Files.deleteIfExists(home);
            Files.deleteIfExists(external);
        }
    }

    @Test public void installsNamespacedExecutableWithoutOverwritingCustomScript() throws Exception {
        Path prefix = Files.createTempDirectory("klyro-prefix");
        Path bin = Files.createDirectory(prefix.resolve("bin"));
        Path target = bin.resolve("klyro-setup-storage");
        try {
            KlyroStorageCommandBridge.installHelper(prefix.toFile());
            assertTrue(Files.isExecutable(target));
            String script = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
            assertTrue(script.contains("KLYRO_STORAGE_BRIDGE_V1"));
            assertTrue(script.contains("storage.request"));
            assertFalse(script.contains("rm -rf"));
            assertTrue(script.contains("240; i++"));
            Files.write(target, "#!/bin/sh\n# user custom\n".getBytes(StandardCharsets.UTF_8));
            KlyroStorageCommandBridge.installHelper(prefix.toFile());
            assertTrue(new String(Files.readAllBytes(target), StandardCharsets.UTF_8).contains("# user custom"));
        } finally {
            Files.deleteIfExists(target);
            Files.deleteIfExists(bin);
            Files.deleteIfExists(prefix);
        }
    }

    @Test public void onlyGeneratedBashProfilesAreAugmentedOnce() {
        String generated = "# Klyro Shell profile\n";
        String augmented = KlyroStorageCommandBridge.augmentGeneratedProfile(generated);
        assertTrue(augmented.contains("termux-setup-storage()"));
        assertEquals(augmented, KlyroStorageCommandBridge.augmentGeneratedProfile(augmented));
        assertEquals("custom bashrc\n",
                KlyroStorageCommandBridge.augmentGeneratedProfile("custom bashrc\n"));
    }
}
