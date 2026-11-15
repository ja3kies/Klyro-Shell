package com.oryn.terminal.termux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Filesystem-only recovery for the two-part proot runtime activation transaction. */
final class ProotRuntimeRecovery {
    interface TreeDeleter {
        void delete(File file) throws IOException;
    }

    private ProotRuntimeRecovery() {}

    static void recover(File marker, File binDir, File previousBin, File stagingBin,
            File loader, File previousLoader, File stagingLoader, TreeDeleter deleter)
            throws IOException {
        boolean markerExists = marker.isFile();
        File temporaryMarker = new File(marker.getParentFile(), marker.getName() + ".tmp");
        boolean committed = false;
        boolean hadBin = false;
        boolean hadLoader = false;

        if (markerExists) {
            String state = new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8);
            String[] lines = state.split("\\n", -1);
            if (lines.length != 4 || !lines[3].isEmpty()
                    || !("hadBin=true".equals(lines[0]) || "hadBin=false".equals(lines[0]))
                    || !("hadLoader=true".equals(lines[1]) || "hadLoader=false".equals(lines[1]))
                    || !("state=PREPARED".equals(lines[2]) || "state=COMMITTED".equals(lines[2]))) {
                throw new IOException("Proot runtime transaction marker is invalid; refusing unsafe recovery.");
            }
            hadBin = "hadBin=true".equals(lines[0]);
            hadLoader = "hadLoader=true".equals(lines[1]);
            committed = "state=COMMITTED".equals(lines[2]);
        }

        if (committed) {
            deleter.delete(previousBin);
            if (previousLoader.exists() && !previousLoader.delete()) {
                throw new IOException("Cannot finish cleanup of previous proot loader.");
            }
        } else {
            if (previousBin.exists()) {
                deleter.delete(binDir);
                move(previousBin, binDir);
            } else if (markerExists && !hadBin) {
                deleter.delete(binDir);
            }

            if (previousLoader.exists()) {
                if (loader.exists() && !loader.delete()) {
                    throw new IOException("Cannot remove interrupted proot loader.");
                }
                File parent = loader.getParentFile();
                if (!parent.mkdirs() && !parent.isDirectory()) {
                    throw new IOException("Cannot recreate private proot loader directory.");
                }
                move(previousLoader, loader);
            } else if (markerExists && !hadLoader && loader.exists() && !loader.delete()) {
                throw new IOException("Cannot remove interrupted new proot loader.");
            }
        }

        deleter.delete(stagingBin);
        if (stagingLoader.exists() && !stagingLoader.delete()) {
            throw new IOException("Cannot remove staged proot loader.");
        }
        if (temporaryMarker.exists() && !temporaryMarker.delete()) {
            throw new IOException("Cannot clear temporary proot transaction marker.");
        }
        if (markerExists && marker.exists() && !marker.delete()) {
            throw new IOException("Cannot clear proot runtime transaction marker.");
        }
    }

    private static void move(File from, File to) throws IOException {
        try {
            Files.move(from.toPath(), to.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(from.toPath(), to.toPath());
        }
    }
}
