package com.oryn.terminal

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test

class KlyroFileSafetyTest {
    @Test fun namesAreNormalizedWithoutTraversal() {
        assertEquals("danger.txt", KlyroFileSafety.safeName("../danger.txt"))
        assertEquals("danger.txt", KlyroFileSafety.safeName("C:\\temp\\danger.txt"))
        assertEquals("imported-file", KlyroFileSafety.safeName("..."))
        assertEquals("a_b.txt", KlyroFileSafety.safeName("a b.txt"))
        assertEquals(80, KlyroFileSafety.safeName("x".repeat(180)).length)
    }

    @Test fun boundedCollisionsAndExistingLinksNotReused() {
        val dir = Files.createTempDirectory("klyro-test-").toFile()
        try {
            File(dir, "safe.txt").writeText("do not overwrite")
            assertEquals("1_safe.txt",
                KlyroFileSafety.availableName(dir, "safe.txt").name)
        } finally {
            dir.listFiles()?.forEach { it.delete() }
            dir.delete()
        }
    }

    @Test fun destinationRejectsSymlinks() {
        val dir = Files.createTempDirectory("klyro-dir-")
        val link = dir.resolve("alias")
        try {
            Files.createSymbolicLink(link, dir)
            try {
                KlyroFileSafety.verifyDestination(link.toFile())
                fail("must reject symlink")
            } catch (expected: IOException) {
                assertTrue(expected.message!!.contains("symbolic link"))
            }
        } finally {
            Files.deleteIfExists(link)
            Files.deleteIfExists(dir)
        }
    }

    @Test fun streamingCopyPreservesDataAndCapsSize() {
        val data = "é
".toByteArray(Charsets.UTF_8)
        val output = ByteArrayOutputStream()
        assertEquals(data.size.toLong(),
            KlyroFileSafety.copyLimited(ByteArrayInputStream(data), output))
        assertArrayEquals(data, output.toByteArray())
        try {
            KlyroFileSafety.copyLimited(
                ByteArrayInputStream(ByteArray(KlyroFileSafety.MAX_FILE_BYTES.toInt() + 1)),
                ByteArrayOutputStream())
            fail("must enforce file size")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("32 MiB"))
        }
    }
}
