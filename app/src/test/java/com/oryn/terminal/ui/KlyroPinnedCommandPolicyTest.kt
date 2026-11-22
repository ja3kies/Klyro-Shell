package com.oryn.terminal.ui
import org.junit.Assert.*
import org.junit.Test

class KlyroPinnedCommandPolicyTest {
    private fun pin(id: String, scope: String?) = KlyroPinnedCommand(id, id, "pwd", scope)
    @Test fun onlyMatchingWorkspaceAndGlobalCommandsAppear() {
        val commands = listOf(pin("g", null), pin("a", "a"), pin("b", "b"))
        assertEquals(listOf("g", "a"), KlyroPinnedCommandPolicy.visible(commands, "a").map { it.id })
        assertEquals(listOf("g"), KlyroPinnedCommandPolicy.visible(commands, "c").map { it.id })
    }
    @Test fun fourVisibleSlotsCannotOverflowThroughGlobalScope() {
        val scoped = (1..4).map { pin("a$it", "a") }
        assertTrue(KlyroPinnedCommandPolicy.fits(scoped + pin("b", "b")))
        assertFalse(KlyroPinnedCommandPolicy.fits(scoped + pin("g", null)))
    }
    @Test fun compoundAndMultilineShellCommandsAreAcceptedButControlsAreNot() {
        assertTrue(KlyroPinnedCommandPolicy.valid("Build", "cd project && ./build.sh\nprintf 'done'"))
        assertFalse(KlyroPinnedCommandPolicy.valid("Bad", "pwd\u001b"))
    }
    @Test fun reorderingPreservesOtherWorkspaceCommands() {
        val commands = listOf(pin("g", null), pin("b", "b"), pin("a", "a"))
        assertEquals(listOf("a", "b", "g"), KlyroPinnedCommandPolicy.move(commands, "a", "a", -1).map { it.id })
    }
}
