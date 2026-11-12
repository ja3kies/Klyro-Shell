package com.oryn.terminal.termux;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/**
 * Explicit-user-gesture bridge between the interactive private Bash and
 * Android's Storage Access Framework. It does NOT claim a SAF tree is a
 * POSIX mount or expose raw content:// URIs to the shell.
 */
public final class KlyroStorageCommandBridge {
    private static final Pattern TOKEN = Pattern.compile("[0-9]{1,12}-[0-9]{1,6}-[0-9]{1,6}");
    private static final String DIRECTORY = ".klyro-control";
    private final File home;

    public KlyroStorageCommandBridge(File home) {
        if (home == null) throw new IllegalArgumentException("Private home required");
        this.home = home;
    }

    private File directory() throws IOException {
        File base = home.getCanonicalFile();
        Path control = new File(base, DIRECTORY).toPath();
        if (Files.isSymbolicLink(control)) throw new IOException("Refusing linked control directory");
        if (!Files.exists(control, LinkOption.NOFOLLOW_LINKS)) {
            Files.createDirectory(control);
        }
        if (!Files.isDirectory(control, LinkOption.NOFOLLOW_LINKS) ||
                !control.toFile().getCanonicalFile().getParentFile().equals(base)) {
            throw new IOException("Storage bridge escaped private home");
        }
        return control.toFile();
    }

    /** Returns a single validated request, atomically consuming the request file. */
    public String poll() throws IOException {
        File request = new File(directory(), "storage.request");
        Path path = request.toPath();
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || request.length() > 80) {
            throw new IOException("Unsafe storage bridge request");
        }
        String candidate = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim();
        if (!TOKEN.matcher(candidate).matches()) {
            // A concurrent writer may not have finished writing the token yet.
            return null;
        }
        Files.deleteIfExists(path);
        return candidate;
    }

    /** Stores only a small status code. No private paths or URIs are returned. */
    public void finish(String token, String status) throws IOException {
        if (token == null || !TOKEN.matcher(token).matches()) {
            throw new IOException("Invalid storage bridge token");
        }
        if (!"OK".equals(status) && !"CANCELLED".equals(status) &&
                !"DENIED".equals(status)) {
            throw new IOException("Invalid storage bridge result");
        }
        File folder = directory();
        Path temporary = Files.createTempFile(folder.toPath(), ".response-", ".tmp");
        try {
            Files.write(temporary, (status + "\n").getBytes(StandardCharsets.UTF_8));
            Files.move(temporary, new File(folder, "storage.response." + token).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static String helperScript() {
        return "#!/data/data/com.termux/files/usr/bin/bash\n"
            + "# KLYRO_STORAGE_BRIDGE_V1\n"
            + "if [ \"$#\" -ne 0 ]; then\n"
            + "  printf '%s\\n' 'Usage: klyro-setup-storage (no arguments)'\n"
            + "  exit 2\n"
            + "fi\n"
            + "umask 077\n"
            + "DIR=\"$HOME/.klyro-control\"\n"
            + "if [ -L \"$DIR\" ]; then printf '%s\\n' 'Unsafe control path'; exit 1; fi\n"
            + "mkdir -p -- \"$DIR\" || exit 1\n"
            + "TOKEN=\"$BASHPID-$RANDOM-$RANDOM\"\n"
            + "REQUEST=\"$DIR/storage.request\"\n"
            + "RESPONSE=\"$DIR/storage.response.$TOKEN\"\n"
            + "cleanup() {\n"
            + "  if [ -f \"$REQUEST\" ]; then\n"
            + "    IFS= read -r ACTIVE < \"$REQUEST\"\n"
            + "    if [ \"$ACTIVE\" = \"$TOKEN\" ]; then rm -f -- \"$REQUEST\"; fi\n"
            + "  fi\n"
            + "  rm -f -- \"$RESPONSE\"\n"
            + "}\n"
            + "trap cleanup EXIT\n"
            + "trap 'exit 130' INT\n"
            + "trap 'exit 143' TERM\n"
            + "trap 'exit 129' HUP\n"
            + "if ! ( set -C; printf '%s\\n' \"$TOKEN\" > \"$REQUEST\" ) 2>/dev/null; then\n"
            + "  printf '%s\\n' 'Another storage request is pending.'\n"
            + "  exit 1\n"
            + "fi\n"
            + "printf '%s\\n' 'Klyro: choose an Android folder in the picker.'\n"
            + "printf '%s\\n' 'Only selected files can be imported through Settings > Files & storage.'\n"
            + "for ((i=0; i<240; i++)); do\n"
            + "  if [ -f \"$RESPONSE\" ]; then\n"
            + "    IFS= read -r RESULT < \"$RESPONSE\"\n"
            + "    rm -f -- \"$RESPONSE\"\n"
            + "    case \"$RESULT\" in\n"
            + "      OK) printf '%s\\n' 'Folder authorized for Klyro SAF access.'; exit 0;;\n"
            + "      CANCELLED) printf '%s\\n' 'Storage setup cancelled.'; exit 1;;\n"
            + "      DENIED) printf '%s\\n' 'Storage permission denied.'; exit 1;;\n"
            + "    esac\n"
            + "    exit 1\n"
            + "  fi\n"
            + "  sleep .5\n"
            + "done\n"
            + "printf '%s\\n' 'Klyro storage request timed out; return to the foreground.'\n"
            + "exit 1\n";
    }

    /** Add only to generated Klyro profiles, respecting user-managed Bash rc. */
    public static String augmentGeneratedProfile(String existing) {
        if (existing == null || !existing.contains("# Klyro Shell profile") ||
                existing.contains("# KLYRO_STORAGE_FUNCTION_V2")) return existing;
        return existing + "\n# KLYRO_STORAGE_FUNCTION_V2\n"
                + "setup-storage() { command klyro-setup-storage \"$@\"; }\n"
                + "termux-setup-storage() { command klyro-setup-storage \"$@\"; }\n";
    }

    /**
     * Installs the Klyro-namespaced helper, never overwriting a custom file or
     * the upstream termux-setup-storage binary from package management.
     */
    public static void installHelper(File prefix) throws IOException {
        KlyroShellHelperInstaller.install(prefix, "klyro-setup-storage",
                "KLYRO_STORAGE_BRIDGE_V1", helperScript());
    }

}
