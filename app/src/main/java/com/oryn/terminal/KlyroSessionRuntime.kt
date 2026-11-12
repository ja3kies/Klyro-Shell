package com.oryn.terminal

/** Process-owned sessions; no inactivity timeout can safely infer that a shell is expendable. */
object KlyroSessionRuntime {
    val sessions = TerminalSessionViewModel()
}
