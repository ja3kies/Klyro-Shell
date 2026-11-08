package com.oryn.terminal.plugins

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroPluginPolicyTest {
    @Test fun acceptsOnlyReadOnlyLiteralCommandTemplates() {
        listOf("pwd", "ls -la", "git status", "git diff --stat",
            "git log -5 --oneline", "uname -a", "whoami", "date",
            "pkg search python", "pkg list-installed").forEach {
            assertTrue("Expected read-only insertion: $it", KlyroPluginManager.safeInsertion(it))
        }
    }

    @Test fun rejectsShellMetacharactersAndAutoExecution() {
        listOf("", " ", "ls\nrm -rf /", "pwd\r", "ls;rm -rf /",
            "git status && rm -rf /", "ls | sh", "git status $(touch /tmp/x)",
            "ls `whoami`", "cat /etc/passwd", "rm -rf ~", "curl example.com",
            "git commit -m x", "ls > output", "ls &", "ls \\x", "  ls").forEach {
            assertFalse("Unsafe plugin suggestion accepted: $it",
                KlyroPluginManager.safeInsertion(it))
        }
    }

    @Test fun rejectsReadOnlyLookingOptionsThatMayExecuteExternalHelpers() {
        listOf("git diff --ext-diff", "git diff --output=foo",
            "git status --porcelain=v2", "git log --format=x",
            "ls -R /", "ls --help", "pkg search --script",
            "pkg search -o", "pkg search " + "p".repeat(41),
            "git diff -R", "git -c alias.a=!sh status").forEach {
            assertFalse("Unreviewed command accepted: $it",
                KlyroPluginManager.safeInsertion(it))
        }
    }

    @Test fun rejectsOverlongAndHiddenControlSuggestions() {
        assertFalse(KlyroPluginManager.safeInsertion("ls " + "a".repeat(160)))
        assertFalse(KlyroPluginManager.safeInsertion("ls\u0000"))
        assertFalse(KlyroPluginManager.safeInsertion("ls\u202Etest"))
    }
}
