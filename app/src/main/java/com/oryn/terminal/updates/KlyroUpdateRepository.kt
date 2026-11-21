package com.oryn.terminal.updates

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

internal data class KlyroUpdateCandidate(
    val channel: UpdateChannel,
    val version: KlyroVersion,
    val notes: String,
    val downloadUrl: String,
    val expectedDigest: String?,
    val isArchive: Boolean,
    val artifactSize: Long = 0,
    val sourceBranch: String? = null
)

internal sealed interface UpdateLookup {
    data class Available(val candidate: KlyroUpdateCandidate) : UpdateLookup
    data object Current : UpdateLookup
    data object NonePublished : UpdateLookup
}

/** Public GitHub API client. All methods are blocking and must run on Dispatchers.IO. */
internal class KlyroUpdateRepository(private val context: Context) {
    private val api = "https://api.github.com/repos/ja3kies/Klyro-Shell"

    fun check(channel: UpdateChannel, branch: String, installed: KlyroVersion): UpdateLookup =
        when (channel) {
            UpdateChannel.STABLE -> checkStable(installed)
            UpdateChannel.PREVIEW -> checkPreview(branch, installed)
        }

    private fun checkStable(installed: KlyroVersion): UpdateLookup =
        checkRelease("$api/releases/latest", UpdateChannel.STABLE, installed)

