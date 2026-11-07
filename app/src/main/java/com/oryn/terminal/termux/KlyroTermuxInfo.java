package com.oryn.terminal.termux;

import java.io.File;
import java.io.IOException;

/** Termux-style read-only environment summary, scoped to Klyro's own Bash. */
public final class KlyroTermuxInfo {
    private KlyroTermuxInfo() {}

    public static String helperScript() {
        return "#!/data/data/com.termux/files/usr/bin/bash\n"
            + "# KLYRO_INFO_BRIDGE_V1\n"
            + "if [ \"$#\" -ne 0 ]; then printf '%s\\n' 'Usage: termux-info'; exit 2; fi\n"
            + "printf 'Klyro Shell - private Termux-compatible environment\\n'\n"
            + "printf 'Bash: %s\\n' \"$BASH_VERSION\"\n"
            + "printf 'Architecture: %s\\n' \"$(uname -m 2>/dev/null || echo unknown)\"\n"
            + "printf 'Prefix: %s\\n' \"$PREFIX\"\n"
            + "printf 'Home: %s\\n' \"$HOME\"\n"
            + "if command -v pkg >/dev/null 2>&1; then\n"
            + "  printf 'Package manager: pkg present\\n'\n"
            + "else\n"
            + "  printf 'Package manager: pkg missing\\n'\n"
            + "fi\n"
            + "printf 'Android files: Settings > Files & storage (SAF)\\n'\n"
            + "printf 'Storage mount: not provided by SAF\\n'\n";
    }

    public static void installHelper(File prefix) throws IOException {
        KlyroShellHelperInstaller.install(prefix, "klyro-info",
                "KLYRO_INFO_BRIDGE_V1", helperScript());
    }

    public static String augmentGeneratedProfile(String existing) {
        if (existing == null || !existing.contains("# Klyro Shell profile") ||
                existing.contains("# KLYRO_INFO_FUNCTION_V1")) return existing;
        return existing + "\n# KLYRO_INFO_FUNCTION_V1\n"
                + "termux-info() { command klyro-info \"$@\"; }\n";
    }
}
