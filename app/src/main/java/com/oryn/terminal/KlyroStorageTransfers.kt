package com.oryn.terminal

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.oryn.terminal.core.OrynPaths
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.CancellationException

/** Blocking document-provider work. Call only from a worker dispatcher. */
internal class KlyroStorageTransfers(private val context: Context) {
    private val resolver get() = context.contentResolver
    private val home get() = OrynPaths(context.filesDir, File(context.applicationInfo.nativeLibraryDir)).home

    fun importFile(uri: Uri, checkCancelled: () -> Unit): String {
        val destination = File(home, "imports")
        KlyroFileSafety.verifyDestination(destination)
        var sourceName: String? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameColumn >= 0) sourceName = cursor.getString(nameColumn)
                if (sizeColumn >= 0 && !cursor.isNull(sizeColumn) && cursor.getLong(sizeColumn) > KlyroFileSafety.MAX_FILE_BYTES)
                    throw IOException("File exceeds 32 MiB limit")
            }
        }
        val name = copyDocument(uri, destination, KlyroFileSafety.safeName(sourceName), checkCancelled)
        return "Imported to ~/imports/$name. The original file is unchanged."
    }

    private fun copyDocument(uri: Uri, destination: File, name: String, checkCancelled: () -> Unit): String {
        checkCancelled()
        val temporary = File.createTempFile(".klyro-import-", ".tmp", destination)
        try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(temporary).use { output -> KlyroFileSafety.copyLimited(input, output, checkCancelled) }
            } ?: throw IOException("Cannot open selected file")
            checkCancelled()
            return KlyroFileSafety.commitImport(temporary, destination, name).name
        } finally { if (temporary.exists()) temporary.delete() }
    }

    fun importFolders(folders: List<String>, checkCancelled: () -> Unit): String {
        val destination = File(home, "storage-imports")
        KlyroFileSafety.verifyDestination(destination)
        val grants = resolver.persistedUriPermissions.filter { it.isReadPermission }.map { it.uri }.toSet()
        var imported = 0
        var unavailableFolders = 0
        var failedFiles = 0
        var oversizedFiles = 0
        var readableFolders = 0
        var scanned = 0
        for (raw in folders) {
            checkCancelled()
            if (imported >= 30 || scanned >= 300) break
            val tree = Uri.parse(raw)
            if (tree !in grants) { unavailableFolders++; continue }
            try {
                val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
                val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE)
                resolver.query(children, projection, null, null, null)?.use { cursor ->
                    readableFolders++
                    while (imported < 30 && scanned < 300 && cursor.moveToNext()) {
                        checkCancelled()
                        scanned++
                        val mime = cursor.getString(2)
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) continue
                        if (!cursor.isNull(3) && cursor.getLong(3) > KlyroFileSafety.MAX_FILE_BYTES) { oversizedFiles++; continue }
                        try {
                            val uri = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                            copyDocument(uri, destination, KlyroFileSafety.safeName(cursor.getString(1), "file"), checkCancelled)
                            imported++
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { checkCancelled(); failedFiles++ }
                    }
                } ?: throw IOException("Cannot read authorized folder")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { checkCancelled(); unavailableFolders++ }
        }
        if (readableFolders == 0) throw IOException("No readable authorized folders; reconnect a folder")
        return buildString {
            append("Imported $imported files to ~/storage-imports.")
            if (oversizedFiles > 0) append(" $oversizedFiles files exceed 32 MiB.")
            if (failedFiles > 0) append(" $failedFiles files could not be copied.")
            if (unavailableFolders > 0) append(" $unavailableFolders folders unavailable; reconnect them.")
            if (imported == 30) append(" The 30-file batch limit was reached.")
            if (scanned == 300) append(" Inspection stopped after 300 entries; use Import a file for others.")
            if (imported == 0 && failedFiles == 0 && oversizedFiles == 0 && unavailableFolders == 0 && scanned < 300) append(" No importable files were found at folder root.")
            append(" Originals and existing imported files are unchanged.")
        }
    }

    fun exportFile(uri: Uri, relativePath: String, checkCancelled: () -> Unit): String {
        KlyroFileSafety.openExport(home, relativePath).use { input ->
            checkCancelled()
            val output = resolver.openOutputStream(uri, "w") ?: throw IOException("Unable to open destination")
            output.use { KlyroFileSafety.copyLimited(input, it, checkCancelled) }
        }
        return "Export completed. The private source file is unchanged."
    }

    fun validateExport(relativePath: String): String = KlyroFileSafety.exportFile(home, relativePath).name
}
