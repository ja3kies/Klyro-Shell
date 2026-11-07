package com.oryn.terminal.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Curated command reference. Every insertion is literal shell text without Enter. */
public final class KlyroCommandCatalog {
    public static final class Entry {
        public final String command;
        public final String insertion;
        public final String purpose;
        public final String category;
        public final String note;
        public final boolean insertable;
        public final boolean caution;

        private Entry(String command, String insertion, String purpose, String category,
                      String note, boolean insertable, boolean caution) {
            this.command = command;
            this.insertion = insertion;
            this.purpose = purpose;
            this.category = category;
            this.note = note;
            this.insertable = insertable;
            this.caution = caution;
        }
    }

    private KlyroCommandCatalog() {}

    private static Entry builtin(String command, String insertion, String purpose) {
        return new Entry(command, insertion, purpose, "Bash built-ins",
                "Available in the active Bash shell", true, false);
    }

    private static Entry pkg(String command, String insertion, String purpose, boolean caution) {
        return new Entry(command, insertion, purpose, "Packages",
                "Requires Klyro's private pkg runtime", true, caution);
    }

    private static Entry utility(String command, String insertion, String purpose) {
        return new Entry(command, insertion, purpose, "Utilities",
                "Availability depends on installed packages", true, false);
    }

    private static final List<Entry> ENTRIES;
    static {
        List<Entry> commands = new ArrayList<>();
        commands.add(builtin("pwd", "pwd", "Show the current directory"));
        commands.add(builtin("cd", "cd ", "Change directory (add a path)"));
        commands.add(utility("ls", "ls -la", "List files, including hidden files"));
        commands.add(builtin("echo", "echo ", "Print text or a variable"));
        commands.add(builtin("printf", "printf ", "Format output precisely"));
        commands.add(builtin("history", "history", "Show Bash command history"));
        commands.add(builtin("help", "help", "Open Bash built-in command help"));
        commands.add(builtin("jobs", "jobs", "Show background jobs in this shell"));
        commands.add(builtin("fg", "fg", "Resume a job in the foreground"));
        commands.add(builtin("bg", "bg", "Resume a stopped job in the background"));
        commands.add(builtin("exit", "exit", "Leave the active shell; this closes its tab"));
        commands.add(pkg("pkg search", "pkg search ", "Find an available package", false));
        commands.add(pkg("pkg install", "pkg install ", "Install a package in private storage", true));
        commands.add(pkg("pkg update", "pkg update", "Refresh package repository metadata", true));
        commands.add(pkg("pkg upgrade", "pkg upgrade", "Upgrade installed packages", true));
        commands.add(pkg("pkg list-installed", "pkg list-installed", "List installed packages", false));
        commands.add(utility("clear", "clear", "Clear the shell screen"));
        commands.add(utility("git status", "git status", "Show repository changes"));
        commands.add(utility("python", "python", "Open Python if installed"));
        commands.add(utility("node", "node", "Open Node.js if installed"));
        commands.add(new Entry("termux-change-repo", "termux-change-repo",
                "Configure Termux repository mirrors", "Repository tools",
                "Only if the command exists in Klyro's private prefix", true, true));
        commands.add(new Entry("termux-setup-storage", "",
                "Termux Android storage setup", "Android integration",
                "Not verified in Klyro. Use Settings > Files & storage and Android's document picker.",
                false, false));
        ENTRIES = Collections.unmodifiableList(commands);
    }

    public static List<Entry> all() { return ENTRIES; }

    public static List<Entry> search(String query) {
        if (query == null || query.trim().isEmpty()) return ENTRIES;
        if (query.length() > KlyroCommandSearch.MAX_QUERY_LENGTH) return Collections.emptyList();
        List<Entry> matching = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (KlyroCommandSearch.matches(query, entry.command,
                    entry.purpose + " " + entry.category + " " + entry.note)) {
                matching.add(entry);
            }
        }
        return matching;
    }

    public static String safeInsertion(Entry entry) {
        if (entry == null || !entry.insertable || entry.insertion == null) return "";
        if (entry.insertion.indexOf('\n') >= 0 || entry.insertion.indexOf('\r') >= 0 ||
                entry.insertion.indexOf('\0') >= 0) return "";
        return entry.insertion;
    }
}
