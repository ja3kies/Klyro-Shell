package com.oryn.terminal

/** A process death loses PTYs; a configuration change retains ViewModel sessions. */
internal object KlyroSessionRecovery {
    fun shouldNotify(previouslyHadSessions: Boolean, liveCount: Int): Boolean =
        previouslyHadSessions && liveCount == 0
}