    private fun checkPreview(branch: String, installed: KlyroVersion): UpdateLookup {
        val safeBranch = KlyroUpdatePolicy.branchOrMain(branch)
        val branchHash = MessageDigest.getInstance("SHA-256")
            .digest(safeBranch.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        // The global Preview tracks successful signed builds across branches.
        // Keep support for older branch-only publications during migration.
        val latest = checkRelease("$api/releases/tags/preview-latest", UpdateChannel.PREVIEW, installed)
        if (latest is UpdateLookup.Available) return latest
        val local = checkRelease("$api/releases/tags/preview-$branchHash", UpdateChannel.PREVIEW,
            installed, safeBranch)
        return if (local == UpdateLookup.NonePublished) latest else local
    }

    private fun checkRelease(
        url: String,
        channel: UpdateChannel,
        installed: KlyroVersion,
        branch: String? = null
    ): UpdateLookup {
        val release = try {
            getJson(url)
        } catch (failure: UpdateException) {
            if (failure.message == "GitHub returned HTTP 404") return UpdateLookup.NonePublished
            throw failure
        }
        if (release.optBoolean("draft", true) ||
            release.optBoolean("prerelease") != (channel == UpdateChannel.PREVIEW)) {
            return UpdateLookup.NonePublished
        }
        val assets = release.optJSONArray("assets")
            ?: throw UpdateException("Release is missing its asset list")
        val apk = assets.findByName("klyro-shell.apk")
            ?: throw UpdateException("Release is missing the signed APK")
        val manifestAsset = assets.findByName("klyro-update.json")
            ?: throw UpdateException("Release is missing update metadata")
        val manifest = getJson(manifestAsset.optString("browser_download_url"))
        if (branch != null && manifest.optString("branch") != branch) {
            throw UpdateException("Preview metadata does not match the installed branch")
        }
        if (channel == UpdateChannel.PREVIEW) {
            val publishedBranch = manifest.optString("branch")
            if (publishedBranch.isBlank() || KlyroUpdatePolicy.branchOrMain(publishedBranch) != publishedBranch)
                throw UpdateException("Preview metadata contains an invalid source branch")
        }
        val candidate = manifest.toCandidate(
            channel = channel,
            notes = release.optString("body").take(8_000),
            downloadUrl = apk.optString("browser_download_url"),
            digest = apk.optString("digest").takeIf(String::isNotBlank),
            archive = false,
            size = apk.optLong("size", 0)
        ) ?: throw UpdateException("Release update metadata failed validation")
        return if (KlyroUpdatePolicy.isNewer(installed, candidate.version)) {
            UpdateLookup.Available(candidate.copy(
                sourceBranch = manifest.optString("branch").takeIf(String::isNotBlank)
            ))
        } else UpdateLookup.Current
    }

    fun download(candidate: KlyroUpdateCandidate, onProgress: (Long, Long) -> Unit): File {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { it.delete() }
        val source = File(updatesDir, if (candidate.isArchive) "preview.zip" else "download.apk")
        try {
            transfer(candidate.downloadUrl, source,
                candidate.artifactSize.takeIf { it > 0 } ?: KlyroUpdatePolicy.MAX_PREVIEW_ZIP_BYTES,
                candidate.channel == UpdateChannel.PREVIEW, onProgress)
        } catch (failure: Throwable) {
            source.delete()
            throw failure
        }
        candidate.expectedDigest?.let { expected ->
            val normalized = KlyroUpdatePolicy.normalizeDigest(expected)
                ?: throw UpdateException("GitHub returned an invalid SHA-256 digest")
            if (sha256(source) != normalized) {
                source.delete()
                throw UpdateException("Downloaded file failed SHA-256 verification")
            }
        }
        if (!candidate.isArchive) return source
        val apk = File(updatesDir, "klyro-shell.apk")
        val manifestFile = File(updatesDir, "klyro-update.json")
        var found = 0
        var manifestFound = 0
        try {
            ZipInputStream(BufferedInputStream(source.inputStream())).use { zip ->
                var entries = 0
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (++entries > 2 || entry.isDirectory) throw UpdateException("Preview archive contains unexpected entries")
                    val target = when (entry.name) {
                        "klyro-shell.apk" -> apk.also { found++ }
                        "klyro-update.json" -> manifestFile.also { manifestFound++ }
                        else -> throw UpdateException("Preview archive contains an unexpected file")
                    }
                    if (found > 1 || manifestFound > 1) throw UpdateException("Preview archive contains duplicate update files")
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var total = 0L
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            total += read
                            val limit = if (target == apk) KlyroUpdatePolicy.MAX_APK_BYTES else 64L * 1024L
                            if (total > limit) throw UpdateException("Update file exceeds the size limit")
                            output.write(buffer, 0, read)
                        }
                    }
                }
            }
            source.delete()
            if (found != 1 || manifestFound != 1 || !apk.isFile || apk.length() == 0L) throw UpdateException("Signed APK or metadata was not found in preview archive")
            val manifest = JSONObject(manifestFile.readText())
            if (manifest.optString("packageName") != KlyroUpdatePolicy.PACKAGE_NAME ||
                manifest.optLong("versionCode") != candidate.version.code ||
                manifest.optString("branch") != candidate.sourceBranch ||
                KlyroUpdatePolicy.normalizeDigest(manifest.optString("apkSha256")) != sha256(apk)) {
                throw UpdateException("Preview APK metadata or digest does not match")
            }
            manifestFile.delete()
        } catch (failure: Throwable) {
            source.delete(); apk.delete(); manifestFile.delete()
            throw failure
        }
        return apk
    }

    private fun JSONObject.toCandidate(
        channel: UpdateChannel, notes: String, downloadUrl: String,
        digest: String?, archive: Boolean, size: Long
    ): KlyroUpdateCandidate? {
        if (optString("packageName") != KlyroUpdatePolicy.PACKAGE_NAME) return null
        val code = optLong("versionCode", 0)
        val name = optString("versionName").take(80)
        val apkDigest = KlyroUpdatePolicy.normalizeDigest(optString("apkSha256"))
        val publishedDigest = KlyroUpdatePolicy.normalizeDigest(digest)
        if (code <= 0 || name.isBlank() || downloadUrl.isBlank() || apkDigest == null ||
            (!archive && size !in 1..KlyroUpdatePolicy.MAX_APK_BYTES) ||
            (!archive && publishedDigest != null && publishedDigest != apkDigest)) return null
        return KlyroUpdateCandidate(channel, KlyroVersion(code, name), notes,
            downloadUrl, if (archive) digest else "sha256:$apkDigest", archive, size)
    }

    private fun JSONArray.findByName(name: String): JSONObject? =
        (0 until length()).mapNotNull { optJSONObject(it) }.firstOrNull { it.optString("name") == name }

    private fun getJson(url: String): JSONObject {
        if (!url.startsWith("https://api.github.com/") && !url.startsWith("https://github.com/")) {
            throw UpdateException("Unexpected update metadata URL")
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 12_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "Klyro-Shell-Updater")
            connection.connect()
            if (connection.responseCode !in 200..299) throw UpdateException("GitHub returned HTTP ${connection.responseCode}")
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > 512 * 1024) throw UpdateException("Update metadata exceeds size limit")
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return JSONObject(bytes.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }

    private fun transfer(
        url: String, destination: File, maxBytes: Long, isPreview: Boolean,
        onProgress: (Long, Long) -> Unit
    ) {
        if (!url.startsWith("https://api.github.com/") && !url.startsWith("https://github.com/")) {
            throw UpdateException("Unexpected download URL")
        }
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", "Klyro-Shell-Updater")
            connection.connect()
            if (connection.responseCode !in 200..299) {
                if (isPreview && connection.responseCode in setOf(404, 410)) {
                    throw UpdateException("This Preview build has expired. Check again to find the latest build.",
                        stalePreview = true)
                }
                if (isPreview && connection.responseCode == 401) {
                    throw UpdateException("This Preview download is unavailable without signing in. Check for another Preview.",
                        stalePreview = true)
                }
                throw UpdateException("Download failed with HTTP ${connection.responseCode}")
            }
            val declared = connection.contentLengthLong
            if (declared > maxBytes) throw UpdateException("Download exceeds the size limit")
            connection.inputStream.use { input -> FileOutputStream(destination).use { output ->
                val buffer = ByteArray(32 * 1024)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw UpdateException("Download exceeds the size limit")
                    output.write(buffer, 0, count)
                    onProgress(total, declared)
                }
            } }
        } catch (failure: Throwable) {
            destination.delete()
            throw failure
        } finally { connection.disconnect() }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

internal class UpdateException(message: String, val stalePreview: Boolean = false) : Exception(message)
