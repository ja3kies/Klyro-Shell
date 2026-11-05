package com.oryn.terminal.ui

import org.junit.Assert.*
import org.junit.Test

class KlyroHexInputTest {
    @Test fun normalizesAsciiHexInputAndRejectsNonAsciiDigits() {
        assertEquals("#00F0FF", KlyroHexInput.sanitize("#00f0ff"))
        assertEquals("#12ABEF", KlyroHexInput.sanitize("#12abefZZ"))
        assertEquals("#00AB", KlyroHexInput.sanitize("#0٠0ab"))
        assertFalse(KlyroHexInput.isComplete(KlyroHexInput.sanitize("#0٠0ab")))
        assertTrue(KlyroHexInput.isComplete("#F57C41"))
        assertFalse(KlyroHexInput.isComplete("#FFFFF"))
        assertFalse(KlyroHexInput.isComplete("#000000FF"))
        assertFalse(KlyroHexInput.isComplete("000000"))
    }
}
