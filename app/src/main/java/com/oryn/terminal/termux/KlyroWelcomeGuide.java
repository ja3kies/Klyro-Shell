package com.oryn.terminal.termux;

/**
 * Klyro-owned first-session guide. The upstream documentation is attributed,
 * while Android storage instructions reflect Klyro's own SAF integration.
 */
public final class KlyroWelcomeGuide {
    public static final String MARKER = "# KLYRO_WELCOME_GUIDE_V1";
    public static final String MESSAGE =
            "Welcome to Klyro Shell!\n" +
            "\n" +
            "A private Android terminal with Termux-compatible packages.\n" +
            "Termux docs:      https://termux.dev/docs\n" +
            "Termux community: https://termux.dev/community\n" +
            "\n" +
            "Working with packages:\n" +
            " - Search:   pkg search <query>\n" +
            " - Install:  pkg install <package>\n" +
            " - Update:   pkg update\n" +
            " - Upgrade:  pkg upgrade\n" +
            "\n" +
            "Common shell commands:\n" +
            " - Location: pwd\n" +
            " - Files:    ls -la\n" +
            " - Change:   cd <directory>\n" +
            " - Leave:    exit\n" +
            "\n" +
            "Android files and storage:\n" +
            " - Open Klyro Settings > Files & storage.\n" +
            " - termux-setup-storage is not verified for Klyro.\n" +
            "\n" +
            "Open Command Palette to discover commands or read this guide.\n";

    private KlyroWelcomeGuide() {}

    /**
     * Only an interactive shell prints the message. Noclobber on a private
     * sentinel keeps the greeting once-only across concurrent first tabs.
     * Never write the welcome text as terminal input or execute its examples.
     */
    public static String shellRcSnippet() {
        StringBuilder snippet = new StringBuilder("\n" + MARKER + "\n")
            .append("case \"$-\" in *i*)\n")
            .append("  if ( set -C; : > \"$HOME/.klyro-welcome-shown\" ) 2>/dev/null; then\n");
        for (String line : MESSAGE.split("\n", -1)) {
            if (line.isEmpty()) {
                snippet.append("    printf '\\n'\n");
            } else {
                String quoted = line.replace("'", "'\"'\"'");
                snippet.append("    printf '%s\\n' '").append(quoted).append("'\n");
            }
        }
        snippet.append("  fi ;;\nesac\n");
        return snippet.toString();
    }

    public static String augmentGeneratedProfile(String existingProfile) {
        if (existingProfile == null || !existingProfile.contains("# Klyro Shell profile")
                || existingProfile.contains(MARKER)) return existingProfile;
        return existingProfile + shellRcSnippet();
    }
}
