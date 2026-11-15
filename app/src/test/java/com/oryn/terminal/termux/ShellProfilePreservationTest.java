package com.oryn.terminal.termux;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.AccessDeniedException;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;

/** Real filesystem checks: migration must preserve user bytes and dotfile topology. */
public class ShellProfilePreservationTest {
    @Rule public TemporaryFolder tmp = new TemporaryFolder();

    private File profile() { return new File(tmp.getRoot(), ".bashrc"); }
    private File backup() { return new File(tmp.getRoot(), ".bashrc.klyro-before-color.bak"); }
    private byte[] generated() {
        return ("# Klyro Shell profile\n"
                + "export PS1='\\[\\e[38;2;0;240;255m\\]klyro\\[\\e[0m\\] \\w $ '\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    @Test public void missingProfileGetsCompleteGeneratedDefaults() throws Exception {
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        String text = new String(Files.readAllBytes(profile().toPath()), StandardCharsets.UTF_8);
        assertTrue(text.contains("# Klyro Shell profile"));
        assertTrue(text.contains(KlyroWelcomeGuide.MARKER));
        assertTrue(text.contains("# KLYRO_INFO_FUNCTION_V1"));
        assertFalse(backup().exists());
    }

    @Test public void existingEmptyBashrcAndProfileAreUserOwned() throws Exception {
        Files.createFile(profile().toPath());
        File login = new File(tmp.getRoot(), ".profile");
        Files.createFile(login.toPath());
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        TermuxBootstrapInstaller.writeIfAbsent(login, "generated login\n");
        assertEquals(0, Files.size(profile().toPath()));
        assertEquals(0, Files.size(login.toPath()));
        assertFalse(backup().exists());
    }

    @Test public void userProfileBytesAreNotAugmented() throws Exception {
        byte[] content = "# personal configuration\nexport EDITOR=vim\n".getBytes(StandardCharsets.UTF_8);
        Files.write(profile().toPath(), content);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertArrayEquals(content, Files.readAllBytes(profile().toPath()));
        assertFalse(backup().exists());
    }

    @Test public void generatedDotfileSymlinkAndItsTargetRemainIntact() throws Exception {
        Path target = new File(tmp.getRoot(), "shared shell config").toPath();
        Files.write(target, generated());
        Files.createSymbolicLink(profile().toPath(), target);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertTrue(Files.isSymbolicLink(profile().toPath()));
        assertEquals(target, Files.readSymbolicLink(profile().toPath()));
        assertArrayEquals(generated(), Files.readAllBytes(target));
        assertFalse(backup().exists());
    }

    @Test public void hardlinkedGeneratedDotfileRetainsItsSharedInode() throws Exception {
        Path shared = new File(tmp.getRoot(), "shared profile").toPath();
        Files.write(shared, generated());
        try {
            Files.createLink(profile().toPath(), shared);
        } catch (AccessDeniedException denied) {
            if (!Files.isRegularFile(Paths.get("/system/bin/linker64"))) throw denied;
            org.junit.Assume.assumeTrue("Android host denied hardlink creation; Linux CI must exercise this case", false);
            return;
        }
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertTrue(Files.isSameFile(profile().toPath(), shared));
        assertArrayEquals(generated(), Files.readAllBytes(shared));
        assertEquals(2, ((Number) Files.getAttribute(shared, "unix:nlink")).intValue());
        assertFalse(backup().exists());
    }

    @Test public void generatedMigrationPreservesExecutableAndCustomPosixPermissions() throws Exception {
        Files.write(profile().toPath(), generated());
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-x---");
        Files.setPosixFilePermissions(profile().toPath(), permissions);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertEquals(permissions, Files.getPosixFilePermissions(profile().toPath()));
        assertArrayEquals(generated(), Files.readAllBytes(backup().toPath()));
        assertTrue(new String(Files.readAllBytes(profile().toPath()), StandardCharsets.UTF_8)
                .contains(KlyroWelcomeGuide.MARKER));
    }

    @Test public void danglingDotfileLinksDoNotCreateTheirTargets() throws Exception {
        Path target = new File(tmp.getRoot(), "absent target").toPath();
        Files.createSymbolicLink(profile().toPath(), target);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        TermuxBootstrapInstaller.writeIfAbsent(profile(), "must not follow link\n");
        assertTrue(Files.isSymbolicLink(profile().toPath()));
        assertFalse(Files.exists(target));
        assertFalse(backup().exists());
    }

    @Test public void directoryNamedBashrcIsPreserved() throws Exception {
        Files.createDirectory(profile().toPath());
        Path child = profile().toPath().resolve("user-file");
        Files.write(child, generated());
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertTrue(Files.isDirectory(profile().toPath()));
        assertArrayEquals(generated(), Files.readAllBytes(child));
    }

    @Test public void malformedUtf8IsNotReencodedOrBackedUp() throws Exception {
        byte[] old = generated();
        byte[] invalid = java.util.Arrays.copyOf(old, old.length + 1);
        invalid[old.length] = (byte) 0xff;
        Files.write(profile().toPath(), invalid);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertArrayEquals(invalid, Files.readAllBytes(profile().toPath()));
        assertFalse(backup().exists());
    }

    @Test public void oversizedGeneratedProfileIsPreservedWithoutMigration() throws Exception {
        byte[] content = java.util.Arrays.copyOf(generated(), TermuxBootstrapInstaller.MAX_PROFILE_BYTES + 1);
        Files.write(profile().toPath(), content);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertArrayEquals(content, Files.readAllBytes(profile().toPath()));
        assertFalse(backup().exists());
    }

    @Test public void binaryProfileWithNulIsNotMigrated() throws Exception {
        byte[] binary = java.util.Arrays.copyOf(generated(), generated().length + 1);
        Files.write(profile().toPath(), binary);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertArrayEquals(binary, Files.readAllBytes(profile().toPath()));
        assertFalse(backup().exists());
    }

    @Test public void generatedMigrationKeepsExactOriginalAndIsIdempotent() throws Exception {
        Files.write(profile().toPath(), generated());
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertArrayEquals(generated(), Files.readAllBytes(backup().toPath()));
        byte[] migrated = Files.readAllBytes(profile().toPath());
        assertFalse(java.util.Arrays.equals(generated(), migrated));
        assertTrue(new String(migrated, StandardCharsets.UTF_8).contains(KlyroWelcomeGuide.MARKER));
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertArrayEquals(migrated, Files.readAllBytes(profile().toPath()));
        assertArrayEquals(generated(), Files.readAllBytes(backup().toPath()));
    }

    @Test public void unsafeBackupDoesNotFollowLinkOrChangeProfile() throws Exception {
        Files.write(profile().toPath(), generated());
        Path target = new File(tmp.getRoot(), "backup target").toPath();
        Files.createSymbolicLink(backup().toPath(), target);
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertTrue(Files.isSymbolicLink(backup().toPath()));
        assertFalse(Files.exists(target));
        assertArrayEquals(generated(), Files.readAllBytes(profile().toPath()));
    }

    @Test public void userEditSupersedingMigrationSnapshotIsNotReplaced() throws Exception {
        String snapshot = new String(generated(), StandardCharsets.UTF_8);
        byte[] edited = "# updated by my editor\nexport EDITOR=vim\n".getBytes(StandardCharsets.UTF_8);
        Files.write(profile().toPath(), edited);
        TermuxBootstrapInstaller.replaceGeneratedProfile(profile(), snapshot, "generated replacement\n");
        assertArrayEquals(edited, Files.readAllBytes(profile().toPath()));
        assertFalse(backup().exists());
    }

    @Test public void existingUserBackupIsNeverOverwritten() throws Exception {
        byte[] userBackup = "my saved original\n".getBytes(StandardCharsets.UTF_8);
        Files.write(backup().toPath(), userBackup);
        Files.write(profile().toPath(), generated());
        TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
        assertArrayEquals(userBackup, Files.readAllBytes(backup().toPath()));
    }

    @Test public void concurrentMigrationsPublishOneCompleteProfileAndOriginalBackup() throws Exception {
        Files.write(profile().toPath(), generated());
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int n = 0; n < 32; n++) {
                tasks.add(workers.submit(() -> {
                    try { TermuxBootstrapInstaller.writeKlyroBashProfile(profile()); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }));
            }
            for (Future<?> task : tasks) task.get();
            assertArrayEquals(generated(), Files.readAllBytes(backup().toPath()));
            byte[] migrated = Files.readAllBytes(profile().toPath());
            assertTrue(new String(migrated, StandardCharsets.UTF_8).contains(KlyroWelcomeGuide.MARKER));
            TermuxBootstrapInstaller.writeKlyroBashProfile(profile());
            assertArrayEquals(migrated, Files.readAllBytes(profile().toPath()));
            File[] leftovers = tmp.getRoot().listFiles((dir, name) -> name.startsWith(".klyro-ps1-"));
            assertNotNull(leftovers);
            assertEquals(0, leftovers.length);
        } finally {
            workers.shutdownNow();
        }
    }

    @Test public void absentLoginAndVersionMarkerAreCreatedOnlyOnce() throws Exception {
        for (String name : new String[]{".profile", ".oryn-bootstrap-version"}) {
            File target = new File(tmp.getRoot(), name);
            TermuxBootstrapInstaller.writeIfAbsent(target, "first\n");
            TermuxBootstrapInstaller.writeIfAbsent(target, "replacement\n");
            assertArrayEquals("first\n".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(target.toPath()));
        }
    }

    @Test public void concurrentDefaultWritersNeverTruncateOneAnother() throws Exception {
        File target = new File(tmp.getRoot(), ".profile");
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            List<Future<?>> tasks = new ArrayList<>();
            for (int n = 0; n < 32; n++) {
                final String text = "writer-" + n + "\n";
                tasks.add(workers.submit(() -> {
                    try { TermuxBootstrapInstaller.writeIfAbsent(target, text); }
                    catch (Exception e) { throw new RuntimeException(e); }
                }));
            }
            for (Future<?> task : tasks) task.get();
            String result = new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8);
            assertTrue(result.matches("writer-[0-9]+\\n"));
        } finally {
            workers.shutdownNow();
        }
    }
}
