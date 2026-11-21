package com.oryn.terminal.updates

import org.junit.Assert.assertEquals
import org.junit.Test

class KlyroPreviewSelectionTest {
    private fun available(code: Long, branch: String): UpdateLookup.Available =
        UpdateLookup.Available(KlyroUpdateCandidate(
            channel = UpdateChannel.PREVIEW,
            version = KlyroVersion(code, "preview-$code"),
            notes = "",
            downloadUrl = "https://github.com/example",
            expectedDigest = null,
            isArchive = false,
            sourceBranch = branch
        ))

    @Test fun highestVersionWinsRegardlessOfPublicationScope() {
        val older = available(100, "main")
        val newer = available(101, "feature/preview")
        assertEquals(newer, newestPreview(older, newer))
        assertEquals(newer, newestPreview(newer, older))
    }

    @Test fun missingReleaseDoesNotMaskAvailableOrCurrentVersion() {
        val update = available(101, "main")
        assertEquals(update, newestPreview(UpdateLookup.NonePublished, update))
        assertEquals(update, newestPreview(update, UpdateLookup.NonePublished))
        assertEquals(UpdateLookup.Current,
            newestPreview(UpdateLookup.Current, UpdateLookup.NonePublished))
        assertEquals(UpdateLookup.Current,
            newestPreview(UpdateLookup.NonePublished, UpdateLookup.Current))
        assertEquals(UpdateLookup.NonePublished,
            newestPreview(UpdateLookup.NonePublished, UpdateLookup.NonePublished))
    }
}
