package com.oryn.terminal.updates

/** Pure update rules shared by metadata parsing and the Settings UI. */
enum class UpdateChannel(val storageValue: String, val label: String) {
    STABLE("stable", "Stable"),
    PREVIEW("preview", "Preview");

    companion object {
        fun fromStorage(value: String?): UpdateChannel =
            entries.firstOrNull { it.storageValue == value } ?: STABLE
    }
}

internal data class KlyroVersion(val code: Long, val name: String)

internal data class PreviewArtifact(
    val name: String,
    val expired: Boolean,
    val sizeBytes: Long,
    val branch: String,
    val workflowPath: String,
    val runConclusion: String,
    val digest: String?
)

internal object KlyroUpdatePolicy {
    const val PACKAGE_NAME = "com.klyro.shell"
    const val SIGNED_PREVIEW_ARTIFACT = "klyro-shell-debug"
    const val WORKFLOW_PATH = ".github/workflows/android-build.yml"
    const val MAX_APK_BYTES = 250L * 1024L * 1024L
    const val MAX_PREVIEW_ZIP_BYTES = 300L * 1024L * 1024L

    fun branchOrMain(raw: String?): String {
        val branch = raw?.trim().orEmpty()
        return branch.takeIf {
            it.isNotEmpty() && it.length <= 180 &&
                it.matches(Regex("[A-Za-z0-9._/-]+")) &&
                !it.startsWith("-") && !it.contains("..") && !it.endsWith("/")
        } ?: "main"
    }

    fun downloadPercent(received: Long, declared: Long, assetSize: Long): Int {
        val total = declared.takeIf { it > 0 } ?: assetSize
        if (total <= 0) return 0
        return (received.coerceIn(0L, total).toDouble() / total * 100).toInt()
    }

    fun isNewer(installed: KlyroVersion, candidate: KlyroVersion): Boolean =
        candidate.code > installed.code

    fun isEligiblePreview(artifact: PreviewArtifact, branch: String): Boolean =
        artifact.name == SIGNED_PREVIEW_ARTIFACT && !artifact.expired &&
            artifact.sizeBytes in 1..MAX_PREVIEW_ZIP_BYTES &&
            artifact.branch == branch && artifact.workflowPath == WORKFLOW_PATH &&
            artifact.runConclusion == "success" &&
            artifact.digest?.matches(Regex("sha256:[a-fA-F0-9]{64}")) == true

    fun normalizeDigest(value: String?): String? = value?.trim()?.lowercase()
        ?.removePrefix("sha256:")
        ?.takeIf { it.matches(Regex("[a-f0-9]{64}")) }
}
