package com.oryn.terminal.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KlyroUpdatePolicyTest {
    @Test fun downloadProgressUsesReleaseSizeWhenTransferLengthIsUnknown() {
        assertEquals(50, KlyroUpdatePolicy.downloadPercent(100, -1, 200))
        assertEquals(25, KlyroUpdatePolicy.downloadPercent(100, 400, 200))
        assertEquals(100, KlyroUpdatePolicy.downloadPercent(300, -1, 200))
        assertEquals(0, KlyroUpdatePolicy.downloadPercent(0, -1, 0))
    }

    private val valid = PreviewArtifact(
        name = "klyro-shell-debug", expired = false, sizeBytes = 10_000,
        branch = "feat/in-app-updates-20261118",
        workflowPath = ".github/workflows/android-build.yml",
        runConclusion = "success", digest = "sha256:" + "a".repeat(64)
    )

    @Test fun stableIsTheDefaultForMissingOrUnknownStoredValue() {
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromStorage(null))
        assertEquals(UpdateChannel.STABLE, UpdateChannel.fromStorage("nightly"))
        assertEquals(UpdateChannel.PREVIEW, UpdateChannel.fromStorage("preview"))
    }

    @Test fun previewBuildBranchIsKeptWhenSafe() {
        assertEquals("feat/ui", KlyroUpdatePolicy.branchOrMain("feat/ui"))
        assertEquals("main", KlyroUpdatePolicy.branchOrMain(null))
    }

    @Test fun previewBranchRejectsUnsafeOrMalformedInput() {
        listOf("", "../main", "-main", "feature/..", "topic/", "a b", "a?b")
            .forEach { assertEquals("main", KlyroUpdatePolicy.branchOrMain(it)) }
    }

    @Test fun onlyStrictlyHigherVersionCodesAreUpdates() {
        val installed = KlyroVersion(100, "0.4.0")
        assertTrue(KlyroUpdatePolicy.isNewer(installed, KlyroVersion(101, "0.3.0")))
        assertFalse(KlyroUpdatePolicy.isNewer(installed, KlyroVersion(100, "0.4.1")))
        assertFalse(KlyroUpdatePolicy.isNewer(installed, KlyroVersion(99, "9.9.9")))
    }

    @Test fun acceptsOnlyUnexpiredSignedPreviewFromExpectedSuccessfulWorkflowAndBranch() {
        assertTrue(KlyroUpdatePolicy.isEligiblePreview(valid, "feat/in-app-updates-20261118"))
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(name = "klyro-shell-debug-key"), valid.branch))
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(expired = true), valid.branch))
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(branch = "main"), valid.branch))
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(runConclusion = "failure"), valid.branch))
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(workflowPath = "other.yml"), valid.branch))
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(digest = null), valid.branch))
    }

    @Test fun previewArtifactSizeIsBounded() {
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(sizeBytes = 0), valid.branch))
        assertFalse(KlyroUpdatePolicy.isEligiblePreview(valid.copy(sizeBytes = Long.MAX_VALUE), valid.branch))
    }

    @Test fun digestNormalizationAcceptsOnlySha256Hex() {
        assertEquals("a".repeat(64), KlyroUpdatePolicy.normalizeDigest("sha256:" + "A".repeat(64)))
        assertEquals(null, KlyroUpdatePolicy.normalizeDigest("sha1:" + "a".repeat(64)))
        assertEquals(null, KlyroUpdatePolicy.normalizeDigest("sha256:xyz"))
    }
}
