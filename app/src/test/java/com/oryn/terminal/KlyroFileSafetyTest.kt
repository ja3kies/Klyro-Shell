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
        assertEquals("a b.txt", KlyroFileSafety.safeName("a b.txt"))
        assertEquals(".gitignore", KlyroFileSafety.safeName(".gitignore"))
        assertEquals("café 😀.txt", KlyroFileSafety.safeName("café 😀.txt"))
        assertEquals("bad_name.txt", KlyroFileSafety.safeName("bad\u202ename.txt"))
        assertFalse(KlyroFileSafety.safeName("x".repeat(79) + "😀").last().isHighSurrogate())
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

    @Test fun exportRejectsSymlinkAndOutsidePrivateHome() {
        val home = Files.createTempDirectory("klyro-home-")
        val outside = Files.createTempFile("klyro-other-", ".txt")
        val dir = home.resolve("imports")
        Files.createDirectories(dir)
        val link = dir.resolve("klyro-export.txt")
        try {
            Files.write(outside, "secret".toByteArray())
            Files.createSymbolicLink(link, outside)
            try {
                KlyroFileSafety.verifyExportSource(home.toFile(), link.toFile())
                fail("must not export through symlink")
            } catch (expected: IOException) {
                assertTrue(expected.message!!.contains("private"))
            }
            try {
                KlyroFileSafety.verifyExportSource(home.toFile(), outside.toFile())
                fail("must not export outside home")
            } catch (expected: IOException) {
                assertTrue(expected.message!!.contains("private"))
            }
        } finally {
            Files.deleteIfExists(link)
            Files.deleteIfExists(dir)
            Files.deleteIfExists(home)
            Files.deleteIfExists(outside)
        }
    }

    @Test fun exportSelectsArbitraryPrivateFilesAndRejectsTraversal() {
        val home = Files.createTempDirectory("klyro-export-").toFile()
        val project = File(home, "project é").apply { mkdir() }
        val source = File(project, "archive 😀.zip").apply { writeBytes(byteArrayOf(0, 1, 2, 3)) }
        try {
            assertEquals(source, KlyroFileSafety.exportFile(home, "project é/archive 😀.zip"))
            for (path in listOf("../outside", "/etc/passwd", "", "invalid\u0000name")) {
                try {
                    KlyroFileSafety.exportFile(home, path)
                    fail("Unsafe export path accepted: $path")
                } catch (expected: IOException) { }
            }
        } finally { source.delete(); project.delete(); home.delete() }
    }

    @Test fun streamingCopyPreservesDataAndCapsSize() {
        val data = "é\n".toByteArray(Charsets.UTF_8)
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
