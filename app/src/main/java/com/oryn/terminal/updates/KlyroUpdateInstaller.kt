package com.oryn.terminal.updates

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

internal sealed interface InstallAction {
    data object Started : InstallAction
    data object PermissionRequired : InstallAction
}

/** Validates the APK locally before the Android package installer receives it. */
internal object KlyroUpdateInstaller {
    fun install(context: Context, file: File, candidate: KlyroUpdateCandidate): InstallAction {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")))
            return InstallAction.PermissionRequired
        }
        validate(context, file, candidate)
        val uri = FileProvider.getUriForFile(context,
            "${context.packageName}.updates", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        return InstallAction.Started
    }

    @Suppress("DEPRECATION")
    fun validate(context: Context, file: File, candidate: KlyroUpdateCandidate) {
        if (!file.isFile || file.length() !in 1..KlyroUpdatePolicy.MAX_APK_BYTES) {
            throw UpdateException("Downloaded APK is missing or outside the size limit")
        }
        candidate.expectedDigest?.takeIf { !candidate.isArchive }?.let { expected ->
            val digest = KlyroUpdatePolicy.normalizeDigest(expected)
                ?: throw UpdateException("Invalid APK digest")
            if (sha256(file) != digest) throw UpdateException("APK SHA-256 does not match release metadata")
        }
        val flags = if (Build.VERSION.SDK_INT >= 28)
            PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val candidateInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw UpdateException("Downloaded file is not a valid Android package")
        if (candidateInfo.packageName != KlyroUpdatePolicy.PACKAGE_NAME) {
            throw UpdateException("Downloaded APK belongs to a different app")
        }
        val candidateCode = if (Build.VERSION.SDK_INT >= 28)
            candidateInfo.longVersionCode else candidateInfo.versionCode.toLong()
        if (candidateCode != candidate.version.code) throw UpdateException("APK version does not match the update metadata")
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        val installedCode = if (Build.VERSION.SDK_INT >= 28)
            installed.longVersionCode else installed.versionCode.toLong()
        if (candidateCode <= installedCode) throw UpdateException("Update is not newer than the installed app")
        val installedSigners = if (Build.VERSION.SDK_INT >= 28)
            installed.signingInfo?.apkContentsSigners.orEmpty() else installed.signatures.orEmpty()
        val candidateSigners = if (Build.VERSION.SDK_INT >= 28)
            candidateInfo.signingInfo?.apkContentsSigners.orEmpty() else candidateInfo.signatures.orEmpty()
        if (installedSigners.isEmpty() || candidateSigners.isEmpty() ||
            installedSigners.map { sha256(it.toByteArray()) }.toSet() !=
            candidateSigners.map { sha256(it.toByteArray()) }.toSet()) {
            throw UpdateException("APK signing certificate does not match this Klyro installation")
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
