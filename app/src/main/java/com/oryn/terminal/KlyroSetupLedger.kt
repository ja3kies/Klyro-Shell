package com.oryn.terminal

/** Process-local installer journal; lifecycle callbacks never own the sole copy of a result. */
internal data class KlyroSetupSnapshot(
    val phase: String,
    val message: String,
    val progress: Int,
    val success: Boolean?,
    val details: String?
)

internal object KlyroSetupLedger {
    @Volatile private var latest: KlyroSetupSnapshot? = null

    @Synchronized fun begin(phase: String, message: String) {
        latest = KlyroSetupSnapshot(phase, message, 0, null, null)
    }

    @Synchronized fun progress(phase: String, message: String, percent: Int) {
        val previous = latest ?: return
        if (previous.phase == phase && previous.success == null) {
            latest = previous.copy(message = message, progress = percent.coerceIn(0, 100))
        }
    }

    @Synchronized fun finish(phase: String, success: Boolean, message: String, details: String?) {
        val previous = latest ?: return
        if (previous.phase == phase && previous.success == null) {
            latest = previous.copy(message = message, progress = if (success) 100 else previous.progress,
                success = success, details = details)
        }
    }

    fun snapshot(): KlyroSetupSnapshot? = latest
}
