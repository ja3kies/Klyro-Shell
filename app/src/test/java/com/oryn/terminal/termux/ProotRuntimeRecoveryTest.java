package com.oryn.terminal.termux;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ProotRuntimeRecoveryTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private File root, bin, backupBin, stagingBin, loader, backupLoader, stagingLoader, marker;

    @Before public void setUp() throws Exception {
        root = temp.newFolder("runtime");
        bin = new File(root, "bin");
        backupBin = new File(root, "bin.previous");
        stagingBin = new File(root, "bin.staging");
        loader = new File(root, "prefix/proot-loader");
        backupLoader = new File(root, "proot-loader.previous");
        stagingLoader = new File(root, "proot-loader.staging");
        marker = new File(root, "proot-runtime.transaction");
    }

    @Test public void preparedBeforeAnyMoveKeepsOldRuntime() throws Exception {
        directoryWithFile(bin, "old-bin");
        file(loader, "old-loader");
        writeMarker(true, true, false);
        recover();
        assertFile(bin, "old-bin");
        assertText(loader, "old-loader");
        assertFalse(marker.exists());
    }

    @Test public void interruptionAfterBinBackupRestoresBothComponents() throws Exception {
        directoryWithFile(backupBin, "old-bin");
        file(loader, "old-loader");
        directoryWithFile(stagingBin, "new-bin");
        file(stagingLoader, "new-loader");
        writeMarker(true, true, false);
        recover();
        assertFile(bin, "old-bin");
        assertText(loader, "old-loader");
        assertFalse(backupBin.exists());
    }

    @Test public void interruptionAfterBothBackupsRestoresBothComponents() throws Exception {
        directoryWithFile(backupBin, "old-bin");
        file(backupLoader, "old-loader");
        directoryWithFile(bin, "partial-bin");
        file(loader, "partial-loader");
        writeMarker(true, true, false);
        recover();
        assertFile(bin, "old-bin");
        assertText(loader, "old-loader");
    }

    @Test public void interruptionAfterFirstNewComponentRestoresOldPair() throws Exception {
        directoryWithFile(backupBin, "old-bin");
        file(backupLoader, "old-loader");
        directoryWithFile(bin, "new-bin");
        writeMarker(true, true, false);
        recover();
        assertFile(bin, "old-bin");
        assertText(loader, "old-loader");
    }

    @Test public void interruptionAfterBothNewComponentsButBeforeCommitRestoresOldPair() throws Exception {
        directoryWithFile(backupBin, "old-bin");
        file(backupLoader, "old-loader");
        directoryWithFile(bin, "new-bin");
        file(loader, "new-loader");
        writeMarker(true, true, false);
        recover();
        assertFile(bin, "old-bin");
        assertText(loader, "old-loader");
    }

    @Test public void firstInstallRollbackRemovesPartialNewComponents() throws Exception {
        directoryWithFile(bin, "partial-bin");
        file(loader, "partial-loader");
        writeMarker(false, false, false);
        recover();
        assertFalse(bin.exists());
        assertFalse(loader.exists());
    }

    @Test public void committedTransactionKeepsNewRuntimeAndDeletesBackups() throws Exception {
        directoryWithFile(bin, "new-bin");
        file(loader, "new-loader");
        directoryWithFile(backupBin, "old-bin");
        file(backupLoader, "old-loader");
        writeMarker(true, true, true);
        recover();
        assertFile(bin, "new-bin");
        assertText(loader, "new-loader");
        assertFalse(backupBin.exists());
        assertFalse(backupLoader.exists());
    }

    @Test public void legacyBackupsAreRestoredWithoutMarker() throws Exception {
        directoryWithFile(backupBin, "old-bin");
        file(backupLoader, "old-loader");
        directoryWithFile(bin, "partial-bin");
        file(loader, "partial-loader");
        recover();
        assertFile(bin, "old-bin");
        assertText(loader, "old-loader");
    }

    @Test public void malformedMarkerRefusesRecoveryWithoutDeletingAnything() throws Exception {
        directoryWithFile(bin, "valuable-bin");
        file(loader, "valuable-loader");
        Files.write(marker.toPath(), "hadBin=true\nhadLoader=false\nstate=PREPARED-extra\n"
                .getBytes(StandardCharsets.UTF_8));
        try {
            recover();
            fail("Expected invalid marker rejection");
        } catch (IOException expected) {
            assertFile(bin, "valuable-bin");
            assertText(loader, "valuable-loader");
            assertTrue(marker.exists());
        }
    }

    @Test public void temporaryMarkerIsRemovedDuringRecovery() throws Exception {
        file(new File(root, marker.getName() + ".tmp"), "partial");
        writeMarker(false, false, false);
        recover();
        assertFalse(new File(root, marker.getName() + ".tmp").exists());
    }

    private void recover() throws IOException {
        ProotRuntimeRecovery.recover(marker, bin, backupBin, stagingBin,
                loader, backupLoader, stagingLoader, ProotRuntimeRecoveryTest::deleteTree);
    }

    private void writeMarker(boolean hadBin, boolean hadLoader, boolean committed) throws IOException {
        String value = "hadBin=" + hadBin + "\nhadLoader=" + hadLoader + "\nstate="
                + (committed ? "COMMITTED" : "PREPARED") + "\n";
        Files.write(marker.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private static void directoryWithFile(File directory, String name) throws IOException {
        if (!directory.mkdirs() && !directory.isDirectory()) throw new IOException("mkdir failed");
        file(new File(directory, "payload"), name);
    }

    private static void file(File file, String value) throws IOException {
        File parent = file.getParentFile();
        if (!parent.mkdirs() && !parent.isDirectory()) throw new IOException("mkdir failed");
        Files.write(file.toPath(), value.getBytes(StandardCharsets.UTF_8));
    }

    private static void assertFile(File directory, String expected) throws IOException {
        assertEquals(expected, new String(Files.readAllBytes(new File(directory, "payload").toPath()),
                StandardCharsets.UTF_8));
    }

    private static void assertText(File file, String expected) throws IOException {
        assertEquals(expected, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    private static void deleteTree(File file) throws IOException {
        if (!file.exists()) return;
        if (Files.isSymbolicLink(file.toPath())) {
            Files.delete(file.toPath());
            return;
        }
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        Files.delete(file.toPath());
    }
}
