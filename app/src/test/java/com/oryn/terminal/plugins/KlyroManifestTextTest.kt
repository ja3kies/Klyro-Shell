package com.oryn.terminal.plugins

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.charset.CharacterCodingException

class KlyroManifestTextTest {
    @Test fun validUnicodeIsPreserved() {
        assertEquals("café", KlyroManifestText.decodeUtf8("café".toByteArray(Charsets.UTF_8)))
    }

    @Test(expected = CharacterCodingException::class)
    fun truncatedMultibyteCharactersAreRejected() {
        KlyroManifestText.decodeUtf8(byteArrayOf(0xC3.toByte()))
    }

    @Test(expected = CharacterCodingException::class)
    fun invalidUtf8SequencesAreRejected() {
        KlyroManifestText.decodeUtf8(byteArrayOf(0xC0.toByte(), 0xAF.toByte()))
    }
}
