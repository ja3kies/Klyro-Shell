package com.oryn.terminal

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.LinkOption

/** Shared, bounded SAF file-import rules. Never follow a shell-created
 * symbolic link into an external app or clobber an existing file. */
internal object KlyroFileSafety {
    const val MAX_FILE_BYTES = 32L * 1024 * 1024
    const val MAX_COLLISIONS = 1000
    private val unsafeChars = Regex("""[\p{C}/\\:*?"<>|]""")

    fun safeName(raw: String?, fallback: String = "imported-file"): String =
        raw.orEmpty().substringAfterLast('/').substringAfterLast('\\')
            .replace(unsafeChars, "_").trim().take(80)
            .trimEnd { Character.isHighSurrogate(it) }
            .let { if (it.isBlank() || it.all { char -> char == '.' }) fallback else it }

    fun verifyDestination(directory: File) {
        if (Files.isSymbolicLink(directory.toPath())) {
            throw IOException("Refusing a symbolic link as import destination")
        }
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Cannot create private import destination")
        }
        if (Files.isSymbolicLink(directory.toPath())) {
            throw IOException("Refusing a symbolic link as import destination")
        }
    }

    fun exportFile(home: File, relativePath: String): File {
        if (relativePath.isBlank() || File(relativePath).isAbsolute || relativePath.contains('\u0000')) {
            throw IOException("Choose a relative path inside HOME")
        }
        return File(home, relativePath).also { verifyExportSource(home, it) }
    }

    fun verifyExportSource(home: File, source: File) {
        val root = home.canonicalFile.toPath()
        val parent = source.parentFile ?: throw IOException("Export must be inside HOME")
        val normalized = source.toPath().toAbsolutePath().normalize()
        if (!normalized.startsWith(root)
            || Files.isSymbolicLink(source.toPath())
            || Files.isSymbolicLink(parent.toPath())
            || !source.canonicalFile.toPath().startsWith(root)) {
            throw IOException("Export must be a regular Klyro-private file")
        }
        if (!source.isFile) throw IOException("Choose an existing regular file inside HOME")
        if (source.length() > MAX_FILE_BYTES) throw IOException("Export limited to 32 MiB")
    }

    fun availableName(directory: File, name: String): File {
        val safe = safeName(name)
        for (index in 0 until MAX_COLLISIONS) {
            val file = File(directory, if (index == 0) safe else "${index}_${safe}")
            if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) return file
        }
        throw IOException("Too many files with this name; rename the source file")
    }

    fun openExport(home: File, relativePath: String): InputStream =
        Files.newInputStream(exportFile(home, relativePath).toPath(), LinkOption.NOFOLLOW_LINKS)

    @Synchronized
    fun commitImport(temporary: File, directory: File, name: String): File {
        verifyDestination(directory)
        if (temporary.parentFile?.canonicalFile != directory.canonicalFile ||
            !Files.isRegularFile(temporary.toPath(), LinkOption.NOFOLLOW_LINKS)) {
            throw IOException("Import temporary file must belong to its destination")
        }
        val target = availableName(directory, name)
        Files.move(temporary.toPath(), target.toPath())
        return target
    }

    fun copyLimited(input: InputStream, output: OutputStream, checkCancelled: () -> Unit = {}): Long {
        val buffer = ByteArray(8192)
        var bytes = 0L
        while (true) {
            if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Transfer interrupted")
            checkCancelled()
            val count = input.read(buffer)
            checkCancelled()
            if (count < 0) break
            if (count == 0) throw IOException("Input stream made no progress")
            bytes += count
            if (bytes > MAX_FILE_BYTES) throw IOException("File exceeds 32 MiB limit")
            output.write(buffer, 0, count)
        }
        return bytes
    }
}
