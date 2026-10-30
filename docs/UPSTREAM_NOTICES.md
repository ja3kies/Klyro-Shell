# Upstream notices

## Chimera Terminal — PTY implementation

The initial Oryn PTY adapter and native JNI bridge are derived from source code in:

- Repository: https://github.com/ChimeraAnt-DEV/Android-Terminal
- Upstream components: `PtyProcess.java` and `pty_bridge.cpp`
- Local Oryn components: `app/src/main/java/com/oryn/terminal/core/PtyProcess.java` and `app/src/main/cpp/pty_bridge.cpp`

The code was adapted for Oryn's Java package, JNI symbols, and native library name. This notice records provenance; it is not a substitute for the upstream project's terms. Oryn is currently intended for personal use. Review the exact upstream terms and all dependency notices before redistributing source or binaries.


## Termux ARM64 proot runtime

The ARM64 runtime assets are unmodified files extracted from official Termux APT packages. CI verifies the signed Termux stable repository metadata and pins these package versions:

| Package | Version | Upstream source |
|---|---:|---|
| `proot` | `5.1.107.101` | https://github.com/termux/proot |
| `libtalloc` | `2.5.0` | https://www.samba.org/ftp/talloc/ |
| `libandroid-shmem` | `0.7` | https://github.com/termux/libandroid-shmem |

The corresponding Termux package build recipes declare the applicable licences. Copyright/license files are copied into the generated APK assets when supplied by each package. The complete upstream licence terms remain authoritative.

The Termux repository signing keys are sourced from the pinned `termux/termux-packages` keyring revision used by the CI workflow.

Oryn is currently intended for personal use. Review all upstream licence obligations before redistributing the derived application, APK, or source.
