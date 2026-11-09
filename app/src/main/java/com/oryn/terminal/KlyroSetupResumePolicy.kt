package com.oryn.terminal

/** Avoid restarting a failed installation automatically just because an Activity was recreated. */
internal enum class KlyroSetupResumeAction { OBSERVE, SHOW_FAILURE, VERIFY_PROOT, IDLE }

internal object KlyroSetupResumePolicy {
    fun decide(
        busy: Boolean,
        bootstrapInstalled: Boolean,
        previous: KlyroSetupSnapshot?
    ): KlyroSetupResumeAction = when {
        busy -> KlyroSetupResumeAction.OBSERVE
        previous?.success == false -> KlyroSetupResumeAction.SHOW_FAILURE
        bootstrapInstalled -> KlyroSetupResumeAction.VERIFY_PROOT
        else -> KlyroSetupResumeAction.IDLE
    }
}
